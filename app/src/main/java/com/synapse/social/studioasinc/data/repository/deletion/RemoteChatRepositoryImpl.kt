package com.synapse.social.studioasinc.data.repository.deletion

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.data.model.deletion.DeletionOperation
import com.synapse.social.studioasinc.data.model.deletion.OperationStatus
import com.synapse.social.studioasinc.data.model.deletion.StorageType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.math.min
import kotlin.math.pow
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Supabase-based implementation of RemoteChatRepository
 * Handles remote database deletion operations with network error handling and retry logic
 * Requirements: 1.2, 2.2, 4.1, 4.2
 */
class RemoteChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RemoteChatRepository {
    
    companion object {
        private const val TAG = "RemoteChatRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY = 1000L // 1 second
        private const val MAX_RETRY_DELAY = 8000L // 8 seconds
        private const val BATCH_SIZE = 100 // Process deletions in batches for performance
    }
    
    private val client = SupabaseClient.client
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val connectivityMonitor = ConnectivityMonitor(context)
    
    // Lazy initialization to avoid circular dependency
    private val _retryQueueManager by lazy { RetryQueueManager(context, this) }
    
    init {
        connectivityMonitor.startMonitoring()
    }
    
    /**
     * Delete all messages for a specific user from remote database
     * Requirements: 1.2
     */
    override suspend fun deleteAllMessages(userId: String): RepositoryResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting deleteAllMessages for user: $userId")
                
                if (!isNetworkAvailable()) {
                    Log.w(TAG, "Network not available, queueing deletion for retry")
                    val operation = createDeletionOperation(userId, null)
                    queueDeletionForRetry(operation)
                    return@withContext RepositoryResult.Failure(
                        "Network not available. Deletion queued for retry.",
                        retryable = true
                    )
                }
                
                // Get message count before deletion for progress tracking
                val messageCount = getMessageCount(userId)
                Log.d(TAG, "Found $messageCount messages to delete for user: $userId")
                
                // Delete messages in batches for better performance
                var totalDeleted = 0
                var offset = 0
                
                while (true) {
                    val batchResult = deleteMessagesBatch(userId, null, offset)
                    when (batchResult) {
                        is RepositoryResult.Success -> {
                            totalDeleted += batchResult.messagesDeleted
                            if (batchResult.messagesDeleted < BATCH_SIZE) {
                                // Last batch processed
                                break
                            }
                            offset += BATCH_SIZE
                        }
                        is RepositoryResult.Failure -> {
                            if (batchResult.retryable) {
                                val operation = createDeletionOperation(userId, null)
                                queueDeletionForRetry(operation)
                            }
                            return@withContext batchResult
                        }
                    }
                }
                
                // Verify deletion completed successfully
                val remainingCount = getMessageCount(userId)
                if (remainingCount > 0) {
                    Log.w(TAG, "Deletion incomplete: $remainingCount messages remaining")
                    return@withContext RepositoryResult.Failure(
                        "Deletion incomplete: $remainingCount messages remaining",
                        retryable = true
                    )
                }
                
                Log.d(TAG, "Successfully deleted $totalDeleted messages for user: $userId")
                RepositoryResult.Success(totalDeleted)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete all messages for user: $userId", e)
                val isRetryable = isRetryableError(e)
                
                if (isRetryable) {
                    val operation = createDeletionOperation(userId, null)
                    queueDeletionForRetry(operation)
                }
                
                RepositoryResult.Failure(
                    "Remote deletion failed: ${e.message}",
                    retryable = isRetryable
                )
            }
        }
    }
    
    /**
     * Delete messages for specific chats from remote database
     * Requirements: 2.2
     */
    override suspend fun deleteMessagesForChats(chatIds: List<String>): RepositoryResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting deleteMessagesForChats for ${chatIds.size} chats")
                
                if (!isNetworkAvailable()) {
                    Log.w(TAG, "Network not available, queueing deletion for retry")
                    val operation = createDeletionOperation(null, chatIds)
                    queueDeletionForRetry(operation)
                    return@withContext RepositoryResult.Failure(
                        "Network not available. Deletion queued for retry.",
                        retryable = true
                    )
                }
                
                // Get message count before deletion
                val messageCount = getMessageCountForChats(chatIds)
                Log.d(TAG, "Found $messageCount messages to delete for chats: $chatIds")
                
                // Delete messages in batches for better performance
                var totalDeleted = 0
                var offset = 0
                
                while (true) {
                    val batchResult = deleteMessagesBatch(null, chatIds, offset)
                    when (batchResult) {
                        is RepositoryResult.Success -> {
                            totalDeleted += batchResult.messagesDeleted
                            if (batchResult.messagesDeleted < BATCH_SIZE) {
                                // Last batch processed
                                break
                            }
                            offset += BATCH_SIZE
                        }
                        is RepositoryResult.Failure -> {
                            if (batchResult.retryable) {
                                val operation = createDeletionOperation(null, chatIds)
                                queueDeletionForRetry(operation)
                            }
                            return@withContext batchResult
                        }
                    }
                }
                
                // Verify deletion completed successfully
                val remainingCount = getMessageCountForChats(chatIds)
                if (remainingCount > 0) {
                    Log.w(TAG, "Deletion incomplete: $remainingCount messages remaining")
                    return@withContext RepositoryResult.Failure(
                        "Deletion incomplete: $remainingCount messages remaining",
                        retryable = true
                    )
                }
                
                Log.d(TAG, "Successfully deleted $totalDeleted messages for chats: $chatIds")
                RepositoryResult.Success(totalDeleted)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete messages for chats: $chatIds", e)
                val isRetryable = isRetryableError(e)
                
                if (isRetryable) {
                    val operation = createDeletionOperation(null, chatIds)
                    queueDeletionForRetry(operation)
                }
                
                RepositoryResult.Failure(
                    "Remote deletion failed: ${e.message}",
                    retryable = isRetryable
                )
            }
        }
    }
    
    /**
     * Delete messages in batches for better performance
     * Private helper method for batch processing
     */
    private suspend fun deleteMessagesBatch(
        userId: String?,
        chatIds: List<String>?,
        offset: Int
    ): RepositoryResult {
        return try {
            val deletedCount = if (userId != null) {
                // Delete all messages for user
                client.from("messages").delete {
                    filter {
                        or {
                            eq("sender_id", userId)
                            eq("receiver_id", userId)
                        }
                    }
                    limit(BATCH_SIZE.toLong())
                }
                // Since Supabase doesn't return count from delete, we estimate
                min(BATCH_SIZE, getMessageCount(userId))
            } else if (chatIds != null) {
                // Delete messages for specific chats
                client.from("messages").delete {
                    filter {
                        isIn("chat_id", chatIds)
                    }
                    limit(BATCH_SIZE.toLong())
                }
                // Since Supabase doesn't return count from delete, we estimate
                min(BATCH_SIZE, getMessageCountForChats(chatIds))
            } else {
                0
            }
            
            RepositoryResult.Success(deletedCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message batch", e)
            RepositoryResult.Failure(
                "Batch deletion failed: ${e.message}",
                retryable = isRetryableError(e)
            )
        }
    }
    
    /**
     * Queue a deletion operation for retry when network is restored
     * Requirements: 4.2
     */
    override suspend fun queueDeletionForRetry(operation: DeletionOperation): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Queueing deletion operation for retry: ${operation.id}")
                
                val retryData = buildJsonObject {
                    put("id", UUID.randomUUID().toString())
                    put("operation_id", operation.id)
                    put("scheduled_retry_time", System.currentTimeMillis() + calculateRetryDelay(operation.retryCount))
                    put("max_retries", MAX_RETRY_ATTEMPTS)
                    put("current_retry", operation.retryCount)
                }
                
                client.from("deletion_retry_queue").insert(retryData)
                
                // Also update the operation status
                val operationUpdate = buildJsonObject {
                    put("status", OperationStatus.QUEUED_FOR_RETRY.name)
                    put("retry_count", operation.retryCount + 1)
                }
                
                client.from("deletion_operations").update(operationUpdate) {
                    filter { eq("id", operation.id) }
                }
                
                Log.d(TAG, "Successfully queued deletion operation for retry")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to queue deletion operation for retry", e)
                false
            }
        }
    }
    
    /**
     * Check network connectivity status
     * Requirements: 4.1, 4.5
     */
    override suspend fun isNetworkAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val network = connectivityManager.activeNetwork ?: return@withContext false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@withContext false
                
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking network availability", e)
                false
            }
        }
    }
    
    /**
     * Get count of messages for a user (for progress tracking)
     * Requirements: 6.1, 6.2
     */
    override suspend fun getMessageCount(userId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from("messages")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            or {
                                eq("sender_id", userId)
                                eq("receiver_id", userId)
                            }
                        }
                    }
                    .decodeList<JsonObject>()
                
                result.size
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get message count for user: $userId", e)
                0
            }
        }
    }
    
    /**
     * Get count of messages for specific chats
     * Requirements: 6.1, 6.2
     */
    override suspend fun getMessageCountForChats(chatIds: List<String>): Int {
        return withContext(Dispatchers.IO) {
            try {
                if (chatIds.isEmpty()) return@withContext 0
                
                val result = client.from("messages")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            isIn("chat_id", chatIds)
                        }
                    }
                    .decodeList<JsonObject>()
                
                result.size
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get message count for chats: $chatIds", e)
                0
            }
        }
    }
    
    /**
     * Verify that deletion was completed successfully on remote
     * Requirements: 5.4
     */
    override suspend fun verifyDeletionComplete(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val remainingCount = getMessageCount(userId)
                remainingCount == 0
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to verify deletion completion for user: $userId", e)
                false
            }
        }
    }
    
    /**
     * Verify that specific chats were deleted successfully on remote
     * Requirements: 5.4
     */
    override suspend fun verifyChatsDeleted(chatIds: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val remainingCount = getMessageCountForChats(chatIds)
                remainingCount == 0
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to verify chats deletion: $chatIds", e)
                false
            }
        }
    }
    
    /**
     * Create a deletion operation record for tracking
     * Private helper method
     */
    private fun createDeletionOperation(userId: String?, chatIds: List<String>?): DeletionOperation {
        return DeletionOperation(
            id = UUID.randomUUID().toString(),
            storageType = StorageType.REMOTE_DATABASE,
            status = OperationStatus.PENDING,
            chatIds = chatIds,
            messagesAffected = 0, // Will be updated after deletion
            timestamp = System.currentTimeMillis(),
            retryCount = 0
        )
    }
    
    /**
     * Calculate exponential backoff delay for retry attempts
     * Requirements: 4.1, 4.2
     */
    private fun calculateRetryDelay(retryCount: Int): Long {
        val delay = INITIAL_RETRY_DELAY * (2.0.pow(retryCount.toDouble())).toLong()
        return min(delay, MAX_RETRY_DELAY)
    }
    
    /**
     * Determine if an error is retryable based on its type
     * Requirements: 4.1, 4.2
     */
    private fun isRetryableError(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        
        return when {
            // Network-related errors are retryable
            message.contains("network") -> true
            message.contains("timeout") -> true
            message.contains("connection") -> true
            message.contains("unreachable") -> true
            message.contains("no route") -> true
            
            // Server errors (5xx) are retryable
            message.contains("server error") -> true
            message.contains("internal server error") -> true
            message.contains("service unavailable") -> true
            message.contains("gateway timeout") -> true
            
            // Rate limiting is retryable
            message.contains("rate limit") -> true
            message.contains("too many requests") -> true
            
            // Client errors (4xx) are generally not retryable
            message.contains("unauthorized") -> false
            message.contains("forbidden") -> false
            message.contains("not found") -> false
            message.contains("bad request") -> false
            
            // Default to not retryable for unknown errors
            else -> false
        }
    }
    
    /**
     * Get retry queue manager for external access
     * Requirements: 4.2, 4.5
     */
    fun getRetryQueueManager(): RetryQueueManager = _retryQueueManager
    
    /**
     * Get connectivity monitor for external access
     * Requirements: 4.1, 4.5
     */
    fun getConnectivityMonitor(): ConnectivityMonitor = connectivityMonitor
    
    /**
     * Clean up resources when repository is no longer needed
     */
    fun cleanup() {
        try {
            connectivityMonitor.stopMonitoring()
            _retryQueueManager.cleanup()
            Log.d(TAG, "RemoteChatRepository cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}