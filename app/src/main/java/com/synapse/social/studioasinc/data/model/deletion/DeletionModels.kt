package com.synapse.social.studioasinc.data.model.deletion

import kotlinx.serialization.Serializable

/**
 * Represents a request to delete chat history
 * Requirements: 1.1, 2.1, 4.1
 */
@Serializable
data class DeletionRequest(
    val id: String,
    val userId: String,
    val type: DeletionType,
    val chatIds: List<String>? = null,
    val timestamp: Long,
    val requiresConfirmation: Boolean = true
) : java.io.Serializable

/**
 * Type of deletion operation
 * Requirements: 1.1, 2.1
 */
enum class DeletionType : java.io.Serializable {
    COMPLETE_HISTORY,
    SELECTIVE_CHATS
}

/**
 * Result of a deletion operation
 * Requirements: 1.1, 2.1, 4.1
 */
@Serializable
data class DeletionResult(
    val success: Boolean,
    val completedOperations: List<DeletionOperation>,
    val failedOperations: List<DeletionOperation>,
    val totalMessagesDeleted: Int,
    val errors: List<DeletionError>
)

/**
 * Individual deletion operation for a specific storage system
 * Requirements: 1.1, 2.1, 4.1
 */
@Serializable
data class DeletionOperation(
    val id: String,
    val storageType: StorageType,
    val status: OperationStatus,
    val chatIds: List<String>?,
    val messagesAffected: Int,
    val timestamp: Long,
    val retryCount: Int = 0
)

/**
 * Type of storage system
 * Requirements: 1.1, 1.2, 1.3
 */
enum class StorageType {
    LOCAL_DATABASE,
    REMOTE_DATABASE,
    CACHE_STORAGE,
    TEMPORARY_FILES
}

/**
 * Status of a deletion operation
 * Requirements: 4.1, 4.2
 */
enum class OperationStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    QUEUED_FOR_RETRY
}

/**
 * Progress tracking for deletion operations
 * Requirements: 6.1, 6.2, 6.4
 */
@Serializable
data class DeletionProgress(
    val totalOperations: Int,
    val completedOperations: Int,
    val currentOperation: String?,
    val estimatedTimeRemaining: Long?,
    val canCancel: Boolean
)

/**
 * Error information for failed deletion operations
 * Requirements: 4.1, 4.3, 4.4
 */
@Serializable
sealed class DeletionError {
    @Serializable
    data class NetworkError(val message: String, val retryable: Boolean) : DeletionError()
    
    @Serializable
    data class DatabaseError(val message: String, val storageType: StorageType) : DeletionError()
    
    @Serializable
    data class ValidationError(val message: String, val field: String) : DeletionError()
    
    @Serializable
    data class SystemError(val message: String, val recoverable: Boolean) : DeletionError()
}