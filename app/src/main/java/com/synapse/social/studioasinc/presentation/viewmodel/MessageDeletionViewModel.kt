package com.synapse.social.studioasinc.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.repository.MessageDeletionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for message deletion operations
 * Manages deletion state and coordinates repository operations
 * 
 * Requirements: 1.1, 2.1, 2.3, 5.4, 6.5
 */
class MessageDeletionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessageDeletionRepository()
    private val context = application.applicationContext

    // Deletion state management
    private val _deletionState = MutableStateFlow<DeletionState>(DeletionState.Idle)
    val deletionState: StateFlow<DeletionState> = _deletionState.asStateFlow()

    // Error state management
    private val _errorState = MutableSharedFlow<String>()
    val errorState: SharedFlow<String> = _errorState.asSharedFlow()

    // Optimistic update state - tracks messages pending deletion
    private val _optimisticDeletedMessages = MutableStateFlow<Set<String>>(emptySet())
    val optimisticDeletedMessages: StateFlow<Set<String>> = _optimisticDeletedMessages.asStateFlow()

    /**
     * Delete messages for the current user only
     * Marks messages as deleted in user_deleted_messages table
     * Uses optimistic updates for immediate UI feedback
     * 
     * Requirements: 1.1, 1.4, 6.5, 7.2
     * 
     * @param messageIds List of message IDs to delete
     * @param userId Current user ID
     */
    fun deleteMessagesForMe(messageIds: List<String>, userId: String) {
        if (messageIds.isEmpty()) {
            viewModelScope.launch {
                _errorState.emit(context.getString(R.string.error_deletion_no_messages))
            }
            return
        }

        if (userId.isBlank()) {
            viewModelScope.launch {
                _errorState.emit(context.getString(R.string.error_deletion_user_required))
            }
            return
        }

        viewModelScope.launch {
            // Optimistic update - mark messages as deleted immediately
            _optimisticDeletedMessages.value = _optimisticDeletedMessages.value + messageIds
            _deletionState.value = DeletionState.Deleting

            val result = repository.deleteForMe(messageIds, userId)

            result.onSuccess {
                // Keep optimistic updates in place - they're now confirmed
                _deletionState.value = DeletionState.Success(messageIds.size)
            }.onFailure { exception ->
                // Revert optimistic updates on failure
                _optimisticDeletedMessages.value = _optimisticDeletedMessages.value - messageIds.toSet()
                
                val errorMessage = when {
                    exception.message?.contains("network", ignoreCase = true) == true ->
                        context.getString(R.string.error_deletion_network)
                    else ->
                        context.getString(R.string.error_deletion_generic)
                }
                _deletionState.value = DeletionState.Error(errorMessage)
                _errorState.emit(errorMessage)
            }
        }
    }

    /**
     * Delete messages for everyone in the chat
     * Updates is_deleted and delete_for_everyone fields in messages table
     * Only message owners can delete for everyone
     * Uses optimistic updates for immediate UI feedback
     * 
     * Requirements: 2.1, 2.4, 2.5, 6.5, 7.2
     * 
     * @param messageIds List of message IDs to delete
     * @param userId Current user ID (must be the sender)
     */
    fun deleteMessagesForEveryone(messageIds: List<String>, userId: String) {
        if (messageIds.isEmpty()) {
            viewModelScope.launch {
                _errorState.emit(context.getString(R.string.error_deletion_no_messages))
            }
            return
        }

        if (userId.isBlank()) {
            viewModelScope.launch {
                _errorState.emit(context.getString(R.string.error_deletion_user_required))
            }
            return
        }

        viewModelScope.launch {
            // Validate ownership before attempting deletion
            val ownsAllMessages = validateMessageOwnership(messageIds, userId)
            
            if (!ownsAllMessages) {
                val errorMessage = context.getString(R.string.error_deletion_permission)
                _deletionState.value = DeletionState.Error(errorMessage)
                _errorState.emit(errorMessage)
                return@launch
            }

            // Optimistic update - mark messages as deleted immediately
            _optimisticDeletedMessages.value = _optimisticDeletedMessages.value + messageIds
            _deletionState.value = DeletionState.Deleting

            val result = repository.deleteForEveryone(messageIds, userId)

            result.onSuccess {
                // Keep optimistic updates in place - they're now confirmed
                _deletionState.value = DeletionState.Success(messageIds.size)
            }.onFailure { exception ->
                // Revert optimistic updates on failure
                _optimisticDeletedMessages.value = _optimisticDeletedMessages.value - messageIds.toSet()
                
                val errorMessage = when {
                    exception.message?.contains("network", ignoreCase = true) == true ->
                        context.getString(R.string.error_deletion_network)
                    exception.message?.contains("own messages", ignoreCase = true) == true ->
                        context.getString(R.string.error_deletion_permission)
                    else ->
                        context.getString(R.string.error_deletion_generic)
                }
                _deletionState.value = DeletionState.Error(errorMessage)
                _errorState.emit(errorMessage)
            }
        }
    }

    /**
     * Validate that the user owns all specified messages
     * Used for ownership validation before delete for everyone
     * 
     * Requirements: 2.3, 5.4
     * 
     * @param messageIds List of message IDs to validate
     * @param userId User ID to validate against
     * @return true if user owns all messages, false otherwise
     */
    private suspend fun validateMessageOwnership(messageIds: List<String>, userId: String): Boolean {
        if (messageIds.isEmpty() || userId.isBlank()) {
            return false
        }

        val ownedMessageIds = repository.getMessagesBySenderId(messageIds, userId)
        return ownedMessageIds.size == messageIds.size
    }

    /**
     * Reset deletion state to idle
     * Called after handling deletion result
     */
    fun resetState() {
        _deletionState.value = DeletionState.Idle
    }

    /**
     * Clear optimistic deleted messages
     * Called after successful deletion is confirmed and UI is updated
     */
    fun clearOptimisticDeletes() {
        _optimisticDeletedMessages.value = emptySet()
    }

    /**
     * Check if a message is optimistically deleted
     * Used by UI to immediately hide deleted messages
     * @param messageId Message ID to check
     * @return true if message is optimistically deleted
     */
    fun isOptimisticallyDeleted(messageId: String): Boolean {
        return _optimisticDeletedMessages.value.contains(messageId)
    }
}

/**
 * Sealed class representing deletion operation states
 * 
 * Requirements: 6.5
 */
sealed class DeletionState {
    /**
     * Idle state - no deletion operation in progress
     */
    object Idle : DeletionState()

    /**
     * Deleting state - deletion operation in progress
     */
    object Deleting : DeletionState()

    /**
     * Success state - deletion completed successfully
     * @param deletedCount Number of messages deleted
     */
    data class Success(val deletedCount: Int) : DeletionState()

    /**
     * Error state - deletion failed
     * @param message User-friendly error message
     */
    data class Error(val message: String) : DeletionState()
}
