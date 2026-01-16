package com.synapse.social.studioasinc.domain.model.models

/**
 * Represents metadata information about a media file.
 * Used for validation and processing decisions.
 */
data class MediaMetadata(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
