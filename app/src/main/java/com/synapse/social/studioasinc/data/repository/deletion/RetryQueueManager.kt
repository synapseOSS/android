package com.synapse.social.studioasinc.data.repository.deletion

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.data.model.deletion.DeletionOperation
import com.synapse.social.studioasinc.data.model.deletion.OperationStatus
import com.synapse.social.studioasinc.data.model.deletion.StorageType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.min
import kotlin.math.pow

/**
 * Manages retry queue for failed deletion operations
 * Handles exponential backoff and automatic retry when connectivity is restored
 * Requirements: 4.1, 4.2, 4.5
 */
class RetryQueueManager(
    private val context: Context,
    private val remoteChatRepository: RemoteChatRepository
) {
    
    companion object {
        private const val TAG = "RetryQueueManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY = 1000L // 1 second
        private const val MAX_RETRY_DELAY = 8000L // 8 seconds
        private const val RETRY_CHECK_INTERVAL = 30000L // 30 seconds
    }
    
    private val client = SupabaseClient.client
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Network connectivity state
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()
    
    // Retry queue processing state
    private val _isProcessingQueue = MutableStateFlow(false)
    val isProcessingQueue: StateFlow<Boolean> = _isProcessingQueue.asStateFlow()
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var retryJob: Job? = null
    
    init {
        startNetworkMonitoring()
        startRetryQueueProcessor()
    }
    
    /**
     * Start monitoring network connectivity changes
     * Requirements: 4.1, 4.5
     */
    private fun startNetworkMonitoring() {
        try {
            // Initial network state check
            updateNetworkState()
            
            // Register network callback for connectivity changes
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network became available")
                    _isNetworkAvailable.value = true
                    triggerRetryProcessing()
                }
                
                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost")
                    updateNetworkState()
                }
                
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                     networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    
                    if (hasInternet && !_isNetworkAvailable.value) {
                        Log.d(TAG, "Network capabilities changed - internet available")
                        _isNetworkAvailable.value = true
                        triggerRetryProcessing()
                    } else if (!hasInternet && _isNetworkAvailable.value) {
                        Log.d(TAG, "Network capabilities changed - internet lost")
                        _isNetworkAvailable.value = false
                    }
                }
            }
            
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
            
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start network monitoring", e)
        }
    }
    
    /**
     * Update current network state
     */
    private fun updateNetworkState() {
        coroutineScope.launch {
            _isNetworkAvailable.value = remoteChatRepository.isNetworkAvailable()
        }
    }
    
    /**
     * Start the retry queue processor that runs periodically
     * Requirements: 4.2, 4.5
     */
    private fun startRetryQueueProcessor() {
        retryJob = coroutineScope.launch {
            while (isActive) {
                try {
                    if (_isNetworkAvailable.value) {
                        processRetryQueue()
                    }
                    delay(RETRY_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in retry queue processor", e)
                    delay(RETRY_CHECK_INTERVAL)
                }
            }
        }
    }
    
    /**
     * Trigger immediate retry processing when network becomes available
     * Requirements: 4.5
     */
    private fun triggerRetryProcessing() {
        coroutineScope.launch {
            if (_isNetworkAvailable.value && !_isProcessingQueue.value) {
                processRetryQueue()
            }
        }
    }
    
    /**
     * Process pending retry operations
     * Requirements: 4.2, 4.5
     */
    private suspend fun processRetryQueue() {
        if (_isProcessingQueue.value) {
            Log.d(TAG, "Retry queue processing already in progress")
            return
        }
        
        _isProcessingQueue.value = true
        
        try {
            Log.d(TAG, "Processing retry queue")
            
            // Get pending retry operations that are due for retry
            val currentTime = System.currentTimeMillis()
            val pendingRetries = client.from("deletion_retry_queue")
                .select(columns = Columns.raw("*")) {
                    filter {
                        lte("scheduled_retry_time", currentTime)
                        lt("current_retry", "max_retries")
                    }
                }
                .decodeList<JsonObject>()
            
            Log.d(TAG, "Found ${pendingRetries.size} pending retry operations")
            
            for (retryRecord in pendingRetries) {
                try {
                    val operationId = retryRecord["operation_id"]?.toString() ?: continue
                    val currentRetry = retryRecord["current_retry"]?.toString()?.toIntOrNull() ?: 0
                    val maxRetries = retryRecord["max_retries"]?.toString()?.toIntOrNull() ?: MAX_RETRY_ATTEMPTS
                    
                    if (currentRetry >= maxRetries) {
                        // Mark as failed and remove from queue
                        markOperationAsFailed(operationId)
                        removeFromRetryQueue(retryRecord["id"]?.toString() ?: "")
                        continue
                    }
                    
                    // Get the original deletion operation
                    val operation = getOperationById(operationId)
                    if (operation != null) {
                        val retryResult = retryDeletionOperation(operation)
                        
                        if (retryResult) {
                            // Success - remove from retry queue
                            removeFromRetryQueue(retryRecord["id"]?.toString() ?: "")
                            markOperationAsCompleted(operationId)
                            Log.d(TAG, "Successfully retried operation: $operationId")
                        } else {
                            // Failed - update retry count and schedule next retry
                            val nextRetry = currentRetry + 1
                            if (nextRetry < maxRetries) {
                                val nextRetryTime = currentTime + calculateRetryDelay(nextRetry)
                                updateRetrySchedule(retryRecord["id"]?.toString() ?: "", nextRetry, nextRetryTime)
                                Log.d(TAG, "Retry failed for operation: $operationId, scheduled next retry")
                            } else {
                                // Max retries exceeded
                                markOperationAsFailed(operationId)
                                removeFromRetryQueue(retryRecord["id"]?.toString() ?: "")
                                Log.w(TAG, "Max retries exceeded for operation: $operationId")
                            }
                        }
                    } else {
                        // Operation not found - remove from queue
                        removeFromRetryQueue(retryRecord["id"]?.toString() ?: "")
                        Log.w(TAG, "Operation not found for retry: $operationId")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing retry operation", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing retry queue", e)
        } finally {
            _isProcessingQueue.value = false
        }
    }
    
    /**
     * Retry a specific deletion operation
     * Requirements: 4.2
     */
    private suspend fun retryDeletionOperation(operation: DeletionOperation): Boolean {
        return try {
            Log.d(TAG, "Retrying deletion operation: ${operation.id}")
            
            val result = if (operation.chatIds != null) {
                remoteChatRepository.deleteMessagesForChats(operation.chatIds)
            } else {
                // Assume it's a user-based deletion - we need to get userId from operation
                // For now, we'll skip this as we don't have userId in the operation
                // This would need to be enhanced to store userId in the operation
                Log.w(TAG, "Cannot retry user-based deletion without userId in operation")
                return false
            }
            
            when (result) {
                is RepositoryResult.Success -> {
                    Log.d(TAG, "Retry successful for operation: ${operation.id}")
                    true
                }
                is RepositoryResult.Failure -> {
                    Log.w(TAG, "Retry failed for operation: ${operation.id} - ${result.error}")
                    false
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying deletion operation: ${operation.id}", e)
            false
        }
    }
    
    /**
     * Get deletion operation by ID
     */
    private suspend fun getOperationById(operationId: String): DeletionOperation? {
        return try {
            val result = client.from("deletion_operations")
                .select(columns = Columns.raw("*")) {
                    filter { eq("id", operationId) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            result?.let { jsonObject ->
                DeletionOperation(
                    id = jsonObject["id"]?.toString() ?: "",
                    storageType = StorageType.valueOf(jsonObject["storage_type"]?.toString() ?: "REMOTE_DATABASE"),
                    status = OperationStatus.valueOf(jsonObject["status"]?.toString() ?: "PENDING"),
                    chatIds = jsonObject["chat_ids"]?.toString()?.let { 
                        // Parse JSON array string to list
                        if (it.startsWith("[") && it.endsWith("]")) {
                            it.substring(1, it.length - 1)
                                .split(",")
                                .map { chatId -> chatId.trim().removeSurrounding("\"") }
                                .filter { chatId -> chatId.isNotEmpty() }
                        } else null
                    },
                    messagesAffected = jsonObject["messages_affected"]?.toString()?.toIntOrNull() ?: 0,
                    timestamp = jsonObject["created_at"]?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
                    retryCount = jsonObject["retry_count"]?.toString()?.toIntOrNull() ?: 0
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting operation by ID: $operationId", e)
            null
        }
    }
    
    /**
     * Mark operation as completed
     */
    private suspend fun markOperationAsCompleted(operationId: String) {
        try {
            val updateData = buildJsonObject {
                put("status", OperationStatus.COMPLETED.name)
                put("completed_at", System.currentTimeMillis())
            }
            
            client.from("deletion_operations").update(updateData) {
                filter { eq("id", operationId) }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marking operation as completed: $operationId", e)
        }
    }
    
    /**
     * Mark operation as failed
     */
    private suspend fun markOperationAsFailed(operationId: String) {
        try {
            val updateData = buildJsonObject {
                put("status", OperationStatus.FAILED.name)
                put("error_message", "Max retry attempts exceeded")
            }
            
            client.from("deletion_operations").update(updateData) {
                filter { eq("id", operationId) }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marking operation as failed: $operationId", e)
        }
    }
    
    /**
     * Remove operation from retry queue
     */
    private suspend fun removeFromRetryQueue(retryId: String) {
        try {
            client.from("deletion_retry_queue").delete {
                filter { eq("id", retryId) }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from retry queue: $retryId", e)
        }
    }
    
    /**
     * Update retry schedule for an operation
     */
    private suspend fun updateRetrySchedule(retryId: String, nextRetry: Int, nextRetryTime: Long) {
        try {
            val updateData = buildJsonObject {
                put("current_retry", nextRetry)
                put("scheduled_retry_time", nextRetryTime)
            }
            
            client.from("deletion_retry_queue").update(updateData) {
                filter { eq("id", retryId) }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating retry schedule: $retryId", e)
        }
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
     * Get current retry queue status
     */
    suspend fun getRetryQueueStatus(): RetryQueueStatus {
        return try {
            val pendingCount = client.from("deletion_retry_queue")
                .select(columns = Columns.raw("id"))
                .decodeList<JsonObject>()
                .size
            
            val failedCount = client.from("deletion_operations")
                .select(columns = Columns.raw("id")) {
                    filter { eq("status", OperationStatus.FAILED.name) }
                }
                .decodeList<JsonObject>()
                .size
            
            RetryQueueStatus(
                pendingRetries = pendingCount,
                failedOperations = failedCount,
                isNetworkAvailable = _isNetworkAvailable.value,
                isProcessing = _isProcessingQueue.value
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting retry queue status", e)
            RetryQueueStatus(0, 0, false, false)
        }
    }
    
    /**
     * Manually trigger retry queue processing
     */
    fun triggerManualRetry() {
        coroutineScope.launch {
            if (_isNetworkAvailable.value) {
                processRetryQueue()
            } else {
                Log.w(TAG, "Cannot trigger manual retry - network not available")
            }
        }
    }
    
    /**
     * Queue a deletion operation for retry
     * Requirements: 4.2
     */
    suspend fun queueForRetry(operation: DeletionOperation): Boolean {
        return try {
            val retryTime = System.currentTimeMillis() + calculateRetryDelay(operation.retryCount)
            
            val retryData = buildJsonObject {
                put("id", java.util.UUID.randomUUID().toString())
                put("operation_id", operation.id)
                put("scheduled_retry_time", retryTime)
                put("max_retries", MAX_RETRY_ATTEMPTS)
                put("current_retry", operation.retryCount)
            }
            
            client.from("deletion_retry_queue").insert(retryData)
            
            Log.d(TAG, "Queued operation for retry: ${operation.id}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error queueing operation for retry: ${operation.id}", e)
            false
        }
    }
    
    /**
     * Schedule a retry for an operation with specific delay
     * Requirements: 4.2
     */
    suspend fun scheduleRetry(operation: DeletionOperation, delayMs: Long): Boolean {
        return try {
            val retryTime = System.currentTimeMillis() + delayMs
            
            val retryData = buildJsonObject {
                put("id", java.util.UUID.randomUUID().toString())
                put("operation_id", operation.id)
                put("scheduled_retry_time", retryTime)
                put("max_retries", MAX_RETRY_ATTEMPTS)
                put("current_retry", operation.retryCount)
            }
            
            client.from("deletion_retry_queue").insert(retryData)
            
            Log.d(TAG, "Scheduled retry for operation: ${operation.id} in ${delayMs}ms")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling retry for operation: ${operation.id}", e)
            false
        }
    }
    
    /**
     * Get failed operations for a user
     * Requirements: 4.4
     */
    suspend fun getFailedOperations(userId: String): List<DeletionOperation> {
        return try {
            val result = client.from("deletion_operations")
                .select(columns = Columns.raw("*")) {
                    filter { 
                        eq("user_id", userId)
                        eq("status", OperationStatus.FAILED.name)
                    }
                }
                .decodeList<JsonObject>()
            
            result.mapNotNull { jsonObject ->
                try {
                    DeletionOperation(
                        id = jsonObject["id"]?.toString() ?: "",
                        storageType = StorageType.valueOf(jsonObject["storage_type"]?.toString() ?: "REMOTE_DATABASE"),
                        status = OperationStatus.valueOf(jsonObject["status"]?.toString() ?: "FAILED"),
                        chatIds = jsonObject["chat_ids"]?.toString()?.let { 
                            if (it.startsWith("[") && it.endsWith("]")) {
                                it.substring(1, it.length - 1)
                                    .split(",")
                                    .map { chatId -> chatId.trim().removeSurrounding("\"") }
                                    .filter { chatId -> chatId.isNotEmpty() }
                            } else null
                        },
                        messagesAffected = jsonObject["messages_affected"]?.toString()?.toIntOrNull() ?: 0,
                        timestamp = jsonObject["created_at"]?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
                        retryCount = jsonObject["retry_count"]?.toString()?.toIntOrNull() ?: 0
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing operation from JSON", e)
                    null
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting failed operations for user: $userId", e)
            emptyList()
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            networkCallback?.let { callback ->
                connectivityManager.unregisterNetworkCallback(callback)
            }
            retryJob?.cancel()
            coroutineScope.cancel()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

/**
 * Status information for the retry queue
 */
data class RetryQueueStatus(
    val pendingRetries: Int,
    val failedOperations: Int,
    val isNetworkAvailable: Boolean,
    val isProcessing: Boolean
)