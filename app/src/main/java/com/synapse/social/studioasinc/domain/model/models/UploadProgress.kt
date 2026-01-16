package com.synapse.social.studioasinc.domain.model.models

/**
 * Represents the progress state of a file upload operation.
 * Used to track individual upload progress for UI updates.
 */
data class UploadProgress(
    val uploadId: String,
    val fileName: String,
    val progress: Float,  // 0.0 to 1.0
    val bytesUploaded: Long,
    val totalBytes: Long,
    val state: UploadState,
    val error: String? = null,
    val result: MediaUploadResult? = null
)

/**
 * Represents the different states of an upload operation.
 */
enum class UploadState {
    QUEUED,
    COMPRESSING,
    UPLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
