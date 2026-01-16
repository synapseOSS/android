package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.domain.model.User
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages user profile operations and caching
 */
object UserProfileManager {

    private val dbService = SupabaseDatabaseService()
    private val authService = SupabaseAuthenticationService()
    private val profileCache = java.util.concurrent.ConcurrentHashMap<String, User>()

    /**
     * Gets a user profile by UID, with caching
     */
    suspend fun getUserProfile(uid: String): User? {
        // Check cache first
        profileCache[uid]?.let { return it }
        
        return try {
            val result = dbService.getSingle("users", "uid", uid).getOrNull()
            android.util.Log.d("UserProfileManager", "Database result for uid $uid: $result")
            if (result != null) {
                val user = User(
                    uid = result["uid"] as? String ?: "",
                    username = result["username"] as? String ?: "",
                    email = result["email"] as? String ?: "",
                    displayName = result["display_name"] as? String ?: "",
                    avatar = result["avatar"] as? String,
                    bio = result["bio"] as? String,
                    followersCount = (result["followers_count"] as? String)?.toIntOrNull() ?: 0,
                    followingCount = (result["following_count"] as? String)?.toIntOrNull() ?: 0,
                    postsCount = (result["posts_count"] as? String)?.toIntOrNull() ?: 0
                )
                // Cache the user
                profileCache[uid] = user
                user
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Updates a user profile
     */
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Boolean {
        return try {
            dbService.update("users", updates, "uid", uid)
            // Clear cache for this user
            profileCache.remove(uid)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the current user's profile
     */
    suspend fun getCurrentUserProfile(): User? {
        val currentUid = authService.getCurrentUserId() ?: return null
        return getUserProfile(currentUid)
    }

    /**
     * Updates the current user's profile
     */
    suspend fun updateCurrentUserProfile(updates: Map<String, Any?>): Boolean {
        val currentUid = authService.getCurrentUserId() ?: return false
        return updateUserProfile(currentUid, updates)
    }

    /**
     * Searches for users by username or display name
     */
    suspend fun searchUsers(query: String, limit: Int = 20): List<User> {
        return try {
            val results = dbService.selectWithFilter("users", "*", "username", "%$query%").getOrNull() ?: emptyList()
            
            results.take(limit).mapNotNull { result ->
                try {
                    User(
                        uid = result["uid"] as? String ?: "",
                        username = result["username"] as? String ?: "",
                        email = result["email"] as? String ?: "",
                        displayName = result["display_name"] as? String ?: "",
                        avatar = result["avatar"] as? String,
                        bio = result["bio"] as? String,
                        followersCount = (result["followers_count"] as? String)?.toIntOrNull() ?: 0,
                        followingCount = (result["following_count"] as? String)?.toIntOrNull() ?: 0,
                        postsCount = (result["posts_count"] as? String)?.toIntOrNull() ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets multiple user profiles by UIDs
     */
    suspend fun getUserProfiles(uids: List<String>): List<User> {
        if (uids.isEmpty()) return emptyList()

        // Check cache for all UIDs
        val cachedUsers = uids.mapNotNull { profileCache[it] }
        val missingUids = uids.filter { !profileCache.containsKey(it) }

        if (missingUids.isEmpty()) {
            return cachedUsers
        }

        return try {
            // Fetch missing users
            val results = dbService.selectWhereIn("users", "*", "uid", missingUids).getOrNull() ?: emptyList()

            val fetchedUsers = results.mapNotNull { result ->
                try {
                    val user = User(
                        uid = result["uid"] as? String ?: "",
                        username = result["username"] as? String ?: "",
                        email = result["email"] as? String ?: "",
                        displayName = result["display_name"] as? String ?: "",
                        avatar = result["avatar"] as? String,
                        bio = result["bio"] as? String,
                        followersCount = (result["followers_count"] as? String)?.toIntOrNull() ?: 0,
                        followingCount = (result["following_count"] as? String)?.toIntOrNull() ?: 0,
                        postsCount = (result["posts_count"] as? String)?.toIntOrNull() ?: 0
                    )
                    // Cache the user
                    profileCache[user.uid] = user
                    user
                } catch (e: Exception) {
                    null
                }
            }

            // Return combined list (cached + fetched) - Note: this might not return users that don't exist
            cachedUsers + fetchedUsers
        } catch (e: Exception) {
            cachedUsers
        }
    }

    /**
     * Clears the profile cache
     */
    fun clearCache() {
        profileCache.clear()
    }

    /**
     * Clears cache for a specific user
     */
    fun clearUserCache(uid: String) {
        profileCache.remove(uid)
    }

    /**
     * Checks if a user exists
     */
    suspend fun userExists(uid: String): Boolean {
        return try {
            val result = dbService.getSingle("users", "uid", uid).getOrNull()
            result != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a username is available
     */
    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val results = dbService.selectWithFilter("users", "*", "username", username).getOrNull() ?: emptyList()
            results.isEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
