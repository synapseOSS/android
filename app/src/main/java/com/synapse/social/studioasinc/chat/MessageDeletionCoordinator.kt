package com.synapse.social.studioasinc.chat

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.presentation.viewmodel.MessageDeletionViewModel
import com.synapse.social.studioasinc.data.repository.MessageDeletionRepository
import kotlinx.coroutines.launch

/**
 * Coordinates the message deletion flow
 * Orchestrates deletion operations, validates permissions, and manages dialog display
 * 
 * Requirements: 2.3, 5.4, 5.1, 5.2, 5.3, 5.5, 5.6
 * 
 * @param activity The ChatActivity instance
 * @param viewModel The MessageDeletionViewModel for deletion operations
 * @param currentUserId The ID of the current user
 */
class MessageDeletionCoordinator(
    private val activity: AppCompatActivity,
    private val viewModel: MessageDeletionViewModel,
    private val currentUserId: String
) {
    private val repository = MessageDeletionRepository()

    /**
     * Initiate the deletion flow for selected messages
     * Validates ownership and shows appropriate deletion dialog
     * 
     * Requirements: 2.3, 5.4
     * 
     * @param messageIds List of message IDs to delete
     */
    fun initiateDelete(messageIds: List<String>) {
        if (messageIds.isEmpty()) {
            return
        }

        if (currentUserId.isBlank()) {
            return
        }

        // Validate ownership asynchronously
        activity.lifecycleScope.launch {
            val canDeleteForEveryone = validateOwnership(messageIds)
            showDeletionDialog(messageIds, canDeleteForEveryone)
        }
    }

    /**
     * Show deletion confirmation dialog with appropriate options
     * Handles both single and multiple message deletion
     * 
     * Requirements: 5.1, 5.2, 5.3, 5.4
     * 
     * @param messageIds List of message IDs to delete
     * @param canDeleteForEveryone Whether user owns all messages (enables "Delete for everyone" option)
     */
    private fun showDeletionDialog(messageIds: List<String>, canDeleteForEveryone: Boolean) {
        val dialog = DeleteConfirmationDialog.newInstance(
            messageIds = messageIds,
            canDeleteForEveryone = canDeleteForEveryone
        )
        
        // Set callback for deletion execution
        dialog.setDeletionCallback { selectedMessageIds, deleteForEveryone ->
            executeDelete(selectedMessageIds, deleteForEveryone)
        }
        
        dialog.show(activity.supportFragmentManager, "DeleteConfirmationDialog")
    }

    /**
     * Execute the deletion operation via ViewModel
     * Calls appropriate ViewModel function based on deletion type
     * 
     * Requirements: 5.5, 5.6
     * 
     * @param messageIds List of message IDs to delete
     * @param deleteForEveryone Whether to delete for everyone or just current user
     */
    private fun executeDelete(messageIds: List<String>, deleteForEveryone: Boolean) {
        if (deleteForEveryone) {
            viewModel.deleteMessagesForEveryone(messageIds, currentUserId)
        } else {
            viewModel.deleteMessagesForMe(messageIds, currentUserId)
        }
    }

    /**
     * Validate that the user owns all specified messages
     * Used to determine if "Delete for everyone" option should be available
     * 
     * Requirements: 2.3, 5.4
     * 
     * @param messageIds List of message IDs to validate
     * @return true if user owns all messages, false otherwise
     */
    private suspend fun validateOwnership(messageIds: List<String>): Boolean {
        if (messageIds.isEmpty() || currentUserId.isBlank()) {
            return false
        }

        val ownedMessageIds = repository.getMessagesBySenderId(messageIds, currentUserId)
        return ownedMessageIds.size == messageIds.size
    }
}
