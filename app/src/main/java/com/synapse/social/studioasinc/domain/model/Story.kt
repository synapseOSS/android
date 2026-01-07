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
    val mediaType: StoryMediaType = StoryMediaType.PHOTO,
    val content: String? = null,
    @SerialName("duration")
    val duration: Int = 5,
    @SerialName("privacy_setting")
    val privacy: StoryPrivacy = StoryPrivacy.ALL_FRIENDS,
    @SerialName("views_count")
    val viewCount: Int = 0,
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
        return if (mediaType == StoryMediaType.VIDEO && duration > 0) duration else 5
    }
}
