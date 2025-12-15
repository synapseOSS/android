package com.synapse.social.studioasinc.data.repository.deletion

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import com.synapse.social.studioasinc.data.model.deletion.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory-efficient deletion processor for handling large chat histories.
 * Uses streaming, chunked processing, and memory management techniques.
 * Requirements: 6.1, 6.2
 */
@Singleton
class MemoryEfficientDeletionProcessor @Inject constructor(
    private val localChatRepository: LocalChatRepository,
    private val remoteChatRepository: RemoteChatRepository,
    private val chatCacheManager: ChatCacheManager
) {
    
    /**
     * Processes large deletion operations with memory-efficient streaming
     * Requirements: 6.1, 6.2
     */
    suspend fun processLargeDeletion(
        userId: String,
        chatIds: List<String>?,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY
    ): Flow<DeletionProgress> = flow {
        
        val totalChats = chatIds?.size ?: estimateTotalChats(userId)
        val totalChunks = if (chatIds != null) {
            (chatIds.size + chunkSize - 1) / chunkSize
        } else {
            (totalChats + chunkSize - 1) / chunkSize
        }
        
        var processedChats = 0
        var processedChunks = 0
        
        if (chatIds != null) {
            // Process specific chats in memory-efficient chunks
            chatIds.asFlow()
                .chunked(chunkSize)
                .flowOn(Dispatchers.IO)
                .collect { chunk ->
                    processChunk(chunk, maxConcurrency)
                    
                    processedChats += chunk.size
                    processedChunks++
                    
                    emit(DeletionProgress(
                        totalOperations = totalChunks * 3, // 3 storage systems per chunk
                        completedOperations = processedChunks * 3,
                        currentOperation = "Processing chunk $processedChunks/$totalChunks",
                        estimatedTimeRemaining = calculateEstimatedTime(
                            processedChunks, 
                            totalChunks, 
                            System.currentTimeMillis()
                        ),
                        canCancel = true
                    ))
                    
                    // Force garbage collection after each chunk to manage memory
                    if (processedChunks % GC_FREQUENCY == 0) {
                        System.gc()
                        delay(GC_DELAY_MS)
                    }
                }
        } else {
            // Process all chats using streaming approach
            streamAllChatsForDeletion(userId, chunkSize, maxConcurrency)
                .collect { progress ->
                    emit(progress)
                    
                    // Periodic garbage collection for large operations
                    if (progress.completedOperations % (GC_FREQUENCY * 3) == 0) {
                        System.gc()
                        delay(GC_DELAY_MS)
                    }
                }
        }
    }
    
    /**
     * Processes a chunk of chat IDs with controlled concurrency
     */
    private suspend fun processChunk(
        chatIds: List<String>,
        maxConcurrency: Int
    ): Triple<RepositoryResult, RepositoryResult, CacheResult> = coroutineScope {
        
        // Use semaphore to control concurrency and prevent memory overload
        val semaphore = Semaphore(maxConcurrency)
        
        val jobs = listOf(
            // Local deletion job
            async<RepositoryResult> {
                semaphore.acquire()
                try {
                    localChatRepository.deleteMessagesForChats(chatIds)
                } catch (e: Exception) {
                    android.util.Log.e("MemoryEfficientProcessor", "Local deletion failed", e)
                    RepositoryResult.Failure("Local deletion failed: ${e.message}")
                } finally {
                    semaphore.release()
                }
            },
            
            // Remote deletion job
            async<RepositoryResult> {
                semaphore.acquire()
                try {
                    remoteChatRepository.deleteMessagesForChats(chatIds)
                } catch (e: Exception) {
                    android.util.Log.e("MemoryEfficientProcessor", "Remote deletion failed", e)
                    RepositoryResult.Failure("Remote deletion failed: ${e.message}")
                } finally {
                    semaphore.release()
                }
            },
            
            // Cache deletion job
            async<CacheResult> {
                semaphore.acquire()
                try {
                    chatCacheManager.clearCacheForChats(chatIds)
                } catch (e: Exception) {
                    android.util.Log.e("MemoryEfficientProcessor", "Cache deletion failed", e)
                    CacheResult.Failure("Cache deletion failed: ${e.message}")
                } finally {
                    semaphore.release()
                }
            }
        )
        
        // Wait for all jobs to complete
        val results = jobs.awaitAll()
        Triple(
            results[0] as RepositoryResult,
            results[1] as RepositoryResult, 
            results[2] as CacheResult
        )
    }
    
    /**
     * Streams all chats for deletion using memory-efficient approach
     */
    private suspend fun streamAllChatsForDeletion(
        userId: String,
        chunkSize: Int,
        maxConcurrency: Int
    ): Flow<DeletionProgress> = flow {
        
        var processedChunks = 0
        val startTime = System.currentTimeMillis()
        
        // Stream chat IDs in chunks to avoid loading all into memory
        getChatIdsStream(userId, chunkSize)
            .flowOn(Dispatchers.IO)
            .collect { chatIdChunk ->
                if (chatIdChunk.isNotEmpty()) {
                    processChunk(chatIdChunk, maxConcurrency)
                    processedChunks++
                    
                    emit(DeletionProgress(
                        totalOperations = -1, // Unknown total for streaming
                        completedOperations = processedChunks * 3,
                        currentOperation = "Processing chunk $processedChunks (streaming)",
                        estimatedTimeRemaining = null, // Cannot estimate for streaming
                        canCancel = true
                    ))
                }
            }
    }
    
    /**
     * Gets a stream of chat IDs in chunks to avoid memory overload
     */
    private suspend fun getChatIdsStream(
        userId: String,
        chunkSize: Int
    ): Flow<List<String>> = flow {
        
        var offset = 0
        var hasMore = true
        
        while (hasMore) {
            try {
                // Fetch chat IDs in small batches
                val chatIds = fetchChatIdsBatch(userId, offset, chunkSize)
                
                if (chatIds.isNotEmpty()) {
                    emit(chatIds)
                    offset += chatIds.size
                    hasMore = chatIds.size == chunkSize // Continue if we got a full batch
                } else {
                    hasMore = false
                }
                
                // Small delay to prevent overwhelming the database
                delay(BATCH_DELAY_MS)
                
            } catch (e: Exception) {
                android.util.Log.e("MemoryEfficientProcessor", "Failed to fetch chat IDs batch", e)
                hasMore = false
            }
        }
    }
    
    /**
     * Fetches a batch of chat IDs from the database
     */
    private suspend fun fetchChatIdsBatch(
        userId: String,
        offset: Int,
        limit: Int
    ): List<String> {
        return try {
            // This would typically query the database for chat IDs
            // Implementation depends on the specific database schema
            localChatRepository.getChatIdsBatch(userId, offset, limit)
        } catch (e: Exception) {
            android.util.Log.e("MemoryEfficientProcessor", "Failed to fetch chat IDs", e)
            emptyList()
        }
    }
    
    /**
     * Estimates total number of chats for a user
     */
    private suspend fun estimateTotalChats(userId: String): Int {
        return try {
            localChatRepository.getChatCount(userId)
        } catch (e: Exception) {
            android.util.Log.e("MemoryEfficientProcessor", "Failed to estimate chat count", e)
            1000 // Default estimate
        }
    }
    
    /**
     * Calculates estimated time remaining based on current progress
     */
    private fun calculateEstimatedTime(
        processedChunks: Int,
        totalChunks: Int,
        startTime: Long
    ): Long? {
        if (processedChunks == 0) return null
        
        val elapsedTime = System.currentTimeMillis() - startTime
        val avgTimePerChunk = elapsedTime / processedChunks
        val remainingChunks = totalChunks - processedChunks
        
        return remainingChunks * avgTimePerChunk
    }
    
    companion object {
        private const val DEFAULT_CHUNK_SIZE = 100 // Process 100 chats at a time
        private const val DEFAULT_MAX_CONCURRENCY = 3 // Max 3 concurrent operations
        private const val GC_FREQUENCY = 10 // Run GC every 10 chunks
        private const val GC_DELAY_MS = 100L // Wait 100ms after GC
        private const val BATCH_DELAY_MS = 50L // Delay between database batches
    }
}

/**
 * Extension function to chunk a flow into lists
 */
private fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> = flow {
    val chunk = mutableListOf<T>()
    collect { item ->
        chunk.add(item)
        if (chunk.size == size) {
            emit(chunk.toList())
            chunk.clear()
        }
    }
    if (chunk.isNotEmpty()) {
        emit(chunk.toList())
    }
}

