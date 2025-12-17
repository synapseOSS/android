package com.synapse.social.studioasinc.ui.deletion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.data.repository.deletion.ChatHistoryManager
import com.synapse.social.studioasinc.data.model.deletion.DeletionResult
import com.synapse.social.studioasinc.data.model.deletion.DeletionProgress
import com.synapse.social.studioasinc.data.model.deletion.DeletionOperation
import com.synapse.social.studioasinc.data.model.deletion.OperationStatus
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for the Chat History Deletion screen.
 * 
 * Manages the state for chat history deletion operations including:
 * - Available chats for deletion
 * - Deletion progress and status
 * - Deletion history
 * - Error handling and user notifications
 * 
 * Requirements: 2.4, 3.4, 6.5
 */
@HiltViewModel
class ChatHistoryDeletionViewModel @Inject constructor(
    private val chatHistoryManager: ChatHistoryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatHistoryDeletionUiState())
    val uiState: StateFlow<ChatHistoryDeletionUiState> = _uiState.asStateFlow()

    private val _availableChats = MutableStateFlow<List<ChatDeletionInfo>>(emptyList())
    val availableChats: StateFlow<List<ChatDeletionInfo>> = _availableChats.asStateFlow()

    private val _deletionHistory = MutableStateFlow<List<DeletionHistoryItem>>(emptyList())
    val deletionHistory: StateFlow<List<DeletionHistoryItem>> = _deletionHistory.asStateFlow()

    init {
        loadAvailableChats()
        loadDeletionHistory()
        observeDeletionProgress()
    }

    /**
     * Load available chats for deletion.
     */
    private fun loadAvailableChats() {
        viewModelScope.launch {
            try {
                // TODO: Implement actual chat loading from repository
                // For now, using mock data
                val mockChats = listOf(
                    ChatDeletionInfo(
                        chatId = "chat1",
                        chatName = "John Doe",
                        messageCount = 156,
                        lastMessageDate = "2 days ago"
                    ),
                    ChatDeletionInfo(
                        chatId = "chat2",
                        chatName = "Work Group",
                        messageCount = 423,
                        lastMessageDate = "1 hour ago"
                    ),
                    ChatDeletionInfo(
                        chatId = "chat3",
                        chatName = "Family Chat",
                        messageCount = 89,
                        lastMessageDate = "Yesterday"
                    )
                )
                _availableChats.value = mockChats
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load available chats: ${e.message}"
                )
            }
        }
    }

    /**
     * Load deletion history from the repository.
     */
    private fun loadDeletionHistory() {
        viewModelScope.launch {
            try {
                val history = chatHistoryManager.getDeletionHistory(getCurrentUserId())
                _deletionHistory.value = history.map { mapToHistoryItem(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load deletion history: ${e.message}"
                )
            }
        }
    }

    private fun mapToHistoryItem(operation: DeletionOperation): DeletionHistoryItem {
        val operationType = if (operation.chatIds == null) "Complete History Deletion" else "Selective Chat Deletion"

        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val dateString = sdf.format(Date(operation.timestamp))

        val statusString = when (operation.status) {
            OperationStatus.PENDING -> "Pending"
            OperationStatus.IN_PROGRESS -> "In Progress"
            OperationStatus.COMPLETED -> "Completed"
            OperationStatus.FAILED -> "Failed"
            OperationStatus.QUEUED_FOR_RETRY -> "Queued for Retry"
        }

        val isSuccess = operation.status == OperationStatus.COMPLETED
        val isError = operation.status == OperationStatus.FAILED
        val canRetry = operation.status == OperationStatus.FAILED || operation.status == OperationStatus.QUEUED_FOR_RETRY

        return DeletionHistoryItem(
            id = operation.id,
            operationType = operationType,
            timestamp = dateString,
            status = statusString,
            messagesAffected = operation.messagesAffected,
            isSuccess = isSuccess,
            isError = isError,
            canRetry = canRetry
        )
    }

    /**
     * Observe deletion progress from the chat history manager.
     */
    private fun observeDeletionProgress() {
        viewModelScope.launch {
            chatHistoryManager.getDeleteProgress().collect { progress ->
                _uiState.value = _uiState.value.copy(
                    deletionProgress = progress,
                    isDeleting = progress?.let { it.completedOperations < it.totalOperations } ?: false
                )
            }
        }
    }

    /**
     * Delete all chat history.
     */
    fun deleteAllHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isDeleting = true,
                    error = null
                )

                val result = chatHistoryManager.deleteAllHistory(getCurrentUserId())
                
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    lastDeletionResult = result
                )

                if (result.success) {
                    // Refresh available chats and history
                    loadAvailableChats()
                    loadDeletionHistory()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = "Failed to delete chat history: ${e.message}"
                )
            }
        }
    }

    /**
     * Delete selected chats.
     */
    fun deleteSelectedChats(selectedChats: List<ChatDeletionInfo>) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isDeleting = true,
                    error = null
                )

                val chatIds = selectedChats.map { it.chatId }
                val result = chatHistoryManager.deleteSpecificChats(getCurrentUserId(), chatIds)
                
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    lastDeletionResult = result
                )

                if (result.success) {
                    // Refresh available chats and history
                    loadAvailableChats()
                    loadDeletionHistory()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = "Failed to delete selected chats: ${e.message}"
                )
            }
        }
    }

    /**
     * Cancel ongoing deletion operation.
     */
    fun cancelDeletion() {
        viewModelScope.launch {
            try {
                val cancelled = chatHistoryManager.cancelDeletion()
                if (cancelled) {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        deletionProgress = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to cancel deletion: ${e.message}"
                )
            }
        }
    }

    /**
     * Retry a failed deletion operation.
     */
    fun retryFailedDeletion(deletionId: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement retry logic in ChatHistoryManager
                // For now, just refresh the history
                loadDeletionHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to retry deletion: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear deletion progress.
     */
    fun clearProgress() {
        _uiState.value = _uiState.value.copy(
            deletionProgress = null,
            lastDeletionResult = null
        )
    }

    /**
     * Navigate to full deletion history screen.
     */
    fun navigateToFullHistory() {
        // TODO: Implement navigation to full history screen
    }

    /**
     * Get current user ID.
     */
    fun getCurrentUserId(): String {
        // TODO: Get actual user ID from authentication service
        return "current_user_id"
    }

    /**
     * Get total message count across all chats.
     */
    fun getTotalMessageCount(): Int {
        return _availableChats.value.sumOf { it.messageCount }
    }
}

/**
 * UI state for the Chat History Deletion screen.
 */
data class ChatHistoryDeletionUiState(
    val isDeleting: Boolean = false,
    val deletionProgress: DeletionProgress? = null,
    val lastDeletionResult: DeletionResult? = null,
    val error: String? = null
)