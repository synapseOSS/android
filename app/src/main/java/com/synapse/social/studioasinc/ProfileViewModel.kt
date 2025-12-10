package com.synapse.social.studioasinc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.Follow
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * ViewModel for managing profile data and operations
 * Uses direct Supabase calls without wrapper services
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val client = SupabaseClient.client

    private val _userProfile = MutableLiveData<State<User>>()
    val userProfile: LiveData<State<User>> = _userProfile

    private val _userPosts = MutableLiveData<State<List<Post>>>()
    val userPosts: LiveData<State<List<Post>>> = _userPosts

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing

    private val _isProfileLiked = MutableLiveData<Boolean>()
    val isProfileLiked: LiveData<Boolean> = _isProfileLiked

    sealed class State<out T> {
        object Loading : State<Nothing>()
        data class Success<T>(val data: T) : State<T>()
        data class Error(val message: String) : State<Nothing>()
    }
    
    private suspend fun getCurrentUserUid(): String? {
        return try {
            val authId = client.auth.currentUserOrNull()?.id
            if (authId == null) {
                android.util.Log.e("ProfileViewModel", "No authenticated user found")
                return null
            }
            
            android.util.Log.d("ProfileViewModel", "Auth ID: $authId")
            
            // In this app, the auth ID IS the UID (stored in users.uid column)
            // Verify the user exists in the database
            val userCheck = client.from("users")
                .select(columns = Columns.raw("uid")) {
                    filter { eq("uid", authId) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            if (userCheck != null) {
                android.util.Log.d("ProfileViewModel", "User found in database with UID: $authId")
                return authId
            }
            
            android.util.Log.e("ProfileViewModel", "User not found in database with UID: $authId")
            null
        } catch (e: Exception) {
            android.util.Log.e("ProfileViewModel", "Failed to get user UID: ${e.message}", e)
            null
        }
    }

    /**
     * Loads user profile data - direct Supabase call
     */
    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                _userProfile.value = State.Loading
                
                val result = client.from("users")
                    .select(columns = Columns.raw("*")) {
                        filter { eq("uid", uid) }
                    }
                    .decodeSingleOrNull<JsonObject>()
                
                if (result != null) {
                    val user = User(
                        uid = result["uid"]?.toString()?.removeSurrounding("\"") ?: uid,
                        username = result["username"]?.toString()?.removeSurrounding("\""),
                        email = result["email"]?.toString()?.removeSurrounding("\""),
                        displayName = result["display_name"]?.toString()?.removeSurrounding("\"") 
                            ?: result["nickname"]?.toString()?.removeSurrounding("\""),
                        profileImageUrl = result["avatar"]?.toString()?.removeSurrounding("\"") 
                            ?: result["profile_image_url"]?.toString()?.removeSurrounding("\""),
                        profileCoverImage = result["profile_cover_image"]?.toString()?.removeSurrounding("\""),
                        bio = result["bio"]?.toString()?.removeSurrounding("\"") 
                            ?: result["biography"]?.toString()?.removeSurrounding("\""),
                        joinDate = result["join_date"]?.toString()?.removeSurrounding("\""),
                        createdAt = result["created_at"]?.toString()?.removeSurrounding("\""),
                        followersCount = result["followers_count"]?.toString()?.toIntOrNull() ?: 0,
                        followingCount = result["following_count"]?.toString()?.toIntOrNull() ?: 0,
                        postsCount = result["posts_count"]?.toString()?.toIntOrNull() ?: 0,
                        status = result["status"]?.toString()?.removeSurrounding("\"") ?: "offline"
                    )
                    _userProfile.value = State.Success(user)
                } else {
                    _userProfile.value = State.Error("User not found")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Failed to load profile", e)
                _userProfile.value = State.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Gets user posts
     */
    fun getUserPosts(uid: String) {
        viewModelScope.launch {
            try {
                _userPosts.value = State.Loading
                val postRepository = com.synapse.social.studioasinc.data.repository.PostRepository(
                    AppDatabase.getDatabase(getApplication()).postDao()
                )
                postRepository.getUserPosts(uid)
                    .onSuccess { posts ->
                        _userPosts.value = State.Success(posts)
                    }
                    .onFailure { exception ->
                        _userPosts.value = State.Error(exception.message ?: "Failed to load posts")
                    }
            } catch (e: Exception) {
                _userPosts.value = State.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Toggles follow status - direct Supabase call
     */
    fun toggleFollow(targetUid: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ProfileViewModel", "toggleFollow called for target: $targetUid")
                val currentUid = getCurrentUserUid()
                if (currentUid == null) {
                    android.util.Log.e("ProfileViewModel", "Failed to get current user UID")
                    return@launch
                }
                android.util.Log.d("ProfileViewModel", "Current user UID: $currentUid")
                
                val isCurrentlyFollowing = _isFollowing.value ?: false
                android.util.Log.d("ProfileViewModel", "Currently following: $isCurrentlyFollowing")
                
                if (isCurrentlyFollowing) {
                    // Unfollow
                    android.util.Log.d("ProfileViewModel", "Attempting to unfollow...")
                    client.from("follows").delete {
                        filter {
                            eq("follower_id", currentUid)
                            eq("following_id", targetUid)
                        }
                    }
                    _isFollowing.value = false
                    android.util.Log.d("ProfileViewModel", "Unfollowed successfully")
                } else {
                    // Follow
                    android.util.Log.d("ProfileViewModel", "Attempting to follow...")
                    val followData = buildJsonObject {
                        put("follower_id", currentUid)
                        put("following_id", targetUid)
                        // Don't set created_at - let database handle it with default value
                    }
                    client.from("follows").insert(followData)
                    _isFollowing.value = true
                    android.util.Log.d("ProfileViewModel", "Followed successfully")
                }
                
                // Refresh profile to update counts
                loadUserProfile(targetUid)
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error toggling follow", e)
            }
        }
    }

    /**
     * Toggles profile like status - direct Supabase call
     */
    fun toggleProfileLike(targetUid: String) {
        viewModelScope.launch {
            try {
                val currentUid = getCurrentUserUid() ?: return@launch
                val isCurrentlyLiked = _isProfileLiked.value ?: false
                
                if (isCurrentlyLiked) {
                    // Unlike profile
                    client.from("profile_likes").delete {
                        filter {
                            eq("liker_uid", currentUid)
                            eq("profile_uid", targetUid)
                        }
                    }
                    _isProfileLiked.value = false
                    android.util.Log.d("ProfileViewModel", "Profile unliked successfully")
                } else {
                    // Like profile - use JsonObject to avoid serialization issues
                    val likeData = kotlinx.serialization.json.buildJsonObject {
                        put("liker_uid", kotlinx.serialization.json.JsonPrimitive(currentUid))
                        put("profile_uid", kotlinx.serialization.json.JsonPrimitive(targetUid))
                    }
                    client.from("profile_likes").insert(likeData)
                    _isProfileLiked.value = true
                    android.util.Log.d("ProfileViewModel", "Profile liked successfully")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error toggling profile like", e)
            }
        }
    }



    /**
     * Fetches initial follow state - direct Supabase call
     */
    fun fetchInitialFollowState(targetUid: String) {
        viewModelScope.launch {
            try {
                val currentUid = getCurrentUserUid() ?: return@launch
                
                val result = client.from("follows")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("follower_id", currentUid)
                            eq("following_id", targetUid)
                        }
                    }
                    .decodeList<JsonObject>()
                
                _isFollowing.value = result.isNotEmpty()
                android.util.Log.d("ProfileViewModel", "Follow state: ${result.isNotEmpty()}")
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error fetching follow state", e)
                _isFollowing.value = false
            }
        }
    }

    /**
     * Fetches initial profile like state - direct Supabase call
     */
    fun fetchInitialProfileLikeState(targetUid: String) {
        viewModelScope.launch {
            try {
                val currentUid = getCurrentUserUid() ?: return@launch
                
                val result = client.from("profile_likes")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("liker_uid", currentUid)
                            eq("profile_uid", targetUid)
                        }
                    }
                    .decodeList<JsonObject>()
                
                _isProfileLiked.value = result.isNotEmpty()
                android.util.Log.d("ProfileViewModel", "Profile like state: ${result.isNotEmpty()}")
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error fetching profile like state", e)
                _isProfileLiked.value = false
            }
        }
    }

    /**
     * Toggles post like status - direct Supabase call
     */
    fun togglePostLike(postId: String) {
        viewModelScope.launch {
            try {
                val currentUid = getCurrentUserUid() ?: return@launch
                
                // Check if post is already liked
                val existingLike = client.from("post_likes")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("user_id", currentUid)
                            eq("post_id", postId)
                        }
                    }
                    .decodeSingleOrNull<JsonObject>()
                
                if (existingLike != null) {
                    // Unlike post
                    client.from("post_likes").delete {
                        filter {
                            eq("user_id", currentUid)
                            eq("post_id", postId)
                        }
                    }
                    android.util.Log.d("ProfileViewModel", "Post unliked successfully")
                } else {
                    // Like post
                    val likeData = buildJsonObject {
                        put("user_id", JsonPrimitive(currentUid))
                        put("post_id", JsonPrimitive(postId))
                    }
                    client.from("post_likes").insert(likeData)
                    android.util.Log.d("ProfileViewModel", "Post liked successfully")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error toggling post like", e)
            }
        }
    }

    /**
     * Toggles favorite status for a post - direct Supabase call
     */
    fun toggleFavorite(postId: String) {
        viewModelScope.launch {
            try {
                val currentUid = getCurrentUserUid() ?: return@launch
                
                // Check if post is already favorited
                val existingFavorite = client.from("favorites")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("user_id", currentUid)
                            eq("post_id", postId)
                        }
                    }
                    .decodeSingleOrNull<JsonObject>()
                
                if (existingFavorite != null) {
                    // Remove from favorites
                    client.from("favorites").delete {
                        filter {
                            eq("user_id", currentUid)
                            eq("post_id", postId)
                        }
                    }
                    android.util.Log.d("ProfileViewModel", "Post removed from favorites")
                } else {
                    // Add to favorites
                    val favoriteData = buildJsonObject {
                        put("user_id", JsonPrimitive(currentUid))
                        put("post_id", JsonPrimitive(postId))
                    }
                    client.from("favorites").insert(favoriteData)
                    android.util.Log.d("ProfileViewModel", "Post added to favorites")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error toggling favorite", e)
            }
        }
    }
}
