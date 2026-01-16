package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Repository for post bookmarking/favorites.
 * Requirements: 8.1, 8.2
 */
class BookmarkRepository {
    private val client = SupabaseClient.client
    
    @Serializable
    private data class Favorite(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("collection_id") val collectionId: String? = null
    )
    
    /**
     * Check if post is bookmarked by current user.
     * Requirement: 8.1
     */
    suspend fun isBookmarked(postId: String): Result<Boolean> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id 
            ?: return Result.failure(Exception("Not authenticated"))
        
        val favorites = client.from("favorites")
            .select(Columns.list("id")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<Favorite>()
        
        favorites.isNotEmpty()
    }
    
    /**
     * Toggle bookmark status for a post.
     * Returns true if bookmarked, false if removed.
     * Requirement: 8.2
     */
    suspend fun toggleBookmark(postId: String, collectionId: String? = null): Result<Boolean> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id 
            ?: return Result.failure(Exception("Not authenticated"))
        
        val existing = client.from("favorites")
            .select(Columns.list("id")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<Favorite>()
            .firstOrNull()
        
        if (existing != null) {
            client.from("favorites")
                .delete { filter { eq("id", existing.id!!) } }
            Log.d(TAG, "Bookmark removed: $postId")
            false
        } else {
            client.from("favorites")
                .insert(Favorite(postId = postId, userId = userId, collectionId = collectionId))
            Log.d(TAG, "Bookmark added: $postId")
            true
        }
    }
    
    companion object {
        private const val TAG = "BookmarkRepository"
    }
}
