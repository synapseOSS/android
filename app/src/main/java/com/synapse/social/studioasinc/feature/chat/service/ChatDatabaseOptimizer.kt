package com.synapse.social.studioasinc.chat.service

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Service for optimized database operations related to typing indicators and read receipts.
 * Uses batch operations and optimized queries for better performance.
 * 
 * Requirements: 6.5
 */
class ChatDatabaseOptimizer {
    
    companion object {
        private const val TAG = "ChatDatabaseOptimizer"
        private const val MAX_BATCH_SIZE = 100
        private const val TYPING_CLEANUP_THRESHOLD_MS = 3600000L // 1 hour
        private const val STALE_TYPING_THRESHOLD_MS = 10000L // 10 seconds
    }
    
    private val supabase = SupabaseClient.client
    
    /**
     * Batch update message states for multiple messages efficiently.
     * Uses the optimized batch_update_message_state database function.
     * 
     * @param messageIds List of message IDs to update
     * @param newState New message state (sent, delivered, read, failed)
     * @param userId Optional user ID for additional filtering
     * @return Number of messages updated
     */
    suspend fun batchUpdateMessageState(
        messageIds: List<String>,
        newState: String,
        userId: String? = null
    ): Int = withContext(Dispatchers.IO) {
        if (messageIds.isEmpty()) {
            Log.w(TAG, "No message IDs provided for batch update")
            return@withContext 0
        }
        
        try {
            // Split into batches to avoid query size limits
            val batches = messageIds.chunked(MAX_BATCH_SIZE)
            var totalUpdated = 0
            
            for (batch in batches) {
                Log.d(TAG, "Batch updating ${batch.size} messages to state: $newState")
                
                val result = supabase.postgrest.rpc(
                    function = "batch_update_message_state",
                    parameters = BatchUpdateParams(
                        message_ids = batch,
                        new_state = newState,
                        user_id = userId
                    )
                )
                
                val updatedCount = result.data as? Int ?: 0
                totalUpdated += updatedCount
                
                Log.d(TAG, "Batch updated $updatedCount messages in this batch")
            }
            
            Log.i(TAG, "Successfully batch updated $totalUpdated messages to state: $newState")
            totalUpdated
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to batch update message states", e)
            throw e
        }
    }
    
    /**
     * Upsert typing status efficiently using the optimized database function.
     * 
     * @param chatId Chat room identifier
     * @param userId User identifier
     * @param isTyping Whether the user is currently typing
     */
    suspend fun upsertTypingStatus(
        chatId: String,
        userId: String,
        isTyping: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Upserting typing status - chatId: $chatId, userId: $userId, isTyping: $isTyping")
            
            supabase.postgrest.rpc(
                function = "upsert_typing_status",
                parameters = TypingStatusParams(
                    p_chat_id = chatId,
                    p_user_id = userId,
                    p_is_typing = isTyping
                )
            )
            
            Log.d(TAG, "Successfully upserted typing status")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upsert typing status", e)
            throw e
        }
    }
    
    /**
     * Clean up old typing status records to prevent table bloat.
     * Removes records older than 1 hour.
     * 
     * @return Number of records cleaned up
     */
    suspend fun cleanupOldTypingStatus(): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Cleaning up old typing status records")
            
            val result = supabase.postgrest.rpc(
                function = "cleanup_old_typing_status"
            )
            
            val deletedCount = result.data as? Int ?: 0
            
            if (deletedCount > 0) {
                Log.i(TAG, "Cleaned up $deletedCount old typing status records")
            } else {
                Log.d(TAG, "No old typing status records to clean up")
            }
            
            deletedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean up old typing status records", e)
            throw e
        }
    }
    
    /**
     * Clean up stale typing indicators (older than 10 seconds).
     * Sets is_typing to false for stale records.
     * 
     * @return Number of records updated
     */
    suspend fun cleanupStaleTypingStatus(): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Cleaning up stale typing status records")
            
            val result = supabase.postgrest.rpc(
                function = "cleanup_stale_typing_status"
            )
            
            val updatedCount = result.data as? Int ?: 0
            
            if (updatedCount > 0) {
                Log.i(TAG, "Cleaned up $updatedCount stale typing indicators")
            } else {
                Log.v(TAG, "No stale typing indicators to clean up")
            }
            
            updatedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean up stale typing status", e)
            throw e
        }
    }
    
    /**
     * Get message state statistics for performance monitoring.
     * 
     * @param chatId Optional chat ID to filter results
     * @return List of message state statistics
     */
    suspend fun getMessageStateStats(chatId: String? = null): List<MessageStateStats> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching message state statistics")
            
            val finalQuery = supabase.postgrest["message_state_stats"]
                .select {
                    if (chatId != null) {
                        filter {
                            eq("chat_id", chatId)
                        }
                    }
                }
            
            val result = finalQuery.decodeList<MessageStateStats>()
            
            Log.d(TAG, "Retrieved ${result.size} message state statistics")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get message state statistics", e)
            throw e
        }
    }
    
    /**
     * Get typing activity statistics for performance monitoring.
     * 
     * @param chatId Optional chat ID to filter results
     * @return List of typing activity statistics
     */
    suspend fun getTypingActivityStats(chatId: String? = null): List<TypingActivityStats> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching typing activity statistics")
            
            val finalQuery = supabase.postgrest["typing_activity_stats"]
                .select {
                    if (chatId != null) {
                        filter {
                            eq("chat_id", chatId)
                        }
                    }
                }
            
            val result = finalQuery.decodeList<TypingActivityStats>()
            
            Log.d(TAG, "Retrieved ${result.size} typing activity statistics")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get typing activity statistics", e)
            throw e
        }
    }
    
    /**
     * Perform routine maintenance tasks for optimal performance.
     * This includes cleaning up old and stale typing status records.
     * 
     * @return Maintenance summary
     */
    suspend fun performMaintenance(): MaintenanceSummary = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting routine database maintenance")
            
            val startTime = System.currentTimeMillis()
            
            // Clean up old typing status records
            val oldRecordsDeleted = cleanupOldTypingStatus()
            
            // Clean up stale typing indicators
            val staleRecordsUpdated = cleanupStaleTypingStatus()
            
            val duration = System.currentTimeMillis() - startTime
            
            val summary = MaintenanceSummary(
                oldRecordsDeleted = oldRecordsDeleted,
                staleRecordsUpdated = staleRecordsUpdated,
                durationMs = duration,
                timestamp = System.currentTimeMillis()
            )
            
            Log.i(TAG, "Database maintenance completed in ${duration}ms: $summary")
            summary
            
        } catch (e: Exception) {
            Log.e(TAG, "Database maintenance failed", e)
            throw e
        }
    }
    
    /**
     * Check if database maintenance is needed based on activity.
     * 
     * @return true if maintenance should be performed
     */
    suspend fun isMaintenanceNeeded(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get typing activity stats to determine if maintenance is needed
            val stats = getTypingActivityStats()
            
            // Maintenance is needed if there are many typing events or old records
            val totalEvents = stats.sumOf { it.total_typing_events }
            val hasOldActivity = stats.any { 
                System.currentTimeMillis() - it.last_activity_timestamp > TYPING_CLEANUP_THRESHOLD_MS 
            }
            
            val maintenanceNeeded = totalEvents > 1000 || hasOldActivity
            
            Log.d(TAG, "Maintenance needed: $maintenanceNeeded (total events: $totalEvents, has old activity: $hasOldActivity)")
            maintenanceNeeded
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check if maintenance is needed, assuming it is", e)
            true // Err on the side of caution
        }
    }
}

// Data classes for RPC parameters

@Serializable
data class BatchUpdateParams(
    val message_ids: List<String>,
    val new_state: String,
    val user_id: String? = null
)

@Serializable
data class TypingStatusParams(
    val p_chat_id: String,
    val p_user_id: String,
    val p_is_typing: Boolean
)

// Data classes for statistics

@Serializable
data class MessageStateStats(
    val chat_id: String,
    val message_state: String,
    val count: Int,
    val avg_delivery_time_ms: Double? = null,
    val avg_read_time_ms: Double? = null
)

@Serializable
data class TypingActivityStats(
    val chat_id: String,
    val total_typing_events: Int,
    val active_typing_count: Int,
    val last_activity_timestamp: Long,
    val avg_timestamp: Double
)

@Serializable
data class MaintenanceSummary(
    val oldRecordsDeleted: Int,
    val staleRecordsUpdated: Int,
    val durationMs: Long,
    val timestamp: Long
)
