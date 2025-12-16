package com.synapse.social.studioasinc.data.local.deletion

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for deletion operations tracking
 * Requirements: 4.2, 5.4
 */
@Dao
interface DeletionDao {
    
    @Query("SELECT * FROM deletion_operations WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDeletionHistory(userId: String): Flow<List<DeletionOperationEntity>>
    
    @Query("SELECT * FROM deletion_operations WHERE id = :operationId")
    suspend fun getDeletionOperation(operationId: String): DeletionOperationEntity?
    
    @Query("SELECT * FROM deletion_operations WHERE status = :status")
    suspend fun getOperationsByStatus(status: String): List<DeletionOperationEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletionOperation(operation: DeletionOperationEntity)
    
    @Update
    suspend fun updateDeletionOperation(operation: DeletionOperationEntity)
    
    @Query("DELETE FROM deletion_operations WHERE id = :operationId")
    suspend fun deleteDeletionOperation(operationId: String)
    
    @Query("DELETE FROM deletion_operations WHERE userId = :userId AND createdAt < :beforeTimestamp")
    suspend fun cleanupOldOperations(userId: String, beforeTimestamp: Long)
}

/**
 * DAO for deletion retry queue
 * Requirements: 4.2, 4.5
 */
@Dao
interface DeletionRetryDao {
    
    @Query("SELECT * FROM deletion_retry_queue WHERE scheduledRetryTime <= :currentTime ORDER BY scheduledRetryTime ASC")
    suspend fun getReadyForRetry(currentTime: Long): List<DeletionRetryQueueEntity>
    
    @Query("SELECT * FROM deletion_retry_queue WHERE operationId = :operationId")
    suspend fun getRetryEntry(operationId: String): DeletionRetryQueueEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRetryEntry(retryEntry: DeletionRetryQueueEntity)
    
    @Update
    suspend fun updateRetryEntry(retryEntry: DeletionRetryQueueEntity)
    
    @Query("DELETE FROM deletion_retry_queue WHERE id = :retryId")
    suspend fun deleteRetryEntry(retryId: String)
    
    @Query("DELETE FROM deletion_retry_queue WHERE operationId = :operationId")
    suspend fun deleteRetryEntriesForOperation(operationId: String)
    
    @Query("DELETE FROM deletion_retry_queue WHERE currentRetry >= maxRetries")
    suspend fun cleanupExhaustedRetries()
}