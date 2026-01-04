package com.synapse.social.studioasinc.data

import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

class MediaInteractionRepository {

    private val client = SupabaseClient.client

    suspend fun likeMedia(mediaId: String, postId: String, userId: String): Result<Unit> = runCatching {
        client.from("media_likes").insert(
            mapOf(
                "media_id" to mediaId,
                "post_id" to postId,
                "user_id" to userId
            )
        )
        updateMediaInteractionCount(postId, mediaId, "likes_count", 1)
    }

    suspend fun unlikeMedia(mediaId: String, userId: String): Result<Unit> = runCatching {
        val response = client.from("media_likes")
            .delete {
                filter {
                    eq("media_id", mediaId)
                    eq("user_id", userId)
                }
            }
        
        val postId = getPostIdForMedia(mediaId)
        updateMediaInteractionCount(postId, mediaId, "likes_count", -1)
    }

    suspend fun getMediaInteractions(postId: String, mediaIds: List<String>): Result<Map<String, MediaInteractionData>> = runCatching {
        val interactions = client.from("media_interactions")
            .select {
                filter {
                    eq("post_id", postId)
                    isIn("media_id", mediaIds)
                }
            }
            .decodeList<MediaInteractionData>()
        
        interactions.associateBy { it.media_id }
    }

    suspend fun checkUserLikedMedia(mediaId: String, userId: String): Result<Boolean> = runCatching {
        val result = client.from("media_likes")
            .select {
                filter {
                    eq("media_id", mediaId)
                    eq("user_id", userId)
                }
            }
            .decodeList<MediaLike>()
        
        result.isNotEmpty()
    }

    private suspend fun updateMediaInteractionCount(postId: String, mediaId: String, field: String, delta: Int) {
        val existing = client.from("media_interactions")
            .select {
                filter {
                    eq("post_id", postId)
                    eq("media_id", mediaId)
                }
            }
            .decodeSingleOrNull<MediaInteractionData>()

        if (existing != null) {
            val newCount = maxOf(0, (existing.likes_count ?: 0) + delta)
            client.from("media_interactions").update(
                mapOf(field to newCount)
            ) {
                filter {
                    eq("post_id", postId)
                    eq("media_id", mediaId)
                }
            }
        } else {
            client.from("media_interactions").insert(
                mapOf(
                    "post_id" to postId,
                    "media_id" to mediaId,
                    "media_index" to 0,
                    field to maxOf(0, delta)
                )
            )
        }
    }

    private suspend fun getPostIdForMedia(mediaId: String): String {
        val like = client.from("media_likes")
            .select {
                filter { eq("media_id", mediaId) }
            }
            .decodeSingle<MediaLike>()
        return like.post_id
    }
}

@Serializable
data class MediaInteractionData(
    val media_id: String,
    val post_id: String,
    val likes_count: Int? = 0,
    val comments_count: Int? = 0
)

@Serializable
data class MediaLike(
    val media_id: String,
    val post_id: String,
    val user_id: String
)
