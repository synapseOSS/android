package com.synapse.social.studioasinc.chat.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.GeminiAIService
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.data.repository.MessageActionRepository
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.NetworkUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * ViewModel for coordinating message action operations
 * Handles reply, forward, edit, delete, and AI summary actions
 */
class MessageActionsViewModel(private val context: Context) : ViewModel() {

    companion object {
        private const val TAG = "MessageActionsViewModel"
        private const val MAX_PREVIEW_LINES = 3
        private const val CHARS_PER_LINE = 50 // Approximate characters per line
    }

    private val repository = MessageActionRepository(context)
    private val geminiService = GeminiAIService(context)
    private val chatService = SupabaseChatService()
    private val actionQueue = com.synapse.social.studioasinc.data.ActionQueue(context)

    // State flows for different action types
    private val _replyState = MutableStateFlow<ReplyState>(ReplyState.Idle)
    val replyState: StateFlow<ReplyState> = _replyState.asStateFlow()

    private val _pendingActionsState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val pendingActionsState: StateFlow<Map<String, Boolean>> = _pendingActionsState.asStateFlow()

    /**
     * Sealed class representing different states for message actions
     */
    sealed class MessageActionState {
        object Idle : MessageActionState()
        object Loading : MessageActionState()
        data class Success(val message: String) : MessageActionState()
        data class Error(val error: String) : MessageActionState()
    }

    /**
     * Data class for forward operation state
     */
    data class ForwardState(
        val selectedChats: List<String> = emptyList(),
        val isForwarding: Boolean = false,
        val forwardedCount: Int = 0,
        val error: String? = null
    )

    /**
     * Data class for AI summary state
     */
    data class AISummaryState(
        val isGenerating: Boolean = false,
        val summary: String? = null,
        val error: String? = null,
        val characterCount: Int = 0,
        val estimatedReadTime: Int = 0,
        val rateLimitResetTime: Long = 0
    )

    /**
     * Sealed class for reply state
     */
    sealed class ReplyState {
        object Idle : ReplyState()
        data class Active(
            val messageId: String,
            val messageText: String,
            val senderName: String,
            val previewText: String
        ) : ReplyState()
    }

    // ==================== Reply Operations ====================

    /**
     * Prepare reply to a message
     * Truncates message text to 3 lines for preview
     * 
     * @param messageId The ID of the message being replied to
     * @param messageText The text content of the message
     * @param senderName The name of the message sender
     */
    fun prepareReply(messageId: String, messageText: String, senderName: String) {
        Log.d(TAG, "Preparing reply to message: $messageId")

        // Truncate message text to 3 lines for preview
        val previewText = truncateToLines(messageText, MAX_PREVIEW_LINES)

        _replyState.value = ReplyState.Active(
            messageId = messageId,
            messageText = messageText,
            senderName = senderName,
            previewText = previewText
        )

        Log.d(TAG, "Reply prepared for message: $messageId")
    }

    /**
     * Clear reply state
     */
    fun clearReply() {
        _replyState.value = ReplyState.Idle
        Log.d(TAG, "Reply state cleared")
    }

    /**
     * Truncate text to specified number of lines
     */
    private fun truncateToLines(text: String, maxLines: Int): String {
        val maxChars = maxLines * CHARS_PER_LINE
        return if (text.length > maxChars) {
            text.take(maxChars) + "..."
        } else {
            text
        }
    }

    // ==================== Forward Operations ====================

    /**
     * Forward a message to multiple chats
     * Emits Loading, Success, or Error states
     * 
     * @param messageId The ID of the message to forward
     * @param messageData The complete message data
     * @param targetChatIds List of chat IDs to forward to
     * @return Flow emitting ForwardState updates
     */
    fun forwardMessage(
        messageId: String,
        messageData: Map<String, Any?>,
        targetChatIds: List<String>
    ): Flow<ForwardState> = flow {
        try {
            Log.d(TAG, "Forwarding message $messageId to ${targetChatIds.size} chats")

            // Check network connectivity
            if (!NetworkUtil.isNetworkAvailable(context)) {
                Log.w(TAG, "Network unavailable, queueing forward action")
                
                // Queue the action for later
                val pendingAction = actionQueue.createForwardAction(messageId, messageData, targetChatIds)
                actionQueue.add(pendingAction)
                
                // Mark message as pending
                markMessageAsPending(messageId, true)
                
                emit(
                    ForwardState(
                        isForwarding = false,
                        error = "No network connection. Message will be forwarded when online."
                    )
                )
                return@flow
            }

            // Emit loading state
            emit(ForwardState(isForwarding = true))

            // Call repository to forward message
            val result = repository.forwardMessageToMultipleChats(messageData, targetChatIds)

            result.fold(
                onSuccess = { forwardedCount ->
                    Log.d(TAG, "Message forwarded to $forwardedCount chats successfully")
                    
                    // Check for partial failures
                    if (forwardedCount < targetChatIds.size) {
                        val failedCount = targetChatIds.size - forwardedCount
                        val errorMessage = context.getString(
                            com.synapse.social.studioasinc.R.string.error_forward_partial,
                            forwardedCount,
                            targetChatIds.size
                        )
                        Log.w(TAG, "Partial forward success: $forwardedCount/${ targetChatIds.size} - MessageId: $messageId")
                        emit(
                            ForwardState(
                                isForwarding = false,
                                forwardedCount = forwardedCount,
                                error = errorMessage
                            )
                        )
                    } else {
                        // Complete success
                        emit(
                            ForwardState(
                                isForwarding = false,
                                forwardedCount = forwardedCount
                            )
                        )
                    }
                },
                onFailure = { error ->
                    val errorMessage = ErrorHandler.getErrorMessage(
                        context = context,
                        errorType = ErrorHandler.ErrorType.FORWARD,
                        exception = error,
                        messageId = messageId
                    )
                    Log.e(TAG, "Forward failed - MessageId: $messageId, TargetChats: ${targetChatIds.size}", error)
                    emit(
                        ForwardState(
                            isForwarding = false,
                            error = errorMessage
                        )
                    )
                }
            )
        } catch (e: Exception) {
            val errorMessage = ErrorHandler.getErrorMessage(
                context = context,
                errorType = ErrorHandler.ErrorType.FORWARD,
                exception = e,
                messageId = messageId
            )
            Log.e(TAG, "Unexpected error forwarding message - MessageId: $messageId, TargetChats: ${targetChatIds.size}", e)
            emit(
                ForwardState(
                    isForwarding = false,
                    error = errorMessage
                )
            )
        }
    }

    // ==================== Edit Operations ====================

    /**
     * Edit a message with validation
     * Validates message age (<48 hours) and non-empty content
     * 
     * @param messageId The ID of the message to edit
     * @param newContent The new message content
     * @return Flow emitting MessageActionState updates
     */
    fun editMessage(messageId: String, newContent: String): Flow<MessageActionState> = flow {
        try {
            Log.d(TAG, "Editing message: $messageId")

            // Validate non-empty content
            if (newContent.isBlank()) {
                val errorMessage = context.getString(com.synapse.social.studioasinc.R.string.error_edit_empty)
                Log.w(TAG, "Edit validation failed: empty content - MessageId: $messageId")
                emit(MessageActionState.Error(errorMessage))
                return@flow
            }

            // Check network connectivity
            if (!NetworkUtil.isNetworkAvailable(context)) {
                Log.w(TAG, "Network unavailable, queueing edit action")
                
                // Queue the action for later
                val pendingAction = actionQueue.createEditAction(messageId, newContent)
                actionQueue.add(pendingAction)
                
                // Mark message as pending
                markMessageAsPending(messageId, true)
                
                emit(MessageActionState.Success("Edit queued. Will be applied when online."))
                return@flow
            }

            // Emit loading state
            emit(MessageActionState.Loading)

            // Call repository to edit message
            val result = repository.editMessage(messageId, newContent)

            result.fold(
                onSuccess = {
                    val successMessage = context.getString(com.synapse.social.studioasinc.R.string.success_edit)
                    Log.d(TAG, "Message edited successfully - MessageId: $messageId, ContentLength: ${newContent.length}")
                    emit(MessageActionState.Success(successMessage))
                },
                onFailure = { error ->
                    val errorMessage = ErrorHandler.getErrorMessage(
                        context = context,
                        errorType = ErrorHandler.ErrorType.EDIT,
                        exception = error,
                        messageId = messageId
                    )
                    Log.e(TAG, "Edit failed - MessageId: $messageId, ContentLength: ${newContent.length}", error)
                    emit(MessageActionState.Error(errorMessage))
                }
            )
        } catch (e: Exception) {
            val errorMessage = ErrorHandler.getErrorMessage(
                context = context,
                errorType = ErrorHandler.ErrorType.EDIT,
                exception = e,
                messageId = messageId
            )
            Log.e(TAG, "Unexpected error editing message - MessageId: $messageId, ContentLength: ${newContent.length}", e)
            emit(MessageActionState.Error(errorMessage))
        }
    }

    // ==================== Delete Operations ====================

    /**
     * Delete a message with option for local-only or server-side deletion
     * 
     * @param messageId The ID of the message to delete
     * @param deleteForEveryone If true, deletes for all users; if false, deletes locally only
     * @return Flow emitting MessageActionState updates
     */
    fun deleteMessage(messageId: String, deleteForEveryone: Boolean): Flow<MessageActionState> = flow {
        try {
            Log.d(TAG, "Deleting message: $messageId (deleteForEveryone=$deleteForEveryone)")

            // Check network connectivity (only for delete for everyone)
            if (deleteForEveryone && !NetworkUtil.isNetworkAvailable(context)) {
                Log.w(TAG, "Network unavailable, queueing delete action")
                
                // Queue the action for later
                val pendingAction = actionQueue.createDeleteAction(messageId, deleteForEveryone)
                actionQueue.add(pendingAction)
                
                // Mark message as pending
                markMessageAsPending(messageId, true)
                
                emit(MessageActionState.Success("Delete queued. Will be applied when online."))
                return@flow
            }

            // Emit loading state
            emit(MessageActionState.Loading)

            // Call appropriate repository method based on deleteForEveryone flag
            val result = if (deleteForEveryone) {
                repository.deleteMessageForEveryone(messageId)
            } else {
                repository.deleteMessageLocally(messageId)
            }

            result.fold(
                onSuccess = {
                    val successMessage = if (deleteForEveryone) {
                        context.getString(com.synapse.social.studioasinc.R.string.success_delete_for_everyone)
                    } else {
                        context.getString(com.synapse.social.studioasinc.R.string.success_delete_for_me)
                    }
                    Log.d(TAG, "Message deleted successfully - MessageId: $messageId, DeleteForEveryone: $deleteForEveryone")
                    emit(MessageActionState.Success(successMessage))
                },
                onFailure = { error ->
                    val errorMessage = ErrorHandler.getErrorMessage(
                        context = context,
                        errorType = ErrorHandler.ErrorType.DELETE,
                        exception = error,
                        messageId = messageId
                    )
                    Log.e(TAG, "Delete failed - MessageId: $messageId, DeleteForEveryone: $deleteForEveryone", error)
                    emit(MessageActionState.Error(errorMessage))
                }
            )
        } catch (e: Exception) {
            val errorMessage = ErrorHandler.getErrorMessage(
                context = context,
                errorType = ErrorHandler.ErrorType.DELETE,
                exception = e,
                messageId = messageId
            )
            Log.e(TAG, "Unexpected error deleting message - MessageId: $messageId, DeleteForEveryone: $deleteForEveryone", e)
            emit(MessageActionState.Error(errorMessage))
        }
    }

    // ==================== AI Summary Operations ====================

    /**
     * Generate AI summary for a message
     * Checks cache first, then calls Gemini API if needed
     * Handles rate limiting and caches successful results
     * 
     * @param messageId The ID of the message to summarize
     * @param messageText The text content to summarize
     * @return Flow emitting AISummaryState updates
     */
    fun generateAISummary(messageId: String, messageText: String): Flow<AISummaryState> = flow {
        try {
            Log.d(TAG, "Generating AI summary for message: $messageId")

            // Check if summary is cached first
            val cachedSummary = repository.getCachedSummary(messageId)
            if (cachedSummary != null) {
                Log.d(TAG, "Using cached summary for message: $messageId")
                
                // Calculate metadata for cached summary
                val characterCount = messageText.length
                val estimatedReadTime = calculateReadingTime(messageText)
                
                emit(
                    AISummaryState(
                        isGenerating = false,
                        summary = cachedSummary,
                        characterCount = characterCount,
                        estimatedReadTime = estimatedReadTime
                    )
                )
                return@flow
            }

            // Check if rate limited
            if (geminiService.isRateLimited()) {
                val resetTime = geminiService.getRateLimitResetTime()
                Log.w(TAG, "Rate limited. Reset time: $resetTime")
                emit(
                    AISummaryState(
                        isGenerating = false,
                        error = "Rate limit reached. Please try again later.",
                        rateLimitResetTime = resetTime
                    )
                )
                return@flow
            }

            // Emit loading state
            emit(AISummaryState(isGenerating = true))

            // Call Gemini API to generate summary
            val result = geminiService.generateSummary(messageText)

            result.fold(
                onSuccess = { summaryResult ->
                    Log.d(TAG, "AI summary generated successfully - MessageId: $messageId, SummaryLength: ${summaryResult.summary.length}, CharCount: ${summaryResult.characterCount}, ReadTime: ${summaryResult.estimatedReadTimeMinutes}min")
                    
                    // Cache the summary
                    repository.cacheSummary(messageId, summaryResult.summary)
                    
                    emit(
                        AISummaryState(
                            isGenerating = false,
                            summary = summaryResult.summary,
                            characterCount = summaryResult.characterCount,
                            estimatedReadTime = summaryResult.estimatedReadTimeMinutes
                        )
                    )
                },
                onFailure = { error ->
                    val errorMessage = ErrorHandler.getErrorMessage(
                        context = context,
                        errorType = ErrorHandler.ErrorType.AI_SUMMARY,
                        exception = error,
                        messageId = messageId
                    )
                    
                    // Check if it's a rate limit error
                    val resetTime = if (ErrorHandler.isRateLimitError(error)) {
                        geminiService.getRateLimitResetTime()
                    } else {
                        0L
                    }
                    
                    Log.e(TAG, "AI summary generation failed - MessageId: $messageId, TextLength: ${messageText.length}, RateLimited: ${ErrorHandler.isRateLimitError(error)}", error)
                    
                    emit(
                        AISummaryState(
                            isGenerating = false,
                            error = errorMessage,
                            rateLimitResetTime = resetTime
                        )
                    )
                }
            )
        } catch (e: Exception) {
            val errorMessage = ErrorHandler.getErrorMessage(
                context = context,
                errorType = ErrorHandler.ErrorType.AI_SUMMARY,
                exception = e,
                messageId = messageId
            )
            Log.e(TAG, "Unexpected error generating AI summary - MessageId: $messageId, TextLength: ${messageText.length}", e)
            emit(
                AISummaryState(
                    isGenerating = false,
                    error = errorMessage
                )
            )
        }
    }

    /**
     * Calculate estimated reading time in minutes
     * Based on average reading speed of 200 words per minute
     */
    private fun calculateReadingTime(text: String): Int {
        val wordCount = text.split("\\s+".toRegex()).size
        val minutes = (wordCount.toDouble() / 200).toInt()
        return if (minutes < 1) 1 else minutes
    }

    /**
     * Get cached summary for a message
     * 
     * @param messageId The ID of the message
     * @return Cached summary text or null if not cached
     */
    fun getCachedSummary(messageId: String): String? {
        return repository.getCachedSummary(messageId)
    }

    /**
     * Clear all cached summaries
     */
    fun clearSummaryCache() {
        viewModelScope.launch {
            try {
                repository.clearSummaryCache()
                Log.d(TAG, "Summary cache cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing summary cache", e)
            }
        }
    }

    // ==================== Offline Queue Management ====================

    /**
     * Mark a message as having a pending action
     * @param messageId The message ID
     * @param isPending true to mark as pending, false to clear
     */
    private fun markMessageAsPending(messageId: String, isPending: Boolean) {
        val currentState = _pendingActionsState.value.toMutableMap()
        if (isPending) {
            currentState[messageId] = true
        } else {
            currentState.remove(messageId)
        }
        _pendingActionsState.value = currentState
        Log.d(TAG, "Message $messageId pending state: $isPending")
    }

    /**
     * Check if a message has a pending action
     * @param messageId The message ID
     * @return true if message has pending action, false otherwise
     */
    fun isMessagePending(messageId: String): Boolean {
        return _pendingActionsState.value[messageId] == true
    }

    /**
     * Get all pending actions from the queue
     * @return List of pending actions
     */
    fun getPendingActions(): List<com.synapse.social.studioasinc.model.PendingAction> {
        return actionQueue.getAll()
    }

    /**
     * Get count of pending actions
     * @return Number of pending actions
     */
    fun getPendingActionsCount(): Int {
        return actionQueue.size()
    }

    /**
     * Process all queued actions when network becomes available
     * Processes actions in order with exponential backoff for failures
     */
    fun processQueuedActions() {
        viewModelScope.launch {
            try {
                val pendingActions = actionQueue.getAll()
                if (pendingActions.isEmpty()) {
                    Log.d(TAG, "No pending actions to process")
                    return@launch
                }

                Log.d(TAG, "Processing ${pendingActions.size} queued actions")

                for (action in pendingActions) {
                    try {
                        val success = processAction(action)
                        
                        if (success) {
                            // Remove successfully processed action
                            actionQueue.remove(action.id)
                            markMessageAsPending(action.messageId, false)
                            Log.d(TAG, "Successfully processed action: ${action.id}")
                        } else {
                            // Increment retry count
                            val updatedAction = action.copy(retryCount = action.retryCount + 1)
                            
                            // Check if max retries reached (3 attempts)
                            if (updatedAction.retryCount >= 3) {
                                Log.w(TAG, "Max retries reached for action: ${action.id}, removing from queue")
                                actionQueue.remove(action.id)
                                markMessageAsPending(action.messageId, false)
                            } else {
                                // Update action with new retry count
                                actionQueue.update(updatedAction)
                                Log.d(TAG, "Action failed, retry count: ${updatedAction.retryCount}")
                                
                                // Exponential backoff delay
                                val delayMs = calculateBackoffDelay(updatedAction.retryCount)
                                kotlinx.coroutines.delay(delayMs)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing action: ${action.id}", e)
                        
                        // Increment retry count on exception
                        val updatedAction = action.copy(retryCount = action.retryCount + 1)
                        if (updatedAction.retryCount >= 3) {
                            actionQueue.remove(action.id)
                            markMessageAsPending(action.messageId, false)
                        } else {
                            actionQueue.update(updatedAction)
                        }
                    }
                }

                Log.d(TAG, "Finished processing queued actions")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing queued actions", e)
            }
        }
    }

    /**
     * Process a single pending action
     * @param action The PendingAction to process
     * @return true if successful, false otherwise
     */
    private suspend fun processAction(action: com.synapse.social.studioasinc.model.PendingAction): Boolean {
        return try {
            Log.d(TAG, "Processing queued action - ActionId: ${action.id}, Type: ${action.actionType}, MessageId: ${action.messageId}, RetryCount: ${action.retryCount}")

            when (action.actionType) {
                com.synapse.social.studioasinc.model.PendingAction.ActionType.EDIT -> {
                    val newContent = action.parameters["newContent"] as? String
                    if (newContent != null) {
                        val result = repository.editMessage(action.messageId, newContent)
                        if (result.isSuccess) {
                            Log.d(TAG, "Queued edit action processed successfully - ActionId: ${action.id}, MessageId: ${action.messageId}")
                        } else {
                            Log.w(TAG, "Queued edit action failed - ActionId: ${action.id}, MessageId: ${action.messageId}, Error: ${result.exceptionOrNull()?.message}")
                        }
                        result.isSuccess
                    } else {
                        Log.e(TAG, "Missing newContent parameter for edit action - ActionId: ${action.id}, MessageId: ${action.messageId}")
                        false
                    }
                }
                
                com.synapse.social.studioasinc.model.PendingAction.ActionType.DELETE -> {
                    val deleteForEveryone = action.parameters["deleteForEveryone"] as? Boolean ?: false
                    val result = if (deleteForEveryone) {
                        repository.deleteMessageForEveryone(action.messageId)
                    } else {
                        repository.deleteMessageLocally(action.messageId)
                    }
                    if (result.isSuccess) {
                        Log.d(TAG, "Queued delete action processed successfully - ActionId: ${action.id}, MessageId: ${action.messageId}, DeleteForEveryone: $deleteForEveryone")
                    } else {
                        Log.w(TAG, "Queued delete action failed - ActionId: ${action.id}, MessageId: ${action.messageId}, DeleteForEveryone: $deleteForEveryone, Error: ${result.exceptionOrNull()?.message}")
                    }
                    result.isSuccess
                }
                
                com.synapse.social.studioasinc.model.PendingAction.ActionType.FORWARD -> {
                    @Suppress("UNCHECKED_CAST")
                    val messageData = action.parameters["messageData"] as? Map<String, Any?>
                    @Suppress("UNCHECKED_CAST")
                    val targetChatIds = action.parameters["targetChatIds"] as? List<String>
                    
                    if (messageData != null && targetChatIds != null) {
                        val result = repository.forwardMessageToMultipleChats(messageData, targetChatIds)
                        if (result.isSuccess) {
                            Log.d(TAG, "Queued forward action processed successfully - ActionId: ${action.id}, MessageId: ${action.messageId}, ForwardedTo: ${result.getOrNull()} chats")
                        } else {
                            Log.w(TAG, "Queued forward action failed - ActionId: ${action.id}, MessageId: ${action.messageId}, TargetChats: ${targetChatIds.size}, Error: ${result.exceptionOrNull()?.message}")
                        }
                        result.isSuccess
                    } else {
                        Log.e(TAG, "Missing parameters for forward action - ActionId: ${action.id}, MessageId: ${action.messageId}, HasMessageData: ${messageData != null}, HasTargetChats: ${targetChatIds != null}")
                        false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing queued action - ActionId: ${action.id}, Type: ${action.actionType}, MessageId: ${action.messageId}", e)
            false
        }
    }

    /**
     * Calculate exponential backoff delay
     * @param retryCount The current retry count
     * @return Delay in milliseconds
     */
    private fun calculateBackoffDelay(retryCount: Int): Long {
        // Exponential backoff: 1s, 2s, 4s
        return (1000L * (1 shl retryCount)).coerceAtMost(4000L)
    }

    /**
     * Register network callback to automatically process queued actions when online
     * Should be called from Activity/Fragment onCreate
     * @return NetworkCallback that should be unregistered in onDestroy
     */
    fun registerNetworkCallback(): android.net.ConnectivityManager.NetworkCallback {
        return NetworkUtil.registerNetworkCallback(
            context = context,
            onNetworkAvailable = {
                Log.d(TAG, "Network available, processing queued actions")
                processQueuedActions()
            },
            onNetworkLost = {
                Log.d(TAG, "Network lost")
            }
        )
    }

    /**
     * Unregister network callback
     * Should be called from Activity/Fragment onDestroy
     * @param callback The NetworkCallback to unregister
     */
    fun unregisterNetworkCallback(callback: android.net.ConnectivityManager.NetworkCallback) {
        NetworkUtil.unregisterNetworkCallback(context, callback)
    }
}
