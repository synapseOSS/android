package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

/**
 * Represents a media item (image or video) for upload
 */
@Serializable
data class MediaItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val type: MediaType,
    val thumbnailUrl: String? = null,
    val duration: Long? = null,
    val size: Long? = null,
    val mimeType: String? = null,
    @Transient
    val likesCount: Int = 0,
    @Transient
    val commentsCount: Int = 0,
    @Transient
    val userHasLiked: Boolean = false
)

/**
 * Types of media supported
 */
@Serializable
enum class MediaType {
    IMAGE,
    VIDEO
}
