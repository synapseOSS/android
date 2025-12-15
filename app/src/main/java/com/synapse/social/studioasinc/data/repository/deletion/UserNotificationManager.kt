package com.synapse.social.studioasinc.data.repository.deletion

import com.synapse.social.studioasinc.data.model.deletion.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification types for deletion operations
 * Requirements: 1.4, 1.5, 4.4
 */
sealed class DeletionNotification {
    data class Started(val request: DeletionRequest) : DeletionNotification()
    data class Progress(val progress: DeletionProgress) : DeletionNotification()
    data class Completed(val request: DeletionRequest, val result: DeletionResult) : DeletionNotification()
    data class PartiallyCompleted(val request: DeletionRequest, val result: DeletionResult) : DeletionNotification()
    data class Failed(val request: DeletionRequest, val error: DeletionError) : DeletionNotification()
    data class Cancelled(val request: DeletionRequest) : DeletionNotification()
    data class RetryStarted(val operationCount: Int) : DeletionNotification()
    data class RetryCompleted(val successCount: Int, val failureCount: Int) : DeletionNotification()
    data class RetryFailed(val error: String) : DeletionNotification()
    data class RetryScheduled(val operation: DeletionOperation, val delayMs: Long) : DeletionNotification()
    data class MaxRetriesExceeded(val operation: DeletionOperation) : DeletionNotification()
    data class ErrorWithSuggestions(val error: DeletionError, val suggestions: List<RecoverySuggestion>) : DeletionNotification()
    
    // Enhanced notification types for comprehensive user feedback
    data class DetailedSuccess(
        val request: DeletionRequest,
        val result: DeletionResult,
        val storageLocations: List<StorageType>,
        val timestamp: Long
    ) : DeletionNotification()
    
    data class DetailedError(
        val request: DeletionRequest,
        val error: DeletionError,
        val context: String,
        val suggestions: List<RecoverySuggestion>,
        val timestamp: Long
    ) : DeletionNotification()
    
    data class NetworkStatusChange(
        val isConnected: Boolean,
        val pendingOperations: Int,
        val timestamp: Long
    ) : DeletionNotification()
    
    data class RetryQueueStatus(
        val queueSize: Int,
        val isProcessing: Boolean,
        val timestamp: Long
    ) : DeletionNotification()
}

/**
 * Interface for user notification management during deletion operations
 * Requirements: 1.4, 1.5, 4.4
 */
interface UserNotificationManager {
    
    /**
     * Get flow of deletion notifications
     * Requirements: 1.4, 1.5
     */
    fun getNotifications(): Flow<DeletionNotification>
    
    /**
     * Notify user that deletion has started
     * Requirements: 1.4
     */
    suspend fun notifyDeletionStarted(request: DeletionRequest)
    
    /**
     * Notify user of deletion progress
     * Requirements: 1.4
     */
    suspend fun notifyDeletionProgress(progress: DeletionProgress)
    
    /**
     * Notify user that deletion completed successfully
     * Requirements: 1.4
     */
    suspend fun notifyDeletionCompleted(request: DeletionRequest, result: DeletionResult)
    
    /**
     * Notify user that deletion partially completed
     * Requirements: 1.5, 4.4
     */
    suspend fun notifyDeletionPartiallyCompleted(request: DeletionRequest, result: DeletionResult)
    
    /**
     * Notify user that deletion failed
     * Requirements: 1.5
     */
    suspend fun notifyDeletionFailed(request: DeletionRequest, error: DeletionError)
    
    /**
     * Notify user that deletion was cancelled
     * Requirements: 1.4
     */
    suspend fun notifyDeletionCancelled(request: DeletionRequest)
    
    /**
     * Notify user that retry has started
     * Requirements: 4.4
     */
    suspend fun notifyRetryStarted(operationCount: Int)
    
    /**
     * Notify user that retry completed
     * Requirements: 4.4
     */
    suspend fun notifyRetryCompleted(successCount: Int, failureCount: Int)
    
    /**
     * Notify user that retry failed
     * Requirements: 4.4
     */
    suspend fun notifyRetryFailed(error: String)
    
    /**
     * Notify user that retry was scheduled
     * Requirements: 4.4
     */
    suspend fun notifyRetryScheduled(operation: DeletionOperation, delayMs: Long)
    
    /**
     * Notify user that max retries were exceeded
     * Requirements: 4.4
     */
    suspend fun notifyMaxRetriesExceeded(operation: DeletionOperation)
    
    /**
     * Notify user of error with recovery suggestions
     * Requirements: 1.5, 4.4
     */
    suspend fun notifyErrorWithSuggestions(error: DeletionError, suggestions: List<RecoverySuggestion>)
}

/**
 * Implementation of UserNotificationManager for comprehensive user feedback
 * Requirements: 1.4, 1.5, 4.4
 */
@Singleton
class UserNotificationManagerImpl @Inject constructor() : UserNotificationManager {
    
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _notifications = MutableSharedFlow<DeletionNotification>(
        replay = 1,
        extraBufferCapacity = 10
    )
    
    /**
     * Get flow of deletion notifications
     * Requirements: 1.4, 1.5
     */
    override fun getNotifications(): Flow<DeletionNotification> = _notifications.asSharedFlow()
    
    /**
     * Notify user that deletion has started
     * Requirements: 1.4
     */
    override suspend fun notifyDeletionStarted(request: DeletionRequest) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.Started(request))
        }
    }
    
    /**
     * Notify user of deletion progress
     * Requirements: 1.4
     */
    override suspend fun notifyDeletionProgress(progress: DeletionProgress) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.Progress(progress))
        }
    }
    
    /**
     * Notify user that deletion completed successfully
     * Requirements: 1.4
     */
    override suspend fun notifyDeletionCompleted(request: DeletionRequest, result: DeletionResult) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.Completed(request, result))
        }
    }
    
    /**
     * Notify user that deletion partially completed
     * Requirements: 1.5, 4.4
     */
    override suspend fun notifyDeletionPartiallyCompleted(request: DeletionRequest, result: DeletionResult) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.PartiallyCompleted(request, result))
        }
    }
    
    /**
     * Notify user that deletion failed
     * Requirements: 1.5
     */
    override suspend fun notifyDeletionFailed(request: DeletionRequest, error: DeletionError) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.Failed(request, error))
        }
    }
    
    /**
     * Notify user that deletion was cancelled
     * Requirements: 1.4
     */
    override suspend fun notifyDeletionCancelled(request: DeletionRequest) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.Cancelled(request))
        }
    }
    
    /**
     * Notify user that retry has started
     * Requirements: 4.4
     */
    override suspend fun notifyRetryStarted(operationCount: Int) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.RetryStarted(operationCount))
        }
    }
    
    /**
     * Notify user that retry completed
     * Requirements: 4.4
     */
    override suspend fun notifyRetryCompleted(successCount: Int, failureCount: Int) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.RetryCompleted(successCount, failureCount))
        }
    }
    
    /**
     * Notify user that retry failed
     * Requirements: 4.4
     */
    override suspend fun notifyRetryFailed(error: String) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.RetryFailed(error))
        }
    }
    
    /**
     * Notify user that retry was scheduled
     * Requirements: 4.4
     */
    override suspend fun notifyRetryScheduled(operation: DeletionOperation, delayMs: Long) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.RetryScheduled(operation, delayMs))
        }
    }
    
    /**
     * Notify user that max retries were exceeded
     * Requirements: 4.4
     */
    override suspend fun notifyMaxRetriesExceeded(operation: DeletionOperation) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.MaxRetriesExceeded(operation))
        }
    }
    
    /**
     * Notify user of error with recovery suggestions
     * Requirements: 1.5, 4.4
     */
    override suspend fun notifyErrorWithSuggestions(error: DeletionError, suggestions: List<RecoverySuggestion>) {
        notificationScope.launch {
            _notifications.emit(DeletionNotification.ErrorWithSuggestions(error, suggestions))
        }
    }
    
    /**
     * Notify user with detailed success summary
     * Requirements: 1.4, 6.5
     */
    suspend fun notifyDetailedSuccess(
        request: DeletionRequest, 
        result: DeletionResult, 
        storageLocations: List<StorageType>
    ) {
        notificationScope.launch {
            val detailedNotification = DeletionNotification.DetailedSuccess(
                request = request,
                result = result,
                storageLocations = storageLocations,
                timestamp = System.currentTimeMillis()
            )
            _notifications.emit(detailedNotification)
        }
    }
    
    /**
     * Notify user with comprehensive error details
     * Requirements: 1.5, 4.4
     */
    suspend fun notifyDetailedError(
        request: DeletionRequest,
        error: DeletionError,
        context: String,
        suggestions: List<RecoverySuggestion> = emptyList()
    ) {
        notificationScope.launch {
            val detailedNotification = DeletionNotification.DetailedError(
                request = request,
                error = error,
                context = context,
                suggestions = suggestions,
                timestamp = System.currentTimeMillis()
            )
            _notifications.emit(detailedNotification)
        }
    }
    
    /**
     * Notify user of network status changes affecting deletions
     * Requirements: 4.1, 4.5
     */
    suspend fun notifyNetworkStatusChange(isConnected: Boolean, pendingOperations: Int) {
        notificationScope.launch {
            val notification = DeletionNotification.NetworkStatusChange(
                isConnected = isConnected,
                pendingOperations = pendingOperations,
                timestamp = System.currentTimeMillis()
            )
            _notifications.emit(notification)
        }
    }
    
    /**
     * Notify user of automatic retry queue processing
     * Requirements: 4.2, 4.5
     */
    suspend fun notifyRetryQueueProcessing(queueSize: Int, isProcessing: Boolean) {
        notificationScope.launch {
            val notification = DeletionNotification.RetryQueueStatus(
                queueSize = queueSize,
                isProcessing = isProcessing,
                timestamp = System.currentTimeMillis()
            )
            _notifications.emit(notification)
        }
    }
}