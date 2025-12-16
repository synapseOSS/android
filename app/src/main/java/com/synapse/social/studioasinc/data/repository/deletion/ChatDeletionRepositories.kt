package com.synapse.social.studioasinc.data.repository.deletion

import com.synapse.social.studioasinc.data.model.deletion.DeletionOperation

/**
 * Result wrapper for repository operations
 * Requirements: 1.1, 1.2, 1.3
 */
sealed class RepositoryResult {
    data class Success(val messagesDeleted: Int) : RepositoryResult()
    data class Failure(val error: String, val retryable: Boolean = false) : RepositoryResult()
}

/**
 * Result wrapper for cache operations
 * Requirements: 1.3
 */
sealed class CacheResult {
    object Success : CacheResult()
    data class Failure(val error: String) : CacheResult()
}

/**
 * Result wrapper for recovery operations
 * Requirements: 4.3, 4.5
 */
sealed class RecoveryResult {
    data class Success(val recoveredOperations: List<DeletionOperation>) : RecoveryResult()
    data class PartialSuccess(
        val recoveredOperations: List<DeletionOperation>,
        val failedOperations: List<DeletionOperation>
    ) : RecoveryResult()
    data class Failure(val error: String) : RecoveryResult()
}

/**
 * Interface for local database chat deletion operations
 * Requirements: 1.1, 2.1, 5.1, 5.4
 */
interface LocalChatRepository {
    
    /**
     * Delete all messages for a specific user from local database
     * Requirements: 1.1
     */
    suspend fun deleteAllMessages(userId: String): RepositoryResult
    
    /**
     * Delete messages for specific chats from local database
     * Requirements: 2.1
     */
    suspend fun deleteMessagesForChats(chatIds: List<String>): RepositoryResult
    
    /**
     * Verify that deletion was completed successfully
     * Requirements: 5.4
     */
    suspend fun verifyDeletionComplete(userId: String): Boolean
    
    /**
     * Verify that specific chats were deleted successfully
     * Requirements: 5.4
     */
    suspend fun verifyChatsDeleted(chatIds: List<String>): Boolean
    
    /**
     * Get count of messages for a user (for progress tracking)
     * Requirements: 6.1, 6.2
     */
    suspend fun getMessageCount(userId: String): Int
    
    /**
     * Get count of messages for specific chats
     * Requirements: 6.1, 6.2
     */
    suspend fun getMessageCountForChats(chatIds: List<String>): Int
    
    /**
     * Get chat IDs in batches for memory-efficient processing
     * Requirements: 6.1, 6.2
     */
    suspend fun getChatIdsBatch(userId: String, offset: Int, limit: Int): List<String>
    
    /**
     * Get total count of chats for a user
     * Requirements: 6.1, 6.2
     */
    suspend fun getChatCount(userId: String): Int
}

/**
 * Interface for remote database (Supabase) chat deletion operations
 * Requirements: 1.2, 2.2, 4.1, 4.2
 */
interface RemoteChatRepository {
    
    /**
     * Delete all messages for a specific user from remote database
     * Requirements: 1.2
     */
    suspend fun deleteAllMessages(userId: String): RepositoryResult
    
    /**
     * Delete messages for specific chats from remote database
     * Requirements: 2.2
     */
    suspend fun deleteMessagesForChats(chatIds: List<String>): RepositoryResult
    
    /**
     * Queue a deletion operation for retry when network is restored
     * Requirements: 4.2
     */
    suspend fun queueDeletionForRetry(operation: DeletionOperation): Boolean
    
    /**
     * Check network connectivity status
     * Requirements: 4.1, 4.5
     */
    suspend fun isNetworkAvailable(): Boolean
    
    /**
     * Get count of messages for a user (for progress tracking)
     * Requirements: 6.1, 6.2
     */
    suspend fun getMessageCount(userId: String): Int
    
    /**
     * Get count of messages for specific chats
     * Requirements: 6.1, 6.2
     */
    suspend fun getMessageCountForChats(chatIds: List<String>): Int
    
    /**
     * Verify that deletion was completed successfully on remote
     * Requirements: 5.4
     */
    suspend fun verifyDeletionComplete(userId: String): Boolean
    
    /**
     * Verify that specific chats were deleted successfully on remote
     * Requirements: 5.4
     */
    suspend fun verifyChatsDeleted(chatIds: List<String>): Boolean
}

/**
 * Interface for cache management operations
 * Requirements: 1.3, 2.3, 5.3, 5.4
 */
interface ChatCacheManager {
    
    /**
     * Clear all cached chat data for a user
     * Requirements: 1.3
     */
    suspend fun clearAllCache(userId: String): CacheResult
    
    /**
     * Clear cached data for specific chats
     * Requirements: 2.3
     */
    suspend fun clearCacheForChats(chatIds: List<String>): CacheResult
    
    /**
     * Perform secure overwrite of cached data to prevent recovery
     * Requirements: 5.3
     */
    suspend fun secureCacheOverwrite(): Boolean
    
    /**
     * Verify that cache was cleared successfully
     * Requirements: 5.4
     */
    suspend fun verifyCacheCleared(userId: String): Boolean
    
    /**
     * Verify that specific chat caches were cleared
     * Requirements: 5.4
     */
    suspend fun verifyChatCachesCleared(chatIds: List<String>): Boolean
    
    /**
     * Clean up temporary files containing chat data
     * Requirements: 5.5
     */
    suspend fun cleanupTemporaryFiles(): CacheResult
    
    /**
     * Get size of cached data for progress estimation
     * Requirements: 6.1, 6.2
     */
    suspend fun getCacheSize(userId: String): Long
    
    /**
     * Get size of cached data for specific chats
     * Requirements: 6.1, 6.2
     */
    suspend fun getCacheSizeForChats(chatIds: List<String>): Long
}