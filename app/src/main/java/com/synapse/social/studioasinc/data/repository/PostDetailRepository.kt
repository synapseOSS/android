package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.domain.model.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/**
 * Repository for fetching detailed post information with author data.
 * Handles post loading, view count incrementing, and media parsing.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3
 */
class PostDetailRepository {
    
    private val client = SupabaseClient.client
    private val reactionRepository = com.synapse.social.studioasinc.data.repository.ReactionRepository()
    
    companion object {
        private const val TAG = "PostDetailRepository"
    }
    
    suspend fun getPostWithDetails(postId: String): Result<PostDetail> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            Log.d(TAG, "Fetching post details for: $postId")
            
            val response = client.from("posts")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!posts_author_uid_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                    """.trimIndent())
                ) {
                    filter { eq("id", postId) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            if (response == null) {
                Log.w(TAG, "Post not found: $postId")
                return@withContext Result.failure(Exception("Post not found"))
            }
            
            val post = parsePostFromJson(response)
            val author = parseUserProfileFromJson(response["users"]?.jsonObject)
                ?: return@withContext Result.failure(Exception("Author not found"))
            
            val currentUserId = client.auth.currentUserOrNull()?.id
            var userReaction: ReactionType? = null
            var isBookmarked = false
            var hasReshared = false
            
            if (currentUserId != null) {
                userReaction = reactionRepository.getUserReaction(postId, "post").getOrNull()
                isBookmarked = checkBookmarkStatus(postId, currentUserId)
                hasReshared = checkReshareStatus(postId, currentUserId)
            }
            
            val reactionSummary = reactionRepository.getReactionSummary(postId, "post").getOrDefault(emptyMap())
            
            var pollResults: List<PollOptionResult>? = null
            var userPollVote: Int? = null
            if (post.hasPoll == true) {
                val pollData = getPollData(postId, currentUserId)
                pollResults = pollData.first
                userPollVote = pollData.second
            }
            
            val postDetail = PostDetail(
                post = post,
                author = author,
                reactionSummary = reactionSummary,
                userReaction = userReaction,
                isBookmarked = isBookmarked,
                hasReshared = hasReshared,
                pollResults = pollResults,
                userPollVote = userPollVote
            )
            
            Log.d(TAG, "Successfully fetched post details for: $postId")
            Result.success(postDetail)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch post details: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun incrementViewCount(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest.rpc("increment_post_views", mapOf("post_id" to postId))
            Log.d(TAG, "Incremented view count for post: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment view count: ${e.message}", e)
            Result.success(Unit)
        }
    }
    
    fun observePostChanges(postId: String): Flow<PostDetail> = flow {
        val result = getPostWithDetails(postId)
        if (result.isSuccess) {
            emit(result.getOrThrow())
        }
    }
    
    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            client.from("posts")
                .update({ set("is_deleted", true) }) {
                    filter { eq("id", postId) }
                }

            Log.d(TAG, "Post deleted successfully: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete post: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================
    
    private fun parsePostFromJson(data: JsonObject): Post {
        val post = Post(
            id = data["id"]?.jsonPrimitive?.contentOrNull ?: "",
            key = data["key"]?.jsonPrimitive?.contentOrNull,
            authorUid = data["author_uid"]?.jsonPrimitive?.contentOrNull ?: "",
            postText = data["post_text"]?.jsonPrimitive?.contentOrNull,
            postImage = data["post_image"]?.jsonPrimitive?.contentOrNull,
            postType = data["post_type"]?.jsonPrimitive?.contentOrNull,
            postHideViewsCount = data["post_hide_views_count"]?.jsonPrimitive?.contentOrNull,
            postHideLikeCount = data["post_hide_like_count"]?.jsonPrimitive?.contentOrNull,
            postHideCommentsCount = data["post_hide_comments_count"]?.jsonPrimitive?.contentOrNull,
            postDisableComments = data["post_disable_comments"]?.jsonPrimitive?.contentOrNull,
            postVisibility = data["post_visibility"]?.jsonPrimitive?.contentOrNull,
            publishDate = data["publish_date"]?.jsonPrimitive?.contentOrNull,
            timestamp = data["timestamp"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
            likesCount = data["likes_count"]?.jsonPrimitive?.intOrNull ?: 0,
            commentsCount = data["comments_count"]?.jsonPrimitive?.intOrNull ?: 0,
            viewsCount = data["views_count"]?.jsonPrimitive?.intOrNull ?: 0,
            resharesCount = data["reshares_count"]?.jsonPrimitive?.intOrNull ?: 0,
            isEncrypted = data["is_encrypted"]?.jsonPrimitive?.booleanOrNull,
            nonce = data["nonce"]?.jsonPrimitive?.contentOrNull,
            encryptionKeyId = data["encryption_key_id"]?.jsonPrimitive?.contentOrNull,
            isDeleted = data["is_deleted"]?.jsonPrimitive?.booleanOrNull,
            isEdited = data["is_edited"]?.jsonPrimitive?.booleanOrNull,
            editedAt = data["edited_at"]?.jsonPrimitive?.contentOrNull,
            deletedAt = data["deleted_at"]?.jsonPrimitive?.contentOrNull,
            hasPoll = data["has_poll"]?.jsonPrimitive?.booleanOrNull,
            pollQuestion = data["poll_question"]?.jsonPrimitive?.contentOrNull,
            pollOptions = data["poll_options"]?.jsonArray?.mapNotNull {
                val obj = it.jsonObject
                val text = obj["text"]?.jsonPrimitive?.contentOrNull
                val votes = obj["votes"]?.jsonPrimitive?.intOrNull ?: 0
                if (text != null) PollOption(text, votes) else null
            },
            pollEndTime = data["poll_end_time"]?.jsonPrimitive?.contentOrNull,
            pollAllowMultiple = data["poll_allow_multiple"]?.jsonPrimitive?.booleanOrNull,
            hasLocation = data["has_location"]?.jsonPrimitive?.booleanOrNull,
            locationName = data["location_name"]?.jsonPrimitive?.contentOrNull,
            locationAddress = data["location_address"]?.jsonPrimitive?.contentOrNull,
            locationLatitude = data["location_latitude"]?.jsonPrimitive?.doubleOrNull,
            locationLongitude = data["location_longitude"]?.jsonPrimitive?.doubleOrNull,
            locationPlaceId = data["location_place_id"]?.jsonPrimitive?.contentOrNull,
            youtubeUrl = data["youtube_url"]?.jsonPrimitive?.contentOrNull
        )
        
        val mediaData = data["media_items"]?.takeIf { it !is JsonNull }?.jsonArray
        if (mediaData != null && mediaData.isNotEmpty()) {
            post.mediaItems = parseMediaItems(mediaData)
        }
        
        return post
    }
    
    private fun parseMediaItems(mediaData: JsonArray): MutableList<MediaItem> {
        return mediaData.mapNotNull { item ->
            try {
                val mediaMap = item.jsonObject
                val url = mediaMap["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap["type"]?.jsonPrimitive?.contentOrNull ?: "IMAGE"
                MediaItem(
                    id = mediaMap["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = url,
                    type = if (typeStr.equals("VIDEO", ignoreCase = true)) MediaType.VIDEO else MediaType.IMAGE,
                    thumbnailUrl = mediaMap["thumbnailUrl"]?.jsonPrimitive?.contentOrNull,
                    duration = mediaMap["duration"]?.jsonPrimitive?.longOrNull,
                    size = mediaMap["size"]?.jsonPrimitive?.longOrNull,
                    mimeType = mediaMap["mimeType"]?.jsonPrimitive?.contentOrNull
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse media item: ${e.message}")
                null
            }
        }.toMutableList()
    }
    
    private fun parseUserProfileFromJson(userData: JsonObject?): UserProfile? {
        if (userData == null) return null
        
        return try {
            UserProfile(
                uid = userData["uid"]?.jsonPrimitive?.contentOrNull ?: return null,
                username = userData["username"]?.jsonPrimitive?.contentOrNull ?: "",
                displayName = userData["display_name"]?.jsonPrimitive?.contentOrNull ?: "",
                email = userData["email"]?.jsonPrimitive?.contentOrNull ?: "",
                bio = userData["bio"]?.jsonPrimitive?.contentOrNull,
                avatar = userData["avatar"]?.jsonPrimitive?.contentOrNull,
                followersCount = userData["followers_count"]?.jsonPrimitive?.intOrNull ?: 0,
                followingCount = userData["following_count"]?.jsonPrimitive?.intOrNull ?: 0,
                postsCount = userData["posts_count"]?.jsonPrimitive?.intOrNull ?: 0,
                status = userData["status"]?.jsonPrimitive?.contentOrNull ?: "offline",
                account_type = userData["account_type"]?.jsonPrimitive?.contentOrNull ?: "user",
                verify = userData["verify"]?.jsonPrimitive?.booleanOrNull ?: false,
                banned = userData["banned"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user profile: ${e.message}")
            null
        }
    }


    
    private suspend fun checkBookmarkStatus(postId: String, userId: String): Boolean {
        return try {
            val bookmark = client.from("favorites")
                .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()
            bookmark != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check bookmark status: ${e.message}")
            false
        }
    }
    
    private suspend fun checkReshareStatus(postId: String, userId: String): Boolean {
        return try {
            val reshare = client.from("reshares")
                .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()
            reshare != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check reshare status: ${e.message}")
            false
        }
    }
    
    private suspend fun getPollData(postId: String, userId: String?): Pair<List<PollOptionResult>?, Int?> {
        return try {
            val post = client.from("posts")
                .select { filter { eq("id", postId) } }
                .decodeSingleOrNull<JsonObject>()
            
            val options = post?.get("poll_options")?.jsonArray?.mapNotNull {
                it.jsonObject["text"]?.jsonPrimitive?.contentOrNull
            } ?: emptyList()
            
            if (options.isEmpty()) {
                return Pair(null, null)
            }
            
            val votes = client.from("poll_votes")
                .select { filter { eq("post_id", postId) } }
                .decodeList<JsonObject>()
            
            val voteCounts = votes.groupBy { 
                it["option_index"]?.jsonPrimitive?.intOrNull ?: 0 
            }.mapValues { it.value.size }
            
            val pollResults = PollOptionResult.calculateResults(options, voteCounts)
            
            val userVote = if (userId != null) {
                votes.find { it["user_id"]?.jsonPrimitive?.contentOrNull == userId }
                    ?.get("option_index")?.jsonPrimitive?.intOrNull
            } else null
            
            Pair(pollResults, userVote)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get poll data: ${e.message}")
            Pair(null, null)
        }
    }
    
    private fun mapSupabaseError(exception: Exception): String {
        val message = exception.message ?: "Unknown error"
        
        Log.e(TAG, "Supabase error: $message", exception)
        
        return when {
            message.contains("PGRST200") -> "Database table not found"
            message.contains("PGRST100") -> "Database column does not exist"
            message.contains("PGRST116") -> "Post not found"
            message.contains("relation", ignoreCase = true) -> "Database table does not exist"
            message.contains("column", ignoreCase = true) -> "Database column mismatch"
            message.contains("policy", ignoreCase = true) || message.contains("rls", ignoreCase = true) -> 
                "Permission denied"
            message.contains("connection", ignoreCase = true) || message.contains("network", ignoreCase = true) -> 
                "Connection failed. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            message.contains("unauthorized", ignoreCase = true) -> "Permission denied."
            else -> "Failed to load post: $message"
        }
    }
    
    fun hasYouTubeUrl(post: Post): Boolean {
        return !post.youtubeUrl.isNullOrEmpty()
    }
    
    fun isPostEdited(post: Post): Boolean {
        return post.isEdited == true
    }
    
    fun getEditedTimestamp(post: Post): String? {
        return if (post.isEdited == true) post.editedAt else null
    }
    
    fun getAuthorBadge(author: UserProfile): String? {
        return when {
            author.verify -> "verified"
            author.account_type == "premium" -> "premium"
            else -> null
        }
    }
    
    fun isPostDataComplete(postDetail: PostDetail): Boolean {
        val post = postDetail.post
        val author = postDetail.author
        
        if (post.id.isEmpty()) return false
        if (post.authorUid.isEmpty()) return false
        if (author.uid.isEmpty()) return false
        
        if (post.postType == "IMAGE" || post.postType == "VIDEO") {
            if (post.mediaItems.isNullOrEmpty() && post.postImage.isNullOrEmpty()) {
                if (post.postText.isNullOrEmpty()) return false
            }
        }
        
        if (post.hasLocation == true) {
            if (post.locationName.isNullOrEmpty() && post.locationAddress.isNullOrEmpty()) {
                return false
            }
        }
        
        if (post.hasPoll == true) {
            if (post.pollQuestion.isNullOrEmpty() || post.pollOptions.isNullOrEmpty()) {
                return false
            }
        }
        
        return true
    }
}
