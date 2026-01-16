package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background worker for performing media cache cleanup tasks.
 * Uses WorkManager to schedule periodic cleanup of expired and LRU files.
 */
class MediaCacheCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "MediaCacheCleanupWorker"
        const val WORK_NAME = "media_cache_cleanup"
        private const val CLEANUP_INTERVAL_HOURS = 24L // Daily cleanup
        
        /**
         * Schedule periodic media cache cleanup.
         * 
         * @param context Application context
         */
        fun schedulePeriodicCleanup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                // Removed setRequiresDeviceIdle(true) because it conflicts with backoff criteria
                .build()
            
            val cleanupRequest = PeriodicWorkRequestBuilder<MediaCacheCleanupWorker>(
                CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    cleanupRequest
                )
            
            Log.i(TAG, "Scheduled periodic media cache cleanup every $CLEANUP_INTERVAL_HOURS hours")
        }
        
        /**
         * Cancel scheduled media cache cleanup.
         * 
         * @param context Application context
         */
        fun cancelPeriodicCleanup(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled periodic media cache cleanup")
        }
        
        /**
         * Run cleanup immediately (one-time).
         * 
         * @param context Application context
         */
        fun runCleanupNow(context: Context) {
            val immediateRequest = OneTimeWorkRequestBuilder<MediaCacheCleanupWorker>()
                .build()
            
            WorkManager.getInstance(context).enqueue(immediateRequest)
            Log.i(TAG, "Scheduled immediate media cache cleanup")
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting media cache cleanup work")
        
        return@withContext try {
            val mediaCache = MediaCache(applicationContext)
            val initialStats = mediaCache.getCacheStats()
            
            Log.d(TAG, "Cache stats before cleanup: ${initialStats.fileCount} files, ${initialStats.totalSize}B")
            
            // Perform expired file cleanup
            mediaCache.evictExpired()
            
            // Perform LRU cleanup if still over size limit
            mediaCache.evictLRU()
            
            val finalStats = mediaCache.getCacheStats()
            val filesRemoved = initialStats.fileCount - finalStats.fileCount
            val bytesFreed = initialStats.totalSize - finalStats.totalSize
            
            Log.i(TAG, "Media cache cleanup completed successfully:")
            Log.i(TAG, "- Files removed: $filesRemoved")
            Log.i(TAG, "- Bytes freed: ${bytesFreed}B")
            Log.i(TAG, "- Final cache size: ${finalStats.totalSize}B (${finalStats.fileCount} files)")
            
            // Return success with cleanup summary
            Result.success(
                workDataOf(
                    "cleanup_performed" to true,
                    "files_removed" to filesRemoved,
                    "bytes_freed" to bytesFreed,
                    "final_cache_size" to finalStats.totalSize,
                    "final_file_count" to finalStats.fileCount,
                    "timestamp" to System.currentTimeMillis()
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Media cache cleanup failed", e)
            
            // Return retry for transient errors, failure for permanent errors
            when {
                runAttemptCount < 3 -> {
                    Log.w(TAG, "Cache cleanup failed, attempt $runAttemptCount, will retry")
                    Result.retry()
                }
                else -> {
                    Log.e(TAG, "Cache cleanup failed permanently after $runAttemptCount attempts")
                    Result.failure(
                        workDataOf(
                            "cleanup_performed" to false,
                            "error" to (e.message ?: "Unknown error"),
                            "attempts" to runAttemptCount
                        )
                    )
                }
            }
        }
    }
}

/**
 * Helper class for managing media cache cleanup scheduling and monitoring.
 */
class MediaCacheCleanupManager(private val context: Context) {
    
    companion object {
        private const val TAG = "MediaCacheCleanupManager"
    }
    
    /**
     * Initialize media cache cleanup scheduling.
     * Should be called from Application.onCreate().
     */
    fun initialize() {
        Log.d(TAG, "Initializing media cache cleanup manager")
        MediaCacheCleanupWorker.schedulePeriodicCleanup(context)
    }
    
    /**
     * Shutdown media cache cleanup.
     * Should be called when the app is being destroyed.
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down media cache cleanup manager")
        MediaCacheCleanupWorker.cancelPeriodicCleanup(context)
    }
    
    /**
     * Trigger immediate cleanup.
     * Can be called from settings or when cache is full.
     */
    fun runCleanupNow() {
        Log.d(TAG, "Running immediate media cache cleanup")
        MediaCacheCleanupWorker.runCleanupNow(context)
    }
    
    /**
     * Get the status of the last cleanup work.
     * 
     * @return WorkInfo for the last cleanup work, or null if none found
     */
    suspend fun getLastCleanupStatus(): WorkInfo? = withContext(Dispatchers.IO) {
        try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(MediaCacheCleanupWorker.WORK_NAME)
                .get()
            
            return@withContext workInfos.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cleanup status", e)
            null
        }
    }
    
    /**
     * Check if cleanup is currently running.
     * 
     * @return true if cleanup work is running
     */
    suspend fun isCleanupRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            val status = getLastCleanupStatus()
            return@withContext status?.state == WorkInfo.State.RUNNING
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if cleanup is running", e)
            false
        }
    }
    
    /**
     * Get cleanup statistics from the last run.
     * 
     * @return Map of cleanup statistics, or empty map if unavailable
     */
    suspend fun getCleanupStats(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val status = getLastCleanupStatus()
            val outputData = status?.outputData
            
            if (outputData != null) {
                return@withContext mapOf(
                    "cleanup_performed" to outputData.getBoolean("cleanup_performed", false),
                    "files_removed" to outputData.getInt("files_removed", 0),
                    "bytes_freed" to outputData.getLong("bytes_freed", 0L),
                    "final_cache_size" to outputData.getLong("final_cache_size", 0L),
                    "final_file_count" to outputData.getInt("final_file_count", 0),
                    "timestamp" to outputData.getLong("timestamp", 0L),
                    "last_run_state" to status.state.name,
                    "error" to (outputData.getString("error") ?: "")
                ).filterValues { value -> 
                    when (value) {
                        is String -> value.isNotEmpty()
                        else -> true
                    }
                }
            }
            
            return@withContext emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cleanup stats", e)
            emptyMap()
        }
    }
}
