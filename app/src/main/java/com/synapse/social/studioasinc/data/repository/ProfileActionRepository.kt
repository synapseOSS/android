package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileActionRepository {
    private val supabase = SupabaseClient.client

    suspend fun lockProfile(userId: String, isLocked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("profiles")
                .update(mapOf("is_private" to isLocked)) {
                    filter { eq("id", userId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveProfile(userId: String, isArchived: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("profiles")
                .update(mapOf("is_archived" to isArchived)) {
                    filter { eq("id", userId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("blocked_users")
                .insert(mapOf(
                    "user_id" to userId,
                    "blocked_user_id" to blockedUserId
                ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportUser(userId: String, reportedUserId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("user_reports")
                .insert(mapOf(
                    "reporter_id" to userId,
                    "reported_user_id" to reportedUserId,
                    "reason" to reason
                ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun muteUser(userId: String, mutedUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("muted_users")
                .insert(mapOf(
                    "user_id" to userId,
                    "muted_user_id" to mutedUserId
                ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
