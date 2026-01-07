package com.synapse.social.studioasinc.data.remote.services

import android.content.Context
import android.net.Uri
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

/**
 * Supabase Storage Service
 * Handles file uploads to Supabase Storage including chat media with enhanced error handling and retry logic
 */
class SupabaseStorageService {
    
    companion object {
        private const val TAG = "SupabaseStorageService"
        private const val CHAT_MEDIA_BUCKET = "chat-media"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY_MS = 1000L
        private const val PROGRESS_UPDATE_INTERVAL_MS = 100L
    }
    
    private val client = SupabaseClient.client
    private val storage = client.storage
    
    /**
     * Upload avatar image to Supabase Storage
     * @param userId User ID for folder organization
     * @param filePath Local file path
     * @return Public URL of uploaded image
     */
    suspend fun uploadAvatar(userId: String, filePath: String): Result<String> {
        return uploadImage(SupabaseClient.BUCKET_USER_AVATARS, userId, filePath)
    }
    
    /**
     * Upload cover image to Supabase Storage
     * @param userId User ID for folder organization
     * @param filePath Local file path
     * @return Public URL of uploaded image
     */
    suspend fun uploadCover(userId: String, filePath: String): Result<String> {
        return uploadImage(SupabaseClient.BUCKET_USER_COVERS, userId, filePath)
    }
    
    /**
     * Upload post image to Supabase Storage
     * @param userId User ID for folder organization
     * @param filePath Local file path
     * @return Public URL of uploaded image
     */
    suspend fun uploadPostImage(userId: String, filePath: String): Result<String> {
        return uploadImage(SupabaseClient.BUCKET_POST_MEDIA, userId, filePath)
    }
    
    /**
     * Generic image upload function
     */
    private suspend fun uploadImage(bucket: String, userId: String, filePath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("SupabaseStorage", "Uploading image from: $filePath to bucket: $bucket")
                
                val file = File(filePath)
                if (!file.exists()) {
                    android.util.Log.e("SupabaseStorage", "File not found: $filePath")
                    return@withContext Result.failure(Exception("File not found: $filePath"))
                }
                
                if (file.length() == 0L) {
                    android.util.Log.e("SupabaseStorage", "File is empty: $filePath")
                    return@withContext Result.failure(Exception("File is empty: $filePath"))
                }
                
                val fileBytes = file.readBytes()
                android.util.Log.d("SupabaseStorage", "File size: ${fileBytes.size} bytes")
                
                val fileName = "${UUID.randomUUID()}.${file.extension}"
                val path = "$userId/$fileName"
                
                android.util.Log.d("SupabaseStorage", "Uploading to path: $path")
                
                // Upload to Supabase Storage with retry logic
                var uploadSuccess = false
                var lastException: Exception? = null
                
                for (attempt in 1..3) {
                    try {
                        storage.from(bucket).upload(path, fileBytes) { upsert = true }
                        uploadSuccess = true
                        break
                    } catch (e: Exception) {
                        lastException = e
                        android.util.Log.w("SupabaseStorage", "Upload attempt $attempt failed", e)
                        if (attempt < 3) {
                            delay(1000L * attempt) // Exponential backoff
                        }
                    }
                }
                
                if (!uploadSuccess) {
                    android.util.Log.e("SupabaseStorage", "Upload failed after 3 attempts")
                    return@withContext Result.failure(lastException ?: Exception("Upload failed after retries"))
                }
                
                // Get public URL
                val publicUrl = storage.from(bucket).publicUrl(path)
                
                if (publicUrl.isBlank()) {
                    android.util.Log.e("SupabaseStorage", "Failed to get public URL")
                    return@withContext Result.failure(Exception("Failed to get public URL"))
                }
                
                android.util.Log.d("SupabaseStorage", "Upload successful: $publicUrl")
                Result.success(publicUrl)
                
            } catch (e: Exception) {
                android.util.Log.e("SupabaseStorage", "Upload failed", e)
                Result.failure(Exception("Upload failed: ${e.message}"))
            }
        }
    }
    
    /**
     * Delete image from Supabase Storage
     */
    suspend fun deleteImage(bucket: String, path: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                storage.from(bucket).delete(path)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Upload file to chat media bucket with progress callback and retry logic
     * @param file File to upload
     * @param path Storage path for the file
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Public URL of uploaded file
     */
    suspend fun uploadFile(
        file: File,
        path: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            if (!file.exists()) {
                return@withContext Result.failure(StorageException.FileNotFound("File not found: ${file.path}"))
            }
            if (file.length() == 0L) {
                return@withContext Result.failure(StorageException.InvalidFile("File is empty: ${file.path}"))
            }
            val bytes = file.readBytes()
            uploadFileBytes(bytes, path, onProgress)
        }
    }

    /**
     * Upload byte array to chat media bucket
     */
    suspend fun uploadFileBytes(
        bytes: ByteArray,
        path: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            retryWithExponentialBackoff(
                maxAttempts = MAX_RETRY_ATTEMPTS,
                operation = "uploadFileBytes",
                block = {
                    uploadFileBytesInternal(bytes, path, onProgress)
                }
            )
        }
    }

    /**
     * Internal upload implementation with progress tracking
     */
    private suspend fun uploadFileBytesInternal(
        fileBytes: ByteArray,
        path: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        try {
            android.util.Log.d(TAG, "Uploading bytes to chat-media: $path (${fileBytes.size} bytes)")
            
            if (fileBytes.isEmpty()) {
                return Result.failure(StorageException.InvalidFile("File bytes are empty"))
            }
            
            // Simulate progress updates during upload
            onProgress(0.1f) // Start progress
            
            // Upload to chat-media bucket
            storage.from(CHAT_MEDIA_BUCKET).upload(path, fileBytes) { upsert = true }
            
            onProgress(0.9f) // Upload complete, getting URL
            
            // Get public URL
            val publicUrl = storage.from(CHAT_MEDIA_BUCKET).publicUrl(path)
            
            onProgress(1.0f) // Complete
            android.util.Log.d(TAG, "Upload successful: $publicUrl")
            
            return Result.success(publicUrl)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Upload failed for path: $path", e)
            return Result.failure(mapStorageException(e, "Upload failed"))
        }
    }
    
    /**
     * Download file from storage with retry logic
     * @param url File URL to download
     * @return File bytes
     */
    suspend fun downloadFile(url: String): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            retryWithExponentialBackoff(
                maxAttempts = MAX_RETRY_ATTEMPTS,
                operation = "downloadFile",
                block = {
                    downloadFileInternal(url)
                }
            )
        }
    }

    /**
     * Internal download implementation
     */
    private suspend fun downloadFileInternal(url: String): Result<ByteArray> {
        try {
            android.util.Log.d(TAG, "Downloading file: $url")
            
            if (url.isBlank()) {
                return Result.failure(StorageException.InvalidUrl("URL cannot be empty"))
            }
            
            // Extract path from URL
            val path = extractPathFromUrl(url, CHAT_MEDIA_BUCKET)
                ?: return Result.failure(StorageException.InvalidUrl("Invalid URL format: $url"))
            
            // Download from chat-media bucket
            val fileBytes = storage.from(CHAT_MEDIA_BUCKET).downloadAuthenticated(path)
            
            if (fileBytes.isEmpty()) {
                return Result.failure(StorageException.EmptyFile("Downloaded file is empty"))
            }
            
            android.util.Log.d(TAG, "Download successful: ${fileBytes.size} bytes")
            return Result.success(fileBytes)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Download failed for URL: $url", e)
            return Result.failure(mapStorageException(e, "Download failed"))
        }
    }
    
    /**
     * Delete file from storage with retry logic
     * @param path Storage path to delete
     */
    suspend fun deleteFile(path: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            retryWithExponentialBackoff(
                maxAttempts = MAX_RETRY_ATTEMPTS,
                operation = "deleteFile",
                block = {
                    deleteFileInternal(path)
                }
            )
        }
    }

    /**
     * Internal delete implementation
     */
    private suspend fun deleteFileInternal(path: String): Result<Unit> {
        try {
            android.util.Log.d(TAG, "Deleting file: $path")
            
            if (path.isBlank()) {
                return Result.failure(StorageException.InvalidPath("Path cannot be empty"))
            }
            
            storage.from(CHAT_MEDIA_BUCKET).delete(path)
            
            android.util.Log.d(TAG, "Delete successful: $path")
            return Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Delete failed for path: $path", e)
            return Result.failure(mapStorageException(e, "Delete failed"))
        }
    }
    
    /**
     * Get public URL for a file
     * @param path Storage path
     * @return Public URL
     */
    fun getPublicUrl(path: String): String {
        return storage.from(CHAT_MEDIA_BUCKET).publicUrl(path)
    }
    
    /**
     * Generate organized storage path for chat media
     * Format: chatId/YYYY/MM/DD/filename
     * @param chatId Chat identifier
     * @param fileName Original file name
     * @return Organized storage path
     */
    fun generateStoragePath(chatId: String, fileName: String): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
        
        // Generate unique filename with UUID
        val fileExtension = fileName.substringAfterLast(".", "")
        val uniqueFileName = "${UUID.randomUUID()}_${fileName.substringBeforeLast(".")}"
        val finalFileName = if (fileExtension.isNotEmpty()) {
            "$uniqueFileName.$fileExtension"
        } else {
            uniqueFileName
        }
        
        return "$chatId/$year/$month/$day/$finalFileName"
    }

    /**
     * Test storage infrastructure setup
     * @param context Application context for testing
     * @return Test results
     */
    suspend fun testStorageInfrastructure(context: Context): Result<String> {
        return try {
            Result.success("Storage infrastructure test removed")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error running storage infrastructure tests", e)
            Result.failure(e)
        }
    }

    /**
     * Extract path from public URL
     */
    fun extractPathFromUrl(url: String, bucket: String): String? {
        return try {
            val bucketPath = "/storage/v1/object/public/$bucket/"
            if (url.contains(bucketPath)) {
                url.substringAfter(bucketPath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Retry operation with exponential backoff
     */
    private suspend fun <T> retryWithExponentialBackoff(
        maxAttempts: Int,
        operation: String,
        block: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                val result = block()
                if (result.isSuccess) {
                    if (attempt > 0) {
                        android.util.Log.d(TAG, "$operation succeeded on attempt ${attempt + 1}")
                    }
                    return result
                }
                
                // If it's a failure result, extract the exception
                lastException = result.exceptionOrNull() as? Exception
                    ?: Exception("Operation failed")
                
            } catch (e: Exception) {
                lastException = e
            }
            
            // Don't retry on certain types of errors
            lastException?.let { exception ->
                if (!shouldRetry(exception)) {
                    android.util.Log.d(TAG, "$operation failed with non-retryable error: ${exception.message}")
                    return Result.failure(exception)
                }
            }
            
            // Don't delay after the last attempt
            if (attempt < maxAttempts - 1) {
                val delayMs = calculateBackoffDelay(attempt)
                android.util.Log.d(TAG, "$operation failed on attempt ${attempt + 1}, retrying in ${delayMs}ms")
                delay(delayMs)
            }
        }
        
        android.util.Log.e(TAG, "$operation failed after $maxAttempts attempts")
        return Result.failure(lastException ?: Exception("Operation failed after $maxAttempts attempts"))
    }

    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoffDelay(attempt: Int): Long {
        return (BASE_RETRY_DELAY_MS * 2.0.pow(attempt.toDouble())).toLong()
    }

    /**
     * Determine if an exception should trigger a retry
     */
    private fun shouldRetry(exception: Exception): Boolean {
        return when (exception) {
            is StorageException.FileNotFound,
            is StorageException.InvalidFile,
            is StorageException.InvalidUrl,
            is StorageException.InvalidPath,
            is StorageException.AuthenticationError -> false
            is StorageException.NetworkError,
            is StorageException.StorageQuotaError,
            is StorageException.ServerError -> true
            else -> {
                // Check exception message for common retryable errors
                val message = exception.message?.lowercase() ?: ""
                when {
                    message.contains("network") -> true
                    message.contains("timeout") -> true
                    message.contains("connection") -> true
                    message.contains("server error") -> true
                    message.contains("503") -> true
                    message.contains("502") -> true
                    message.contains("500") -> true
                    else -> false
                }
            }
        }
    }

    /**
     * Map generic exceptions to specific storage exceptions
     */
    private fun mapStorageException(exception: Exception, defaultMessage: String): StorageException {
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            message.contains("not found") || message.contains("404") -> 
                StorageException.FileNotFound("File not found: ${exception.message}")
            message.contains("unauthorized") || message.contains("401") -> 
                StorageException.AuthenticationError("Authentication failed: ${exception.message}")
            message.contains("quota") || message.contains("storage limit") -> 
                StorageException.StorageQuotaError("Storage quota exceeded: ${exception.message}")
            message.contains("network") || message.contains("connection") || message.contains("timeout") -> 
                StorageException.NetworkError("Network error: ${exception.message}")
            message.contains("500") || message.contains("502") || message.contains("503") -> 
                StorageException.ServerError("Server error: ${exception.message}")
            else -> StorageException.UnknownError("$defaultMessage: ${exception.message}")
        }
    }
}

/**
 * Storage-specific exceptions for better error handling
 */
sealed class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class FileNotFound(message: String) : StorageException(message)
    class InvalidFile(message: String) : StorageException(message)
    class InvalidUrl(message: String) : StorageException(message)
    class InvalidPath(message: String) : StorageException(message)
    class NetworkError(message: String) : StorageException(message)
    class AuthenticationError(message: String) : StorageException(message)
    class StorageQuotaError(message: String) : StorageException(message)
    class ServerError(message: String) : StorageException(message)
    class EmptyFile(message: String) : StorageException(message)
    class UnknownError(message: String) : StorageException(message)
}
