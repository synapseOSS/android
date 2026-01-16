package com.synapse.social.studioasinc.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoryCreateRequest(
    @SerialName("user_id")
    val userId: String,
    @SerialName("media_url")
    val mediaUrl: String?,
    @SerialName("media_type")
    val mediaType: String,
    @SerialName("privacy_setting")
    val privacySetting: String,
    @SerialName("duration")
    val duration: Int?,
    @SerialName("duration_hours")
    val durationHours: Int,
    @SerialName("media_duration_seconds")
    val mediaDurationSeconds: Int?,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("reactions_count")
    val reactionsCount: Int,
    @SerialName("replies_count")
    val repliesCount: Int,
    @SerialName("is_reported")
    val isReported: Boolean,
    @SerialName("moderation_status")
    val moderationStatus: String
)
