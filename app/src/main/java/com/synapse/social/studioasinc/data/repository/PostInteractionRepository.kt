package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class PostInteractionRepository {
    private val client = SupabaseClient.client

    suspend fun likePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("post_likes").insert(
                buildJsonObject {
                    put("post_id", postId)
                    put("user_id", userId)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlikePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("post_likes").delete {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("saved_posts").insert(
                buildJsonObject {
                    put("post_id", postId)
                    put("user_id", userId)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsavePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("saved_posts").delete {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("posts").delete {
                filter {
                    eq("id", postId)
                    eq("author_uid", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportPost(postId: String, userId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("post_reports").insert(
                buildJsonObject {
                    put("post_id", postId)
                    put("reporter_id", userId)
                    put("reason", reason)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
