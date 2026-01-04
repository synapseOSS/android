package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.format.DateTimeFormatter

interface StoryRepository {
    suspend fun hasActiveStory(userId: String): Result<Boolean>
}

class StoryRepositoryImpl : StoryRepository {
    private val client = SupabaseClient.client

    override suspend fun hasActiveStory(userId: String): Result<Boolean> = try {
        // We need to check if there are any stories for the user that expire in the future.
        // Assuming 'expires_at' is a timestamp in ISO 8601 format or similar that Supabase handles.
        // We use 'gt' (greater than) operator to filter stories where expires_at > now.

        val now = Instant.now().toString()

        val count = client.from("stories").select {
            filter {
                eq("user_id", userId)
                gt("expires_at", now)
            }
            count(io.github.jan.supabase.postgrest.query.Count.EXACT)
        }.countOrNull() ?: 0

        Result.success(count > 0)
    } catch (e: Exception) {
        // If the table doesn't exist or query fails, we can return false or propagate the error.
        // For 'hasStory' UI check, returning false on error is often a safe fallback,
        // but here we return failure so the ViewModel can decide.
        Result.failure(e)
    }
}
