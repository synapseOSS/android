package com.synapse.social.studioasinc.chat.service

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import com.synapse.social.studioasinc.backend.SupabaseStorageService
import com.synapse.social.studioasinc.model.models.MediaMetadata
import com.synapse.social.studioasinc.model.models.MediaUploadResult
import com.synapse.social.studioasinc.model.models.UploadProgress
import com.synapse.social.studioasinc.model.models.UploadState
import com.synapse.social.studioasinc.core.util.ImageCompressor
import com.synapse.social.studioasinc.core.util.ThumbnailGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * MediaUploadManager orchestrates file uploads with compression, thumbnail generation, and progress tracking.
 * Manages concurrent uploads and provides real-time progress updates via Flow.
 */
class MediaUploadManager(
    private val context: Context,
    private val storageService: SupabaseStorageService,
    private val imageCompressor: ImageCompressor,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    
    companion object {
        private const val TAG = "MediaUploadManager"
        private const val MAX_CONCURRENT_UPLOADS = 3
        
        // File size limits (in bytes)
        private const val MAX_IMAGE_SIZE = 2 * 1024 * 1024L // 2MB after compression
        private const val MAX_VIDEO_SIZE = 100 * 1024 * 1024L // 100MB
        private const val MAX_AUDIO_SIZE = 20 * 1024 * 1024L // 20MB
        private const val MAX_DOCUMENT_SIZE = 50 * 1024 * 1024L // 50MB
        
        // Supported MIME types
        private val SUPPORTED_IMAGE_TYPES = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
        )
        private val SUPPORTED_VIDEO_TYPES = setOf(
            "video/mp4", "video/quicktime", "video/x-msvideo", "video/x-matroska"
        )
        private val SUPPORTED_AUDIO_TYPES = setOf(
            "audio/mpeg", "audio/wav", "audio/mp4", "audio/ogg", "audio/x-m4a"
        )
        private val SUPPORTED_DOCUMENT_TYPES = setOf(
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
    
    // Upload queue and progress tracking
    private val uploadQueue = Channel<UploadTask>(Channel.UNLIMITED)
    private val activeUploads = ConcurrentHashMap<String, UploadTask>()
    private val _progressFlow = MutableSharedFlow<UploadProgress>()
    val progressFlow: Flow<UploadProgress> = _progressFlow.asSharedFlow()
    
    // Upload task data class
    private data class UploadTask(
        val uploadId: String,
        val uri: Uri,
        val chatId: String,
        val mediaType: MediaType,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String
    )
    
    private enum class MediaType {
        IMAGE, VIDEO, AUDIO, DOCUMENT
    }
    
    init {
        // Start upload workers
        repeat(MAX_CONCURRENT_UPLOADS) { workerId ->
            coroutineScope.launch {
                processUploadQueue(workerId)
            }
        }
    }
    
    /**
     * Uploads an image with compression and thumbnail generation.
     * 
     * @param uri The URI of the image to upload
     * @param chatId The chat ID for storage organization
     * @return Result containing MediaUploadResult or error
     */
    suspend fun uploadImage(uri: Uri, chatId: String): Result<MediaUploadResult> = withContext(Dispatchers.IO) {
        try {
            val metadata = extractMediaMetadata(uri, MediaType.IMAGE)
                .getOrElse { return@withContext Result.failure(it) }
            
            val uploadId = UUID.randomUUID().toString()
            
            // Emit initial progress
            emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.COMPRESSING)
            
            // Compress image
            val compressedFile = imageCompressor.compress(uri)
                .getOrElse { 
                    emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.FAILED, it.message)
                    return@withContext Result.failure(it) 
                }
            
            emitProgress(uploadId, metadata.fileName, 0.3f, 0L, metadata.fileSize, UploadState.COMPRESSING)
            
            // Generate thumbnail
            val thumbnailFile = thumbnailGenerator.generateImageThumbnail(uri)
                .getOrElse { 
                    compressedFile.delete()
                    emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.FAILED, it.message)
                    return@withContext Result.failure(it) 
                }
            
            emitProgress(uploadId, metadata.fileName, 0.5f, 0L, metadata.fileSize, UploadState.UPLOADING)
            
            // Upload main file
            val mainPath = storageService.generateStoragePath(chatId, metadata.fileName)
            val mainUrl = storageService.uploadFile(compressedFile, mainPath) { progress ->
                val totalProgress = 0.5f + (progress * 0.4f) // 50% to 90%
                coroutineScope.launch {
                    emitProgress(uploadId, metadata.fileName, totalProgress, 
                        (progress * compressedFile.length()).toLong(), compressedFile.length(), UploadState.UPLOADING)
                }
            }.getOrElse { 
                    compressedFile.delete()
                    thumbnailFile.delete()
                    emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.FAILED, it.message)
                    return@withContext Result.failure(it) 
                }
                
                // Upload thumbnail
                val thumbnailPath = storageService.generateStoragePath(chatId, "thumb_${metadata.fileName}")
                val thumbnailUrl = storageService.uploadFile(thumbnailFile, thumbnailPath) { progress ->
                    val totalProgress = 0.9f + (progress * 0.1f) // 90% to 100%
                    coroutineScope.launch {
                        emitProgress(uploadId, metadata.fileName, totalProgress, 
                            compressedFile.length(), compressedFile.length(), UploadState.UPLOADING)
                    }
                }.getOrElse { 
                    // Main file uploaded successfully, but thumbnail failed - continue without thumbnail
                    android.util.Log.w(TAG, "Thumbnail upload failed: ${it.message}")
                    null
                }
                
                // Get image dimensions from compressed file
                val dimensions = getImageDimensions(compressedFile)
                
                // Clean up temporary files
                compressedFile.delete()
                thumbnailFile.delete()
                
                val result = MediaUploadResult(
                    url = mainUrl,
                    thumbnailUrl = thumbnailUrl,
                    fileName = metadata.fileName,
                    fileSize = compressedFile.length(),
                    mimeType = metadata.mimeType,
                    width = dimensions?.first,
                    height = dimensions?.second
                )

                emitProgress(uploadId, metadata.fileName, 1.0f, metadata.fileSize, metadata.fileSize, UploadState.COMPLETED, result = result)
                
                Result.success(result)
                
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Image upload failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Uploads a video with thumbnail generation.
     * 
     * @param uri The URI of the video to upload
     * @param chatId The chat ID for storage organization
     * @return Result containing MediaUploadResult or error
     */
    suspend fun uploadVideo(uri: Uri, chatId: String): Result<MediaUploadResult> {
        return withContext(Dispatchers.IO) {
            try {
                val metadata = extractMediaMetadata(uri, MediaType.VIDEO)
                    .getOrElse { return@withContext Result.failure(it) }
                
                val uploadId = UUID.randomUUID().toString()
                
                // Emit initial progress
                emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.COMPRESSING)
                
                // Generate thumbnail
                val thumbnailFile = thumbnailGenerator.generateVideoThumbnail(uri)
                    .getOrElse { 
                        emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.FAILED, it.message)
                        return@withContext Result.failure(it) 
                    }
                
                emitProgress(uploadId, metadata.fileName, 0.2f, 0L, metadata.fileSize, UploadState.UPLOADING)
                
                // Copy video file to temp location for upload
                val videoFile = copyUriToTempFile(uri, "video_${UUID.randomUUID()}")
                    .getOrElse { 
                        thumbnailFile.delete()
                        emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.FAILED, it.message)
                        return@withContext Result.failure(it) 
                    }
                
                // Upload main video file
                val mainPath = storageService.generateStoragePath(chatId, metadata.fileName)
                val mainUrl = storageService.uploadFile(videoFile, mainPath) { progress ->
                    val totalProgress = 0.2f + (progress * 0.7f) // 20% to 90%
                    coroutineScope.launch {
                        emitProgress(uploadId, metadata.fileName, totalProgress, 
                            (progress * videoFile.length()).toLong(), videoFile.length(), UploadState.UPLOADING)
                    }
                }.getOrElse { 
                    videoFile.delete()
                    thumbnailFile.delete()
                    emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.FAILED, it.message)
                    return@withContext Result.failure(it) 
                }
                
                // Upload thumbnail
                val thumbnailPath = storageService.generateStoragePath(chatId, "thumb_${metadata.fileName}")
                val thumbnailUrl = storageService.uploadFile(thumbnailFile, thumbnailPath) { progress ->
                    val totalProgress = 0.9f + (progress * 0.1f) // 90% to 100%
                    coroutineScope.launch {
                        emitProgress(uploadId, metadata.fileName, totalProgress, 
                            videoFile.length(), videoFile.length(), UploadState.UPLOADING)
                    }
                }.getOrElse { 
                    // Main file uploaded successfully, but thumbnail failed - continue without thumbnail
                    android.util.Log.w(TAG, "Video thumbnail upload failed: ${it.message}")
                    null
                }
                
                // Get video metadata
                val videoMetadata = getVideoMetadata(uri)
                
                // Clean up temporary files
                videoFile.delete()
                thumbnailFile.delete()
                
                val result = MediaUploadResult(
                    url = mainUrl,
                    thumbnailUrl = thumbnailUrl,
                    fileName = metadata.fileName,
                    fileSize = metadata.fileSize,
                    mimeType = metadata.mimeType,
                    width = videoMetadata?.width,
                    height = videoMetadata?.height,
                    duration = videoMetadata?.duration
                )

                emitProgress(uploadId, metadata.fileName, 1.0f, metadata.fileSize, metadata.fileSize, UploadState.COMPLETED, result = result)
                
                Result.success(result)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Video upload failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Uploads an audio file with metadata extraction.
     * 
     * @param uri The URI of the audio to upload
     * @param chatId The chat ID for storage organization
     * @return Result containing MediaUploadResult or error
     */
    suspend fun uploadAudio(uri: Uri, chatId: String): Result<MediaUploadResult> {
        return withContext(Dispatchers.IO) {
            try {
                val metadata = extractMediaMetadata(uri, MediaType.AUDIO)
                    .getOrElse { return@withContext Result.failure(it) }
                
                val uploadId = UUID.randomUUID().toString()
                
                // Emit initial progress
                emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.UPLOADING)
                
                // Copy audio file to temp location for upload
                val audioFile = copyUriToTempFile(uri, "audio_${UUID.randomUUID()}")
                    .getOrElse { 
                        emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.FAILED, it.message)
                        return@withContext Result.failure(it) 
                    }
                
                // Upload audio file
                val mainPath = storageService.generateStoragePath(chatId, metadata.fileName)
                val mainUrl = storageService.uploadFile(audioFile, mainPath) { progress ->
                    coroutineScope.launch {
                        emitProgress(uploadId, metadata.fileName, progress, 
                            (progress * audioFile.length()).toLong(), audioFile.length(), UploadState.UPLOADING)
                    }
                }.getOrElse { 
                    audioFile.delete()
                    emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.FAILED, it.message)
                    return@withContext Result.failure(it) 
                }
                
                // Get audio metadata
                val audioMetadata = getAudioMetadata(uri)
                
                // Clean up temporary file
                audioFile.delete()
                
                val result = MediaUploadResult(
                    url = mainUrl,
                    thumbnailUrl = null,
                    fileName = metadata.fileName,
                    fileSize = metadata.fileSize,
                    mimeType = metadata.mimeType,
                    duration = audioMetadata?.duration
                )

                emitProgress(uploadId, metadata.fileName, 1.0f, metadata.fileSize, metadata.fileSize, UploadState.COMPLETED, result = result)
                
                Result.success(result)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Audio upload failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Uploads a document with validation.
     * 
     * @param uri The URI of the document to upload
     * @param chatId The chat ID for storage organization
     * @return Result containing MediaUploadResult or error
     */
    suspend fun uploadDocument(uri: Uri, chatId: String): Result<MediaUploadResult> {
        return withContext(Dispatchers.IO) {
            try {
                val metadata = extractMediaMetadata(uri, MediaType.DOCUMENT)
                    .getOrElse { return@withContext Result.failure(it) }
                
                val uploadId = UUID.randomUUID().toString()
                
                // Emit initial progress
                emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.UPLOADING)
                
                // Copy document file to temp location for upload
                val documentFile = copyUriToTempFile(uri, "doc_${UUID.randomUUID()}")
                    .getOrElse { 
                        emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.FAILED, it.message)
                        return@withContext Result.failure(it) 
                    }
                
                // Upload document file
                val mainPath = storageService.generateStoragePath(chatId, metadata.fileName)
                val mainUrl = storageService.uploadFile(documentFile, mainPath) { progress ->
                    coroutineScope.launch {
                        emitProgress(uploadId, metadata.fileName, progress, 
                            (progress * documentFile.length()).toLong(), documentFile.length(), UploadState.UPLOADING)
                    }
                }.getOrElse { 
                    documentFile.delete()
                    emitProgress(uploadId, metadata.fileName, 0.0f, 0L, metadata.fileSize, UploadState.FAILED, it.message)
                    return@withContext Result.failure(it) 
                }
                
                // Clean up temporary file
                documentFile.delete()
                
                val result = MediaUploadResult(
                    url = mainUrl,
                    thumbnailUrl = null,
                    fileName = metadata.fileName,
                    fileSize = metadata.fileSize,
                    mimeType = metadata.mimeType
                )

                emitProgress(uploadId, metadata.fileName, 1.0f, metadata.fileSize, metadata.fileSize, UploadState.COMPLETED, result = result)
                
                Result.success(result)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Document upload failed", e)
                Result.failure(e)
            }
        }
    }    
   
 /**
     * Uploads multiple files concurrently with progress tracking.
     * 
     * @param uris List of URIs to upload
     * @param chatId The chat ID for storage organization
     * @return Flow of UploadProgress for each file
     */
    suspend fun uploadMultiple(uris: List<Uri>, chatId: String): Flow<UploadProgress> = flow {
        if (uris.isEmpty()) return@flow
        
        // Create upload tasks for each URI
        val tasks = mutableListOf<UploadTask>()
        
        for (uri in uris) {
            try {
                val mediaType = determineMediaType(uri)
                val metadata = extractMediaMetadata(uri, mediaType).getOrThrow()
                
                val task = UploadTask(
                    uploadId = UUID.randomUUID().toString(),
                    uri = uri,
                    chatId = chatId,
                    mediaType = mediaType,
                    fileName = metadata.fileName,
                    fileSize = metadata.fileSize,
                    mimeType = metadata.mimeType
                )
                
                tasks.add(task)
                activeUploads[task.uploadId] = task
                
                // Emit initial queued state
                val progress = UploadProgress(
                    uploadId = task.uploadId,
                    fileName = task.fileName,
                    progress = 0.0f,
                    bytesUploaded = 0L,
                    totalBytes = task.fileSize,
                    state = UploadState.QUEUED
                )
                emit(progress)
                
            } catch (e: Exception) {
                val uploadId = UUID.randomUUID().toString()
                val fileName = getFileNameFromUri(uri) ?: "unknown"
                val progress = UploadProgress(
                    uploadId = uploadId,
                    fileName = fileName,
                    progress = 0.0f,
                    bytesUploaded = 0L,
                    totalBytes = 0L,
                    state = UploadState.FAILED,
                    error = e.message
                )
                emit(progress)
            }
        }
        
        // Queue tasks for processing
        for (task in tasks) {
            uploadQueue.trySend(task)
        }
        
        // Collect progress updates from the shared flow
        progressFlow.collect { progress ->
            if (tasks.any { it.uploadId == progress.uploadId }) {
                emit(progress)
                
                // Remove completed/failed uploads from active list
                if (progress.state == UploadState.COMPLETED || 
                    progress.state == UploadState.FAILED || 
                    progress.state == UploadState.CANCELLED) {
                    activeUploads.remove(progress.uploadId)
                }
            }
        }
    }
    
    /**
     * Cancels an ongoing upload operation.
     * Cleans up partial uploads from storage and updates UI state.
     * 
     * @param uploadId The ID of the upload to cancel
     */
    fun cancelUpload(uploadId: String) {
        activeUploads[uploadId]?.let { task ->
            activeUploads.remove(uploadId)
            
            coroutineScope.launch {
                // Clean up any partial uploads from storage
                try {
                    val mainPath = storageService.generateStoragePath(task.chatId, task.fileName)
                    val thumbnailPath = storageService.generateStoragePath(task.chatId, "thumb_${task.fileName}")
                    
                    // Attempt to delete partial uploads (ignore failures as files might not exist)
                    storageService.deleteFile(mainPath)
                    storageService.deleteFile(thumbnailPath)
                    
                    android.util.Log.d(TAG, "Cleaned up partial uploads for cancelled upload: ${task.fileName}")
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Failed to clean up partial uploads for ${task.fileName}: ${e.message}")
                }
                
                // Update UI state to reflect cancellation
                emitProgress(
                    uploadId = uploadId,
                    fileName = task.fileName,
                    progress = 0.0f,
                    bytesUploaded = 0L,
                    totalBytes = task.fileSize,
                    state = UploadState.CANCELLED
                )
            }
        }
    }
    
    /**
     * Processes the upload queue with concurrent workers.
     */
    private suspend fun processUploadQueue(workerId: Int) {
        for (task in uploadQueue) {
            // Check if upload was cancelled
            if (!activeUploads.containsKey(task.uploadId)) {
                continue
            }
            
            try {
                android.util.Log.d(TAG, "Worker $workerId processing upload: ${task.fileName}")
                
                val result = when (task.mediaType) {
                    MediaType.IMAGE -> uploadImage(task.uri, task.chatId)
                    MediaType.VIDEO -> uploadVideo(task.uri, task.chatId)
                    MediaType.AUDIO -> uploadAudio(task.uri, task.chatId)
                    MediaType.DOCUMENT -> uploadDocument(task.uri, task.chatId)
                }
                
                if (result.isFailure) {
                    emitProgress(
                        uploadId = task.uploadId,
                        fileName = task.fileName,
                        progress = 0.0f,
                        bytesUploaded = 0L,
                        totalBytes = task.fileSize,
                        state = UploadState.FAILED,
                        error = result.exceptionOrNull()?.message
                    )
                } else {
                    // Success is already emitted by individual upload methods with result
                    // We just ensure we don't block here
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Upload processing failed for ${task.fileName}", e)
                emitProgress(
                    uploadId = task.uploadId,
                    fileName = task.fileName,
                    progress = 0.0f,
                    bytesUploaded = 0L,
                    totalBytes = task.fileSize,
                    state = UploadState.FAILED,
                    error = e.message
                )
            } finally {
                activeUploads.remove(task.uploadId)
            }
        }
    }
    
    /**
     * Emits progress update to the shared flow.
     */
    private suspend fun emitProgress(
        uploadId: String,
        fileName: String,
        progress: Float,
        bytesUploaded: Long,
        totalBytes: Long,
        state: UploadState,
        error: String? = null,
        result: MediaUploadResult? = null
    ) {
        val progressUpdate = UploadProgress(
            uploadId = uploadId,
            fileName = fileName,
            progress = progress,
            bytesUploaded = bytesUploaded,
            totalBytes = totalBytes,
            state = state,
            error = error,
            result = result
        )
        
        _progressFlow.emit(progressUpdate)
    }
    
    /**
     * Extracts metadata from a media URI.
     */
    private suspend fun extractMediaMetadata(uri: Uri, mediaType: MediaType): Result<MediaMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = getFileNameFromUri(uri) ?: "unknown_${UUID.randomUUID()}"
                val fileSize = getFileSizeFromUri(uri) ?: 0L
                val mimeType = getMimeTypeFromUri(uri) ?: "application/octet-stream"
                
                // Validate file size based on media type
                val maxSize = when (mediaType) {
                    MediaType.IMAGE -> MAX_IMAGE_SIZE
                    MediaType.VIDEO -> MAX_VIDEO_SIZE
                    MediaType.AUDIO -> MAX_AUDIO_SIZE
                    MediaType.DOCUMENT -> MAX_DOCUMENT_SIZE
                }
                
                if (fileSize > maxSize) {
                    return@withContext Result.failure(
                        IllegalArgumentException("File size ($fileSize bytes) exceeds maximum allowed size ($maxSize bytes) for ${mediaType.name.lowercase()}")
                    )
                }
                
                // Validate MIME type
                val supportedTypes = when (mediaType) {
                    MediaType.IMAGE -> SUPPORTED_IMAGE_TYPES
                    MediaType.VIDEO -> SUPPORTED_VIDEO_TYPES
                    MediaType.AUDIO -> SUPPORTED_AUDIO_TYPES
                    MediaType.DOCUMENT -> SUPPORTED_DOCUMENT_TYPES
                }
                
                if (!supportedTypes.contains(mimeType)) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Unsupported MIME type: $mimeType for ${mediaType.name.lowercase()}")
                    )
                }
                
                val metadata = MediaMetadata(
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType
                )
                
                Result.success(metadata)
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Determines the media type from a URI.
     */
    private fun determineMediaType(uri: Uri): MediaType {
        val mimeType = getMimeTypeFromUri(uri) ?: return MediaType.DOCUMENT
        
        return when {
            SUPPORTED_IMAGE_TYPES.contains(mimeType) -> MediaType.IMAGE
            SUPPORTED_VIDEO_TYPES.contains(mimeType) -> MediaType.VIDEO
            SUPPORTED_AUDIO_TYPES.contains(mimeType) -> MediaType.AUDIO
            SUPPORTED_DOCUMENT_TYPES.contains(mimeType) -> MediaType.DOCUMENT
            else -> MediaType.DOCUMENT
        }
    }
    
    /**
     * Gets the file name from a URI.
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            cursor.getString(nameIndex)
                        } else null
                    }
                }
                "file" -> {
                    File(uri.path ?: return null).name
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets the file size from a URI.
     */
    private fun getFileSizeFromUri(uri: Uri): Long? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1 && cursor.moveToFirst()) {
                            cursor.getLong(sizeIndex)
                        } else null
                    }
                }
                "file" -> {
                    File(uri.path ?: return null).length()
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets the MIME type from a URI.
     */
    private fun getMimeTypeFromUri(uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.getType(uri)
                }
                "file" -> {
                    val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Copies a URI to a temporary file for upload.
     */
    private suspend fun copyUriToTempFile(uri: Uri, prefix: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IllegalArgumentException("Cannot open input stream for URI: $uri"))
                
                val tempFile = File.createTempFile(prefix, null, context.cacheDir)
                
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                Result.success(tempFile)
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Gets image dimensions from a file.
     */
    private fun getImageDimensions(file: File): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets video metadata from a URI.
     */
    private fun getVideoMetadata(uri: Uri): VideoMetadata? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            
            retriever.release()
            
            VideoMetadata(width, height, duration)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets audio metadata from a URI.
     */
    private fun getAudioMetadata(uri: Uri): AudioMetadata? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            
            retriever.release()
            
            AudioMetadata(duration)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Data class for video metadata.
     */
    private data class VideoMetadata(
        val width: Int?,
        val height: Int?,
        val duration: Long?
    )
    
    /**
     * Data class for audio metadata.
     */
    private data class AudioMetadata(
        val duration: Long?
    )
}
