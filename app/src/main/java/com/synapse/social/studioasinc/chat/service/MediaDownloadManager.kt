package com.synapse.social.studioasinc.chat.service

import android.content.Context
import android.util.Log
import com.synapse.social.studioasinc.backend.StorageException
import com.synapse.social.studioasinc.backend.SupabaseStorageService
import com.synapse.social.studioasinc.util.MediaCache
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * MediaDownloadManager
 * Handles media downloads with caching, concurrent download limits, and preloading for galleries.
 * 
 * Features:
 * - Downloads media files with automatic caching
 * - Limits concurrent downloads to 5 maximum
 * - Preloads adjacent images for smooth gallery navigation
 * - Checks cache before downloading
 * - Supports thumbnail downloads
 */
class MediaDownloadManager(
    private val context: Context,
    private val storageService: SupabaseStorageService,
    private val mediaCache: MediaCache,
    private val coroutineScope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "MediaDownloadManager"
        private const val MAX_CONCURRENT_DOWNLOADS = 5
        private const val PRELOAD_COUNT = 3 // Preload next 3 images
        private const val DOWNLOAD_TIMEOUT_MS = 30000L // 30 seconds
    }
    
    private val activeDownloads = AtomicInteger(0)
    private val downloadQueue = Channel<DownloadTask>(Channel.UNLIMITED)
    private val preloadJobs = ConcurrentHashMap<String, Job>()
    
    init {
        // Start download workers
        startDownloadWorkers()
    }
    
    /**
     * Download media file with caching.
     * Checks cache first, downloads if not cached.
     * 
     * @param url Media file URL
     * @param mediaType Type of media (image, video, audio, document)
     * @return Result containing the downloaded file
     */
    suspend fun downloadMedia(url: String, mediaType: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading media: $url (type: $mediaType)")
                
                // Check cache first
                val cachedFile = getCachedMedia(url)
                if (cachedFile != null) {
                    Log.d(TAG, "Cache hit for: $url")
                    return@withContext Result.success(cachedFile)
                }
                
                Log.d(TAG, "Cache miss, downloading: $url")
                
                // Download with timeout
                withTimeout(DOWNLOAD_TIMEOUT_MS) {
                    downloadAndCache(url, mediaType)
                }
                
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Download timeout for: $url", e)
                Result.failure(DownloadException.Timeout("Download timed out after ${DOWNLOAD_TIMEOUT_MS}ms"))
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for: $url", e)
                Result.failure(mapDownloadException(e))
            }
        }
    }
    
    /**
     * Download thumbnail with caching.
     * Thumbnails are cached separately from full media.
     * 
     * @param url Thumbnail URL
     * @return Result containing the downloaded thumbnail file
     */
    suspend fun downloadThumbnail(url: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading thumbnail: $url")
                
                // Check cache first (use thumbnail-specific cache key)
                val thumbnailCacheKey = "thumb_$url"
                val cachedFile = mediaCache.get(thumbnailCacheKey)
                if (cachedFile != null) {
                    Log.d(TAG, "Thumbnail cache hit for: $url")
                    return@withContext Result.success(cachedFile)
                }
                
                Log.d(TAG, "Thumbnail cache miss, downloading: $url")
                
                // Download with timeout
                withTimeout(DOWNLOAD_TIMEOUT_MS) {
                    downloadAndCacheThumbnail(url)
                }
                
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Thumbnail download timeout for: $url", e)
                Result.failure(DownloadException.Timeout("Thumbnail download timed out"))
            } catch (e: Exception) {
                Log.e(TAG, "Thumbnail download failed for: $url", e)
                Result.failure(mapDownloadException(e))
            }
        }
    }
    
    /**
     * Get cached media file without downloading.
     * Returns null if not in cache.
     * 
     * @param url Media file URL
     * @return Cached file or null
     */
    fun getCachedMedia(url: String): File? {
        return mediaCache.get(url)
    }
    
    /**
     * Preload media files for gallery navigation.
     * Preloads the next N images in the background.
     * 
     * @param urls List of media URLs to preload
     */
    fun preloadMedia(urls: List<String>) {
        if (urls.isEmpty()) {
            Log.d(TAG, "No URLs to preload")
            return
        }
        
        Log.d(TAG, "Preloading ${urls.size} media files")
        
        urls.forEach { url ->
            // Skip if already cached
            if (mediaCache.contains(url)) {
                Log.d(TAG, "Skipping preload, already cached: $url")
                return@forEach
            }
            
            // Cancel existing preload job for this URL if any
            preloadJobs[url]?.cancel()
            
            // Start new preload job
            val job = coroutineScope.launch(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Preloading: $url")
                    downloadAndCache(url, "image")
                    Log.d(TAG, "Preload complete: $url")
                } catch (e: CancellationException) {
                    Log.d(TAG, "Preload cancelled: $url")
                } catch (e: Exception) {
                    Log.w(TAG, "Preload failed for: $url", e)
                }
            }
            
            preloadJobs[url] = job
            
            // Clean up job when complete
            job.invokeOnCompletion {
                preloadJobs.remove(url)
            }
        }
    }
    
    /**
     * Preload adjacent images for gallery viewing.
     * Preloads the next 3 images from the current position.
     * 
     * @param urls Complete list of image URLs in the gallery
     * @param currentIndex Current viewing position
     */
    fun preloadGalleryImages(urls: List<String>, currentIndex: Int) {
        if (urls.isEmpty() || currentIndex < 0 || currentIndex >= urls.size) {
            Log.w(TAG, "Invalid gallery preload parameters")
            return
        }
        
        // Calculate preload range: current + next 3 images
        val startIndex = currentIndex
        val endIndex = minOf(currentIndex + PRELOAD_COUNT, urls.size - 1)
        
        val urlsToPreload = urls.subList(startIndex, endIndex + 1)
        
        Log.d(TAG, "Preloading gallery images from index $startIndex to $endIndex")
        preloadMedia(urlsToPreload)
    }
    
    /**
     * Cancel all ongoing preload operations.
     * Useful when user navigates away from gallery.
     */
    fun cancelPreloading() {
        Log.d(TAG, "Cancelling ${preloadJobs.size} preload jobs")
        
        preloadJobs.values.forEach { job ->
            job.cancel()
        }
        
        preloadJobs.clear()
    }
    
    /**
     * Clear the media cache.
     */
    fun clearCache() {
        Log.d(TAG, "Clearing media cache")
        mediaCache.clear()
    }
    
    /**
     * Get cache statistics.
     */
    fun getCacheStats(): MediaCache.CacheStats {
        return mediaCache.getCacheStats()
    }
    
    /**
     * Get number of active downloads.
     */
    fun getActiveDownloadCount(): Int {
        return activeDownloads.get()
    }
    
    // Private helper methods
    
    /**
     * Download file and cache it.
     */
    private suspend fun downloadAndCache(url: String, mediaType: String): Result<File> {
        return try {
            // Wait for download slot if at capacity
            waitForDownloadSlot()
            
            activeDownloads.incrementAndGet()
            
            try {
                // Download from storage
                val result = storageService.downloadFile(url)
                
                if (result.isFailure) {
                    return Result.failure(result.exceptionOrNull() ?: Exception("Download failed"))
                }
                
                val fileBytes = result.getOrThrow()
                
                // Create temporary file
                val tempFile = createTempFile(mediaType, fileBytes)
                
                // Cache the file
                mediaCache.put(url, tempFile)
                
                // Return cached file
                val cachedFile = mediaCache.get(url)
                    ?: return Result.failure(Exception("Failed to cache downloaded file"))
                
                Log.d(TAG, "Downloaded and cached: $url (${fileBytes.size} bytes)")
                Result.success(cachedFile)
                
            } finally {
                activeDownloads.decrementAndGet()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Download and cache failed for: $url", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download thumbnail and cache it.
     */
    private suspend fun downloadAndCacheThumbnail(url: String): Result<File> {
        return try {
            // Wait for download slot if at capacity
            waitForDownloadSlot()
            
            activeDownloads.incrementAndGet()
            
            try {
                // Download from storage
                val result = storageService.downloadFile(url)
                
                if (result.isFailure) {
                    return Result.failure(result.exceptionOrNull() ?: Exception("Thumbnail download failed"))
                }
                
                val fileBytes = result.getOrThrow()
                
                // Create temporary file
                val tempFile = createTempFile("thumbnail", fileBytes)
                
                // Cache with thumbnail-specific key
                val thumbnailCacheKey = "thumb_$url"
                mediaCache.put(thumbnailCacheKey, tempFile)
                
                // Return cached file
                val cachedFile = mediaCache.get(thumbnailCacheKey)
                    ?: return Result.failure(Exception("Failed to cache downloaded thumbnail"))
                
                Log.d(TAG, "Downloaded and cached thumbnail: $url (${fileBytes.size} bytes)")
                Result.success(cachedFile)
                
            } finally {
                activeDownloads.decrementAndGet()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Thumbnail download and cache failed for: $url", e)
            Result.failure(e)
        }
    }
    
    /**
     * Wait for available download slot.
     * Suspends if maximum concurrent downloads reached.
     */
    private suspend fun waitForDownloadSlot() {
        while (activeDownloads.get() >= MAX_CONCURRENT_DOWNLOADS) {
            delay(100) // Wait 100ms before checking again
        }
    }
    
    /**
     * Create temporary file from bytes.
     */
    private fun createTempFile(mediaType: String, bytes: ByteArray): File {
        val extension = when (mediaType.lowercase()) {
            "image" -> "jpg"
            "video" -> "mp4"
            "audio" -> "mp3"
            "document" -> "pdf"
            "thumbnail" -> "jpg"
            else -> "tmp"
        }
        
        val tempFile = File.createTempFile("download_", ".$extension", context.cacheDir)
        tempFile.writeBytes(bytes)
        return tempFile
    }
    
    /**
     * Start background download workers.
     * Currently not used but available for queue-based downloads.
     */
    private fun startDownloadWorkers() {
        // Workers can be implemented if queue-based downloads are needed
        // For now, we use direct download with concurrency limits
    }
    
    /**
     * Map exceptions to download-specific exceptions.
     */
    private fun mapDownloadException(exception: Exception): DownloadException {
        return when (exception) {
            is StorageException.FileNotFound -> 
                DownloadException.FileNotFound("File not found: ${exception.message}")
            is StorageException.NetworkError -> 
                DownloadException.NetworkError("Network error: ${exception.message}")
            is StorageException.AuthenticationError -> 
                DownloadException.AuthenticationError("Authentication failed: ${exception.message}")
            is CancellationException -> 
                DownloadException.Cancelled("Download cancelled")
            else -> 
                DownloadException.UnknownError("Download failed: ${exception.message}")
        }
    }
    
    /**
     * Download task for queue-based processing.
     */
    private data class DownloadTask(
        val url: String,
        val mediaType: String,
        val deferred: CompletableDeferred<Result<File>>
    )
}

/**
 * Download-specific exceptions.
 */
sealed class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class FileNotFound(message: String) : DownloadException(message)
    class NetworkError(message: String) : DownloadException(message)
    class AuthenticationError(message: String) : DownloadException(message)
    class Timeout(message: String) : DownloadException(message)
    class Cancelled(message: String) : DownloadException(message)
    class UnknownError(message: String) : DownloadException(message)
}
