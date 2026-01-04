package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.PollOptionResult
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Repository for poll operations.
 * Handles poll voting and results retrieval.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
class PollRepository {
    private val client = SupabaseClient.client
    
    @Serializable
    private data class PollVote(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("option_index") val optionIndex: Int,
        @SerialName("created_at") val createdAt: String? = null
    )
    
    @Serializable
    private data class PostPollData(
        val id: String,
        @SerialName("poll_options") val pollOptions: List<PollOption>,
        @SerialName("poll_end_time") val pollEndTime: String?
    )
    
    /**
     * Get user's vote for a poll.
     * Returns null if user hasn't voted.
     * 
     * Requirement: 7.2, 7.4
     */
    suspend fun getUserVote(postId: String): Result<Int?> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id 
            ?: return Result.failure(Exception("Not authenticated"))
        
        val votes = client.from("poll_votes")
            .select(Columns.list("option_index")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<PollVote>()
        
        votes.firstOrNull()?.optionIndex
    }
    
    /**
     * Submit a vote for a poll option.
     * Validates poll hasn't ended before accepting vote.
     * 
     * Requirements: 7.3, 7.5
     */
    suspend fun submitVote(postId: String, optionIndex: Int): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id 
            ?: return Result.failure(Exception("Not authenticated"))
        
        // Check if poll has ended
        val post = client.from("posts")
            .select(Columns.list("id", "poll_options", "poll_end_time")) {
                filter { eq("id", postId) }
            }
            .decodeSingle<PostPollData>()
        
        post.pollEndTime?.let { endTime ->
            if (Instant.parse(endTime).isBefore(Instant.now())) {
                return Result.failure(Exception("Poll has ended"))
            }
        }
        
        // Validate option index
        if (optionIndex < 0 || optionIndex >= post.pollOptions.size) {
            return Result.failure(Exception("Invalid option index"))
        }
        
        // Check existing vote
        val existingVote = client.from("poll_votes")
            .select(Columns.list("id")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<PollVote>()
            .firstOrNull()
        
        if (existingVote != null) {
            // Update existing vote
            client.from("poll_votes")
                .update({
                    set("option_index", optionIndex)
                }) {
                    filter { eq("id", existingVote.id!!) }
                }
        } else {
            // Insert new vote
            client.from("poll_votes")
                .insert(PollVote(
                    postId = postId,
                    userId = userId,
                    optionIndex = optionIndex
                ))
        }
        
        Log.d(TAG, "Vote submitted: post=$postId, option=$optionIndex")
    }

    /**
     * Revoke user's vote for a poll.
     */
    suspend fun revokeVote(postId: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))

        // Delete vote
        client.from("poll_votes").delete {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }

        Log.d(TAG, "Vote revoked: post=$postId")
    }
    
    /**
     * Get poll results with vote counts and percentages.
     * 
     * Requirement: 7.1
     */
    suspend fun getPollResults(postId: String): Result<List<PollOptionResult>> = runCatching {
        // Get poll options
        val post = client.from("posts")
            .select(Columns.list("poll_options")) {
                filter { eq("id", postId) }
            }
            .decodeSingle<PostPollData>()
        
        // Get all votes
        val votes = client.from("poll_votes")
            .select(Columns.list("option_index")) {
                filter { eq("post_id", postId) }
            }
            .decodeList<PollVote>()
        
        // Count votes per option
        val voteCounts = votes.groupingBy { it.optionIndex }.eachCount()
        
        PollOptionResult.calculateResults(post.pollOptions.map { it.text }, voteCounts)
    }
    
    companion object {
        private const val TAG = "PollRepository"
    }
}
