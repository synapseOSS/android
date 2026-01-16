package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.net.Uri
import com.synapse.social.studioasinc.core.util.FileManager
import com.synapse.social.studioasinc.core.storage.ImageUploader
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.coroutines.resume

/**
 * Manager for uploading media files to ImgBB
 */
object MediaUploadManager {
    
    /**
     * Uploads multiple media items to ImgBB
     */
    suspend fun uploadMultipleMedia(
        context: Context,
        mediaItems: List<MediaItem>,
        onProgress: (Float) -> Unit,
        onComplete: (List<MediaItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val uploadedItems = mutableListOf<MediaItem>()
                
                mediaItems.forEachIndexed { index, mediaItem ->
                    if (mediaItem.type == MediaType.IMAGE) {
                        try {
                            val filePath = getFilePathFromUri(context, mediaItem.url)
                            if (filePath != null) {
                                val imgbbUrl = uploadToImgBB(context, filePath)
                                val uploadedItem = mediaItem.copy(
                                    id = UUID.randomUUID().toString(),
                                    url = imgbbUrl,
                                    mimeType = "image/jpeg"
                                )
                                uploadedItems.add(uploadedItem)
                                android.util.Log.d("MediaUpload", "Uploaded image: $imgbbUrl")
                            } else {
                                android.util.Log.w("MediaUpload", "Cannot get file path: ${mediaItem.url}")
                                uploadedItems.add(mediaItem)
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("MediaUpload", "Failed to upload ${mediaItem.url}: ${e.message}")
                            uploadedItems.add(mediaItem)
                        }
                    } else {
                        // Videos not supported by ImgBB, keep original URL
                        uploadedItems.add(mediaItem)
                    }
                    
                    val progress = (index + 1).toFloat() / mediaItems.size
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    onComplete(uploadedItems)
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaUpload", "Media upload failed", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Upload failed")
                }
            }
        }
    }
    
    private suspend fun uploadToImgBB(context: Context, filePath: String): String = suspendCancellableCoroutine { continuation ->
        ImageUploader.uploadImage(context, filePath, object : ImageUploader.UploadCallback {
            override fun onUploadComplete(imageUrl: String) {
                continuation.resume(imageUrl)
            }
            
            override fun onUploadError(errorMessage: String) {
                continuation.cancel(Exception(errorMessage))
            }
        })
    }
    
    private fun getFilePathFromUri(context: Context, uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            when {
                uri.scheme == "content" -> FileManager.getPathFromUri(context, uri)
                uri.scheme == "file" -> uri.path
                else -> {
                    val file = File(uriString)
                    if (file.exists()) file.absolutePath else null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaUpload", "Error getting file path", e)
            null
        }
    }
}
