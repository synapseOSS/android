package com.synapse.social.studioasinc.ui.deletion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.data.repository.deletion.ChatHistoryManager
import com.synapse.social.studioasinc.data.model.deletion.*
import javax.inject.Inject

/**
 * ViewModel for message deletion operations and UI state management.
 * 
 * Handles deletion request validation, progress state management, and error handling
 * for both complete history deletion and selective chat deletion operations.
 * 
 * Requirements: 2.4, 3.4, 6.5
 */
@HiltViewModel
class MessageDeletionViewModel @Inject constructor(
    private val chatHistoryManager: ChatHistoryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageDeletionUiState())
    val uiState: StateFlow<MessageDeletionUiState> = _uiState.asStateFlow()

    private val _deletionEvents = MutableSharedFlow<DeletionEvent>()
    val deletionEvents: SharedFlow<DeletionEvent> = _deletionEvents.asSharedFlow()

    init {
        observeDeletionProgress()
        observeDeletionNotifications()
    }

    /**
     * Observe deletion progress from the chat history manager.
     * Requirements: 6.5
     */
    private fun observeDeletionProgress() {
        viewModelScope.launch {
            chatHistoryManager.getDeleteProgress().collect { progress ->
                _uiState.update { currentState ->
                    currentState.copy(
                        deletionProgress = progress,
                        isDeleting = progress != null && progress.completedOperations < progress.totalOperations,
                        canCancel = progress?.canCancel ?: false
                    )
                }
            }
        }
    }
    
    /**
     * Observe deletion notifications for comprehensive user feedback.
     * Requirements: 1.4, 1.5, 4.4, 6.5
     */
    private fun observeDeletionNotifications() {
        viewModelScope.launch {
            // This would need to be injected from the UserNotificationManager
            // For now, we'll handle notifications through the existing event system
            // In a real implementation, you would inject UserNotificationManager and observe its notifications
        }
    }

    /**
     * Initiate complete chat history deletion.
     * 
     * Validates request, updates UI state, and coordinates deletion through ChatHistoryManager.
     * Requirements: 2.4, 3.4
     */
    fun deleteAllHistory(userId: String) {
        if (userId.isBlank()) {
            _uiState.update { it.copy(error = "User ID is required for deletion") }
            return
        }

        if (_uiState.value.isDeleting) {
            _uiState.update { it.copy(error = "Another deletion operation is already in progress") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        isDeleting = true,
                        error = null,
                        lastDeletionType = DeletionType.COMPLETE_HISTORY
                    )
                }

                val result = chatHistoryManager.deleteAllHistory(userId)
                
                handleDeletionResult(result, DeletionType.COMPLETE_HISTORY)
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isDeleting = false,
                        error = "Failed to delete chat history: ${e.message}"
                    )
                }
                
                viewModelScope.launch {
                    _deletionEvents.emit(DeletionEvent.Error("Deletion failed: ${e.message}"))
                }
            }
        }
    }

    /**
     * Initiate selective chat deletion.
     * 
     * Validates request and chat IDs, updates UI state, and coordinates deletion.
     * Requirements: 2.4, 3.4
     */
    fun deleteSpecificChats(userId: String, chatIds: List<String>) {
        if (userId.isBlank()) {
            _uiState.update { it.copy(error = "User ID is required for deletion") }
            return
        }

        if (chatIds.isEmpty()) {
            _uiState.update { it.copy(error = "No chats selected for deletion") }
            return
        }

        if (_uiState.value.isDeleting) {
            _uiState.update { it.copy(error = "Another deletion operation is already in progress") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        isDeleting = true,
                        error = null,
                        lastDeletionType = DeletionType.SELECTIVE_CHATS,
                        selectedChatIds = chatIds
                    )
                }

                val result = chatHistoryManager.deleteSpecificChats(userId, chatIds)
                
                handleDeletionResult(result, DeletionType.SELECTIVE_CHATS)
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isDeleting = false,
                        error = "Failed to delete selected chats: ${e.message}"
                    )
                }
                
                viewModelScope.launch {
                    _deletionEvents.emit(DeletionEvent.Error("Deletion failed: ${e.message}"))
                }
            }
        }
    }

    /**
     * Cancel ongoing deletion operation.
     * Requirements: 6.5
     */
    fun cancelDeletion() {
        if (!_uiState.value.isDeleting) {
            return
        }

        viewModelScope.launch {
            try {
                val cancelled = chatHistoryManager.cancelDeletion()
                
                if (cancelled) {
                    _uiState.update { 
                        it.copy(
                            isDeleting = false,
                            deletionProgress = null,
                            canCancel = false
                        )
                    }
                    
                    _deletionEvents.emit(DeletionEvent.Cancelled)
                } else {
                    _uiState.update { it.copy(error = "Unable to cancel deletion at this time") }
                }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to cancel deletion: ${e.message}") }
            }
        }
    }

    /**
     * Clear error message from UI state.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Reset deletion state after completion.
     */
    fun resetDeletionState() {
        _uiState.update { 
            it.copy(
                isDeleting = false,
                deletionProgress = null,
                lastDeletionResult = null,
                lastDeletionType = null,
                selectedChatIds = emptyList(),
                canCancel = false
            )
        }
    }

    /**
     * Handle retry result and update UI state accordingly.
     * Requirements: 4.1, 4.3, 4.5
     */
    private suspend fun handleRetryResult(result: DeletionResult) {
        _uiState.update { 
            it.copy(
                isDeleting = false,
                isRetrying = false,
                lastDeletionResult = result,
                deletionProgress = null,
                canCancel = false
            )
        }

        when {
            result.success -> {
                _deletionEvents.emit(
                    DeletionEvent.Success(
                        "Retry successful! ${result.totalMessagesDeleted} messages deleted.",
                        result.totalMessagesDeleted
                    )
                )
            }
            
            result.completedOperations.isNotEmpty() && result.failedOperations.isNotEmpty() -> {
                val message = "Retry partially completed. ${result.completedOperations.size} operations succeeded, ${result.failedOperations.size} still failed."
                _deletionEvents.emit(DeletionEvent.PartialSuccess(message, result))
            }
            
            else -> {
                val errorMessage = result.errors.firstOrNull()?.let { error ->
                    when (error) {
                        is DeletionError.NetworkError -> "Network error during retry: ${error.message}"
                        is DeletionError.DatabaseError -> "Database error during retry: ${error.message}"
                        is DeletionError.ValidationError -> "Validation error during retry: ${error.message}"
                        is DeletionError.SystemError -> "System error during retry: ${error.message}"
                    }
                } ?: "Retry failed for unknown reason"
                
                _deletionEvents.emit(DeletionEvent.Error(errorMessage))
            }
        }
    }

    /**
     * Handle deletion result and update UI state accordingly.
     * Requirements: 2.4, 6.5
     */
    private suspend fun handleDeletionResult(result: DeletionResult, deletionType: DeletionType) {
        _uiState.update { 
            it.copy(
                isDeleting = false,
                lastDeletionResult = result,
                deletionProgress = null,
                canCancel = false
            )
        }

        when {
            result.success -> {
                val message = when (deletionType) {
                    DeletionType.COMPLETE_HISTORY -> 
                        "Successfully deleted all chat history (${result.totalMessagesDeleted} messages)"
                    DeletionType.SELECTIVE_CHATS -> 
                        "Successfully deleted ${_uiState.value.selectedChatIds.size} chat(s) (${result.totalMessagesDeleted} messages)"
                }
                
                _deletionEvents.emit(DeletionEvent.Success(message, result.totalMessagesDeleted))
            }
            
            result.completedOperations.isNotEmpty() && result.failedOperations.isNotEmpty() -> {
                val message = "Deletion partially completed. ${result.completedOperations.size} operations succeeded, ${result.failedOperations.size} failed."
                _deletionEvents.emit(DeletionEvent.PartialSuccess(message, result))
            }
            
            else -> {
                val errorMessage = result.errors.firstOrNull()?.let { error ->
                    when (error) {
                        is DeletionError.NetworkError -> "Network error: ${error.message}"
                        is DeletionError.DatabaseError -> "Database error: ${error.message}"
                        is DeletionError.ValidationError -> "Validation error: ${error.message}"
                        is DeletionError.SystemError -> "System error: ${error.message}"
                    }
                } ?: "Deletion failed for unknown reason"
                
                _deletionEvents.emit(DeletionEvent.Error(errorMessage))
            }
        }
    }

    /**
     * Retry failed deletion operations.
     * 
     * Attempts to retry specific failed operations or all failed operations for a user.
     * Requirements: 4.1, 4.3, 4.5
     */
    fun retryFailedOperations(userId: String, operations: List<DeletionOperation>? = null) {
        if (userId.isBlank()) {
            _uiState.update { it.copy(error = "User ID is required for retry") }
            return
        }

        if (_uiState.value.isDeleting) {
            _uiState.update { it.copy(error = "Cannot retry while another deletion is in progress") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        isDeleting = true,
                        error = null,
                        isRetrying = true
                    )
                }

                val result = if (operations != null) {
                    // Retry specific operations
                    chatHistoryManager.retrySpecificOperations(operations)
                } else {
                    // Retry all failed operations for user
                    chatHistoryManager.retryAllFailedOperations(userId)
                }
                
                handleRetryResult(result)
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isDeleting = false,
                        isRetrying = false,
                        error = "Failed to retry operations: ${e.message}"
                    )
                }
                
                viewModelScope.launch {
                    _deletionEvents.emit(DeletionEvent.Error("Retry failed: ${e.message}"))
                }
            }
        }
    }

    /**
     * Get failed operations for the current user.
     * Requirements: 4.3, 4.4
     */
    fun getFailedOperations(userId: String) {
        if (userId.isBlank()) {
            _uiState.update { it.copy(error = "User ID is required") }
            return
        }

        viewModelScope.launch {
            try {
                val failedOps = chatHistoryManager.getFailedOperations(userId)
                _uiState.update { 
                    it.copy(
                        failedOperations = failedOps,
                        error = null
                    )
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to load failed operations: ${e.message}") 
                }
            }
        }
    }

    /**
     * Handle partial completion by allowing user to choose retry or accept.
     * Requirements: 4.3, 4.4
     */
    fun handlePartialCompletion(result: DeletionResult, userChoice: PartialCompletionChoice) {
        viewModelScope.launch {
            when (userChoice) {
                PartialCompletionChoice.RETRY_FAILED -> {
                    if (result.failedOperations.isNotEmpty()) {
                        retryFailedOperations(getCurrentUserId(), result.failedOperations)
                    }
                }
                PartialCompletionChoice.ACCEPT_PARTIAL -> {
                    _uiState.update { 
                        it.copy(
                            lastDeletionResult = result.copy(success = true), // Mark as accepted
                            isDeleting = false
                        )
                    }
                    _deletionEvents.emit(
                        DeletionEvent.Success(
                            "Partial deletion accepted. ${result.totalMessagesDeleted} messages deleted.",
                            result.totalMessagesDeleted
                        )
                    )
                }
            }
        }
    }

    /**
     * Get current user ID for deletion operations.
     * This should be replaced with actual authentication service integration.
     */
    fun getCurrentUserId(): String {
        // TODO: Integrate with actual authentication service
        return "current_user_id"
    }

    /**
     * Validate deletion request before processing.
     * Requirements: 2.4, 3.4
     */
    fun validateDeletionRequest(userId: String, chatIds: List<String>? = null): ValidationResult {
        if (userId.isBlank()) {
            return ValidationResult.Invalid("User ID is required")
        }

        if (_uiState.value.isDeleting) {
            return ValidationResult.Invalid("Another deletion operation is in progress")
        }

        chatIds?.let { ids ->
            if (ids.isEmpty()) {
                return ValidationResult.Invalid("No chats selected for deletion")
            }
            
            if (ids.any { it.isBlank() }) {
                return ValidationResult.Invalid("Invalid chat ID found in selection")
            }
        }

        return ValidationResult.Valid
    }
}

/**
 * UI state for message deletion operations.
 * Requirements: 2.4, 6.5
 */
data class MessageDeletionUiState(
    val isDeleting: Boolean = false,
    val isRetrying: Boolean = false,
    val deletionProgress: DeletionProgress? = null,
    val lastDeletionResult: DeletionResult? = null,
    val lastDeletionType: DeletionType? = null,
    val selectedChatIds: List<String> = emptyList(),
    val failedOperations: List<DeletionOperation> = emptyList(),
    val error: String? = null,
    val canCancel: Boolean = false
)

/**
 * Events emitted by the MessageDeletionViewModel.
 * Requirements: 6.5
 */
sealed class DeletionEvent {
    data class Success(val message: String, val deletedCount: Int) : DeletionEvent()
    data class PartialSuccess(val message: String, val result: DeletionResult) : DeletionEvent()
    data class Error(val message: String) : DeletionEvent()
    object Cancelled : DeletionEvent()
}

/**
 * Validation result for deletion requests.
 * Requirements: 2.4, 3.4
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

/**
 * User choice for handling partial completion.
 * Requirements: 4.3, 4.4
 */
enum class PartialCompletionChoice {
    RETRY_FAILED,
    ACCEPT_PARTIAL
}