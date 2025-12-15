package com.synapse.social.studioasinc.data.repository.deletion

import com.synapse.social.studioasinc.data.model.deletion.DeletionProgress
import com.synapse.social.studioasinc.data.model.deletion.StorageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for tracking progress of deletion operations
 * Requirements: 6.1, 6.2, 6.4
 */
interface ProgressTracker {
    
    /**
     * Initialize progress tracking for a new deletion operation
     * Requirements: 6.1
     */
    suspend fun initializeProgress(totalOperations: Int, estimatedMessages: Int)
    
    /**
     * Update progress for a specific storage system
     * Requirements: 6.1, 6.2
     */
    suspend fun updateStorageProgress(
        storageType: StorageType,
        messagesProcessed: Int,
        isCompleted: Boolean
    )
    
    /**
     * Update current operation description
     * Requirements: 6.1
     */
    suspend fun updateCurrentOperation(operation: String)
    
    /**
     * Update estimated time remaining
     * Requirements: 6.2
     */
    suspend fun updateEstimatedTime(timeRemaining: Long)
    
    /**
     * Mark operation as cancellable or not
     * Requirements: 6.4
     */
    suspend fun setCancellable(canCancel: Boolean)
    
    /**
     * Get current progress state
     * Requirements: 6.1
     */
    fun getProgress(): StateFlow<DeletionProgress?>
    
    /**
     * Reset progress tracking
     * Requirements: 6.1
     */
    suspend fun resetProgress()
    
    /**
     * Check if operation is cancelled
     * Requirements: 6.4
     */
    fun isCancelled(): Boolean
    
    /**
     * Set cancellation flag
     * Requirements: 6.4
     */
    suspend fun cancel()
}

/**
 * Implementation of progress tracking for deletion operations
 * Requirements: 6.1, 6.2, 6.4
 */
@Singleton
class ProgressTrackerImpl @Inject constructor() : ProgressTracker {
    
    private val _progress = MutableStateFlow<DeletionProgress?>(null)
    override fun getProgress(): StateFlow<DeletionProgress?> = _progress.asStateFlow()
    
    private val progressMutex = Mutex()
    private val isCancelled = AtomicBoolean(false)
    
    // Progress tracking state
    private val totalOperations = AtomicInteger(0)
    private val completedOperations = AtomicInteger(0)
    private val totalMessages = AtomicInteger(0)
    private val processedMessages = AtomicInteger(0)
    private val startTime = AtomicLong(0)
    
    // Storage-specific progress
    private val storageProgress = mutableMapOf<StorageType, StorageProgressInfo>()
    
    data class StorageProgressInfo(
        var messagesProcessed: Int = 0,
        var isCompleted: Boolean = false,
        var startTime: Long = 0
    )
    
    /**
     * Initialize progress tracking for a new deletion operation
     * Requirements: 6.1
     */
    override suspend fun initializeProgress(totalOperations: Int, estimatedMessages: Int) {
        progressMutex.withLock {
            isCancelled.set(false)
            this.totalOperations.set(totalOperations)
            completedOperations.set(0)
            totalMessages.set(estimatedMessages)
            processedMessages.set(0)
            startTime.set(System.currentTimeMillis())
            
            // Initialize storage progress
            storageProgress.clear()
            StorageType.values().forEach { storageType ->
                storageProgress[storageType] = StorageProgressInfo(
                    startTime = System.currentTimeMillis()
                )
            }
            
            updateProgressState(
                currentOperation = "Initializing deletion...",
                estimatedTime = calculateEstimatedTime(),
                canCancel = true
            )
        }
    }
    
    /**
     * Update progress for a specific storage system
     * Requirements: 6.1, 6.2
     */
    override suspend fun updateStorageProgress(
        storageType: StorageType,
        messagesProcessed: Int,
        isCompleted: Boolean
    ) {
        progressMutex.withLock {
            val storageInfo = storageProgress[storageType] ?: return
            
            // Update processed messages count
            val previousProcessed = storageInfo.messagesProcessed
            storageInfo.messagesProcessed = messagesProcessed
            processedMessages.addAndGet(messagesProcessed - previousProcessed)
            
            // Update completion status
            if (isCompleted && !storageInfo.isCompleted) {
                storageInfo.isCompleted = true
                completedOperations.incrementAndGet()
            }
            
            updateProgressState(
                currentOperation = generateCurrentOperationText(),
                estimatedTime = calculateEstimatedTime(),
                canCancel = !isCompleted
            )
        }
    }
    
    /**
     * Update current operation description
     * Requirements: 6.1
     */
    override suspend fun updateCurrentOperation(operation: String) {
        progressMutex.withLock {
            updateProgressState(
                currentOperation = operation,
                estimatedTime = calculateEstimatedTime(),
                canCancel = _progress.value?.canCancel ?: true
            )
        }
    }
    
    /**
     * Update estimated time remaining
     * Requirements: 6.2
     */
    override suspend fun updateEstimatedTime(timeRemaining: Long) {
        progressMutex.withLock {
            updateProgressState(
                currentOperation = _progress.value?.currentOperation,
                estimatedTime = timeRemaining,
                canCancel = _progress.value?.canCancel ?: true
            )
        }
    }
    
    /**
     * Mark operation as cancellable or not
     * Requirements: 6.4
     */
    override suspend fun setCancellable(canCancel: Boolean) {
        progressMutex.withLock {
            updateProgressState(
                currentOperation = _progress.value?.currentOperation,
                estimatedTime = _progress.value?.estimatedTimeRemaining,
                canCancel = canCancel
            )
        }
    }
    
    /**
     * Reset progress tracking
     * Requirements: 6.1
     */
    override suspend fun resetProgress() {
        progressMutex.withLock {
            isCancelled.set(false)
            totalOperations.set(0)
            completedOperations.set(0)
            totalMessages.set(0)
            processedMessages.set(0)
            startTime.set(0)
            storageProgress.clear()
            _progress.value = null
        }
    }
    
    /**
     * Check if operation is cancelled
     * Requirements: 6.4
     */
    override fun isCancelled(): Boolean = isCancelled.get()
    
    /**
     * Set cancellation flag
     * Requirements: 6.4
     */
    override suspend fun cancel() {
        isCancelled.set(true)
        progressMutex.withLock {
            updateProgressState(
                currentOperation = "Cancelling deletion...",
                estimatedTime = null,
                canCancel = false
            )
        }
    }
    
    // Private helper methods
    
    private fun updateProgressState(
        currentOperation: String?,
        estimatedTime: Long?,
        canCancel: Boolean
    ) {
        _progress.value = DeletionProgress(
            totalOperations = totalOperations.get(),
            completedOperations = completedOperations.get(),
            currentOperation = currentOperation,
            estimatedTimeRemaining = estimatedTime,
            canCancel = canCancel && !isCancelled.get()
        )
    }
    
    private fun generateCurrentOperationText(): String {
        val activeStorages = storageProgress.filter { !it.value.isCompleted }
        
        return when {
            activeStorages.isEmpty() -> "Completing deletion..."
            activeStorages.size == 1 -> {
                val storageType = activeStorages.keys.first()
                "Processing ${storageType.name.lowercase().replace('_', ' ')}..."
            }
            else -> {
                val storageNames = activeStorages.keys.joinToString(", ") { 
                    it.name.lowercase().replace('_', ' ')
                }
                "Processing multiple storages: $storageNames"
            }
        }
    }
    
    private fun calculateEstimatedTime(): Long? {
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - startTime.get()
        
        if (elapsed < 1000 || processedMessages.get() == 0) {
            // Not enough data for estimation
            return null
        }
        
        val totalMsgs = totalMessages.get()
        val processedMsgs = processedMessages.get()
        
        if (processedMsgs >= totalMsgs) {
            return 0L
        }
        
        // Calculate processing rate (messages per millisecond)
        val processingRate = processedMsgs.toDouble() / elapsed.toDouble()
        
        if (processingRate <= 0) {
            return null
        }
        
        // Estimate remaining time
        val remainingMessages = totalMsgs - processedMsgs
        val estimatedRemainingTime = (remainingMessages / processingRate).toLong()
        
        // Add some buffer (20%) for safety
        return (estimatedRemainingTime * 1.2).toLong()
    }
    
    /**
     * Get detailed progress information for debugging
     * Requirements: 6.1, 6.2
     */
    fun getDetailedProgress(): DetailedProgress {
        return DetailedProgress(
            totalOperations = totalOperations.get(),
            completedOperations = completedOperations.get(),
            totalMessages = totalMessages.get(),
            processedMessages = processedMessages.get(),
            elapsedTime = System.currentTimeMillis() - startTime.get(),
            storageProgress = storageProgress.toMap(),
            isCancelled = isCancelled.get()
        )
    }
    
    data class DetailedProgress(
        val totalOperations: Int,
        val completedOperations: Int,
        val totalMessages: Int,
        val processedMessages: Int,
        val elapsedTime: Long,
        val storageProgress: Map<StorageType, StorageProgressInfo>,
        val isCancelled: Boolean
    )
}

/**
 * Cancellation manager for handling cancellation requests
 * Requirements: 6.4
 */
@Singleton
class CancellationManager @Inject constructor(
    private val progressTracker: ProgressTracker
) {
    
    private val cancellationCallbacks = mutableListOf<suspend () -> Unit>()
    private val callbackMutex = Mutex()
    
    /**
     * Register a callback to be called when cancellation is requested
     * Requirements: 6.4
     */
    suspend fun registerCancellationCallback(callback: suspend () -> Unit) {
        callbackMutex.withLock {
            cancellationCallbacks.add(callback)
        }
    }
    
    /**
     * Unregister a cancellation callback
     * Requirements: 6.4
     */
    suspend fun unregisterCancellationCallback(callback: suspend () -> Unit) {
        callbackMutex.withLock {
            cancellationCallbacks.remove(callback)
        }
    }
    
    /**
     * Request cancellation of current operation
     * Requirements: 6.4
     */
    suspend fun requestCancellation(): Boolean {
        if (progressTracker.getProgress().value?.canCancel != true) {
            return false
        }
        
        progressTracker.cancel()
        
        // Execute all cancellation callbacks
        callbackMutex.withLock {
            cancellationCallbacks.forEach { callback ->
                try {
                    callback()
                } catch (e: Exception) {
                    // Log error but continue with other callbacks
                }
            }
        }
        
        return true
    }
    
    /**
     * Check if cancellation has been requested
     * Requirements: 6.4
     */
    fun isCancellationRequested(): Boolean = progressTracker.isCancelled()
    
    /**
     * Clear all cancellation callbacks
     * Requirements: 6.4
     */
    suspend fun clearCallbacks() {
        callbackMutex.withLock {
            cancellationCallbacks.clear()
        }
    }
}

/**
 * Time estimation utility for deletion operations
 * Requirements: 6.2
 */
object TimeEstimator {
    
    // Base processing rates (messages per second) for different storage types
    private const val LOCAL_DB_RATE = 2000.0
    private const val REMOTE_DB_RATE = 500.0
    private const val CACHE_RATE = 5000.0
    
    /**
     * Estimate time for deletion operation based on message count and storage types
     * Requirements: 6.2
     */
    fun estimateTime(
        messageCount: Int,
        storageTypes: List<StorageType>,
        networkLatency: Long = 100L
    ): Long {
        if (messageCount <= 0 || storageTypes.isEmpty()) {
            return 0L
        }
        
        val maxTime = storageTypes.maxOfOrNull { storageType ->
            val rate = when (storageType) {
                StorageType.LOCAL_DATABASE -> LOCAL_DB_RATE
                StorageType.REMOTE_DATABASE -> REMOTE_DB_RATE
                StorageType.CACHE_STORAGE -> CACHE_RATE
                StorageType.TEMPORARY_FILES -> CACHE_RATE
            }
            
            val baseTime = (messageCount / rate * 1000).toLong()
            
            // Add network latency for remote operations
            if (storageType == StorageType.REMOTE_DATABASE) {
                baseTime + networkLatency
            } else {
                baseTime
            }
        } ?: 0L
        
        // Add 20% buffer for safety
        return (maxTime * 1.2).toLong()
    }
    
    /**
     * Estimate time for batch operations
     * Requirements: 6.2
     */
    fun estimateBatchTime(
        batches: List<Pair<Int, List<StorageType>>>,
        networkLatency: Long = 100L
    ): Long {
        return batches.sumOf { (messageCount, storageTypes) ->
            estimateTime(messageCount, storageTypes, networkLatency)
        }
    }
    
    /**
     * Format time duration for display
     * Requirements: 6.2
     */
    fun formatDuration(milliseconds: Long): String {
        if (milliseconds < 0) return "Unknown"
        
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            seconds > 0 -> "${seconds}s"
            else -> "< 1s"
        }
    }
}