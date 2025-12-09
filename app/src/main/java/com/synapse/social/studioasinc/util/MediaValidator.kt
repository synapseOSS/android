package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.synapse.social.studioasinc.model.models.MediaMetadata

/**
 * MediaValidator validates media files for type and size constraints.
 * Ensures files meet requirements before upload processing.
 */
class MediaValidator(private val context: Context) {
    
    companion object {
        // File size limits (in bytes)
        private const val MAX_IMAGE_SIZE = 2 * 1024 * 1024L // 2MB after compression
        private const val MAX_VIDEO_SIZE = 100 * 1024 * 1024L // 100MB
        private const val MAX_AUDIO_SIZE = 20 * 1024 * 1024L // 20MB
        private const val MAX_DOCUMENT_SIZE = 50 * 1024 * 1024L // 50MB
        
        // Allowed MIME types for each media category
        private val ALLOWED_IMAGE_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
        )
        
        private val ALLOWED_VIDEO_TYPES = setOf(
            "video/mp4",
            "video/quicktime",
            "video/x-msvideo",
            "video/x-matroska"
        )
        
        private val ALLOWED_AUDIO_TYPES = setOf(
            "audio/mpeg",
            "audio/wav",
            "audio/mp4",
            "audio/ogg",
            "audio/x-m4a"
        )
        
        private val ALLOWED_DOCUMENT_TYPES = setOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain"
        )
    }
    
    /**
     * Media type categories for validation.
     */
    enum class MediaType {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT
    }
    
    /**
     * Validation error types with descriptive messages.
     */
    sealed class ValidationError(message: String) : Exception(message) {
        class FileTooLarge(val actualSize: Long, val maxSize: Long, val mediaType: MediaType) : 
            ValidationError("File size (${formatFileSize(actualSize)}) exceeds maximum allowed size (${formatFileSize(maxSize)}) for ${mediaType.name.lowercase()}")
        
        class UnsupportedFormat(val mimeType: String, val mediaType: MediaType) : 
            ValidationError("Unsupported file format: $mimeType for ${mediaType.name.lowercase()}. Please select a supported file type.")
        
        class InvalidFile(val reason: String) : 
            ValidationError("Invalid file: $reason")
        
        class UnknownMimeType : 
            ValidationError("Unable to determine file type. Please select a valid media file.")
        
        companion object {
            private fun formatFileSize(bytes: Long): String {
                return when {
                    bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
                    bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
                    else -> "$bytes bytes"
                }
            }
        }
    }
    
    /**
     * Validates a file for the specified media type.
     * Checks MIME type and file size constraints.
     * 
     * @param uri The URI of the file to validate
     * @param expectedType The expected media type category
     * @return Result containing MediaMetadata if valid, or ValidationError if invalid
     */
    fun validateFile(uri: Uri, expectedType: MediaType): Result<MediaMetadata> {
        return try {
            // Extract file metadata
            val fileName = getFileNameFromUri(uri)
                ?: return Result.failure(ValidationError.InvalidFile("Unable to read file name"))
            
            val fileSize = getFileSizeFromUri(uri)
                ?: return Result.failure(ValidationError.InvalidFile("Unable to determine file size"))
            
            val mimeType = getMimeTypeFromUri(uri)
                ?: return Result.failure(ValidationError.UnknownMimeType())
            
            // Validate MIME type
            val allowedTypes = getAllowedTypesForCategory(expectedType)
            if (!allowedTypes.contains(mimeType.lowercase())) {
                return Result.failure(ValidationError.UnsupportedFormat(mimeType, expectedType))
            }
            
            // Validate file size
            val maxSize = getMaxSizeForType(expectedType)
            if (fileSize > maxSize) {
                return Result.failure(ValidationError.FileTooLarge(fileSize, maxSize, expectedType))
            }
            
            // Create and return metadata
            val metadata = MediaMetadata(
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType
            )
            
            Result.success(metadata)
            
        } catch (e: Exception) {
            Result.failure(ValidationError.InvalidFile(e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Validates an image file.
     * Images must be max 2MB after compression.
     * 
     * @param uri The URI of the image to validate
     * @return Result containing MediaMetadata if valid, or ValidationError if invalid
     */
    fun validateImage(uri: Uri): Result<MediaMetadata> {
        return validateFile(uri, MediaType.IMAGE)
    }
    
    /**
     * Validates a video file.
     * Videos must be max 100MB.
     * 
     * @param uri The URI of the video to validate
     * @return Result containing MediaMetadata if valid, or ValidationError if invalid
     */
    fun validateVideo(uri: Uri): Result<MediaMetadata> {
        return validateFile(uri, MediaType.VIDEO)
    }
    
    /**
     * Validates an audio file.
     * Audio files must be max 20MB.
     * 
     * @param uri The URI of the audio to validate
     * @return Result containing MediaMetadata if valid, or ValidationError if invalid
     */
    fun validateAudio(uri: Uri): Result<MediaMetadata> {
        return validateFile(uri, MediaType.AUDIO)
    }
    
    /**
     * Validates a document file.
     * Documents must be max 50MB.
     * 
     * @param uri The URI of the document to validate
     * @return Result containing MediaMetadata if valid, or ValidationError if invalid
     */
    fun validateDocument(uri: Uri): Result<MediaMetadata> {
        return validateFile(uri, MediaType.DOCUMENT)
    }
    
    /**
     * Checks if a MIME type is supported for any media category.
     * 
     * @param mimeType The MIME type to check
     * @return true if supported, false otherwise
     */
    fun isSupportedMimeType(mimeType: String): Boolean {
        val normalizedType = mimeType.lowercase()
        return ALLOWED_IMAGE_TYPES.contains(normalizedType) ||
                ALLOWED_VIDEO_TYPES.contains(normalizedType) ||
                ALLOWED_AUDIO_TYPES.contains(normalizedType) ||
                ALLOWED_DOCUMENT_TYPES.contains(normalizedType)
    }
    
    /**
     * Determines the media type category from a MIME type.
     * 
     * @param mimeType The MIME type to categorize
     * @return MediaType category or null if unsupported
     */
    fun getMediaTypeFromMimeType(mimeType: String): MediaType? {
        val normalizedType = mimeType.lowercase()
        return when {
            ALLOWED_IMAGE_TYPES.contains(normalizedType) -> MediaType.IMAGE
            ALLOWED_VIDEO_TYPES.contains(normalizedType) -> MediaType.VIDEO
            ALLOWED_AUDIO_TYPES.contains(normalizedType) -> MediaType.AUDIO
            ALLOWED_DOCUMENT_TYPES.contains(normalizedType) -> MediaType.DOCUMENT
            else -> null
        }
    }
    
    /**
     * Gets the maximum allowed file size for a media type.
     * 
     * @param mediaType The media type category
     * @return Maximum file size in bytes
     */
    fun getMaxSizeForType(mediaType: MediaType): Long {
        return when (mediaType) {
            MediaType.IMAGE -> MAX_IMAGE_SIZE
            MediaType.VIDEO -> MAX_VIDEO_SIZE
            MediaType.AUDIO -> MAX_AUDIO_SIZE
            MediaType.DOCUMENT -> MAX_DOCUMENT_SIZE
        }
    }
    
    /**
     * Gets the allowed MIME types for a media category.
     * 
     * @param mediaType The media type category
     * @return Set of allowed MIME types
     */
    private fun getAllowedTypesForCategory(mediaType: MediaType): Set<String> {
        return when (mediaType) {
            MediaType.IMAGE -> ALLOWED_IMAGE_TYPES
            MediaType.VIDEO -> ALLOWED_VIDEO_TYPES
            MediaType.AUDIO -> ALLOWED_AUDIO_TYPES
            MediaType.DOCUMENT -> ALLOWED_DOCUMENT_TYPES
        }
    }
    
    /**
     * Extracts the file name from a URI.
     * 
     * @param uri The URI to extract from
     * @return File name or null if unable to determine
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            cursor.getString(nameIndex)
                        } else null
                    }
                }
                "file" -> {
                    uri.lastPathSegment
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extracts the file size from a URI.
     * 
     * @param uri The URI to extract from
     * @return File size in bytes or null if unable to determine
     */
    private fun getFileSizeFromUri(uri: Uri): Long? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1 && cursor.moveToFirst()) {
                            cursor.getLong(sizeIndex)
                        } else null
                    }
                }
                "file" -> {
                    uri.path?.let { path ->
                        java.io.File(path).length()
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extracts the MIME type from a URI.
     * 
     * @param uri The URI to extract from
     * @return MIME type or null if unable to determine
     */
    private fun getMimeTypeFromUri(uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.getType(uri)
                }
                "file" -> {
                    val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                    if (extension.isNotEmpty()) {
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                    } else null
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
