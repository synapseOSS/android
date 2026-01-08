package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Media type for story content
 */
@Serializable
enum class StoryMediaType {
    @SerialName("photo")
    PHOTO,
    @SerialName("video")
    VIDEO
}

/**
 * Privacy setting for story visibility
 */
@Serializable
enum class StoryPrivacy {
    @SerialName("all_friends")
    ALL_FRIENDS,
    @SerialName("public")
    PUBLIC
}

/**
 * Represents a single story segment (one photo or video in a user's story)
 */
@Serializable
data class Story(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("media_type")
    val mediaType: StoryMediaType? = null,
    val content: String? = null,
    val duration: Int? = null,
    @SerialName("duration_hours")
    val durationHours: Int? = null,
    @SerialName("privacy_setting")
    val privacy: StoryPrivacy? = null,
    @SerialName("views_count")
    val viewCount: Int? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("media_width")
    val mediaWidth: Int? = null,
    @SerialName("media_height")
    val mediaHeight: Int? = null,
    @SerialName("media_duration_seconds")
    val mediaDurationSeconds: Int? = null,
    @SerialName("file_size_bytes")
    val fileSizeBytes: Long? = null,
    @SerialName("reactions_count")
    val reactionsCount: Int? = null,
    @SerialName("replies_count")
    val repliesCount: Int? = null,
    @SerialName("is_reported")
    val isReported: Boolean? = null,
    @SerialName("moderation_status")
    val moderationStatus: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null
) {
    /**
     * Returns the effective media URL
     */
    fun getEffectiveMediaUrl(): String? {
        return mediaUrl
    }
    
    /**
     * Returns the effective duration for display
     */
    fun getDisplayDuration(): Int {
        return when {
            mediaType == StoryMediaType.VIDEO && mediaDurationSeconds != null -> mediaDurationSeconds
            duration != null -> duration
            else -> 5
        }
    }
}
