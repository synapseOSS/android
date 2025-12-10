package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.CommentReaction
import com.synapse.social.studioasinc.model.ReactionType
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
    
    // ==================== POST REACTIONS ====================
    
    /**
     * Toggle a reaction on a post.
     * - If no reaction exists, adds the reaction
     * - If same reaction type exists, removes it
     * - If different reaction type exists, updates to new type
     * 
     * @param postId The ID of the post
     * @param reactionType The type of reaction to toggle
     * @return Result indicating success or failure
     * 
     * Requirements: 3.2, 3.3, 3.4
     */
    suspend fun togglePostReaction(
        postId: String,
        reactionType: ReactionType
    ): Result<ReactionToggleResult> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.failure(Exception("User must be authenticated to react"))
            }
            
            val userId = currentUser.id
            Log.d(TAG, "Toggling post reaction: ${reactionType.name} for post $postId by user $userId")
            
            var lastException: Exception? = null
            repeat(MAX_RETRIES) { attempt ->
                try {
                    // Check for existing reaction
                    val existingReaction = client.from("reactions")
                        .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                        .decodeSingleOrNull<JsonObject>()
                    
                    val result = if (existingReaction != null) {
                        val existingType = existingReaction["reaction_type"]?.jsonPrimitive?.contentOrNull
                        if (existingType == reactionType.name.lowercase()) {
                            // Same reaction - remove it
                            client.from("reactions")
                                .delete { filter { eq("post_id", postId); eq("user_id", userId) } }
                            Log.d(TAG, "Reaction removed for post $postId")
                            ReactionToggleResult.REMOVED
                        } else {
                            // Different reaction - update it
                            client.from("reactions")
                                .update({
                                    set("reaction_type", reactionType.name.lowercase())
                                    set("updated_at", java.time.Instant.now().toString())
                                }) { filter { eq("post_id", postId); eq("user_id", userId) } }
                            Log.d(TAG, "Reaction updated to ${reactionType.name} for post $postId")
                            ReactionToggleResult.UPDATED
                        }
                    } else {
                        // No existing reaction - add new one
                        client.from("reactions").insert(buildJsonObject {
                            put("user_id", userId)
                            put("post_id", postId)
                            put("reaction_type", reactionType.name.lowercase())
                        })
                        Log.d(TAG, "New reaction ${reactionType.name} added for post $postId")
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
            Log.e(TAG, "Failed to toggle post reaction: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    
    /**
     * Get aggregated reaction counts for a post.
     * 
     * @param postId The ID of the post
     * @return Result containing a map of ReactionType to count
     * 
     * Requirements: 3.5
     */
    suspend fun getPostReactionSummary(postId: String): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            Log.d(TAG, "Fetching reaction summary for post $postId")
            
            val reactions = client.from("reactions")
                .select { filter { eq("post_id", postId) } }
                .decodeList<JsonObject>()
            
            val summary = reactions
                .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                .mapValues { it.value.size }
            
            Log.d(TAG, "Reaction summary for post $postId: $summary")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get post reaction summary: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    
    /**
     * Get the current user's reaction for a post.
     * 
     * @param postId The ID of the post
     * @return Result containing the user's ReactionType or null if no reaction
     * 
     * Requirements: 3.3
     */
    suspend fun getUserPostReaction(postId: String): Result<ReactionType?> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.success(null)
            }
            
            val userId = currentUser.id
            Log.d(TAG, "Fetching user reaction for post $postId")
            
            val reaction = client.from("reactions")
                .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()
            
            val reactionType = reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull?.let {
                ReactionType.fromString(it)
            }
            
            Log.d(TAG, "User reaction for post $postId: $reactionType")
            Result.success(reactionType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user post reaction: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    
    // ==================== COMMENT REACTIONS ====================
    
    /**
     * Toggle a reaction on a comment.
     * - If no reaction exists, adds the reaction
     * - If same reaction type exists, removes it
     * - If different reaction type exists, updates to new type
     * 
     * @param commentId The ID of the comment
     * @param reactionType The type of reaction to toggle
     * @return Result indicating success or failure
     * 
     * Requirements: 6.2, 6.3, 6.4
     */
    suspend fun toggleCommentReaction(
        commentId: String,
        reactionType: ReactionType
    ): Result<ReactionToggleResult> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.failure(Exception("User must be authenticated to react"))
            }
            
            val userId = currentUser.id
            Log.d(TAG, "Toggling comment reaction: ${reactionType.name} for comment $commentId by user $userId")
            
            var lastException: Exception? = null
            repeat(MAX_RETRIES) { attempt ->
                try {
                    // Check for existing reaction
                    val existingReaction = client.from("comment_reactions")
                        .select { filter { eq("comment_id", commentId); eq("user_id", userId) } }
                        .decodeSingleOrNull<JsonObject>()
                    
                    val result = if (existingReaction != null) {
                        val existingType = existingReaction["reaction_type"]?.jsonPrimitive?.contentOrNull
                        if (existingType == reactionType.name.lowercase()) {
                            // Same reaction - remove it
                            client.from("comment_reactions")
                                .delete { filter { eq("comment_id", commentId); eq("user_id", userId) } }
                            Log.d(TAG, "Comment reaction removed for comment $commentId")
                            ReactionToggleResult.REMOVED
                        } else {
                            // Different reaction - update it
                            client.from("comment_reactions")
                                .update({
                                    set("reaction_type", reactionType.name.lowercase())
                                    set("updated_at", java.time.Instant.now().toString())
                                }) { filter { eq("comment_id", commentId); eq("user_id", userId) } }
                            Log.d(TAG, "Comment reaction updated to ${reactionType.name} for comment $commentId")
                            ReactionToggleResult.UPDATED
                        }
                    } else {
                        // No existing reaction - add new one
                        client.from("comment_reactions").insert(buildJsonObject {
                            put("user_id", userId)
                            put("comment_id", commentId)
                            put("reaction_type", reactionType.name.lowercase())
                        })
                        Log.d(TAG, "New comment reaction ${reactionType.name} added for comment $commentId")
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
            Log.e(TAG, "Failed to toggle comment reaction: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    
    /**
     * Get aggregated reaction counts for a comment.
     * 
     * @param commentId The ID of the comment
     * @return Result containing a map of ReactionType to count
     * 
     * Requirements: 6.2
     */
    suspend fun getCommentReactionSummary(commentId: String): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            Log.d(TAG, "Fetching reaction summary for comment $commentId")
            
            val reactions = client.from("comment_reactions")
                .select { filter { eq("comment_id", commentId) } }
                .decodeList<JsonObject>()
            
            val summary = reactions
                .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                .mapValues { it.value.size }
            
            Log.d(TAG, "Reaction summary for comment $commentId: $summary")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get comment reaction summary: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    
    /**
     * Get the current user's reaction for a comment.
     * 
     * @param commentId The ID of the comment
     * @return Result containing the user's ReactionType or null if no reaction
     * 
     * Requirements: 6.3
     */
    suspend fun getUserCommentReaction(commentId: String): Result<ReactionType?> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.success(null)
            }
            
            val userId = currentUser.id
            Log.d(TAG, "Fetching user reaction for comment $commentId")
            
            val reaction = client.from("comment_reactions")
                .select { filter { eq("comment_id", commentId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()
            
            val reactionType = reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull?.let {
                ReactionType.fromString(it)
            }
            
            Log.d(TAG, "User reaction for comment $commentId: $reactionType")
            Result.success(reactionType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user comment reaction: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
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
