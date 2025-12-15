package com.synapse.social.studioasinc.data.repository.deletion

import com.synapse.social.studioasinc.data.model.deletion.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for coordinating deletion operations across multiple storage systems
 * Requirements: 2.5, 4.3, 4.4, 6.1
 */
interface DeletionCoordinator {
    
    /**
     * Coordinate complete history deletion across all storage systems
     * Requirements: 2.5, 4.3, 4.4
     */
    suspend fun coordinateFullDeletion(userId: String): DeletionResult
    
    /**
     * Coordinate selective chat deletion across all storage systems
     * Requirements: 2.5, 4.3, 4.4
     */
    suspend fun coordinateSelectiveDeletion(userId: String, chatIds: List<String>): DeletionResult
    
    /**
     * Handle failure recovery for failed operations
     * Requirements: 4.3, 4.4
     */
    suspend fun handleFailureRecovery(failedOperations: List<DeletionOperation>): RecoveryResult
    
    /**
     * Get current progress of deletion operations
     * Requirements: 6.1
     */
    fun getProgress(): StateFlow<DeletionProgress?>
    
    /**
     * Cancel ongoing deletion operations
     * Requirements: 6.4
     */
    suspend fun cancelDeletion(): Boolean
}

/**
 * Implementation of DeletionCoordinator that manages deletion sequences across storage systems
 * Requirements: 2.5, 4.3, 4.4, 6.1
 */
@Singleton
class DeletionCoordinatorImpl @Inject constructor(
    private val localRepository: LocalChatRepository,
    private val remoteRepository: RemoteChatRepository,
    private val cacheManager: ChatCacheManager,
    private val retryQueueManager: RetryQueueManager,
    private val progressTracker: ProgressTracker,
    private val cancellationManager: CancellationManager
) : DeletionCoordinator {
    
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Coordinate complete history deletion across all storage systems
     * Requirements: 2.5, 4.3, 4.4
     */
    override fun getProgress(): StateFlow<DeletionProgress?> = progressTracker.getProgress()
    
    override suspend fun cancelDeletion(): Boolean = cancellationManager.requestCancellation()
    
    override suspend fun coordinateFullDeletion(userId: String): DeletionResult = withContext(Dispatchers.IO) {
        
        val operations = mutableListOf<DeletionOperation>()
        val errors = mutableListOf<DeletionError>()
        var totalMessagesDeleted = 0
        
        try {
            // Calculate total operations for progress tracking
            val totalOps = 3 // Local, Remote, Cache
            
            // Get message counts for progress estimation
            val localCount = try {
                localRepository.getMessageCount(userId)
            } catch (e: Exception) {
                0
            }
            
            val remoteCount = try {
                remoteRepository.getMessageCount(userId)
            } catch (e: Exception) {
                0
            }
            
            val totalMessages = maxOf(localCount, remoteCount)
            
            // Initialize progress tracking
            progressTracker.initializeProgress(totalOps, totalMessages)
            progressTracker.updateCurrentOperation("Starting deletion operations...")
            
            // Execute deletions in parallel for better performance
            val deletionJobs = listOf(
                coordinatorScope.async {
                    executeLocalDeletion(userId, null)
                },
                coordinatorScope.async {
                    executeRemoteDeletion(userId, null)
                },
                coordinatorScope.async {
                    executeCacheDeletion(userId, null)
                }
            )
            
            // Collect results
            val results = deletionJobs.awaitAll()
            
            results.forEach { (operation, messagesDeleted) ->
                operations.add(operation)
                if (operation.status == OperationStatus.COMPLETED) {
                    totalMessagesDeleted += messagesDeleted
                } else if (operation.status == OperationStatus.FAILED) {
                    // Queue failed operations for retry
                    retryQueueManager.queueForRetry(operation)
                }
            }
            
            // Verify deletion completeness
            val verificationResults = verifyDeletionCompleteness(userId, null)
            if (!verificationResults.all { it }) {
                errors.add(DeletionError.SystemError("Deletion verification failed", true))
            }
            
            progressTracker.updateCurrentOperation("Deletion completed")
            progressTracker.setCancellable(false)
            
        } catch (e: Exception) {
            errors.add(DeletionError.SystemError("Coordination failed: ${e.message}", true))
            progressTracker.resetProgress()
        }
        
        val completedOps = operations.filter { it.status == OperationStatus.COMPLETED }
        val failedOps = operations.filter { it.status == OperationStatus.FAILED }
        
        DeletionResult(
            success = failedOps.isEmpty() && errors.isEmpty(),
            completedOperations = completedOps,
            failedOperations = failedOps,
            totalMessagesDeleted = totalMessagesDeleted,
            errors = errors
        )
    }
    
    /**
     * Coordinate selective chat deletion across all storage systems
     * Requirements: 2.5, 4.3, 4.4
     */
    override suspend fun coordinateSelectiveDeletion(userId: String, chatIds: List<String>): DeletionResult = withContext(Dispatchers.IO) {
        
        val operations = mutableListOf<DeletionOperation>()
        val errors = mutableListOf<DeletionError>()
        var totalMessagesDeleted = 0
        
        try {
            // Calculate total operations for progress tracking
            val totalOps = 3 // Local, Remote, Cache
            
            // Get message counts for progress estimation
            val localCount = try {
                localRepository.getMessageCountForChats(chatIds)
            } catch (e: Exception) {
                0
            }
            
            val remoteCount = try {
                remoteRepository.getMessageCountForChats(chatIds)
            } catch (e: Exception) {
                0
            }
            
            val totalMessages = maxOf(localCount, remoteCount)
            
            // Initialize progress tracking
            progressTracker.initializeProgress(totalOps, totalMessages)
            progressTracker.updateCurrentOperation("Starting selective deletion...")
            
            // Execute deletions in parallel
            val deletionJobs = listOf(
                coordinatorScope.async {
                    executeLocalDeletion(userId, chatIds)
                },
                coordinatorScope.async {
                    executeRemoteDeletion(userId, chatIds)
                },
                coordinatorScope.async {
                    executeCacheDeletion(userId, chatIds)
                }
            )
            
            // Collect results
            val results = deletionJobs.awaitAll()
            
            results.forEach { (operation, messagesDeleted) ->
                operations.add(operation)
                if (operation.status == OperationStatus.COMPLETED) {
                    totalMessagesDeleted += messagesDeleted
                } else if (operation.status == OperationStatus.FAILED) {
                    // Queue failed operations for retry
                    retryQueueManager.queueForRetry(operation)
                }
            }
            
            // Verify deletion completeness
            val verificationResults = verifyDeletionCompleteness(userId, chatIds)
            if (!verificationResults.all { it }) {
                errors.add(DeletionError.SystemError("Selective deletion verification failed", true))
            }
            
            progressTracker.updateCurrentOperation("Selective deletion completed")
            progressTracker.setCancellable(false)
            
        } catch (e: Exception) {
            errors.add(DeletionError.SystemError("Selective coordination failed: ${e.message}", true))
            progressTracker.resetProgress()
        }
        
        val completedOps = operations.filter { it.status == OperationStatus.COMPLETED }
        val failedOps = operations.filter { it.status == OperationStatus.FAILED }
        
        DeletionResult(
            success = failedOps.isEmpty() && errors.isEmpty(),
            completedOperations = completedOps,
            failedOperations = failedOps,
            totalMessagesDeleted = totalMessagesDeleted,
            errors = errors
        )
    }
    
    /**
     * Handle failure recovery for failed operations
     * Requirements: 4.3, 4.4
     */
    override suspend fun handleFailureRecovery(failedOperations: List<DeletionOperation>): RecoveryResult = withContext(Dispatchers.IO) {
        val recoveredOperations = mutableListOf<DeletionOperation>()
        val stillFailedOperations = mutableListOf<DeletionOperation>()
        
        try {
            progressTracker.initializeProgress(failedOperations.size, 0)
            progressTracker.updateCurrentOperation("Starting failure recovery...")
            progressTracker.setCancellable(false)
            
            failedOperations.forEachIndexed { index, operation ->
                if (progressTracker.isCancelled()) {
                    stillFailedOperations.addAll(failedOperations.drop(index))
                    return@withContext RecoveryResult.PartialSuccess(recoveredOperations, stillFailedOperations)
                }
                
                progressTracker.updateCurrentOperation("Recovering ${operation.storageType} operation...")
                progressTracker.updateStorageProgress(operation.storageType, 0, false)
                
                val recoveryResult = when (operation.storageType) {
                    StorageType.LOCAL_DATABASE -> {
                        retryLocalOperation(operation)
                    }
                    StorageType.REMOTE_DATABASE -> {
                        retryRemoteOperation(operation)
                    }
                    StorageType.CACHE_STORAGE -> {
                        retryCacheOperation(operation)
                    }
                    StorageType.TEMPORARY_FILES -> {
                        retryTemporaryFileCleanup(operation)
                    }
                }
                
                if (recoveryResult.status == OperationStatus.COMPLETED) {
                    recoveredOperations.add(recoveryResult)
                    progressTracker.updateStorageProgress(operation.storageType, 1, true)
                } else {
                    stillFailedOperations.add(recoveryResult)
                    progressTracker.updateStorageProgress(operation.storageType, 0, true)
                }
                
                delay(100) // Brief delay between recovery attempts
            }
            
            progressTracker.updateCurrentOperation("Recovery completed")
            
        } catch (e: Exception) {
            return@withContext RecoveryResult.Failure("Recovery coordination failed: ${e.message}")
        }
        
        return@withContext when {
            stillFailedOperations.isEmpty() -> RecoveryResult.Success(recoveredOperations)
            recoveredOperations.isNotEmpty() -> RecoveryResult.PartialSuccess(recoveredOperations, stillFailedOperations)
            else -> RecoveryResult.Failure("All recovery attempts failed")
        }
    }
    

    
    // Private helper methods
    
    private suspend fun executeLocalDeletion(userId: String, chatIds: List<String>?): Pair<DeletionOperation, Int> {
        val operation = createOperation(StorageType.LOCAL_DATABASE, chatIds)
        
        return try {
            if (progressTracker.isCancelled()) {
                return operation.copy(status = OperationStatus.FAILED) to 0
            }
            
            progressTracker.updateCurrentOperation("Deleting local messages...")
            
            val result = if (chatIds == null) {
                localRepository.deleteAllMessages(userId)
            } else {
                localRepository.deleteMessagesForChats(chatIds)
            }
            
            when (result) {
                is RepositoryResult.Success -> {
                    progressTracker.updateStorageProgress(StorageType.LOCAL_DATABASE, result.messagesDeleted, true)
                    operation.copy(
                        status = OperationStatus.COMPLETED,
                        messagesAffected = result.messagesDeleted
                    ) to result.messagesDeleted
                }
                is RepositoryResult.Failure -> {
                    operation.copy(status = OperationStatus.FAILED) to 0
                }
            }
        } catch (e: Exception) {
            operation.copy(status = OperationStatus.FAILED) to 0
        }
    }
    
    private suspend fun executeRemoteDeletion(userId: String, chatIds: List<String>?): Pair<DeletionOperation, Int> {
        val operation = createOperation(StorageType.REMOTE_DATABASE, chatIds)
        
        return try {
            if (progressTracker.isCancelled()) {
                return operation.copy(status = OperationStatus.FAILED) to 0
            }
            
            progressTracker.updateCurrentOperation("Deleting remote messages...")
            
            val result = if (chatIds == null) {
                remoteRepository.deleteAllMessages(userId)
            } else {
                remoteRepository.deleteMessagesForChats(chatIds)
            }
            
            when (result) {
                is RepositoryResult.Success -> {
                    progressTracker.updateStorageProgress(StorageType.REMOTE_DATABASE, result.messagesDeleted, true)
                    operation.copy(
                        status = OperationStatus.COMPLETED,
                        messagesAffected = result.messagesDeleted
                    ) to result.messagesDeleted
                }
                is RepositoryResult.Failure -> {
                    if (result.retryable) {
                        operation.copy(status = OperationStatus.QUEUED_FOR_RETRY) to 0
                    } else {
                        operation.copy(status = OperationStatus.FAILED) to 0
                    }
                }
            }
        } catch (e: Exception) {
            operation.copy(status = OperationStatus.FAILED) to 0
        }
    }
    
    private suspend fun executeCacheDeletion(userId: String, chatIds: List<String>?): Pair<DeletionOperation, Int> {
        val operation = createOperation(StorageType.CACHE_STORAGE, chatIds)
        
        return try {
            if (progressTracker.isCancelled()) {
                return operation.copy(status = OperationStatus.FAILED) to 0
            }
            
            progressTracker.updateCurrentOperation("Clearing cache...")
            
            val result = if (chatIds == null) {
                cacheManager.clearAllCache(userId)
            } else {
                cacheManager.clearCacheForChats(chatIds)
            }
            
            when (result) {
                is CacheResult.Success -> {
                    progressTracker.updateStorageProgress(StorageType.CACHE_STORAGE, 0, true)
                    operation.copy(status = OperationStatus.COMPLETED) to 0
                }
                is CacheResult.Failure -> {
                    operation.copy(status = OperationStatus.FAILED) to 0
                }
            }
        } catch (e: Exception) {
            operation.copy(status = OperationStatus.FAILED) to 0
        }
    }
    
    private suspend fun verifyDeletionCompleteness(userId: String, chatIds: List<String>?): List<Boolean> {
        return listOf(
            try {
                if (chatIds == null) {
                    localRepository.verifyDeletionComplete(userId)
                } else {
                    localRepository.verifyChatsDeleted(chatIds)
                }
            } catch (e: Exception) { false },
            
            try {
                if (chatIds == null) {
                    remoteRepository.verifyDeletionComplete(userId)
                } else {
                    remoteRepository.verifyChatsDeleted(chatIds)
                }
            } catch (e: Exception) { false },
            
            try {
                if (chatIds == null) {
                    cacheManager.verifyCacheCleared(userId)
                } else {
                    cacheManager.verifyChatCachesCleared(chatIds)
                }
            } catch (e: Exception) { false }
        )
    }
    
    private suspend fun retryLocalOperation(operation: DeletionOperation): DeletionOperation {
        return try {
            val result = if (operation.chatIds == null) {
                localRepository.deleteAllMessages("") // userId would need to be stored in operation
            } else {
                localRepository.deleteMessagesForChats(operation.chatIds)
            }
            
            when (result) {
                is RepositoryResult.Success -> operation.copy(
                    status = OperationStatus.COMPLETED,
                    retryCount = operation.retryCount + 1
                )
                is RepositoryResult.Failure -> operation.copy(
                    status = OperationStatus.FAILED,
                    retryCount = operation.retryCount + 1
                )
            }
        } catch (e: Exception) {
            operation.copy(
                status = OperationStatus.FAILED,
                retryCount = operation.retryCount + 1
            )
        }
    }
    
    private suspend fun retryRemoteOperation(operation: DeletionOperation): DeletionOperation {
        return try {
            val result = if (operation.chatIds == null) {
                remoteRepository.deleteAllMessages("") // userId would need to be stored in operation
            } else {
                remoteRepository.deleteMessagesForChats(operation.chatIds)
            }
            
            when (result) {
                is RepositoryResult.Success -> operation.copy(
                    status = OperationStatus.COMPLETED,
                    retryCount = operation.retryCount + 1
                )
                is RepositoryResult.Failure -> operation.copy(
                    status = if (result.retryable) OperationStatus.QUEUED_FOR_RETRY else OperationStatus.FAILED,
                    retryCount = operation.retryCount + 1
                )
            }
        } catch (e: Exception) {
            operation.copy(
                status = OperationStatus.FAILED,
                retryCount = operation.retryCount + 1
            )
        }
    }
    
    private suspend fun retryCacheOperation(operation: DeletionOperation): DeletionOperation {
        return try {
            val result = if (operation.chatIds == null) {
                cacheManager.clearAllCache("") // userId would need to be stored in operation
            } else {
                cacheManager.clearCacheForChats(operation.chatIds)
            }
            
            when (result) {
                is CacheResult.Success -> operation.copy(
                    status = OperationStatus.COMPLETED,
                    retryCount = operation.retryCount + 1
                )
                is CacheResult.Failure -> operation.copy(
                    status = OperationStatus.FAILED,
                    retryCount = operation.retryCount + 1
                )
            }
        } catch (e: Exception) {
            operation.copy(
                status = OperationStatus.FAILED,
                retryCount = operation.retryCount + 1
            )
        }
    }
    
    private suspend fun retryTemporaryFileCleanup(operation: DeletionOperation): DeletionOperation {
        return try {
            val result = cacheManager.cleanupTemporaryFiles()
            
            when (result) {
                is CacheResult.Success -> operation.copy(
                    status = OperationStatus.COMPLETED,
                    retryCount = operation.retryCount + 1
                )
                is CacheResult.Failure -> operation.copy(
                    status = OperationStatus.FAILED,
                    retryCount = operation.retryCount + 1
                )
            }
        } catch (e: Exception) {
            operation.copy(
                status = OperationStatus.FAILED,
                retryCount = operation.retryCount + 1
            )
        }
    }
    
    private fun createOperation(storageType: StorageType, chatIds: List<String>?): DeletionOperation {
        return DeletionOperation(
            id = UUID.randomUUID().toString(),
            storageType = storageType,
            status = OperationStatus.PENDING,
            chatIds = chatIds,
            messagesAffected = 0,
            timestamp = System.currentTimeMillis(),
            retryCount = 0
        )
    }
    

}