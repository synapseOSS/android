package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Repository for submitting user feedback.
 * Requirement: 9.5
 */
class FeedbackRepository {
    private val client = SupabaseClient.client

    @Serializable
    data class Feedback(
        @SerialName("user_id") val userId: String,
        val category: String,
        val description: String,
        @SerialName("app_version") val appVersion: String,
        @SerialName("build_number") val buildNumber: String,
        @SerialName("device_info") val deviceInfo: String
    )

    /**
     * Submit user feedback.
     */
    suspend fun submitFeedback(
        category: String,
        description: String,
        appVersion: String,
        buildNumber: String,
        deviceInfo: String
    ): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))

        client.from("feedback")
            .insert(Feedback(
                userId = userId,
                category = category,
                description = description,
                appVersion = appVersion,
                buildNumber = buildNumber,
                deviceInfo = deviceInfo
            ))

        Log.d(TAG, "Feedback submitted: category=$category")
        Unit
    }

    companion object {
        private const val TAG = "FeedbackRepository"
    }
}
