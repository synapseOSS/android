package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.local.database.CommentDao
import com.synapse.social.studioasinc.data.local.database.CommentEntity
import com.synapse.social.studioasinc.data.repository.CommentMapper
import com.synapse.social.studioasinc.domain.model.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.statement.bodyAsText
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class CommentRepository(private val commentDao: CommentDao) {
    
    private val client = SupabaseClient.client
    private val reactionRepository = com.synapse.social.studioasinc.data.repository.ReactionRepository()
    
    companion object {
        private const val TAG = "CommentRepository"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 100L
    }
    
    fun getComments(postId: String): Flow<Result<List<Comment>>> {
        return commentDao.getCommentsForPost(postId).map<List<CommentEntity>, Result<List<Comment>>> { entities ->
            Result.success(entities.map { CommentMapper.toModel(it) })
        }.catch { e ->
            emit(Result.failure(Exception("Error getting comments from database: ${e.message}")))
        }
    }
    
    suspend fun refreshComments(postId: String, limit: Int = 50, offset: Int = 0): Result<Unit> {
        return try {
            val response = client.from("comments")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                    """.trimIndent())
                ) {
                    filter { 
                        eq("post_id", postId)
                        exact("parent_comment_id", null)
                    }
                    order("created_at", Order.ASCENDING)
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<JsonObject>()
            
            val comments = mutableListOf<CommentWithUser>()
            for (json in response) {
                parseCommentFromJson(json)?.let { comments.add(it) }
            }
            
            commentDao.insertAll(comments.map { 
                CommentMapper.toEntity(it.toComment(), it.user?.username, it.user?.avatar) 
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch comments: ${e.message}", e)
            Result.failure(e)
        }
    }

    // New method for PagingSource
    suspend fun fetchComments(postId: String, limit: Int = 50, offset: Int = 0): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
             val response = client.from("comments")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                    """.trimIndent())
                ) {
                    filter {
                        eq("post_id", postId)
                        exact("parent_comment_id", null) // Fetch only roots for pagination
                    }
                    order("created_at", Order.ASCENDING)
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<JsonObject>()

            val comments = mutableListOf<CommentWithUser>()
            for (json in response) {
                parseCommentFromJson(json)?.let { comments.add(it) }
            }

            Result.success(comments)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch comments list: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getReplies(commentId: String): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching replies for comment: $commentId")
            val response = client.from("comments")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                    """.trimIndent())
                ) {
                    filter { eq("parent_comment_id", commentId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<JsonObject>()
            
            Log.d(TAG, "Raw response size: ${response.size}")
            
            val replies = mutableListOf<CommentWithUser>()
            for (json in response) {
                parseCommentFromJson(json)?.let { 
                    replies.add(it)
                    Log.d(TAG, "Parsed reply: ${it.id} - ${it.content}")
                }
            }
            
            Log.d(TAG, "Successfully parsed ${replies.size} replies")
            
            // Store replies in local database
            commentDao.insertAll(replies.map { 
                CommentMapper.toEntity(it.toComment(), it.user?.username, it.user?.avatar) 
            })
            
            Result.success(replies)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch replies: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun createComment(
        postId: String,
        content: String,
        mediaUrl: String? = null,
        parentCommentId: String? = null
    ): Result<CommentWithUser> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.failure(Exception("User must be authenticated to comment"))
            }
            
            val userId = currentUser.id
            Log.d(TAG, "Creating comment for post: $postId by user: $userId")
            
            var lastException: Exception? = null
            repeat(MAX_RETRIES) { attempt ->
                try {
                    val insertData = buildJsonObject {
                        put("post_id", postId)
                        put("user_id", userId)
                        put("content", content)
                        if (mediaUrl != null) put("media_url", mediaUrl)
                        if (parentCommentId != null) put("parent_comment_id", parentCommentId)
                    }
                    
                    val response = client.from("comments")
                        .insert(insertData) {
                            select(
                                columns = Columns.raw("""
                                    *,
                                    users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                                """.trimIndent())
                            )
                        }
                        .decodeSingleOrNull<JsonObject>()
                    
                    if (response == null) {
                        return@withContext Result.failure(Exception("Failed to create comment"))
                    }
                    
                    val comment = parseCommentFromJson(response)
                        ?: return@withContext Result.failure(Exception("Failed to parse created comment"))
                    
                    commentDao.insertAll(listOf(CommentMapper.toEntity(comment.toComment())))

                    if (parentCommentId != null) {
                        updateRepliesCount(parentCommentId, 1)
                    }
                    
                    updatePostCommentsCount(postId, 1)
                    
                    // Process mentions
                    processMentions(postId, comment.id, content, userId, parentCommentId)
                    
                    Log.d(TAG, "Comment created successfully: ${comment.id}")
                    return@withContext Result.success(comment)
                } catch (e: Exception) {
                    lastException = e
                    val isRLSError = e.message?.contains("policy", true) == true
                    if (isRLSError || attempt == MAX_RETRIES - 1) throw e
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
            
            Result.failure(Exception(mapSupabaseError(lastException ?: Exception("Unknown error"))))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create comment: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun deleteComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.failure(Exception("User must be authenticated to delete comment"))
            }
            
            Log.d(TAG, "Deleting comment: $commentId")
            
            val existingComment = client.from("comments")
                .select { filter { eq("id", commentId) } }
                .decodeSingleOrNull<JsonObject>()
            
            if (existingComment == null) {
                return@withContext Result.failure(Exception("Comment not found"))
            }
            
            val commentUserId = existingComment["user_id"]?.jsonPrimitive?.contentOrNull
            if (commentUserId != currentUser.id) {
                return@withContext Result.failure(Exception("Cannot delete another user's comment"))
            }
            
            val postId = existingComment["post_id"]?.jsonPrimitive?.contentOrNull
            val parentCommentId = existingComment["parent_comment_id"]?.jsonPrimitive?.contentOrNull
            
            client.from("comments")
                .update({
                    set("is_deleted", true)
                    set("content", "[deleted]")
                    set("deleted_at", java.time.Instant.now().toString())
                }) {
                    filter { eq("id", commentId) }
                }
            
            if (parentCommentId != null) {
                updateRepliesCount(parentCommentId, -1)
            }
            
            if (postId != null) {
                updatePostCommentsCount(postId, -1)
            }
            
            Log.d(TAG, "Comment deleted successfully: $commentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete comment: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun editComment(commentId: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.failure(Exception("User must be authenticated to edit comment"))
            }
            
            Log.d(TAG, "Editing comment: $commentId")
            
            val existingComment = client.from("comments")
                .select { filter { eq("id", commentId) } }
                .decodeSingleOrNull<JsonObject>()
            
            if (existingComment == null) {
                return@withContext Result.failure(Exception("Comment not found"))
            }
            
            val commentUserId = existingComment["user_id"]?.jsonPrimitive?.contentOrNull
            if (commentUserId != currentUser.id) {
                return@withContext Result.failure(Exception("Cannot edit another user's comment"))
            }
            
            val isDeleted = existingComment["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false
            if (isDeleted) {
                return@withContext Result.failure(Exception("Cannot edit a deleted comment"))
            }
            
            client.from("comments")
                .update({
                    set("content", content)
                    set("is_edited", true)
                    set("updated_at", java.time.Instant.now().toString())
                }) {
                    filter { eq("id", commentId) }
                }
            
            Log.d(TAG, "Comment edited successfully: $commentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to edit comment: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun pinComment(commentId: String, postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))
            
            val post = client.from("posts")
                .select { filter { eq("id", postId) } }
                .decodeSingleOrNull<JsonObject>()
                ?: return@withContext Result.failure(Exception("Post not found"))
            
            val postAuthor = post["author_uid"]?.jsonPrimitive?.contentOrNull
            if (postAuthor != currentUser.id) {
                return@withContext Result.failure(Exception("Only post author can pin comments"))
            }
            
            client.from("comments")
                .update({ set("is_pinned", true) }) {
                    filter { eq("id", commentId) }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pin comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun hideComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))
            
            client.from("comments")
                .update({
                    set("is_hidden", true)
                    set("hidden_by", currentUser.id)
                }) {
                    filter { eq("id", commentId) }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun reportComment(commentId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))
            
            client.from("comment_reports")
                .insert(buildJsonObject {
                    put("comment_id", commentId)
                    put("reporter_id", currentUser.id)
                    put("reason", reason)
                    put("created_at", java.time.Instant.now().toString())
                })
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report comment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun parseCommentFromJson(data: JsonObject): CommentWithUser? {
        return try {
            val user = parseUserProfileFromJson(data["users"]?.jsonObject)
            val commentId = data["id"]?.jsonPrimitive?.contentOrNull ?: return null
            
            val reactionSummary = reactionRepository.getReactionSummary(commentId, "comment").getOrDefault(emptyMap())
            val userReaction = reactionRepository.getUserReaction(commentId, "comment").getOrNull()
            
            CommentWithUser(
                id = commentId,
                postId = data["post_id"]?.jsonPrimitive?.contentOrNull ?: return null,
                userId = data["user_id"]?.jsonPrimitive?.contentOrNull ?: return null,
                parentCommentId = data["parent_comment_id"]?.jsonPrimitive?.contentOrNull,
                content = data["content"]?.jsonPrimitive?.contentOrNull ?: "",
                mediaUrl = data["media_url"]?.jsonPrimitive?.contentOrNull,
                createdAt = data["created_at"]?.jsonPrimitive?.contentOrNull ?: "",
                updatedAt = data["updated_at"]?.jsonPrimitive?.contentOrNull,
                likesCount = data["likes_count"]?.jsonPrimitive?.intOrNull ?: 0,
                repliesCount = data["replies_count"]?.jsonPrimitive?.intOrNull ?: 0,
                isDeleted = data["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false,
                isEdited = data["is_edited"]?.jsonPrimitive?.booleanOrNull ?: false,
                isPinned = data["is_pinned"]?.jsonPrimitive?.booleanOrNull ?: false,
                user = user,
                reactionSummary = reactionSummary,
                userReaction = userReaction
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse comment: ${e.message}")
            null
        }
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
    
    private suspend fun updateRepliesCount(commentId: String, delta: Int) {
        try {
            val actualCount = client.from("comments")
                .select(columns = Columns.raw("")) {
                    filter {
                        eq("parent_comment_id", commentId)
                        eq("is_deleted", false)
                    }
                    count(Count.EXACT)
                }
                .countOrNull() ?: 0

            client.from("comments")
                .update({ set("replies_count", actualCount) }) {
                    filter { eq("id", commentId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update replies count: ${e.message}")
        }
    }
    
    private suspend fun updatePostCommentsCount(postId: String, delta: Int) {
        try {
            val post = client.from("posts")
                .select { filter { eq("id", postId) } }
                .decodeSingleOrNull<JsonObject>()
            
            val currentCount = post?.get("comments_count")?.jsonPrimitive?.intOrNull ?: 0
            val newCount = maxOf(0, currentCount + delta)
            
            client.from("posts")
                .update({ set("comments_count", newCount) }) {
                    filter { eq("id", postId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update post comments count: ${e.message}")
        }
    }
    
    private fun mapSupabaseError(exception: Exception): String {
        val message = exception.message ?: "Unknown error"
        
        Log.e(TAG, "Supabase error: $message", exception)
        
        return when {
            message.contains("PGRST200") -> "Database table not found"
            message.contains("PGRST100") -> "Database column does not exist"
            message.contains("PGRST116") -> "Comment not found"
            message.contains("relation", ignoreCase = true) -> "Database table does not exist"
            message.contains("column", ignoreCase = true) -> "Database column mismatch"
            message.contains("policy", ignoreCase = true) || message.contains("rls", ignoreCase = true) -> 
                "Permission denied"
            message.contains("connection", ignoreCase = true) || message.contains("network", ignoreCase = true) -> 
                "Connection failed. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            message.contains("unauthorized", ignoreCase = true) -> "Permission denied."
            else -> "Failed to process comment: $message"
        }
    }
    
    private suspend fun processMentions(
        postId: String,
        commentId: String,
        content: String,
        senderId: String,
        parentCommentId: String?
    ) {
        try {
            // Extract mentions from the comment content
            val mentionedUsers = com.synapse.social.studioasinc.core.domain.parser.MentionParser.extractMentions(content)
            
            // Process mentions if any exist
            if (mentionedUsers.isNotEmpty()) {
                Log.d(TAG, "Processing mentions: $mentionedUsers")
                // Add your mention processing logic here if needed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process mentions: ${e.message}", e)
        }
    }
}

private fun CommentWithUser.toComment(): Comment {
    return Comment(
        key = this.id,
        postKey = this.postId,
        uid = this.userId,
        comment = this.content,
        push_time = this.createdAt.toString(),
        replyCommentKey = this.parentCommentId
    )
}
