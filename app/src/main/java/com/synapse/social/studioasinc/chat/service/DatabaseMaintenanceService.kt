package com.synapse.social.studioasinc.chat.service

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background service for performing database maintenance tasks.
 * Uses WorkManager to schedule periodic cleanup and optimization tasks.
 * 
 * Requirements: 6.5
 */
class DatabaseMaintenanceService(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "DatabaseMaintenanceService"
        const val WORK_NAME = "database_maintenance"
        private const val MAINTENANCE_INTERVAL_HOURS = 6L
        
        /**
         * Schedule periodic database maintenance.
         * 
         * @param context Application context
         */
        fun schedulePeriodicMaintenance(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val maintenanceRequest = PeriodicWorkRequestBuilder<DatabaseMaintenanceService>(
                MAINTENANCE_INTERVAL_HOURS, TimeUnit.HOURS
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
                    maintenanceRequest
                )
            
            Log.i(TAG, "Scheduled periodic database maintenance every $MAINTENANCE_INTERVAL_HOURS hours")
        }
        
        /**
         * Cancel scheduled database maintenance.
         * 
         * @param context Application context
         */
        fun cancelPeriodicMaintenance(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled periodic database maintenance")
        }
        
        /**
         * Run maintenance immediately (one-time).
         * 
         * @param context Application context
         */
        fun runMaintenanceNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val immediateRequest = OneTimeWorkRequestBuilder<DatabaseMaintenanceService>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueue(immediateRequest)
            Log.i(TAG, "Scheduled immediate database maintenance")
        }
    }
    
    private val dbOptimizationService = DatabaseOptimizationService()
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting database maintenance work")
        
        return@withContext try {
            // Check if maintenance is actually needed
            val maintenanceNeeded = dbOptimizationService.isMaintenanceNeeded()
            
            if (!maintenanceNeeded) {
                Log.i(TAG, "Database maintenance not needed, skipping")
                return@withContext Result.success(
                    workDataOf(
                        "maintenance_performed" to false,
                        "reason" to "not_needed"
                    )
                )
            }
            
            Log.i(TAG, "Database maintenance needed, performing cleanup")
            
            // Perform the maintenance
            val summary = dbOptimizationService.performMaintenance()
            
            // Log the results
            Log.i(TAG, "Database maintenance completed successfully:")
            Log.i(TAG, "- Old records deleted: ${summary.oldRecordsDeleted}")
            Log.i(TAG, "- Stale records updated: ${summary.staleRecordsUpdated}")
            Log.i(TAG, "- Duration: ${summary.durationMs}ms")
            
            // Return success with maintenance summary
            Result.success(
                workDataOf(
                    "maintenance_performed" to true,
                    "old_records_deleted" to summary.oldRecordsDeleted,
                    "stale_records_updated" to summary.staleRecordsUpdated,
                    "duration_ms" to summary.durationMs,
                    "timestamp" to summary.timestamp
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Database maintenance failed", e)
            
            // Return retry for transient errors, failure for permanent errors
            when {
                e.message?.contains("network", ignoreCase = true) == true -> {
                    Log.w(TAG, "Network error during maintenance, will retry")
                    Result.retry()
                }
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    Log.w(TAG, "Timeout during maintenance, will retry")
                    Result.retry()
                }
                runAttemptCount < 3 -> {
                    Log.w(TAG, "Maintenance failed, attempt $runAttemptCount, will retry")
                    Result.retry()
                }
                else -> {
                    Log.e(TAG, "Maintenance failed permanently after $runAttemptCount attempts")
                    Result.failure(
                        workDataOf(
                            "maintenance_performed" to false,
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
 * Helper class for managing database maintenance scheduling and monitoring.
 */
class DatabaseMaintenanceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DatabaseMaintenanceManager"
    }
    
    /**
     * Initialize database maintenance scheduling.
     * Should be called from Application.onCreate().
     */
    fun initialize() {
        Log.d(TAG, "Initializing database maintenance manager")
        DatabaseMaintenanceService.schedulePeriodicMaintenance(context)
    }
    
    /**
     * Shutdown database maintenance.
     * Should be called when the app is being destroyed.
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down database maintenance manager")
        DatabaseMaintenanceService.cancelPeriodicMaintenance(context)
    }
    
    /**
     * Trigger immediate maintenance if needed.
     * Can be called from settings or debug screens.
     */
    fun runMaintenanceIfNeeded() {
        Log.d(TAG, "Checking if immediate maintenance is needed")
        DatabaseMaintenanceService.runMaintenanceNow(context)
    }
    
    /**
     * Get the status of the last maintenance work.
     * 
     * @return WorkInfo for the last maintenance work, or null if none found
     */
    suspend fun getLastMaintenanceStatus(): WorkInfo? = withContext(Dispatchers.IO) {
        try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(DatabaseMaintenanceService.WORK_NAME)
                .get()
            
            return@withContext workInfos.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get maintenance status", e)
            null
        }
    }
    
    /**
     * Check if maintenance is currently running.
     * 
     * @return true if maintenance work is running
     */
    suspend fun isMaintenanceRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            val status = getLastMaintenanceStatus()
            return@withContext status?.state == WorkInfo.State.RUNNING
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if maintenance is running", e)
            false
        }
    }
    
    /**
     * Get maintenance statistics from the last run.
     * 
     * @return Map of maintenance statistics, or empty map if unavailable
     */
    suspend fun getMaintenanceStats(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val status = getLastMaintenanceStatus()
            val outputData = status?.outputData
            
            if (outputData != null) {
                return@withContext mapOf(
                    "maintenance_performed" to outputData.getBoolean("maintenance_performed", false),
                    "old_records_deleted" to outputData.getInt("old_records_deleted", 0),
                    "stale_records_updated" to outputData.getInt("stale_records_updated", 0),
                    "duration_ms" to outputData.getLong("duration_ms", 0L),
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
            Log.e(TAG, "Failed to get maintenance stats", e)
            emptyMap()
        }
    }
}
