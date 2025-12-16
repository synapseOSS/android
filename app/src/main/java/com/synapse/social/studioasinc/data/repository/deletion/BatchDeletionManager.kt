package com.synapse.social.studioasinc.data.repository.deletion

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.synapse.social.studioasinc.data.model.deletion.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages batch deletion operations for improved performance with large datasets.
 * Implements chunked processing, parallel execution, and memory-efficient operations.
 * Requirements: 6.1, 6.2
 */
@Singleton
class BatchDeletionManager @Inject constructor(
    private val localChatRepository: LocalChatRepository,
    private val remoteChatRepository: RemoteChatRepository,
    private val chatCacheManager: ChatCacheManager,
    private val progressTracker: ProgressTracker
) {
    
    private val _batchProgress = MutableStateFlow<BatchProgress?>(null)
    val batchProgress: StateFlow<BatchProgress?> = _batchProgress.asStateFlow()
    
    private var currentBatchJob: Job? = null
    
    /**
     * Performs batch deletion with chunked processing for large datasets
     * Requirements: 6.1, 6.2
     */
    suspend fun performBatchDeletion(
        userId: String,
        chatIds: List<String>?,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): DeletionResult = withContext(Dispatchers.IO) {
        
        val totalOperations = calculateTotalOperations(chatIds)
        var completedOperations = 0
        val errors = mutableListOf<DeletionError>()
        val completedOps = mutableListOf<DeletionOperation>()
        val failedOps = mutableListOf<DeletionOperation>()
        
        try {
            _batchProgress.value = BatchProgress(
                totalBatches = if (chatIds != null) (chatIds.size + batchSize - 1) / batchSize else 1,
                completedBatches = 0,
                currentBatch = 1,
                totalOperations = totalOperations,
                completedOperations = 0,
                estimatedTimeRemaining = null,
                isProcessing = true
            )
            
            val startTime = System.currentTimeMillis()
            
            if (chatIds == null) {
                // Complete deletion - process in memory-efficient chunks
                val result = performCompleteHistoryDeletion(userId, batchSize)
                completedOps.addAll(result.completedOperations)
                failedOps.addAll(result.failedOperations)
                errors.addAll(result.errors)
            } else {
                // Selective deletion - process chat IDs in batches
                val chatBatches = chatIds.chunked(batchSize)
                
                for ((batchIndex, batch) in chatBatches.withIndex()) {
                    currentBatchJob = launch {
                        val batchResult = processChatBatch(userId, batch, batchIndex + 1)
                        
                        completedOps.addAll(batchResult.completedOperations)
                        failedOps.addAll(batchResult.failedOperations)
                        errors.addAll(batchResult.errors)
                        
                        completedOperations += batchResult.completedOperations.size
                        
                        // Update progress with time estimation
                        val elapsedTime = System.currentTimeMillis() - startTime
                        val estimatedTotal = if (completedOperations > 0) {
                            (elapsedTime * totalOperations) / completedOperations
                        } else {
                            null
                        }
                        val estimatedRemaining = estimatedTotal?.let { it - elapsedTime }
                        
                        _batchProgress.value = _batchProgress.value?.copy(
                            completedBatches = batchIndex + 1,
                            currentBatch = minOf(batchIndex + 2, chatBatches.size),
                            completedOperations = completedOperations,
                            estimatedTimeRemaining = estimatedRemaining
                        )
                    }
                    
                    currentBatchJob?.join()
                    
                    // Yield to prevent blocking the thread for too long
                    yield()
                }
            }
            
            _batchProgress.value = _batchProgress.value?.copy(
                isProcessing = false,
                estimatedTimeRemaining = 0L
            )
            
            return@withContext DeletionResult(
                success = failedOps.isEmpty(),
                completedOperations = completedOps,
                failedOperations = failedOps,
                totalMessagesDeleted = completedOps.sumOf { it.messagesAffected },
                errors = errors
            )
            
        } catch (e: Exception) {
            _batchProgress.value = _batchProgress.value?.copy(
                isProcessing = false,
                estimatedTimeRemaining = null
            )
            
            errors.add(DeletionError.SystemError(
                message = "Batch deletion failed: ${e.message}",
                recoverable = true
            ))
            
            return@withContext DeletionResult(
                success = false,
                completedOperations = completedOps,
                failedOperations = failedOps,
                totalMessagesDeleted = completedOps.sumOf { it.messagesAffected },
                errors = errors
            )
        }
    }
    
    /**
     * Processes a batch of chat IDs with parallel execution
     */
    private suspend fun processChatBatch(
        userId: String,
        chatIds: List<String>,
        batchNumber: Int
    ): DeletionResult = coroutineScope {
        
        val operations = mutableListOf<DeletionOperation>()
        val errors = mutableListOf<DeletionError>()
        
        // Process storage systems in parallel for better performance
        val localJob = async {
            try {
                val operation = DeletionOperation(
                    id = "batch_${batchNumber}_local_${System.currentTimeMillis()}",
                    storageType = StorageType.LOCAL_DATABASE,
                    status = OperationStatus.IN_PROGRESS,
                    chatIds = chatIds,
                    messagesAffected = 0,
                    timestamp = System.currentTimeMillis()
                )
                
                val result = localChatRepository.deleteMessagesForChats(chatIds)
                
                when (result) {
                    is RepositoryResult.Success -> {
                        operation.copy(
                            status = OperationStatus.COMPLETED,
                            messagesAffected = result.messagesDeleted
                        )
                    }
                    is RepositoryResult.Failure -> {
                        errors.add(DeletionError.DatabaseError(
                            message = result.error,
                            storageType = StorageType.LOCAL_DATABASE
                        ))
                        operation.copy(status = OperationStatus.FAILED)
                    }
                }
            } catch (e: Exception) {
                errors.add(DeletionError.DatabaseError(
                    message = "Local deletion error: ${e.message}",
                    storageType = StorageType.LOCAL_DATABASE
                ))
                null
            }
        }
        
        val remoteJob = async {
            try {
                val operation = DeletionOperation(
                    id = "batch_${batchNumber}_remote_${System.currentTimeMillis()}",
                    storageType = StorageType.REMOTE_DATABASE,
                    status = OperationStatus.IN_PROGRESS,
                    chatIds = chatIds,
                    messagesAffected = 0,
                    timestamp = System.currentTimeMillis()
                )
                
                val result = remoteChatRepository.deleteMessagesForChats(chatIds)
                
                when (result) {
                    is RepositoryResult.Success -> {
                        operation.copy(
                            status = OperationStatus.COMPLETED,
                            messagesAffected = result.messagesDeleted
                        )
                    }
                    is RepositoryResult.Failure -> {
                        errors.add(DeletionError.NetworkError(
                            message = result.error,
                            retryable = result.retryable
                        ))
                        operation.copy(status = OperationStatus.FAILED)
                    }
                }
            } catch (e: Exception) {
                errors.add(DeletionError.NetworkError(
                    message = "Remote deletion error: ${e.message}",
                    retryable = true
                ))
                null
            }
        }
        
        val cacheJob = async {
            try {
                val operation = DeletionOperation(
                    id = "batch_${batchNumber}_cache_${System.currentTimeMillis()}",
                    storageType = StorageType.CACHE_STORAGE,
                    status = OperationStatus.IN_PROGRESS,
                    chatIds = chatIds,
                    messagesAffected = 0,
                    timestamp = System.currentTimeMillis()
                )
                
                val result = chatCacheManager.clearCacheForChats(chatIds)
                
                when (result) {
                    is CacheResult.Success -> {
                        operation.copy(
                            status = OperationStatus.COMPLETED,
                            messagesAffected = 0 // Cache operations don't have message count
                        )
                    }
                    is CacheResult.Failure -> {
                        errors.add(DeletionError.SystemError(
                            message = result.error,
                            recoverable = true
                        ))
                        operation.copy(status = OperationStatus.FAILED)
                    }
                }
            } catch (e: Exception) {
                errors.add(DeletionError.SystemError(
                    message = "Cache clearing error: ${e.message}",
                    recoverable = true
                ))
                null
            }
        }
        
        // Wait for all operations to complete
        val results = awaitAll(localJob, remoteJob, cacheJob)
        operations.addAll(results.filterNotNull())
        
        val completedOps = operations.filter { it.status == OperationStatus.COMPLETED }
        val failedOps = operations.filter { it.status == OperationStatus.FAILED }
        
        return@coroutineScope DeletionResult(
            success = failedOps.isEmpty(),
            completedOperations = completedOps,
            failedOperations = failedOps,
            totalMessagesDeleted = completedOps.sumOf { it.messagesAffected },
            errors = errors
        )
    }
    
    /**
     * Performs complete history deletion with memory-efficient processing
     */
    private suspend fun performCompleteHistoryDeletion(
        userId: String,
        batchSize: Int
    ): DeletionResult = coroutineScope {
        
        val operations = mutableListOf<DeletionOperation>()
        val errors = mutableListOf<DeletionError>()
        
        // Process each storage system with memory-efficient batching
        val localJob = async {
            try {
                val operation = DeletionOperation(
                    id = "complete_local_${System.currentTimeMillis()}",
                    storageType = StorageType.LOCAL_DATABASE,
                    status = OperationStatus.IN_PROGRESS,
                    chatIds = null,
                    messagesAffected = 0,
                    timestamp = System.currentTimeMillis()
                )
                
                val result = localChatRepository.deleteAllMessages(userId)
                
                when (result) {
                    is RepositoryResult.Success -> {
                        operation.copy(
                            status = OperationStatus.COMPLETED,
                            messagesAffected = result.messagesDeleted
                        )
                    }
                    is RepositoryResult.Failure -> {
                        errors.add(DeletionError.DatabaseError(
                            message = result.error,
                            storageType = StorageType.LOCAL_DATABASE
                        ))
                        operation.copy(status = OperationStatus.FAILED)
                    }
                }
            } catch (e: Exception) {
                errors.add(DeletionError.DatabaseError(
                    message = "Complete local deletion error: ${e.message}",
                    storageType = StorageType.LOCAL_DATABASE
                ))
                null
            }
        }
        
        val remoteJob = async {
            try {
                val operation = DeletionOperation(
                    id = "complete_remote_${System.currentTimeMillis()}",
                    storageType = StorageType.REMOTE_DATABASE,
                    status = OperationStatus.IN_PROGRESS,
                    chatIds = null,
                    messagesAffected = 0,
                    timestamp = System.currentTimeMillis()
                )
                
                val result = remoteChatRepository.deleteAllMessages(userId)
                
                when (result) {
                    is RepositoryResult.Success -> {
                        operation.copy(
                            status = OperationStatus.COMPLETED,
                            messagesAffected = result.messagesDeleted
                        )
                    }
                    is RepositoryResult.Failure -> {
                        errors.add(DeletionError.NetworkError(
                            message = result.error,
                            retryable = result.retryable
                        ))
                        operation.copy(status = OperationStatus.FAILED)
                    }
                }
            } catch (e: Exception) {
                errors.add(DeletionError.NetworkError(
                    message = "Complete remote deletion error: ${e.message}",
                    retryable = true
                ))
                null
            }
        }
        
        val cacheJob = async {
            try {
                val operation = DeletionOperation(
                    id = "complete_cache_${System.currentTimeMillis()}",
                    storageType = StorageType.CACHE_STORAGE,
                    status = OperationStatus.IN_PROGRESS,
                    chatIds = null,
                    messagesAffected = 0,
                    timestamp = System.currentTimeMillis()
                )
                
                val result = chatCacheManager.clearAllCache(userId)
                
                when (result) {
                    is CacheResult.Success -> {
                        operation.copy(
                            status = OperationStatus.COMPLETED,
                            messagesAffected = 0 // Cache operations don't have message count
                        )
                    }
                    is CacheResult.Failure -> {
                        errors.add(DeletionError.SystemError(
                            message = result.error,
                            recoverable = true
                        ))
                        operation.copy(status = OperationStatus.FAILED)
                    }
                }
            } catch (e: Exception) {
                errors.add(DeletionError.SystemError(
                    message = "Complete cache clearing error: ${e.message}",
                    recoverable = true
                ))
                null
            }
        }
        
        // Wait for all operations to complete
        val results = awaitAll(localJob, remoteJob, cacheJob)
        operations.addAll(results.filterNotNull())
        
        val completedOps = operations.filter { it.status == OperationStatus.COMPLETED }
        val failedOps = operations.filter { it.status == OperationStatus.FAILED }
        
        return@coroutineScope DeletionResult(
            success = failedOps.isEmpty(),
            completedOperations = completedOps,
            failedOperations = failedOps,
            totalMessagesDeleted = completedOps.sumOf { it.messagesAffected },
            errors = errors
        )
    }
    
    /**
     * Calculates total operations for progress tracking
     */
    private fun calculateTotalOperations(chatIds: List<String>?): Int {
        return if (chatIds == null) {
            3 // Local, Remote, Cache for complete deletion
        } else {
            val batches = (chatIds.size + DEFAULT_BATCH_SIZE - 1) / DEFAULT_BATCH_SIZE
            batches * 3 // Each batch has 3 operations (Local, Remote, Cache)
        }
    }
    
    /**
     * Cancels the current batch operation
     */
    fun cancelBatchOperation() {
        currentBatchJob?.cancel()
        _batchProgress.value = _batchProgress.value?.copy(
            isProcessing = false,
            estimatedTimeRemaining = null
        )
    }
    
    /**
     * Clears batch progress state
     */
    fun clearBatchProgress() {
        _batchProgress.value = null
    }
    
    companion object {
        private const val DEFAULT_BATCH_SIZE = 50 // Process 50 chats at a time
    }
}

/**
 * Data class representing batch deletion progress
 */
data class BatchProgress(
    val totalBatches: Int,
    val completedBatches: Int,
    val currentBatch: Int,
    val totalOperations: Int,
    val completedOperations: Int,
    val estimatedTimeRemaining: Long?,
    val isProcessing: Boolean
)