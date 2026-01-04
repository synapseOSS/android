package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Local file caching system with LRU eviction and automatic cleanup.
 * Provides efficient storage and retrieval of media files with size and time-based eviction.
 */
class MediaCache(private val context: Context) {
    
    companion object {
        private const val TAG = "MediaCache"
        private const val CACHE_DIR_NAME = "media"
        private const val MAX_CACHE_SIZE_BYTES = 500L * 1024 * 1024 // 500MB
        private const val CACHE_EXPIRATION_DAYS = 7L
        private const val CACHE_EXPIRATION_MS = CACHE_EXPIRATION_DAYS * 24 * 60 * 60 * 1000
        private const val METADATA_FILE_SUFFIX = ".meta"
    }
    
    private val cacheDir: File = context.cacheDir.resolve(CACHE_DIR_NAME)
    private val currentCacheSize = AtomicLong(0)
    private val accessTimes = ConcurrentHashMap<String, Long>()
    private val fileSizes = ConcurrentHashMap<String, Long>()
    
    init {
        // Ensure cache directory exists
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        // Initialize cache metadata
        initializeCacheMetadata()
    }
    
    /**
     * Store a file in the cache with the given key.
     * If the cache exceeds size limits, LRU eviction will be triggered.
     */
    fun put(key: String, file: File) {
        // TODO: Verify thread safety and memory usage - File operations should be performed on a background thread
        if (!file.exists() || !file.isFile) {
            Log.w(TAG, "Cannot cache non-existent or invalid file: ${file.path}")
            return
        }
        
        try {
            val cacheKey = generateCacheKey(key)
            val cachedFile = getCacheFile(cacheKey)
            
            // Copy file to cache directory
            file.copyTo(cachedFile, overwrite = true)
            
            val fileSize = cachedFile.length()
            val currentTime = System.currentTimeMillis()
            
            // Update metadata
            updateFileMetadata(cacheKey, fileSize, currentTime)
            
            // Check if we need to evict files due to size limit
            if (currentCacheSize.get() > MAX_CACHE_SIZE_BYTES) {
                evictLRU()
            }
            
            Log.d(TAG, "Cached file with key: $key, size: ${fileSize}B")
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to cache file with key: $key", e)
        }
    }
    
    /**
     * Retrieve a cached file by key.
     * Updates access time for LRU tracking.
     */
    fun get(key: String): File? {
        // TODO: Verify thread safety and memory usage - File existence check and metadata update involves I/O
        val cacheKey = generateCacheKey(key)
        val cachedFile = getCacheFile(cacheKey)
        
        if (!cachedFile.exists()) {
            // Clean up orphaned metadata
            removeFileMetadata(cacheKey)
            return null
        }
        
        // Check if file has expired
        val fileAge = System.currentTimeMillis() - cachedFile.lastModified()
        if (fileAge > CACHE_EXPIRATION_MS) {
            Log.d(TAG, "Cache file expired: $key")
            remove(key)
            return null
        }
        
        // Update access time for LRU
        accessTimes[cacheKey] = System.currentTimeMillis()
        
        Log.d(TAG, "Cache hit for key: $key")
        return cachedFile
    }
    
    /**
     * Remove a specific file from the cache.
     */
    fun remove(key: String) {
        val cacheKey = generateCacheKey(key)
        val cachedFile = getCacheFile(cacheKey)
        
        if (cachedFile.exists()) {
            val fileSize = cachedFile.length()
            
            if (cachedFile.delete()) {
                removeFileMetadata(cacheKey)
                currentCacheSize.addAndGet(-fileSize)
                Log.d(TAG, "Removed cached file: $key")
            } else {
                Log.w(TAG, "Failed to delete cached file: $key")
            }
        }
    }
    
    /**
     * Clear the entire cache.
     */
    fun clear() {
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
            
            accessTimes.clear()
            fileSizes.clear()
            currentCacheSize.set(0)
            
            Log.d(TAG, "Cache cleared")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }
    
    /**
     * Remove files older than the expiration time.
     */
    fun evictExpired() {
        val currentTime = System.currentTimeMillis()
        val expiredFiles = mutableListOf<String>()
        
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.endsWith(METADATA_FILE_SUFFIX)) {
                val fileAge = currentTime - file.lastModified()
                if (fileAge > CACHE_EXPIRATION_MS) {
                    val cacheKey = file.nameWithoutExtension
                    expiredFiles.add(cacheKey)
                }
            }
        }
        
        expiredFiles.forEach { cacheKey ->
            val originalKey = reverseGenerateCacheKey(cacheKey)
            remove(originalKey)
        }
        
        if (expiredFiles.isNotEmpty()) {
            Log.d(TAG, "Evicted ${expiredFiles.size} expired files")
        }
    }
    
    /**
     * Evict least recently used files when cache size exceeds limit.
     */
    fun evictLRU() {
        if (currentCacheSize.get() <= MAX_CACHE_SIZE_BYTES) {
            return
        }
        
        // Sort files by access time (oldest first)
        val sortedByAccess = accessTimes.entries
            .sortedBy { it.value }
            .map { it.key }
        
        var bytesEvicted = 0L
        var filesEvicted = 0
        
        for (cacheKey in sortedByAccess) {
            if (currentCacheSize.get() <= MAX_CACHE_SIZE_BYTES * 0.8) { // Leave 20% buffer
                break
            }
            
            val cachedFile = getCacheFile(cacheKey)
            if (cachedFile.exists()) {
                val fileSize = cachedFile.length()
                
                if (cachedFile.delete()) {
                    removeFileMetadata(cacheKey)
                    currentCacheSize.addAndGet(-fileSize)
                    bytesEvicted += fileSize
                    filesEvicted++
                }
            }
        }
        
        if (filesEvicted > 0) {
            Log.d(TAG, "LRU evicted $filesEvicted files, freed ${bytesEvicted}B")
        }
    }
    
    /**
     * Get current cache size in bytes.
     */
    fun getCacheSize(): Long = currentCacheSize.get()
    
    /**
     * Get number of cached files.
     */
    fun getCacheFileCount(): Int = accessTimes.size
    
    /**
     * Get cache statistics for debugging.
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            totalSize = currentCacheSize.get(),
            fileCount = accessTimes.size,
            maxSize = MAX_CACHE_SIZE_BYTES,
            expirationDays = CACHE_EXPIRATION_DAYS
        )
    }
    
    // Private helper methods
    
    private fun initializeCacheMetadata() {
        var totalSize = 0L
        
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.endsWith(METADATA_FILE_SUFFIX)) {
                val cacheKey = file.nameWithoutExtension
                val fileSize = file.length()
                val accessTime = file.lastModified()
                
                accessTimes[cacheKey] = accessTime
                fileSizes[cacheKey] = fileSize
                totalSize += fileSize
            }
        }
        
        currentCacheSize.set(totalSize)
        Log.d(TAG, "Initialized cache: ${accessTimes.size} files, ${totalSize}B")
    }
    
    private fun updateFileMetadata(cacheKey: String, fileSize: Long, accessTime: Long) {
        // Remove old size if file existed
        val oldSize = fileSizes[cacheKey] ?: 0L
        currentCacheSize.addAndGet(fileSize - oldSize)
        
        // Update metadata
        accessTimes[cacheKey] = accessTime
        fileSizes[cacheKey] = fileSize
    }
    
    private fun removeFileMetadata(cacheKey: String) {
        accessTimes.remove(cacheKey)
        fileSizes.remove(cacheKey)
    }
    
    private fun generateCacheKey(key: String): String {
        // Generate a safe filename from the key using MD5 hash
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(key.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun reverseGenerateCacheKey(cacheKey: String): String {
        // For LRU eviction, we don't need to reverse the hash
        // We'll use the cache key directly for file operations
        return cacheKey
    }
    
    private fun getCacheFile(cacheKey: String): File {
        return cacheDir.resolve(cacheKey)
    }
    
    /**
     * Schedule periodic cache cleanup using WorkManager.
     * Should be called during app initialization.
     */
    fun schedulePeriodicCleanup() {
        MediaCacheCleanupWorker.schedulePeriodicCleanup(context)
    }
    
    /**
     * Cancel scheduled periodic cleanup.
     */
    fun cancelPeriodicCleanup() {
        MediaCacheCleanupWorker.cancelPeriodicCleanup(context)
    }
    
    /**
     * Trigger immediate cleanup.
     */
    fun runCleanupNow() {
        MediaCacheCleanupWorker.runCleanupNow(context)
    }
    
    /**
     * Check if a file exists in cache without updating access time.
     */
    fun contains(key: String): Boolean {
        val cacheKey = generateCacheKey(key)
        val cachedFile = getCacheFile(cacheKey)
        return cachedFile.exists()
    }
    
    /**
     * Get file size for a cached file without retrieving it.
     */
    fun getFileSize(key: String): Long? {
        val cacheKey = generateCacheKey(key)
        return fileSizes[cacheKey]
    }
    
    /**
     * Get last access time for a cached file.
     */
    fun getLastAccessTime(key: String): Long? {
        val cacheKey = generateCacheKey(key)
        return accessTimes[cacheKey]
    }
    
    /**
     * Get all cached file keys (for debugging/monitoring).
     */
    fun getAllKeys(): Set<String> {
        return accessTimes.keys.toSet()
    }
    
    /**
     * Perform maintenance cleanup - remove expired files and enforce size limits.
     * This is called by the background worker.
     */
    fun performMaintenance(): MaintenanceResult {
        val startTime = System.currentTimeMillis()
        val initialFileCount = accessTimes.size
        val initialSize = currentCacheSize.get()
        
        // First, remove expired files
        evictExpired()
        val afterExpiredFileCount = accessTimes.size
        val afterExpiredSize = currentCacheSize.get()
        
        // Then, enforce size limits with LRU
        evictLRU()
        val finalFileCount = accessTimes.size
        val finalSize = currentCacheSize.get()
        
        val duration = System.currentTimeMillis() - startTime
        
        return MaintenanceResult(
            expiredFilesRemoved = initialFileCount - afterExpiredFileCount,
            lruFilesRemoved = afterExpiredFileCount - finalFileCount,
            totalFilesRemoved = initialFileCount - finalFileCount,
            bytesFreed = initialSize - finalSize,
            durationMs = duration,
            finalCacheSize = finalSize,
            finalFileCount = finalFileCount
        )
    }
    
    /**
     * Data class for cache statistics.
     */
    data class CacheStats(
        val totalSize: Long,
        val fileCount: Int,
        val maxSize: Long,
        val expirationDays: Long
    )
    
    /**
     * Data class for maintenance operation results.
     */
    data class MaintenanceResult(
        val expiredFilesRemoved: Int,
        val lruFilesRemoved: Int,
        val totalFilesRemoved: Int,
        val bytesFreed: Long,
        val durationMs: Long,
        val finalCacheSize: Long,
        val finalFileCount: Int
    )
}
