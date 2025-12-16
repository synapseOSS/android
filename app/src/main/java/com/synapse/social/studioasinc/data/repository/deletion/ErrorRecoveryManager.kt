package com.synapse.social.studioasinc.data.repository.deletion

import com.synapse.social.studioasinc.data.model.deletion.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recovery action to be taken for a failed operation
 * Requirements: 4.3, 4.4
 */
sealed class RecoveryAction {
    object Retry : RecoveryAction()
    object Skip : RecoveryAction()
    data class RetryWithDelay(val delayMs: Long) : RecoveryAction()
    data class NotifyUser(val message: String, val actionRequired: Boolean) : RecoveryAction()
    object Abort : RecoveryAction()
}

/**
 * Recovery suggestion for user
 * Requirements: 4.4
 */
data class RecoverySuggestion(
    val title: String,
    val description: String,
    val actionText: String?,
    val isAutomaticRetry: Boolean,
    val priority: RecoveryPriority
)

enum class RecoveryPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Interface for error recovery management
 * Requirements: 1.5, 4.4
 */
interface ErrorRecoveryManager {
    
    /**
     * Handle a deletion error and determine recovery action
     * Requirements: 4.4
     */
    suspend fun handleError(error: DeletionError, request: DeletionRequest): RecoveryAction
    
    /**
     * Schedule retry for a failed operation
     * Requirements: 4.4
     */
    suspend fun scheduleRetry(operation: DeletionOperation, delay: Long): Boolean
    
    /**
     * Get recovery suggestions for an error
     * Requirements: 1.5, 4.4
     */
    suspend fun getRecoverySuggestions(error: DeletionError): List<RecoverySuggestion>
    
    /**
     * Check if an error is recoverable
     * Requirements: 4.4
     */
    fun isRecoverable(error: DeletionError): Boolean
    
    /**
     * Get retry delay for an operation based on retry count
     * Requirements: 4.4
     */
    fun getRetryDelay(retryCount: Int): Long
}

/**
 * Implementation of ErrorRecoveryManager for comprehensive error handling
 * Requirements: 1.5, 4.4
 */
@Singleton
class ErrorRecoveryManagerImpl @Inject constructor(
    private val retryQueueManager: RetryQueueManager,
    private val connectivityMonitor: ConnectivityMonitor,
    private val userNotificationManager: UserNotificationManager
) : ErrorRecoveryManager {
    
    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val BASE_RETRY_DELAY_MS = 1000L // 1 second
        private const val MAX_RETRY_DELAY_MS = 30000L // 30 seconds
        private const val MAX_RETRY_COUNT = 3
    }
    
    /**
     * Handle a deletion error and determine recovery action
     * Requirements: 4.4
     */
    override suspend fun handleError(error: DeletionError, request: DeletionRequest): RecoveryAction = withContext(Dispatchers.IO) {
        
        when (error) {
            is DeletionError.NetworkError -> {
                handleNetworkError(error, request)
            }
            is DeletionError.DatabaseError -> {
                handleDatabaseError(error, request)
            }
            is DeletionError.ValidationError -> {
                handleValidationError(error, request)
            }
            is DeletionError.SystemError -> {
                handleSystemError(error, request)
            }
        }
    }
    
    /**
     * Schedule retry for a failed operation
     * Requirements: 4.4
     */
    override suspend fun scheduleRetry(operation: DeletionOperation, delay: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            if (operation.retryCount >= MAX_RETRY_COUNT) {
                // Max retries exceeded, notify user
                userNotificationManager.notifyMaxRetriesExceeded(operation)
                return@withContext false
            }
            
            // Schedule the retry
            val success = retryQueueManager.scheduleRetry(operation, delay)
            
            if (success) {
                userNotificationManager.notifyRetryScheduled(operation, delay)
            }
            
            return@withContext success
            
        } catch (e: Exception) {
            return@withContext false
        }
    }
    
    /**
     * Get recovery suggestions for an error
     * Requirements: 1.5, 4.4
     */
    override suspend fun getRecoverySuggestions(error: DeletionError): List<RecoverySuggestion> = withContext(Dispatchers.IO) {
        
        when (error) {
            is DeletionError.NetworkError -> {
                listOf(
                    RecoverySuggestion(
                        title = "Network Connection Issue",
                        description = "Unable to connect to the server. The operation will be retried automatically when connection is restored.",
                        actionText = "Check Connection",
                        isAutomaticRetry = true,
                        priority = RecoveryPriority.MEDIUM
                    ),
                    RecoverySuggestion(
                        title = "Manual Retry",
                        description = "You can manually retry the operation now if you believe the connection issue is resolved.",
                        actionText = "Retry Now",
                        isAutomaticRetry = false,
                        priority = RecoveryPriority.LOW
                    )
                )
            }
            is DeletionError.DatabaseError -> {
                when (error.storageType) {
                    StorageType.LOCAL_DATABASE -> listOf(
                        RecoverySuggestion(
                            title = "Local Database Issue",
                            description = "There was a problem accessing the local database. This may be due to insufficient storage space or database corruption.",
                            actionText = "Check Storage",
                            isAutomaticRetry = false,
                            priority = RecoveryPriority.HIGH
                        )
                    )
                    StorageType.REMOTE_DATABASE -> listOf(
                        RecoverySuggestion(
                            title = "Remote Database Issue",
                            description = "There was a problem with the remote database. The operation will be retried automatically.",
                            actionText = "Retry Later",
                            isAutomaticRetry = true,
                            priority = RecoveryPriority.MEDIUM
                        )
                    )
                    else -> emptyList()
                }
            }
            is DeletionError.ValidationError -> {
                listOf(
                    RecoverySuggestion(
                        title = "Invalid Data",
                        description = "The deletion request contains invalid data: ${error.message}",
                        actionText = "Review Selection",
                        isAutomaticRetry = false,
                        priority = RecoveryPriority.HIGH
                    )
                )
            }
            is DeletionError.SystemError -> {
                if (error.recoverable) {
                    listOf(
                        RecoverySuggestion(
                            title = "System Error",
                            description = "A system error occurred but the operation can be retried: ${error.message}",
                            actionText = "Retry",
                            isAutomaticRetry = true,
                            priority = RecoveryPriority.MEDIUM
                        )
                    )
                } else {
                    listOf(
                        RecoverySuggestion(
                            title = "Critical System Error",
                            description = "A critical system error occurred: ${error.message}. Please contact support if this persists.",
                            actionText = "Contact Support",
                            isAutomaticRetry = false,
                            priority = RecoveryPriority.CRITICAL
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Check if an error is recoverable
     * Requirements: 4.4
     */
    override fun isRecoverable(error: DeletionError): Boolean {
        return when (error) {
            is DeletionError.NetworkError -> error.retryable
            is DeletionError.DatabaseError -> true // Most database errors are retryable
            is DeletionError.ValidationError -> false // Validation errors need user intervention
            is DeletionError.SystemError -> error.recoverable
        }
    }
    
    /**
     * Get retry delay for an operation based on retry count (exponential backoff)
     * Requirements: 4.4
     */
    override fun getRetryDelay(retryCount: Int): Long {
        val delay = BASE_RETRY_DELAY_MS * (1L shl retryCount) // Exponential backoff: 1s, 2s, 4s, 8s...
        return minOf(delay, MAX_RETRY_DELAY_MS)
    }
    
    // Private helper methods for specific error types
    
    private suspend fun handleNetworkError(error: DeletionError.NetworkError, request: DeletionRequest): RecoveryAction {
        return if (error.retryable) {
            if (connectivityMonitor.isConnected.value) {
                // Connection is available, retry immediately
                RecoveryAction.Retry
            } else {
                // No connection, wait for connectivity
                RecoveryAction.NotifyUser(
                    "Network connection lost. Deletion will resume when connection is restored.",
                    false
                )
            }
        } else {
            RecoveryAction.NotifyUser(
                "Network error occurred: ${error.message}. Please try again later.",
                true
            )
        }
    }
    
    private suspend fun handleDatabaseError(error: DeletionError.DatabaseError, request: DeletionRequest): RecoveryAction {
        return when (error.storageType) {
            StorageType.LOCAL_DATABASE -> {
                // Local database errors might indicate storage issues
                RecoveryAction.NotifyUser(
                    "Local storage error: ${error.message}. Please check available storage space.",
                    true
                )
            }
            StorageType.REMOTE_DATABASE -> {
                // Remote database errors are usually retryable
                RecoveryAction.RetryWithDelay(getRetryDelay(0))
            }
            else -> {
                RecoveryAction.NotifyUser(
                    "Database error: ${error.message}",
                    true
                )
            }
        }
    }
    
    private suspend fun handleValidationError(error: DeletionError.ValidationError, request: DeletionRequest): RecoveryAction {
        // Validation errors require user intervention
        return RecoveryAction.NotifyUser(
            "Invalid request: ${error.message}. Please check your selection and try again.",
            true
        )
    }
    
    private suspend fun handleSystemError(error: DeletionError.SystemError, request: DeletionRequest): RecoveryAction {
        return if (error.recoverable) {
            RecoveryAction.RetryWithDelay(getRetryDelay(0))
        } else {
            RecoveryAction.NotifyUser(
                "System error: ${error.message}. Please contact support if this persists.",
                true
            )
        }
    }
}