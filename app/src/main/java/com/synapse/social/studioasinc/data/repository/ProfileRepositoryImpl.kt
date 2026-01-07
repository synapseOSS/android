package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.ui.profile.utils.NetworkOptimizer
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import kotlinx.serialization.json.*

@Serializable
data class FollowInsert(
    val follower_id: String,
    val following_id: String
)

/**
 * Implementation of ProfileRepository with Supabase backend integration.
 * 
 * Features:
 * - Request caching with 1-minute TTL
 * - Automatic retry with exponential backoff
 * - RLS policy compliance
 * 
 * Uses SupabaseClient singleton for all database operations.
 */
class ProfileRepositoryImpl(private val client: SupabaseClientType) : ProfileRepository {

    private companion object {
        // JSON field keys
        const val KEY_UID = "uid"
        const val KEY_USERNAME = "username"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_BIO = "bio"
        const val KEY_AVATAR = "avatar"
        const val KEY_COVER_IMAGE = "profile_cover_image"
        const val KEY_VERIFY = "verify"
        const val KEY_IS_PRIVATE = "is_private"
        const val KEY_POSTS_COUNT = "posts_count"
        const val KEY_FOLLOWERS_COUNT = "followers_count"
        const val KEY_FOLLOWING_COUNT = "following_count"
        const val KEY_LOCATION = "location"
        const val KEY_WEBSITE = "website"
        const val KEY_GENDER = "gender"
        const val KEY_PRONOUNS = "pronouns"
        const val KEY_ID = "id"
        const val KEY_AUTHOR_UID = "author_uid"
        const val KEY_POST_TEXT = "post_text"
        const val KEY_POST_IMAGE = "post_image"
        const val KEY_POST_TYPE = "post_type"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_LIKES_COUNT = "likes_count"
        const val KEY_COMMENTS_COUNT = "comments_count"
        const val KEY_VIEWS_COUNT = "views_count"
        const val KEY_HAS_POLL = "has_poll"
        const val KEY_POLL_QUESTION = "poll_question"
        const val KEY_POLL_OPTIONS = "poll_options"
        const val KEY_MEDIA_ITEMS = "media_items"
        const val KEY_USERS = "users"
        const val KEY_URL = "url"
        const val KEY_TYPE = "type"
        const val KEY_THUMBNAIL_URL = "thumbnailUrl" // Backend uses camelCase for this field
        const val KEY_TEXT = "text"
        const val KEY_VOTES = "votes"
        
        // Media types
        const val MEDIA_TYPE_VIDEO = "VIDEO"
        const val MEDIA_TYPE_IMAGE = "IMAGE"
    }

    /**
     * Converts "me" to the actual current user ID, returns the original userId otherwise
     */
    private fun resolveUserId(userId: String): String? {
        return if (userId == "me") {
            client.auth.currentUserOrNull()?.id
        } else {
            userId
        }
    }

    private fun constructMediaUrl(storagePath: String): String = SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_POST_MEDIA, storagePath)
    
    private fun constructAvatarUrl(storagePath: String): String = SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_USER_AVATARS, storagePath)

    private fun JsonObject.getString(key: String, default: String = ""): String = 
        this[key]?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.getNullableString(key: String): String? = 
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.getBoolean(key: String, default: Boolean = false): Boolean = 
        this[key]?.jsonPrimitive?.booleanOrNull ?: default

    private fun JsonObject.getInt(key: String, default: Int = 0): Int = 
        this[key]?.jsonPrimitive?.intOrNull ?: default

    private fun JsonObject.getLong(key: String, default: Long = 0L): Long = 
        this[key]?.jsonPrimitive?.longOrNull ?: default

    override fun getProfile(userId: String): Flow<Result<UserProfile>> = flow {
        // Handle "me" case by converting to actual user ID
        val actualUserId = resolveUserId(userId) ?: run {
            emit(Result.failure(Exception("User not authenticated")))
            return@flow
        }
        
        val cacheKey = "profile_$actualUserId"
        NetworkOptimizer.getCached<UserProfile>(cacheKey)?.let {
            emit(Result.success(it))
            return@flow
        }
        
        try {
            android.util.Log.d("ProfileRepository", "Loading profile for userId: $actualUserId")
            val response = NetworkOptimizer.withRetry {
                client.from("users").select() { 
                    filter { eq(KEY_UID, actualUserId) } 
                }.decodeSingleOrNull<JsonObject>()
            }
            
            android.util.Log.d("ProfileRepository", "Profile query response: $response")
            
            if (response == null) {
                android.util.Log.e("ProfileRepository", "Profile not found for userId: $actualUserId")
                
                // Try to create missing profile if this is the current user
                try {
                    val currentUser = client.auth.currentUserOrNull()
                    if (currentUser != null && currentUser.id == actualUserId) {
                        android.util.Log.d("ProfileRepository", "Attempting to create missing profile for current user")
                        
                        val userMap = mapOf(
                            "uid" to actualUserId,
                            "username" to (currentUser.email?.substringBefore("@") ?: "user"),
                            "email" to (currentUser.email ?: ""),
                            "created_at" to java.time.Instant.now().toString(),
                            "join_date" to java.time.Instant.now().toString(),
                            "account_premium" to false,
                            "verify" to false,
                            "banned" to false,
                            "followers_count" to 0,
                            "following_count" to 0,
                            "posts_count" to 0,
                            "user_level_xp" to 0
                        )
                        
                        client.from("users").insert(userMap)
                        android.util.Log.d("ProfileRepository", "Created missing profile, retrying query")
                        
                        // Retry the query
                        val retryResponse = client.from("users").select() { 
                            filter { eq(KEY_UID, actualUserId) } 
                        }.decodeSingleOrNull<JsonObject>()
                        
                        if (retryResponse != null) {
                            // Continue with profile creation using retryResponse
                            val postCount = try {
                                client.from("posts").select(columns = Columns.raw("count")) {
                                    filter { eq(KEY_AUTHOR_UID, actualUserId) }
                                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                                }.countOrNull() ?: 0
                            } catch (e: Exception) {
                                0
                            }
                            
                            val profile = UserProfile(
                                id = retryResponse.getString(KEY_UID, actualUserId),
                                username = retryResponse.getString(KEY_USERNAME),
                                name = retryResponse.getNullableString(KEY_DISPLAY_NAME),
                                bio = retryResponse.getNullableString(KEY_BIO),
                                avatar = retryResponse.getNullableString(KEY_AVATAR)?.let { constructAvatarUrl(it) },
                                coverImageUrl = retryResponse.getNullableString(KEY_COVER_IMAGE)?.let { constructMediaUrl(it) },
                                isVerified = retryResponse.getBoolean(KEY_VERIFY),
                                isPrivate = retryResponse.getBoolean(KEY_IS_PRIVATE),
                                postCount = postCount.toInt(),
                                followerCount = retryResponse.getInt(KEY_FOLLOWERS_COUNT),
                                followingCount = retryResponse.getInt(KEY_FOLLOWING_COUNT),
                                location = retryResponse.getNullableString(KEY_LOCATION),
                                website = retryResponse.getNullableString(KEY_WEBSITE),
                                gender = retryResponse.getNullableString(KEY_GENDER),
                                pronouns = retryResponse.getNullableString(KEY_PRONOUNS)
                            )
                            NetworkOptimizer.cache(cacheKey, profile)
                            android.util.Log.d("ProfileRepository", "Profile created and loaded successfully")
                            emit(Result.success(profile))
                            return@flow
                        }
                    }
                } catch (createError: Exception) {
                    android.util.Log.e("ProfileRepository", "Failed to create missing profile", createError)
                }
                
                emit(Result.failure(Exception("Profile not found for user: $actualUserId")))
                return@flow
            }
            
            // Query actual post count from posts table
            val postCount = try {
                client.from("posts").select(columns = Columns.raw("count")) {
                    filter { eq(KEY_AUTHOR_UID, actualUserId) }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }.countOrNull() ?: 0
            } catch (e: Exception) {
                0
            }
            
            val profile = UserProfile(
                id = response.getString(KEY_UID, actualUserId),
                username = response.getString(KEY_USERNAME),
                name = response.getNullableString(KEY_DISPLAY_NAME),
                bio = response.getNullableString(KEY_BIO),
                avatar = response.getNullableString(KEY_AVATAR)?.let { constructAvatarUrl(it) },
                coverImageUrl = response.getNullableString(KEY_COVER_IMAGE)?.let { constructMediaUrl(it) },
                isVerified = response.getBoolean(KEY_VERIFY),
                isPrivate = response.getBoolean(KEY_IS_PRIVATE),
                postCount = postCount.toInt(),
                followerCount = response.getInt(KEY_FOLLOWERS_COUNT),
                followingCount = response.getInt(KEY_FOLLOWING_COUNT),
                location = response.getNullableString(KEY_LOCATION),
                website = response.getNullableString(KEY_WEBSITE),
                gender = response.getNullableString(KEY_GENDER),
                pronouns = response.getNullableString(KEY_PRONOUNS)
            )
            NetworkOptimizer.cache(cacheKey, profile)
            android.util.Log.d("ProfileRepository", "Profile loaded successfully for userId: $actualUserId")
            emit(Result.success(profile))
        } catch (e: Exception) {
            android.util.Log.e("ProfileRepository", "Failed to load profile for userId: $actualUserId", e)
            emit(Result.failure(Exception("Failed to load profile: ${e.message}", e)))
        }
    }

    override suspend fun updateProfile(userId: String, profile: UserProfile): Result<UserProfile> = try {
        val updated = client.from("users").update(profile) { filter { eq("uid", userId) } }.decodeSingle<UserProfile>()
        Result.success(updated)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun followUser(userId: String, targetUserId: String): Result<Unit> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val actualTargetUserId = resolveUserId(targetUserId) ?: return Result.failure(Exception("Target user not found"))
        client.from("follows").upsert(
            FollowInsert(follower_id = actualUserId, following_id = actualTargetUserId)
        ) {
            onConflict = "follower_id, following_id"
            ignoreDuplicates = true
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unfollowUser(userId: String, targetUserId: String): Result<Unit> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val actualTargetUserId = resolveUserId(targetUserId) ?: return Result.failure(Exception("Target user not found"))
        client.from("follows").delete { filter { eq("follower_id", actualUserId); eq("following_id", actualTargetUserId) } }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFollowers(userId: String, limit: Int, offset: Int): Result<List<UserProfile>> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val followers = client.from("follows").select() { 
            filter { eq("follower_id", actualUserId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<Map<String, UserProfile>>().mapNotNull { it["following_id"] }
        Result.success(followers)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFollowing(userId: String, limit: Int, offset: Int): Result<List<UserProfile>> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val following = client.from("follows").select() { 
            filter { eq("following_id", actualUserId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<Map<String, UserProfile>>().mapNotNull { it["follower_id"] }
        Result.success(following)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfilePosts(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.domain.model.Post>> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val response = client.from("posts").select(
            columns = Columns.raw("*, users!posts_author_uid_fkey($KEY_UID, $KEY_USERNAME, $KEY_AVATAR, $KEY_VERIFY)")
        ) { 
            filter { eq(KEY_AUTHOR_UID, actualUserId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<JsonObject>()
        
        val posts = response.mapNotNull { data -> parsePost(data) }
        Result.success(posts)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun getMediaItemsByType(userId: String, limit: Int, offset: Int, isVideo: Boolean): Result<List<com.synapse.social.studioasinc.ui.profile.components.MediaItem>> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val response = client.from("posts").select(
            columns = Columns.raw("$KEY_ID, $KEY_MEDIA_ITEMS")
        ) { 
            filter { eq(KEY_AUTHOR_UID, actualUserId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<JsonObject>()
        
        val mediaItems = response.flatMap { data ->
            val postId = data[KEY_ID]?.jsonPrimitive?.contentOrNull ?: return@flatMap emptyList()
            data[KEY_MEDIA_ITEMS]?.takeIf { it !is JsonNull }?.jsonArray?.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap[KEY_URL]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap[KEY_TYPE]?.jsonPrimitive?.contentOrNull ?: MEDIA_TYPE_IMAGE
                val isVideoType = typeStr.equals(MEDIA_TYPE_VIDEO, ignoreCase = true)
                if (isVideoType != isVideo) return@mapNotNull null
                com.synapse.social.studioasinc.ui.profile.components.MediaItem(
                    id = mediaMap[KEY_ID]?.jsonPrimitive?.contentOrNull ?: postId,
                    url = constructMediaUrl(url),
                    isVideo = isVideo
                )
            } ?: emptyList()
        }
        Result.success(mediaItems)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfilePhotos(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.ui.profile.components.MediaItem>> = 
        getMediaItemsByType(userId, limit, offset, isVideo = false)

    override suspend fun getProfileReels(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.ui.profile.components.MediaItem>> = 
        getMediaItemsByType(userId, limit, offset, isVideo = true)

    override suspend fun isFollowing(userId: String, targetUserId: String): Result<Boolean> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val actualTargetUserId = resolveUserId(targetUserId) ?: return Result.failure(Exception("Target user not found"))
        val result = client.from("follows").select() { 
            filter { eq("follower_id", actualUserId); eq("following_id", actualTargetUserId) } 
        }.decodeList<JsonObject>()
        Result.success(result.isNotEmpty())
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun parsePost(data: JsonObject): Post? {
        val post = Post(
            id = data.getNullableString(KEY_ID) ?: return null,
            authorUid = data.getString(KEY_AUTHOR_UID),
            postText = data.getNullableString(KEY_POST_TEXT),
            postImage = data.getNullableString(KEY_POST_IMAGE)?.let { constructMediaUrl(it) },
            postType = data.getNullableString(KEY_POST_TYPE),
            timestamp = data.getLong(KEY_TIMESTAMP),
            likesCount = data.getInt(KEY_LIKES_COUNT),
            commentsCount = data.getInt(KEY_COMMENTS_COUNT),
            viewsCount = data.getInt(KEY_VIEWS_COUNT),
            hasPoll = data.getBoolean(KEY_HAS_POLL),
            pollQuestion = data.getNullableString(KEY_POLL_QUESTION),
            pollOptions = data[KEY_POLL_OPTIONS]?.jsonArray?.mapNotNull {
                val obj = it.jsonObject
                val text = obj.getNullableString(KEY_TEXT) ?: return@mapNotNull null
                PollOption(text, obj.getInt(KEY_VOTES))
            }
        )
        
        data[KEY_USERS]?.jsonObject?.let { userData ->
            post.username = userData.getNullableString(KEY_USERNAME)
            post.avatarUrl = userData.getNullableString(KEY_AVATAR)?.let { constructAvatarUrl(it) }
            post.isVerified = userData.getBoolean(KEY_VERIFY)
        }
        
        data[KEY_MEDIA_ITEMS]?.takeIf { it !is JsonNull }?.jsonArray?.let { mediaData ->
            post.mediaItems = mediaData.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap.getNullableString(KEY_URL) ?: return@mapNotNull null
                MediaItem(
                    id = mediaMap.getString(KEY_ID),
                    url = constructMediaUrl(url),
                    type = if (mediaMap.getNullableString(KEY_TYPE).equals(MEDIA_TYPE_VIDEO, true)) MediaType.VIDEO else MediaType.IMAGE,
                    thumbnailUrl = mediaMap.getNullableString(KEY_THUMBNAIL_URL)?.let { constructMediaUrl(it) }
                )
            }.toMutableList()
        }
        
        return post
    }
}
