package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Repository for reporting posts.
 * Requirement: 10.3
 */
class ReportRepository {
    private val client = SupabaseClient.client
    
    @Serializable
    private data class PostReport(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("reporter_id") val reporterId: String,
        val reason: String,
        val description: String? = null
    )
    
    /**
     * Create a report for a post.
     */
    suspend fun createReport(
        postId: String,
        reason: String,
        description: String? = null
    ): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id 
            ?: return Result.failure(Exception("Not authenticated"))
        
        client.from("post_reports")
            .insert(PostReport(
                postId = postId,
                reporterId = userId,
                reason = reason,
                description = description
            ))
        
        Log.d(TAG, "Report created: post=$postId, reason=$reason")
    }
    
    companion object {
        private const val TAG = "ReportRepository"
    }
}
