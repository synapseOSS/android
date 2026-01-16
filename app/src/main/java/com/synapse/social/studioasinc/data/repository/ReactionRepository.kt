package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.domain.model.CommentReaction
import com.synapse.social.studioasinc.domain.model.ReactionType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/**
 * Repository for handling post and comment reactions.
 * Uses the `reactions` table for post reactions and `comment_reactions` table for comment reactions.
 * 
 * Requirements: 3.2, 3.3, 3.4, 3.5, 6.2, 6.3, 6.4
 */
class ReactionRepository {
    
    private val client = SupabaseClient.client
    
    companion object {
        private const val TAG = "ReactionRepository"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 100L
    }
    
    // ==================== UNIFIED REACTION LOGIC ====================

    /**
     * Toggle a reaction on any target (Post, Comment, etc.)
     * This is the Single Source of Truth for reaction updates.
     *
     * @param targetId The ID of the target (post or comment)
     * @param targetType The type of target ("post" or "comment")
     * @param reactionType The type of reaction to toggle
     */
    suspend fun toggleReaction(
        targetId: String,
        targetType: String,
        reactionType: ReactionType
    ): Result<ReactionToggleResult> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }

            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated to react"))

            val userId = currentUser.id
            val tableName = getTableName(targetType)
            val idColumn = getIdColumn(targetType)

            Log.d(TAG, "Toggling reaction: ${reactionType.name} for $targetType $targetId by user $userId")

            var lastException: Exception? = null
            repeat(MAX_RETRIES) { attempt ->
                try {
                    // Check for existing reaction
                    val existingReaction = client.from(tableName)
                        .select { filter { eq(idColumn, targetId); eq("user_id", userId) } }
                        .decodeSingleOrNull<JsonObject>()

                    val result = if (existingReaction != null) {
                        val existingType = existingReaction["reaction_type"]?.jsonPrimitive?.contentOrNull
                        if (existingType == reactionType.name.lowercase()) {
                            // Same reaction - remove it
                            client.from(tableName)
                                .delete { filter { eq(idColumn, targetId); eq("user_id", userId) } }
                            Log.d(TAG, "Reaction removed for $targetType $targetId")
                            ReactionToggleResult.REMOVED
                        } else {
                            // Different reaction - update it
                            client.from(tableName)
                                .update({
                                    set("reaction_type", reactionType.name.lowercase())
                                    set("updated_at", java.time.Instant.now().toString())
                                }) { filter { eq(idColumn, targetId); eq("user_id", userId) } }
                            Log.d(TAG, "Reaction updated to ${reactionType.name} for $targetType $targetId")
                            ReactionToggleResult.UPDATED
                        }
                    } else {
                        // No existing reaction - add new one
                        client.from(tableName).insert(buildJsonObject {
                            put("user_id", userId)
                            put(idColumn, targetId)
                            put("reaction_type", reactionType.name.lowercase())
                        })
                        Log.d(TAG, "New reaction ${reactionType.name} added for $targetType $targetId")
                        ReactionToggleResult.ADDED
                    }

                    return@withContext Result.success(result)
                } catch (e: Exception) {
                    lastException = e
                    val isRLSError = e.message?.contains("policy", true) == true
                    if (isRLSError || attempt == MAX_RETRIES - 1) throw e
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
            Result.failure(Exception(mapSupabaseError(lastException ?: Exception("Unknown error"))))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle reaction: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    /**
     * Get aggregated reaction counts for any target.
     */
    suspend fun getReactionSummary(
        targetId: String,
        targetType: String
    ): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        try {
            val tableName = getTableName(targetType)
            val idColumn = getIdColumn(targetType)

            val reactions = client.from(tableName)
                .select { filter { eq(idColumn, targetId) } }
                .decodeList<JsonObject>()

            val summary = reactions
                .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                .mapValues { it.value.size }

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    /**
     * Get the current user's reaction for any target.
     */
    suspend fun getUserReaction(
        targetId: String,
        targetType: String
    ): Result<ReactionType?> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull() ?: return@withContext Result.success(null)
            val userId = currentUser.id
            val tableName = getTableName(targetType)
            val idColumn = getIdColumn(targetType)

            val reaction = client.from(tableName)
                .select { filter { eq(idColumn, targetId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()

            val reactionType = reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull?.let {
                ReactionType.fromString(it)
            }
            Result.success(reactionType)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    /**
     * Batch fetch reactions for multiple posts to avoid N+1 queries.
     * Efficiently populates a list of posts with their reaction summaries and current user status.
     */
    suspend fun populatePostReactions(posts: List<com.synapse.social.studioasinc.domain.model.Post>): List<com.synapse.social.studioasinc.domain.model.Post> = withContext(Dispatchers.IO) {
        if (posts.isEmpty()) return@withContext posts

        try {
            val allPostIds = posts.map { it.id }
            val currentUserId = client.auth.currentUserOrNull()?.id
            val allReactions = mutableListOf<JsonObject>()
            
            // Process in chunks to avoid URL length limits
            allPostIds.chunked(20).forEach { chunkIds ->
                 try {
                     val chunkReactions = client.from("reactions")
                        .select { filter { isIn("post_id", chunkIds) } }
                        .decodeList<JsonObject>()
                     allReactions.addAll(chunkReactions)
                 } catch(e: Exception) {
                     Log.e(TAG, "Failed to fetch reactions for chunk", e)
                 }
            }

            // Group by Post ID
            val reactionsByPost = allReactions.groupBy { it["post_id"]?.jsonPrimitive?.contentOrNull }

            posts.map { post ->
                val postReactions = reactionsByPost[post.id] ?: emptyList()

                // Calculate Summary
                val summary = postReactions
                    .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                    .mapValues { it.value.size }

                // Determine User Reaction
                val userReactionType = if (currentUserId != null) {
                    postReactions.find { it["user_id"]?.jsonPrimitive?.contentOrNull == currentUserId }
                        ?.let { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                } else null

                post.copy(reactions = summary, userReaction = userReactionType, likesCount = summary.values.sum())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to populate reactions", e)
            posts
        }
    }

    // ==================== DEPRECATED / LEGACY SUPPORT (Delegates) ====================

    suspend fun togglePostReaction(postId: String, reactionType: ReactionType) =
        toggleReaction(postId, "post", reactionType)

    suspend fun toggleCommentReaction(commentId: String, reactionType: ReactionType) =
        toggleReaction(commentId, "comment", reactionType)

    suspend fun getPostReactionSummary(postId: String) =
        getReactionSummary(postId, "post")

    suspend fun getCommentReactionSummary(commentId: String) =
        getReactionSummary(commentId, "comment")

    suspend fun getUserPostReaction(postId: String) =
        getUserReaction(postId, "post")

    suspend fun getUserCommentReaction(commentId: String) =
        getUserReaction(commentId, "comment")


    // ==================== HELPERS ====================

    private fun getTableName(targetType: String): String {
        return when (targetType.lowercase()) {
            "post" -> "reactions"
            "comment" -> "comment_reactions"
            else -> "reactions"
        }
    }

    private fun getIdColumn(targetType: String): String {
        return when (targetType.lowercase()) {
            "post" -> "post_id"
            "comment" -> "comment_id"
            else -> "post_id"
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Map Supabase errors to user-friendly messages.
     */
    private fun mapSupabaseError(exception: Exception): String {
        val message = exception.message ?: "Unknown error"
        
        Log.e(TAG, "Supabase error: $message", exception)
        
        return when {
            message.contains("PGRST200") -> "Database table not found"
            message.contains("PGRST100") -> "Database column does not exist"
            message.contains("PGRST116") -> "Record not found"
            message.contains("relation", ignoreCase = true) -> "Database table does not exist"
            message.contains("column", ignoreCase = true) -> "Database column mismatch"
            message.contains("policy", ignoreCase = true) || message.contains("rls", ignoreCase = true) -> 
                "Permission denied"
            message.contains("connection", ignoreCase = true) || message.contains("network", ignoreCase = true) -> 
                "Connection failed. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            message.contains("unauthorized", ignoreCase = true) -> "Permission denied."
            else -> "Failed to process reaction: $message"
        }
    }
    
    // ==================== TESTABLE LOGIC METHODS ====================
    
    /**
     * Determine the result of toggling a reaction based on existing state.
     * This is a pure function for testing reaction toggle logic.
     * 
     * @param existingReactionType The current reaction type (null if no reaction)
     * @param newReactionType The reaction type being toggled
     * @return The expected toggle result
     */
    fun determineToggleResult(
        existingReactionType: ReactionType?,
        newReactionType: ReactionType
    ): ReactionToggleResult {
        return when {
            existingReactionType == null -> ReactionToggleResult.ADDED
            existingReactionType == newReactionType -> ReactionToggleResult.REMOVED
            else -> ReactionToggleResult.UPDATED
        }
    }
    
    /**
     * Calculate reaction summary from a list of reaction types.
     * This is a pure function for testing aggregation logic.
     * 
     * @param reactions List of reaction types
     * @return Map of ReactionType to count
     */
    fun calculateReactionSummary(reactions: List<ReactionType>): Map<ReactionType, Int> {
        return reactions.groupingBy { it }.eachCount()
    }
    
    /**
     * Validate that a reaction summary is accurate.
     * The sum of all counts should equal the total number of reactions.
     * 
     * @param summary The reaction summary map
     * @param totalReactions The expected total number of reactions
     * @return True if the summary is accurate
     */
    fun isReactionSummaryAccurate(summary: Map<ReactionType, Int>, totalReactions: Int): Boolean {
        return summary.values.sum() == totalReactions
    }
}

/**
 * Result of a reaction toggle operation.
 */
enum class ReactionToggleResult {
    /** A new reaction was added */
    ADDED,
    /** An existing reaction was removed */
    REMOVED,
    /** An existing reaction was updated to a different type */
    UPDATED
}
