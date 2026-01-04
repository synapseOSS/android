package com.synapse.social.studioasinc.domain.model.models

/**
 * Represents the result of a successful media upload operation.
 * Contains all necessary information to create a ChatAttachment.
 */
data class MediaUploadResult(
    val url: String,
    val thumbnailUrl: String?,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Long? = null
)
