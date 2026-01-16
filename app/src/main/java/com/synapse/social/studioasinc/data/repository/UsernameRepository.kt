package com.synapse.social.studioasinc.data.repository

import android.util.LruCache
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject

class UsernameRepository {
    private val client = SupabaseClient.client
    private val usernameCache = LruCache<String, Boolean>(100)

    suspend fun checkAvailability(username: String): Result<Boolean> {
        return try {
            if (!SupabaseClient.isConfigured()) {
                return Result.failure(Exception("Supabase not configured"))
            }

            // Check cache first
            usernameCache.get(username)?.let { return Result.success(it) }

            val response = client
                .from("users")
                .select {
                    filter {
                        eq("username", username)
                    }
                }
                .decodeList<JsonObject>()

            val isAvailable = response.isEmpty()
            usernameCache.put(username, isAvailable)

            Result.success(isAvailable)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearCache() {
        usernameCache.evictAll()
    }
}
