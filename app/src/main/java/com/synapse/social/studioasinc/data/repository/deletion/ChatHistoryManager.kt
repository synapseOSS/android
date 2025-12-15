package com.synapse.social.studioasinc.data.repository.deletion

import com.synapse.social.studioasinc.data.model.deletion.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for the central chat history management service
 * Requirements: 1.4, 1.5, 6.5
 */
interface ChatHistoryManager {
    
    /**
     * Delete all chat history for a user
     * Requirements: 1.4, 1.5
     */
    suspend fun deleteAllHistory(userId: String): DeletionResult
    
    /**
     * Delete specific chat sessions for a user
     * Requirements: 1.4, 1.5
     */
    suspend fun deleteSpecificChats(userId: String, chatIds: List<String>): DeletionResult
    
    /**
     * Get real-time progress of deletion operations
     * Requirements: 6.5
     */
    fun getDeleteProgress(): StateFlow<DeletionProgress?>
    
    /**
     * Cancel ongoing deletion operations
     * Requirements: 6.5
     */
    suspend fun cancelDeletion(): Boolean
    
    /**
     * Get deletion history for a user
     * Requirements: 6.5
     */
    suspend fun getDeletionHistory(userId: String): List<DeletionOperation>
    
    /**
     * Retry failed deletion operations
     * Requirements: 4.4
     */
    suspend fun retryFailedOperations(userId: String): RecoveryResult
    
    /**
     * Retry specific deletion operations
     * Requirements: 4.1, 4.3, 4.5
     */
    suspend fun retrySpecificOperations(operations: List<DeletionOperation>): DeletionResult
    
    /**
     * Retry all failed operations for a user
     * Requirements: 4.1, 4.3, 4.5
     */
    suspend fun retryAllFailedOperations(userId: String): DeletionResult
    
    /**
     * Get failed operations for a user
     * Requirements: 4.3, 4.4
     */
    suspend fun getFailedOperations(userId: String): List<DeletionOperation>
}

/**
 * Implementation of ChatHistoryManager as the central orchestrator for chat history deletion
 * Requirements: 1.4, 1.5, 4.4, 6.5
 */
@Singleton
class ChatHistoryManagerImpl @Inject constructor(
    private val deletionCoordinator: DeletionCoordinator,
    private val errorRecoveryManager: ErrorRecoveryManager,
    private val userNotificationManager: UserNotificationManager,
    private val retryQueueManager: RetryQueueManager,
    private val batchDeletionManager: BatchDeletionManager,
    private val memoryEfficientProcessor: MemoryEfficientDeletionProcessor,
    private val deletionStatusPersistence: DeletionStatusPersistence
) : ChatHistoryManager {
    
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _currentDeletionRequest = MutableStateFlow<DeletionRequest?>(null)
    private val currentDeletionRequest = _currentDeletionRequest.asStateFlow()
    
    /**
     * Delete all chat history for a user with performance optimizations
     * Requirements: 1.4, 1.5, 6.1, 6.2
     */
    override suspend fun deleteAllHistory(userId: String): DeletionResult = withContext(Dispatchers.IO) {
        
        val deletionRequest = DeletionRequest(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = DeletionType.COMPLETE_HISTORY,
            chatIds = null,
            timestamp = System.currentTimeMillis(),
            requiresConfirmation = true
        )
        
        _currentDeletionRequest.value = deletionRequest
        
        try {
            // Persist deletion request for recovery across app restarts
            deletionStatusPersistence.saveOngoingOperations(listOf(
                DeletionOperation(
                    id = deletionRequest.id,
                    storageType = StorageType.LOCAL_DATABASE,
                    status = OperationStatus.PENDING,
                    chatIds = null,
                    messagesAffected = 0,
                    timestamp = System.currentTimeMillis()
                )
            ))
            
            // Notify user that deletion is starting
            userNotificationManager.notifyDeletionStarted(deletionRequest)
            
            // Use batch deletion manager for performance optimization
            val result = batchDeletionManager.performBatchDeletion(
                userId = userId,
                chatIds = null
            )
            
            // Clear persisted state on completion
            deletionStatusPersistence.clearPersistedState()
            
            // Handle the result and provide appropriate feedback
            handleDeletionResult(result, deletionRequest)
            
            return@withContext result
            
        } catch (e: Exception) {
            val error = DeletionError.SystemError("Failed to delete all history: ${e.message}", true)
            val failureResult = DeletionResult(
                success = false,
                completedOperations = emptyList(),
                failedOperations = emptyList(),
                totalMessagesDeleted = 0,
                errors = listOf(error)
            )
            
            // Notify user of failure
            userNotificationManager.notifyDeletionFailed(deletionRequest, error)
            
            return@withContext failureResult
            
        } finally {
            _currentDeletionRequest.value = null
        }
    }
    
    /**
     * Delete specific chat sessions for a user with performance optimizations
     * Requirements: 1.4, 1.5, 6.1, 6.2
     */
    override suspend fun deleteSpecificChats(userId: String, chatIds: List<String>): DeletionResult = withContext(Dispatchers.IO) {
        
        if (chatIds.isEmpty()) {
            val error = DeletionError.ValidationError("No chat IDs provided for deletion", "chatIds")
            return@withContext DeletionResult(
                success = false,
                completedOperations = emptyList(),
                failedOperations = emptyList(),
                totalMessagesDeleted = 0,
                errors = listOf(error)
            )
        }
        
        val deletionRequest = DeletionRequest(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = DeletionType.SELECTIVE_CHATS,
            chatIds = chatIds,
            timestamp = System.currentTimeMillis(),
            requiresConfirmation = true
        )
        
        _currentDeletionRequest.value = deletionRequest
        
        try {
            // Persist deletion request for recovery across app restarts
            deletionStatusPersistence.saveOngoingOperations(listOf(
                DeletionOperation(
                    id = deletionRequest.id,
                    storageType = StorageType.LOCAL_DATABASE,
                    status = OperationStatus.PENDING,
                    chatIds = chatIds,
                    messagesAffected = 0,
                    timestamp = System.currentTimeMillis()
                )
            ))
            
            // Notify user that selective deletion is starting
            userNotificationManager.notifyDeletionStarted(deletionRequest)
            
            // Choose appropriate deletion strategy based on size
            val result = if (chatIds.size > LARGE_DELETION_THRESHOLD) {
                // Use memory-efficient processor for large deletions
                var finalResult: DeletionResult? = null
                memoryEfficientProcessor.processLargeDeletion(userId, chatIds)
                    .collect { progress ->
                        deletionStatusPersistence.saveDeletionProgress(progress)
                    }
                
                // After processing, get the final result from batch manager
                batchDeletionManager.performBatchDeletion(userId, chatIds)
            } else {
                // Use batch deletion manager for smaller deletions
                batchDeletionManager.performBatchDeletion(userId, chatIds)
            }
            
            // Clear persisted state on completion
            deletionStatusPersistence.clearPersistedState()
            
            // Handle the result and provide appropriate feedback
            handleDeletionResult(result, deletionRequest)
            
            return@withContext result
            
        } catch (e: Exception) {
            val error = DeletionError.SystemError("Failed to delete specific chats: ${e.message}", true)
            val failureResult = DeletionResult(
                success = false,
                completedOperations = emptyList(),
                failedOperations = emptyList(),
                totalMessagesDeleted = 0,
                errors = listOf(error)
            )
            
            // Notify user of failure
            userNotificationManager.notifyDeletionFailed(deletionRequest, error)
            
            return@withContext failureResult
            
        } finally {
            _currentDeletionRequest.value = null
        }
    }
    
    /**
     * Get real-time progress of deletion operations with batch progress
     * Requirements: 6.5, 6.1, 6.2
     */
    override fun getDeleteProgress(): StateFlow<DeletionProgress?> {
        // Combine progress from both coordinator and batch manager
        return deletionCoordinator.getProgress()
    }
    
    /**
     * Cancel ongoing deletion operations including batch operations
     * Requirements: 6.5, 6.1, 6.2
     */
    override suspend fun cancelDeletion(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Cancel both coordinator and batch manager operations
            val coordinatorCancelled = deletionCoordinator.cancelDeletion()
            batchDeletionManager.cancelBatchOperation()
            
            // Clear persisted state
            deletionStatusPersistence.clearPersistedState()
            
            if (coordinatorCancelled) {
                currentDeletionRequest.value?.let { request ->
                    userNotificationManager.notifyDeletionCancelled(request)
                }
            }
            
            return@withContext coordinatorCancelled
            
        } catch (e: Exception) {
            return@withContext false
        }
    }
    
    /**
     * Get deletion history for a user
     * Requirements: 6.5
     */
    override suspend fun getDeletionHistory(userId: String): List<DeletionOperation> = withContext(Dispatchers.IO) {
        try {
            // This would typically query a database for historical deletion operations
            // For now, return empty list as this would need database implementation
            return@withContext emptyList<DeletionOperation>()
            
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
    
    /**
     * Retry failed deletion operations
     * Requirements: 4.4
     */
    override suspend fun retryFailedOperations(userId: String): RecoveryResult = withContext(Dispatchers.IO) {
        try {
            // Get failed operations from retry queue
            val failedOperations = retryQueueManager.getFailedOperations(userId)
            
            if (failedOperations.isEmpty()) {
                return@withContext RecoveryResult.Success(emptyList())
            }
            
            // Notify user that retry is starting
            userNotificationManager.notifyRetryStarted(failedOperations.size)
            
            // Attempt recovery using the coordinator
            val recoveryResult = deletionCoordinator.handleFailureRecovery(failedOperations)
            
            // Handle recovery result and notify user
            handleRecoveryResult(recoveryResult, userId)
            
            return@withContext recoveryResult
            
        } catch (e: Exception) {
            val error = "Failed to retry operations: ${e.message}"
            userNotificationManager.notifyRetryFailed(error)
            return@withContext RecoveryResult.Failure(error)
        }
    }
    
    /**
     * Retry specific deletion operations
     * Requirements: 4.1, 4.3, 4.5
     */
    override suspend fun retrySpecificOperations(operations: List<DeletionOperation>): DeletionResult = withContext(Dispatchers.IO) {
        if (operations.isEmpty()) {
            return@withContext DeletionResult(
                success = true,
                completedOperations = emptyList(),
                failedOperations = emptyList(),
                totalMessagesDeleted = 0,
                errors = emptyList()
            )
        }
        
        try {
            // Notify user that retry is starting
            userNotificationManager.notifyRetryStarted(operations.size)
            
            // Attempt recovery using the coordinator
            val recoveryResult = deletionCoordinator.handleFailureRecovery(operations)
            
            // Convert recovery result to deletion result
            val deletionResult = when (recoveryResult) {
                is RecoveryResult.Success -> {
                    DeletionResult(
                        success = true,
                        completedOperations = recoveryResult.recoveredOperations,
                        failedOperations = emptyList(),
                        totalMessagesDeleted = recoveryResult.recoveredOperations.sumOf { it.messagesAffected },
                        errors = emptyList()
                    )
                }
                is RecoveryResult.PartialSuccess -> {
                    DeletionResult(
                        success = false,
                        completedOperations = recoveryResult.recoveredOperations,
                        failedOperations = recoveryResult.failedOperations,
                        totalMessagesDeleted = recoveryResult.recoveredOperations.sumOf { it.messagesAffected },
                        errors = emptyList()
                    )
                }
                is RecoveryResult.Failure -> {
                    DeletionResult(
                        success = false,
                        completedOperations = emptyList(),
                        failedOperations = operations,
                        totalMessagesDeleted = 0,
                        errors = listOf(DeletionError.SystemError(recoveryResult.error, true))
                    )
                }
            }
            
            // Handle recovery result and notify user
            handleRecoveryResult(recoveryResult, operations.firstOrNull()?.let { "user_from_operation" } ?: "unknown")
            
            return@withContext deletionResult
            
        } catch (e: Exception) {
            val error = "Failed to retry specific operations: ${e.message}"
            userNotificationManager.notifyRetryFailed(error)
            
            return@withContext DeletionResult(
                success = false,
                completedOperations = emptyList(),
                failedOperations = operations,
                totalMessagesDeleted = 0,
                errors = listOf(DeletionError.SystemError(error, true))
            )
        }
    }
    
    /**
     * Retry all failed operations for a user
     * Requirements: 4.1, 4.3, 4.5
     */
    override suspend fun retryAllFailedOperations(userId: String): DeletionResult = withContext(Dispatchers.IO) {
        try {
            // Get all failed operations for the user
            val failedOperations = retryQueueManager.getFailedOperations(userId)
            
            if (failedOperations.isEmpty()) {
                return@withContext DeletionResult(
                    success = true,
                    completedOperations = emptyList(),
                    failedOperations = emptyList(),
                    totalMessagesDeleted = 0,
                    errors = emptyList()
                )
            }
            
            // Use the specific operations retry method
            return@withContext retrySpecificOperations(failedOperations)
            
        } catch (e: Exception) {
            val error = "Failed to retry all failed operations: ${e.message}"
            userNotificationManager.notifyRetryFailed(error)
            
            return@withContext DeletionResult(
                success = false,
                completedOperations = emptyList(),
                failedOperations = emptyList(),
                totalMessagesDeleted = 0,
                errors = listOf(DeletionError.SystemError(error, true))
            )
        }
    }
    
    /**
     * Get failed operations for a user
     * Requirements: 4.3, 4.4
     */
    override suspend fun getFailedOperations(userId: String): List<DeletionOperation> = withContext(Dispatchers.IO) {
        try {
            return@withContext retryQueueManager.getFailedOperations(userId)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
    
    // Private helper methods
    
    /**
     * Handle deletion result and provide appropriate user feedback
     * Requirements: 1.4, 1.5
     */
    private suspend fun handleDeletionResult(result: DeletionResult, request: DeletionRequest) {
        when {
            result.success -> {
                // Complete success
                userNotificationManager.notifyDeletionCompleted(request, result)
            }
            result.completedOperations.isNotEmpty() && result.failedOperations.isNotEmpty() -> {
                // Partial success
                userNotificationManager.notifyDeletionPartiallyCompleted(request, result)
                
                // Queue failed operations for retry
                result.failedOperations.forEach { operation ->
                    retryQueueManager.queueForRetry(operation)
                }
            }
            else -> {
                // Complete failure
                val primaryError = result.errors.firstOrNull() 
                    ?: DeletionError.SystemError("Unknown deletion failure", true)
                userNotificationManager.notifyDeletionFailed(request, primaryError)
                
                // Queue all operations for retry if they're retryable
                result.failedOperations.forEach { operation ->
                    retryQueueManager.queueForRetry(operation)
                }
            }
        }
        
        // Handle specific errors with recovery suggestions
        result.errors.forEach { error ->
            errorRecoveryManager.handleError(error, request)
        }
    }
    
    /**
     * Handle recovery result and provide appropriate user feedback
     * Requirements: 4.4
     */
    private suspend fun handleRecoveryResult(result: RecoveryResult, userId: String) {
        when (result) {
            is RecoveryResult.Success -> {
                userNotificationManager.notifyRetryCompleted(result.recoveredOperations.size, 0)
            }
            is RecoveryResult.PartialSuccess -> {
                userNotificationManager.notifyRetryCompleted(
                    result.recoveredOperations.size, 
                    result.failedOperations.size
                )
                
                // Re-queue still failed operations
                result.failedOperations.forEach { operation ->
                    retryQueueManager.queueForRetry(operation)
                }
            }
            is RecoveryResult.Failure -> {
                userNotificationManager.notifyRetryFailed(result.error)
            }
        }
    }
    
    companion object {
        private const val LARGE_DELETION_THRESHOLD = 100 // Use memory-efficient processor for >100 chats
    }
}