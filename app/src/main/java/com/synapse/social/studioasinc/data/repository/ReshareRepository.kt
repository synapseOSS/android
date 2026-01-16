package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Repository for post resharing.
 * Requirement: 8.5
 */
class ReshareRepository {
    private val client = SupabaseClient.client
    
    @Serializable
    private data class Reshare(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("reshare_text") val reshareText: String? = null
    )
    
    /**
     * Check if current user has reshared a post.
     */
    suspend fun hasReshared(postId: String): Result<Boolean> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id 
            ?: return Result.failure(Exception("Not authenticated"))
        
        val reshares = client.from("reshares")
            .select(Columns.list("id")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<Reshare>()
        
        reshares.isNotEmpty()
    }
    
    /**
     * Create a reshare for a post with optional commentary.
     */
    suspend fun createReshare(postId: String, commentary: String? = null): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id 
            ?: return Result.failure(Exception("Not authenticated"))
        
        // Check if already reshared
        val existing = client.from("reshares")
            .select(Columns.list("id")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<Reshare>()
            .firstOrNull()
        
        if (existing != null) {
            return Result.failure(Exception("Already reshared"))
        }
        
        client.from("reshares")
            .insert(Reshare(postId = postId, userId = userId, reshareText = commentary))
        
        // Increment reshares_count via RPC or trigger (handled by DB)
        Log.d(TAG, "Reshare created: $postId")
    }
    
    companion object {
        private const val TAG = "ReshareRepository"
    }
}
