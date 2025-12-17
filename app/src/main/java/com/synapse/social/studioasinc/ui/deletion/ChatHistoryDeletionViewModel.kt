package com.synapse.social.studioasinc.ui.deletion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.synapse.social.studioasinc.data.repository.deletion.ChatHistoryManager
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.data.model.deletion.DeletionResult
import com.synapse.social.studioasinc.data.model.deletion.DeletionProgress
import javax.inject.Inject

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
    private val chatHistoryManager: ChatHistoryManager,
    private val chatRepository: ChatRepository
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
                chatRepository.getUserChats()
                    .collect { result ->
                        val chats = result.getOrNull()
                        if (chats != null) {
                            coroutineScope {
                                val chatDeletionInfos = chats.map { chat ->
                                    async {
                                        val messageCountResult = chatRepository.getMessageCount(chat.id)
                                        val count = messageCountResult.getOrDefault(0L).toInt()

                                        ChatDeletionInfo(
                                            chatId = chat.id,
                                            chatName = chat.getDisplayName(),
                                            messageCount = count,
                                            lastMessageDate = chat.getFormattedLastMessageTime()
                                        )
                                    }
                                }.awaitAll()

                                _availableChats.value = chatDeletionInfos
                            }
                        } else {
                            val error = result.exceptionOrNull()
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to load available chats: ${error?.message ?: "Unknown error"}"
                            )
                        }
                    }
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
                // TODO: Implement actual history loading from repository
                // For now, using mock data
                val mockHistory = listOf(
                    DeletionHistoryItem(
                        id = "del1",
                        operationType = "Complete History Deletion",
                        timestamp = "2 hours ago",
                        status = "Completed",
                        messagesAffected = 1250,
                        isSuccess = true,
                        isError = false,
                        canRetry = false
                    ),
                    DeletionHistoryItem(
                        id = "del2",
                        operationType = "Selective Chat Deletion",
                        timestamp = "1 day ago",
                        status = "Partially Failed",
                        messagesAffected = 89,
                        isSuccess = false,
                        isError = true,
                        canRetry = true
                    )
                )
                _deletionHistory.value = mockHistory
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load deletion history: ${e.message}"
                )
            }
        }
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