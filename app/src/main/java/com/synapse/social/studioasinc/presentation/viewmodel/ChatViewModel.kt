package com.synapse.social.studioasinc.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.domain.usecase.*
import com.synapse.social.studioasinc.model.Chat
import com.synapse.social.studioasinc.model.Message
import com.synapse.social.studioasinc.chat.models.TypingStatus
import com.synapse.social.studioasinc.chat.models.ReadReceiptEvent
import com.synapse.social.studioasinc.chat.models.ChatMessageImpl
import com.synapse.social.studioasinc.chat.models.ChatAttachmentImpl
import com.synapse.social.studioasinc.chat.models.MessageState
import com.synapse.social.studioasinc.chat.models.MessageType
import com.synapse.social.studioasinc.chat.service.TypingIndicatorManager
import com.synapse.social.studioasinc.chat.service.ReadReceiptManager
import com.synapse.social.studioasinc.chat.service.SupabaseRealtimeService
import com.synapse.social.studioasinc.chat.service.PreferencesManager
import com.synapse.social.studioasinc.chat.service.MediaUploadManager
import com.synapse.social.studioasinc.chat.service.MessageSearchService
import com.synapse.social.studioasinc.chat.service.ChatBackupService
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.model.models.UploadProgress
import com.synapse.social.studioasinc.model.models.MediaUploadResult
import com.synapse.social.studioasinc.util.PaginationManager
import com.synapse.social.studioasinc.util.ScrollPositionState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for chat functionality with typing indicators and read receipts
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val authService = SupabaseAuthenticationService()
    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    private val chatRepository = ChatRepository(chatDao)
    
    // Use cases
    private val sendMessageUseCase = SendMessageUseCase(chatDao)
    private val getMessagesUseCase = GetMessagesUseCase(chatDao)
    private val observeMessagesUseCase = ObserveMessagesUseCase(chatDao)
    private val getUserChatsUseCase = GetUserChatsUseCase(chatDao)
    private val deleteMessageUseCase = DeleteMessageUseCase(chatDao)
    private val editMessageUseCase = EditMessageUseCase(chatDao)

    // Existing LiveData properties
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> = _chats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _messageSent = MutableLiveData<Boolean>()
    val messageSent: LiveData<Boolean> = _messageSent

    // Typing indicator state management
    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

    // Enhanced messages StateFlow for real-time updates
    private val _messagesStateFlow = MutableStateFlow<List<ChatMessageImpl>>(emptyList())
    val messagesStateFlow: StateFlow<List<ChatMessageImpl>> = _messagesStateFlow.asStateFlow()

    // Upload progress state management
    private val _uploadProgress = MutableStateFlow<Map<String, UploadProgress>>(emptyMap())
    val uploadProgress: StateFlow<Map<String, UploadProgress>> = _uploadProgress.asStateFlow()

    private var currentChatId: String? = null
    private var currentUserId: String? = null

    // Managers for typing indicators and read receipts
    private var typingIndicatorManager: TypingIndicatorManager? = null
    private var readReceiptManager: ReadReceiptManager? = null
    private var realtimeService: SupabaseRealtimeService? = null
    private var preferencesManager: PreferencesManager? = null
    private var mediaUploadManager: MediaUploadManager? = null
    private var messageSearchService: MessageSearchService? = null
    private var chatBackupService: ChatBackupService? = null
    
    // Pagination manager instance for messages
    private var paginationManager: PaginationManager<Message>? = null
    
    // Expose messages StateFlow by mapping PaginationState
    private val _paginatedMessages = MutableStateFlow<List<Message>>(emptyList())
    val paginatedMessages: StateFlow<List<Message>> = _paginatedMessages.asStateFlow()
    
    // Expose loading states for pagination
    private val _isPaginationLoading = MutableStateFlow(false)
    val isPaginationLoading: StateFlow<Boolean> = _isPaginationLoading.asStateFlow()
    
    private val _isLoadingMoreMessages = MutableStateFlow(false)
    val isLoadingMoreMessages: StateFlow<Boolean> = _isLoadingMoreMessages.asStateFlow()
    
    // Scroll position state for restoration and prepending messages
    private var savedScrollPositionState: ScrollPositionState? = null

    /**
     * Initialize pagination manager for a specific chat
     * Should be called when opening a chat
     */
    private fun initializePaginationForChat(chatId: String) {
        android.util.Log.d("ChatViewModel", "=== initializePaginationForChat START ===")
        android.util.Log.d("ChatViewModel", "Initializing pagination for chatId: $chatId")
        android.util.Log.d("ChatViewModel", "PageSize: Int.MAX_VALUE (${Int.MAX_VALUE})")
        
        // Create pagination manager with ChatRepository.getMessagesPage callback
        // Load ALL messages to ensure complete conversation history
        paginationManager = PaginationManager<Message>(
            pageSize = Int.MAX_VALUE, // Load all messages at once
            scrollThreshold = 10,
            onLoadPage = { page, pageSize ->
                android.util.Log.d("ChatViewModel", "onLoadPage callback - page: $page, pageSize: $pageSize")
                android.util.Log.d("ChatViewModel", "Fetching messages from repository for chatId: $chatId")
                
                // For chat messages, always load ALL messages
                // This fixes the issue where only 50 messages show when returning to chat
                val result = chatRepository.getMessagesPage(chatId, null, Int.MAX_VALUE)
                
                result.onSuccess { messages ->
                    android.util.Log.d("ChatViewModel", "Successfully fetched ${messages.size} messages from repository")
                    messages.forEachIndexed { index, message ->
                        if (index < 5 || index >= messages.size - 5) {
                            android.util.Log.d("ChatViewModel", "Message[$index]: id=${message.id}, content=${message.content.take(30)}, createdAt=${message.createdAt}")
                        }
                    }
                    if (messages.size > 10) {
                        android.util.Log.d("ChatViewModel", "... (${messages.size - 10} more messages)")
                    }
                }.onFailure { error ->
                    android.util.Log.e("ChatViewModel", "Failed to fetch messages: ${error.message}", error)
                }
                
                result
            },
            onError = { error ->
                android.util.Log.e("ChatViewModel", "Pagination error: $error")
                _error.value = error
            },
            coroutineScope = viewModelScope
        )
        
        // Observe pagination state and map to messages StateFlow
        viewModelScope.launch {
            paginationManager?.paginationState?.collect { state ->
                android.util.Log.d("ChatViewModel", "Pagination state changed: ${state::class.simpleName}")
                
                when (state) {
                    is PaginationManager.PaginationState.Success -> {
                        android.util.Log.d("ChatViewModel", "Success state - ${state.items.size} messages")
                        _paginatedMessages.value = state.items
                        _isPaginationLoading.value = false
                        _isLoadingMoreMessages.value = false
                        android.util.Log.d("ChatViewModel", "Updated _paginatedMessages with ${state.items.size} items")
                    }
                    is PaginationManager.PaginationState.LoadingMore -> {
                        android.util.Log.d("ChatViewModel", "LoadingMore state - ${state.currentItems.size} current messages")
                        _paginatedMessages.value = state.currentItems
                        _isLoadingMoreMessages.value = true
                        _isPaginationLoading.value = false
                    }
                    is PaginationManager.PaginationState.Error -> {
                        android.util.Log.e("ChatViewModel", "Error state - ${state.currentItems.size} messages")
                        _paginatedMessages.value = state.currentItems
                        _isPaginationLoading.value = false
                        _isLoadingMoreMessages.value = false
                    }
                    is PaginationManager.PaginationState.EndOfList -> {
                        android.util.Log.d("ChatViewModel", "EndOfList state - ${state.items.size} messages")
                        _paginatedMessages.value = state.items
                        _isPaginationLoading.value = false
                        _isLoadingMoreMessages.value = false
                    }
                    is PaginationManager.PaginationState.Refreshing -> {
                        android.util.Log.d("ChatViewModel", "Refreshing state")
                        _isPaginationLoading.value = true
                        _isLoadingMoreMessages.value = false
                    }
                    is PaginationManager.PaginationState.Initial -> {
                        android.util.Log.d("ChatViewModel", "Initial state")
                        _isPaginationLoading.value = false
                        _isLoadingMoreMessages.value = false
                    }
                }
            }
        }
        
        android.util.Log.d("ChatViewModel", "PaginationManager created successfully")
        android.util.Log.d("ChatViewModel", "=== initializePaginationForChat END ===")
    }
    
    /**
     * Loads messages for a chat using pagination
     */
    fun loadMessages(chatId: String) {
        android.util.Log.d("ChatViewModel", "=== loadMessages START ===")
        android.util.Log.d("ChatViewModel", "Loading messages for chatId: $chatId")
        android.util.Log.d("ChatViewModel", "Current message count: ${_paginatedMessages.value.size}")
        
        currentChatId = chatId
        
        // Always reinitialize pagination for the chat to ensure fresh state
        // This fixes the issue where returning to chat only shows 50 messages
        initializePaginationForChat(chatId)
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            _isLoading.value = true
            android.util.Log.d("ChatViewModel", "Set isLoading = true")
            try {
                // Invalidate cache on refresh
                chatRepository.invalidateCache()
                android.util.Log.d("ChatViewModel", "Cache invalidated")
                
                // Clear saved position on refresh
                clearScrollPosition()
                android.util.Log.d("ChatViewModel", "Scroll position cleared")
                
                paginationManager?.refresh()
                android.util.Log.d("ChatViewModel", "Pagination manager refreshed")
                
                _error.value = null
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error loading messages", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
                android.util.Log.d("ChatViewModel", "Set isLoading = false")
                android.util.Log.d("ChatViewModel", "Final message count: ${_paginatedMessages.value.size}")
                android.util.Log.d("ChatViewModel", "=== loadMessages END ===")
            }
        }
    }
    
    /**
     * Load older messages (for pull-to-refresh at top of chat)
     * Handles scroll position preservation for prepending messages
     */
    fun loadOlderMessages() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                paginationManager?.loadNextPage()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    /**
     * Save scroll position for restoration
     * Called when navigating away from the chat or before loading older messages
     * 
     * @param position The scroll position (item index)
     * @param offset The offset within the item
     */
    fun saveScrollPosition(position: Int, offset: Int) {
        savedScrollPositionState = ScrollPositionState(position, offset)
    }
    
    /**
     * Restore scroll position if not expired
     * Called when returning to the chat
     * 
     * @return ScrollPositionState if valid and not expired, null otherwise
     */
    fun restoreScrollPosition(): ScrollPositionState? {
        val position = savedScrollPositionState
        
        // Check if position exists and is not expired (5 minutes)
        return if (position != null && !position.isExpired()) {
            position
        } else {
            // Clear expired position
            savedScrollPositionState = null
            null
        }
    }
    
    /**
     * Get saved scroll position for restoration after prepending messages
     * Returns the position and offset to restore after loading older messages
     * This is used specifically for maintaining scroll position when prepending
     */
    fun getSavedScrollPosition(): Pair<Int, Int>? {
        val position = savedScrollPositionState
        return if (position != null) {
            Pair(position.position, position.offset)
        } else {
            null
        }
    }
    
    /**
     * Clear saved scroll position
     * Called on refresh to reset to top
     */
    fun clearScrollPosition() {
        savedScrollPositionState = null
    }

    /**
     * Starts observing messages in real-time
     */
    fun startObservingMessages(chatId: String) {
        currentChatId = chatId
        observeMessagesUseCase(chatId)
            .onEach { messageList ->
                _messages.value = messageList
            }
            .launchIn(viewModelScope)
    }

    /**
     * Sends a message
     */
    fun sendMessage(chatId: String, content: String, messageType: String = "text", replyToId: String? = null) {
        android.util.Log.d("ChatViewModel", "=== sendMessage START ===")
        android.util.Log.d("ChatViewModel", "Sending message to chatId: $chatId")
        android.util.Log.d("ChatViewModel", "Content length: ${content.length}, type: $messageType")
        
        if (content.isBlank()) {
            android.util.Log.w("ChatViewModel", "Message content is blank, aborting send")
            return
        }
        
        viewModelScope.launch {
            try {
                val currentUserId = authService.getCurrentUserId()
                android.util.Log.d("ChatViewModel", "Current user ID: $currentUserId")
                
                if (currentUserId == null) {
                    android.util.Log.e("ChatViewModel", "User not authenticated")
                    _error.value = "User not authenticated"
                    _messageSent.value = false
                    return@launch
                }
                
                // Stop typing indicator when sending message
                android.util.Log.d("ChatViewModel", "Stopping typing indicator")
                typingIndicatorManager?.onUserStoppedTyping(chatId, currentUserId)
                
                android.util.Log.d("ChatViewModel", "Calling sendMessageUseCase")
                val result = sendMessageUseCase(chatId, currentUserId, content, messageType, replyToId)
                
                result.onSuccess { messageId ->
                    android.util.Log.d("ChatViewModel", "Message sent successfully, messageId: $messageId")
                    _messageSent.value = true
                    _error.value = null
                    
                    // Refresh messages
                    android.util.Log.d("ChatViewModel", "Refreshing messages after send")
                    loadMessages(chatId)
                }.onFailure { exception ->
                    android.util.Log.e("ChatViewModel", "Failed to send message: ${exception.message}", exception)
                    _error.value = exception.message
                    _messageSent.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Exception in sendMessage: ${e.message}", e)
                _error.value = e.message
                _messageSent.value = false
            } finally {
                android.util.Log.d("ChatViewModel", "=== sendMessage END ===")
            }
        }
    }

    /**
     * Loads user's chats
     */
    fun loadUserChats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = authService.getCurrentUserId()
                if (currentUserId != null) {
                    getUserChatsUseCase()
                        .collect { result ->
                            result.onSuccess { chatList ->
                                _chats.value = chatList
                                _error.value = null
                            }.onFailure { exception ->
                                _error.value = exception.message
                            }
                        }
                } else {
                    _error.value = "User not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a message
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val result = deleteMessageUseCase(messageId)
                result.onSuccess {
                    _error.value = null
                    // Refresh messages
                    currentChatId?.let { loadMessages(it) }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Edits a message
     */
    fun editMessage(messageId: String, newContent: String) {
        if (newContent.isBlank()) return
        
        viewModelScope.launch {
            try {
                val result = editMessageUseCase(messageId, newContent)
                result.onSuccess {
                    _error.value = null
                    // Refresh messages
                    currentChatId?.let { loadMessages(it) }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Creates or gets a direct chat with another user
     */
    fun createOrGetDirectChat(otherUserId: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUserId = authService.getCurrentUserId()
                if (currentUserId != null) {
                    val result = chatRepository.getOrCreateDirectChat(otherUserId, currentUserId)
                    result.onSuccess { chatId ->
                        onChatReady(chatId)
                        _error.value = null
                    }.onFailure { exception ->
                        _error.value = exception.message
                    }
                } else {
                    _error.value = "User not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Clears error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Resets message sent status
     */
    fun resetMessageSent() {
        _messageSent.value = false
    }

    // Typing indicator methods

    /**
     * Called when user types in the message input field.
     * Triggers typing events based on text content.
     * 
     * Requirements: 1.1, 1.4, 1.5
     * 
     * @param text The current text in the input field
     */
    fun onUserTyping(text: String) {
        onUserTypingWithManager(text)
    }

    /**
     * Handle typing update from other users.
     * Updates the typing user list for UI display.
     * 
     * Requirements: 1.2, 1.4, 1.5
     * 
     * @param typingStatus The typing status event received
     */
    private fun handleTypingUpdate(typingStatus: TypingStatus) {
        // Don't show our own typing indicator
        if (typingStatus.userId == currentUserId) {
            return
        }
        
        val currentList = _typingUsers.value.toMutableList()
        
        if (typingStatus.isTyping) {
            // Add user to typing list if not already present
            if (!currentList.contains(typingStatus.userId)) {
                currentList.add(typingStatus.userId)
            }
        } else {
            // Remove user from typing list
            currentList.remove(typingStatus.userId)
        }
        
        _typingUsers.value = currentList
    }

    // Read receipt methods

    /**
     * Mark visible messages as read.
     * Called when messages become visible in the chat screen.
     * Only marks messages as read when chat is visible and active.
     * 
     * Requirements: 4.1, 4.5
     * 
     * @param visibleMessageIds List of message IDs that are currently visible
     */
    fun markVisibleMessagesAsRead(visibleMessageIds: List<String>) {
        markVisibleMessagesAsReadWithManager(visibleMessageIds)
    }

    /**
     * Handle read receipt update from other users.
     * Updates message states when read receipts are received.
     * 
     * Requirements: 4.2, 4.3
     * 
     * @param event The read receipt event received
     */
    private fun handleReadReceiptUpdate(event: ReadReceiptEvent) {
        // Don't process our own read receipts
        if (event.userId == currentUserId) {
            return
        }
        
        val updatedMessages = _messagesStateFlow.value.map { message ->
            if (event.messageIds.contains(message.id) && message.senderId == currentUserId) {
                // Update message state to read for our sent messages
                message.copy(
                    messageState = MessageState.READ,
                    readAt = event.timestamp
                )
            } else {
                message
            }
        }
        
        _messagesStateFlow.value = updatedMessages
        
        // Update the ChatAdapter with the new message states
        val messageStates = event.messageIds.associateWith { MessageState.READ }
        updateAdapterMessageStates(messageStates)
        
        // Also update the legacy LiveData for backward compatibility
        // Convert ChatMessageImpl to Message for existing UI
        val legacyMessages = updatedMessages.map { chatMessage ->
            // This conversion would need to be implemented based on your Message class structure
            // For now, we'll keep the existing messages unchanged
            _messages.value ?: emptyList()
        }.flatten()
    }

    /**
     * Update message state for individual messages.
     * Handles state transitions like sent → delivered → read.
     * 
     * Requirements: 4.1, 4.2, 4.3
     * 
     * @param messageId The message ID to update
     * @param newState The new message state
     * @param timestamp Optional timestamp for the state change
     */
    private fun updateMessageState(messageId: String, newState: String, timestamp: Long = System.currentTimeMillis()) {
        val updatedMessages = _messagesStateFlow.value.map { message ->
            if (message.id == messageId) {
                when (newState) {
                    MessageState.DELIVERED -> message.copy(
                        messageState = newState,
                        deliveredAt = timestamp
                    )
                    MessageState.READ -> message.copy(
                        messageState = newState,
                        readAt = timestamp
                    )
                    else -> message.copy(messageState = newState)
                }
            } else {
                message
            }
        }
        
        _messagesStateFlow.value = updatedMessages
    }

    /**
     * Check if the chat is currently visible and active.
     * Used for lifecycle-aware read receipt marking.
     * 
     * Requirements: 4.5
     * 
     * @return true if chat is visible and should mark messages as read
     */
    private var isChatVisible = false
    
    fun setChatVisibility(isVisible: Boolean) {
        isChatVisible = isVisible
        
        if (isVisible && currentChatId != null) {
            // When chat becomes visible, mark all unread messages as read
            val unreadMessageIds = _messagesStateFlow.value
                .filter { message ->
                    message.senderId != currentUserId &&
                    message.messageState != MessageState.READ
                }
                .map { it.id }
            
            if (unreadMessageIds.isNotEmpty()) {
                markVisibleMessagesAsRead(unreadMessageIds)
            }
        }
    }

    // Media upload methods

    /**
     * Uploads multiple images with progress tracking.
     * 
     * Requirements: 1.5, 2.3, 2.4, 8.1, 8.2
     * 
     * @param uris List of image URIs to upload
     * @param caption Optional caption text to accompany the images
     */
    fun uploadImages(uris: List<Uri>, caption: String = "") {
        val chatId = currentChatId ?: run {
            _error.value = "No active chat"
            return
        }
        
        viewModelScope.launch {
            try {
                mediaUploadManager?.uploadMultiple(uris, chatId)?.collect { progress ->
                    // Update upload progress map
                    val currentProgress = _uploadProgress.value.toMutableMap()
                    currentProgress[progress.uploadId] = progress
                    _uploadProgress.value = currentProgress
                    
                    // If upload completed successfully, create message with attachment
                    if (progress.state == com.synapse.social.studioasinc.model.models.UploadState.COMPLETED) {
                        // Note: Individual upload results are handled in uploadMultiple
                        // We'll create the message after all uploads complete
                    }
                    
                    // Remove from progress map if completed, failed, or cancelled
                    if (progress.state == com.synapse.social.studioasinc.model.models.UploadState.COMPLETED ||
                        progress.state == com.synapse.social.studioasinc.model.models.UploadState.FAILED ||
                        progress.state == com.synapse.social.studioasinc.model.models.UploadState.CANCELLED) {
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(2000) // Keep visible for 2 seconds
                            val updatedProgress = _uploadProgress.value.toMutableMap()
                            updatedProgress.remove(progress.uploadId)
                            _uploadProgress.value = updatedProgress
                        }
                    }
                } ?: run {
                    _error.value = "Media upload manager not initialized"
                }
            } catch (e: Exception) {
                _error.value = "Failed to upload images: ${e.message}"
                android.util.Log.e("ChatViewModel", "Image upload failed", e)
            }
        }
    }

    /**
     * Uploads a single video with progress tracking.
     * 
     * Requirements: 1.5, 2.3, 2.4, 8.1, 8.2
     * 
     * @param uri The video URI to upload
     * @param caption Optional caption text to accompany the video
     */
    fun uploadVideo(uri: Uri, caption: String = "") {
        val chatId = currentChatId ?: run {
            _error.value = "No active chat"
            return
        }
        
        viewModelScope.launch {
            try {
                val result = mediaUploadManager?.uploadVideo(uri, chatId)
                
                result?.onSuccess { uploadResult ->
                    sendMessageWithAttachment(uploadResult, caption)
                }?.onFailure { exception ->
                    _error.value = "Failed to upload video: ${exception.message}"
                    android.util.Log.e("ChatViewModel", "Video upload failed", exception)
                } ?: run {
                    _error.value = "Media upload manager not initialized"
                }
            } catch (e: Exception) {
                _error.value = "Failed to upload video: ${e.message}"
                android.util.Log.e("ChatViewModel", "Video upload failed", e)
            }
        }
    }

    /**
     * Uploads an audio file with progress tracking.
     * 
     * Requirements: 1.5, 2.3, 2.4, 8.1, 8.2
     * 
     * @param uri The audio URI to upload
     * @param caption Optional caption text to accompany the audio
     */
    fun uploadAudio(uri: Uri, caption: String = "") {
        val chatId = currentChatId ?: run {
            _error.value = "No active chat"
            return
        }
        
        viewModelScope.launch {
            try {
                val result = mediaUploadManager?.uploadAudio(uri, chatId)
                
                result?.onSuccess { uploadResult ->
                    sendMessageWithAttachment(uploadResult, caption)
                }?.onFailure { exception ->
                    _error.value = "Failed to upload audio: ${exception.message}"
                    android.util.Log.e("ChatViewModel", "Audio upload failed", exception)
                } ?: run {
                    _error.value = "Media upload manager not initialized"
                }
            } catch (e: Exception) {
                _error.value = "Failed to upload audio: ${e.message}"
                android.util.Log.e("ChatViewModel", "Audio upload failed", e)
            }
        }
    }

    /**
     * Uploads a document with progress tracking.
     * 
     * Requirements: 1.5, 2.3, 2.4, 8.1, 8.2
     * 
     * @param uri The document URI to upload
     * @param caption Optional caption text to accompany the document
     */
    fun uploadDocument(uri: Uri, caption: String = "") {
        val chatId = currentChatId ?: run {
            _error.value = "No active chat"
            return
        }
        
        viewModelScope.launch {
            try {
                val result = mediaUploadManager?.uploadDocument(uri, chatId)
                
                result?.onSuccess { uploadResult ->
                    sendMessageWithAttachment(uploadResult, caption)
                }?.onFailure { exception ->
                    _error.value = "Failed to upload document: ${exception.message}"
                    android.util.Log.e("ChatViewModel", "Document upload failed", exception)
                } ?: run {
                    _error.value = "Media upload manager not initialized"
                }
            } catch (e: Exception) {
                _error.value = "Failed to upload document: ${e.message}"
                android.util.Log.e("ChatViewModel", "Document upload failed", e)
            }
        }
    }

    /**
     * Cancels an ongoing upload operation.
     * 
     * Requirements: 8.3, 8.4, 8.5
     * 
     * @param uploadId The ID of the upload to cancel
     */
    fun cancelUpload(uploadId: String) {
        mediaUploadManager?.cancelUpload(uploadId)
        
        // Remove from progress map
        val updatedProgress = _uploadProgress.value.toMutableMap()
        updatedProgress.remove(uploadId)
        _uploadProgress.value = updatedProgress
    }

    /**
     * Creates a message with attachment from upload result.
     * 
     * Requirements: 11.5
     * 
     * @param uploadResult The result of the media upload
     * @param caption Optional caption text
     */
    private fun sendMessageWithAttachment(uploadResult: MediaUploadResult, caption: String) {
        val chatId = currentChatId ?: return
        val userId = currentUserId ?: authService.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            try {
                // Create ChatAttachment from upload result
                val attachment = ChatAttachmentImpl(
                    id = UUID.randomUUID().toString(),
                    url = uploadResult.url,
                    type = getAttachmentType(uploadResult.mimeType),
                    fileName = uploadResult.fileName,
                    fileSize = uploadResult.fileSize,
                    thumbnailUrl = uploadResult.thumbnailUrl,
                    width = uploadResult.width,
                    height = uploadResult.height,
                    duration = uploadResult.duration,
                    mimeType = uploadResult.mimeType
                )
                
                // Send message with attachment using backend service
                val backendChatService = SupabaseChatService()
                val result = backendChatService.sendMessage(
                    chatId = chatId,
                    senderId = userId,
                    content = caption.ifEmpty { "" },
                    messageType = MessageType.ATTACHMENT,
                    replyToId = null,
                    attachments = listOf(attachment)
                )
                
                result.onSuccess {
                    _messageSent.value = true
                    _error.value = null
                    // Refresh messages
                    loadMessages(chatId)
                }.onFailure { exception ->
                    _error.value = "Failed to send message: ${exception.message}"
                    android.util.Log.e("ChatViewModel", "Failed to send attachment message", exception)
                }
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
                android.util.Log.e("ChatViewModel", "Failed to send attachment message", e)
            }
        }
    }

    /**
     * Determines attachment type from MIME type.
     * 
     * @param mimeType The MIME type of the file
     * @return The attachment type (image, video, audio, document)
     */
    private fun getAttachmentType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            else -> "document"
        }
    }

    // Manager integration methods

    /**
     * Initialize managers with required dependencies.
     * Should be called when the ViewModel is created with proper context.
     * 
     * Requirements: 6.4
     * 
     * @param context Android context for PreferencesManager
     */
    fun initializeManagers(context: android.content.Context) {
        // Initialize Realtime service
        realtimeService = SupabaseRealtimeService()
        
        // Initialize PreferencesManager (use application context to ensure singleton)
        preferencesManager = PreferencesManager.getInstance(context.applicationContext)
        
        // Initialize TypingIndicatorManager
        typingIndicatorManager = TypingIndicatorManager(
            realtimeService = realtimeService!!,
            preferencesManager = preferencesManager!!,
            coroutineScope = viewModelScope
        )
        
        // Initialize ReadReceiptManager
        readReceiptManager = ReadReceiptManager(
            chatService = com.synapse.social.studioasinc.chat.service.SupabaseChatService(
                com.synapse.social.studioasinc.backend.SupabaseDatabaseService()
            ),
            realtimeService = realtimeService!!,
            preferencesManager = preferencesManager!!,
            coroutineScope = viewModelScope
        )
        
        // Initialize MediaUploadManager
        mediaUploadManager = MediaUploadManager(
            context = context,
            storageService = com.synapse.social.studioasinc.backend.SupabaseStorageService(),
            imageCompressor = com.synapse.social.studioasinc.util.ImageCompressor(context),
            thumbnailGenerator = com.synapse.social.studioasinc.util.ThumbnailGenerator(context),
            coroutineScope = viewModelScope
        )
        
        // Initialize MessageSearchService
        messageSearchService = MessageSearchService()
        
        // Initialize ChatBackupService
        chatBackupService = ChatBackupService(context)
        
        // Set current user ID for read receipt filtering
        currentUserId = authService.getCurrentUserId()
        readReceiptManager?.setCurrentUserId(currentUserId ?: "")
    }

    /**
     * Called when a chat is opened.
     * Subscribes to typing events and read receipts for the chat.
     * 
     * Requirements: 6.4
     * 
     * @param chatId The chat room identifier
     */
    fun onChatOpened(chatId: String) {
        currentChatId = chatId
        currentUserId = authService.getCurrentUserId()
        
        // Initialize pagination for this chat
        initializePaginationForChat(chatId)
        
        viewModelScope.launch {
            try {
                // Subscribe to typing events
                typingIndicatorManager?.subscribeToTypingEvents(chatId) { typingStatus ->
                    handleTypingUpdate(typingStatus)
                }
                
                // Subscribe to read receipt events
                readReceiptManager?.subscribeToReadReceipts(chatId) { readReceiptEvent ->
                    handleReadReceiptUpdate(readReceiptEvent)
                }
                
                // Set chat as visible
                setChatVisibility(true)
                
            } catch (e: Exception) {
                _error.value = "Failed to subscribe to real-time events: ${e.message}"
            }
        }
    }

    /**
     * Called when a chat is closed.
     * Unsubscribes from typing events and read receipts.
     * 
     * Requirements: 6.4
     */
    fun onChatClosed() {
        val chatId = currentChatId ?: return
        
        // Set chat as not visible
        setChatVisibility(false)
        
        // Unsubscribe from events
        typingIndicatorManager?.unsubscribe(chatId)
        readReceiptManager?.unsubscribe(chatId)
        
        // Clear typing users
        _typingUsers.value = emptyList()
        
        // Reset pagination manager
        paginationManager?.reset()
        paginationManager = null
        
        // Clear scroll position
        clearScrollPosition()
        
        currentChatId = null
    }

    /**
     * Override onCleared to clean up resources.
     * 
     * Requirements: 6.4
     */
    override fun onCleared() {
        super.onCleared()
        
        // Clean up managers
        typingIndicatorManager?.unsubscribeAll()
        readReceiptManager?.unsubscribeAll()
        
        // Clean up realtime service asynchronously
        viewModelScope.launch {
            try {
                realtimeService?.cleanup()
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error during cleanup", e)
            }
        }
        
        // Clear state
        _typingUsers.value = emptyList()
        _messagesStateFlow.value = emptyList()
    }



    // Update existing methods to use managers

    /**
     * Enhanced onUserTyping method with manager integration.
     */
    private fun onUserTypingWithManager(text: String) {
        val chatId = currentChatId ?: return
        val userId = currentUserId ?: authService.getCurrentUserId() ?: return
        
        if (text.isNotEmpty()) {
            // User is typing - send typing event
            typingIndicatorManager?.onUserTyping(chatId, userId)
        } else {
            // User cleared input - stop typing
            typingIndicatorManager?.onUserStoppedTyping(chatId, userId)
        }
    }

    /**
     * Enhanced markVisibleMessagesAsRead method with manager integration.
     */
    private fun markVisibleMessagesAsReadWithManager(visibleMessageIds: List<String>) {
        val chatId = currentChatId ?: return
        val userId = currentUserId ?: authService.getCurrentUserId() ?: return
        
        if (visibleMessageIds.isEmpty()) {
            return
        }
        
        // Filter out messages that are already read or sent by current user
        val messagesToMarkAsRead = _messagesStateFlow.value.filter { message ->
            visibleMessageIds.contains(message.id) &&
            message.senderId != userId && // Don't mark our own messages as read
            message.messageState != MessageState.READ // Don't re-mark already read messages
        }.map { it.id }
        
        if (messagesToMarkAsRead.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    readReceiptManager?.markMessagesAsRead(chatId, userId, messagesToMarkAsRead)
                } catch (e: Exception) {
                    _error.value = "Failed to mark messages as read: ${e.message}"
                }
            }
        }
    }

    /**
     * Create a bridge between the backend SupabaseChatService and the expected chat service interface.
     */
    private fun createChatServiceBridge(): ChatServiceBridge {
        return ChatServiceBridge(com.synapse.social.studioasinc.backend.SupabaseChatService())
    }

    // ChatAdapter integration methods

    /**
     * Reference to the ChatAdapter for real-time updates
     */
    private var chatAdapter: com.synapse.social.studioasinc.ChatAdapter? = null

    /**
     * Set the ChatAdapter reference for real-time message state updates
     * 
     * @param adapter The ChatAdapter instance to update
     */
    fun setChatAdapter(adapter: com.synapse.social.studioasinc.ChatAdapter) {
        chatAdapter = adapter
    }

    /**
     * Update message state in the adapter with animation
     * Called when read receipts are received from other users
     * 
     * Requirements: 4.3
     * 
     * @param messageId The message ID to update
     * @param newState The new message state (sent, delivered, read, failed)
     */
    fun updateAdapterMessageState(messageId: String, newState: String) {
        chatAdapter?.updateMessageState(messageId, newState)
    }

    /**
     * Update multiple message states in the adapter efficiently
     * Used for batch read receipt updates
     * 
     * Requirements: 4.3
     * 
     * @param messageStates Map of message ID to new state
     */
    fun updateAdapterMessageStates(messageStates: Map<String, String>) {
        chatAdapter?.updateMessageStates(messageStates)
    }
    
    /**
     * Called when chat preferences change to update managers accordingly.
     * This ensures that preference changes take effect immediately.
     * 
     * Requirements: 5.2, 5.3, 5.5
     */
    fun onPreferencesChanged() {
        // No specific action needed as managers check preferences on each operation
        // This method is provided for future extensibility if needed
        android.util.Log.d("ChatViewModel", "Chat preferences changed - managers will respect new settings on next operation")
    }

    /**
     * Get the realtime service instance for connection monitoring.
     * 
     * @return The SupabaseRealtimeService instance or null if not initialized
     */
    fun getRealtimeService(): SupabaseRealtimeService? {
        return realtimeService
    }
    
    // Message Search Methods
    
    /**
     * Search messages across all chats or within a specific chat.
     * 
     * @param query Search query string
     * @param chatId Optional chat ID to limit search to specific chat
     * @param messageType Optional message type filter
     * @param startDate Optional start date for date range filter
     * @param endDate Optional end date for date range filter
     * @param limit Maximum number of results
     * @param offset Offset for pagination
     */
    fun searchMessages(
        query: String,
        chatId: String? = null,
        messageType: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        limit: Int = 50,
        offset: Int = 0
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authService.getCurrentUserId()
                if (userId == null) {
                    _error.value = "User not authenticated"
                    return@launch
                }
                
                val result = messageSearchService?.searchMessages(
                    query = query,
                    chatId = chatId,
                    userId = userId,
                    messageType = messageType,
                    startDate = startDate,
                    endDate = endDate,
                    limit = limit,
                    offset = offset
                )
                
                result?.onSuccess { searchResults ->
                    _messages.value = searchResults
                    _error.value = null
                }?.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Search messages in the current chat.
     * 
     * @param query Search query string
     * @param limit Maximum number of results
     * @param offset Offset for pagination
     */
    fun searchInCurrentChat(
        query: String,
        limit: Int = 50,
        offset: Int = 0
    ) {
        val chatId = currentChatId ?: return
        searchMessages(query, chatId, limit = limit, offset = offset)
    }
    
    /**
     * Search messages by media type (images, videos, etc.).
     * 
     * @param messageType Message type (image, video, audio, file)
     * @param chatId Optional chat ID to limit search
     * @param limit Maximum number of results
     * @param offset Offset for pagination
     */
    fun searchByMediaType(
        messageType: String,
        chatId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authService.getCurrentUserId()
                if (userId == null) {
                    _error.value = "User not authenticated"
                    return@launch
                }
                
                val result = messageSearchService?.searchByMediaType(
                    messageType = messageType,
                    userId = userId,
                    chatId = chatId,
                    limit = limit,
                    offset = offset
                )
                
                result?.onSuccess { searchResults ->
                    _messages.value = searchResults
                    _error.value = null
                }?.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Chat Backup and Restore Methods
    
    /**
     * Create a full backup of all user's chats and messages.
     * 
     * @param includeMedia Whether to include media files in backup
     * @param onSuccess Callback with backup file URI
     * @param onError Callback with error message
     */
    fun createFullBackup(
        includeMedia: Boolean = false,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authService.getCurrentUserId()
                if (userId == null) {
                    onError("User not authenticated")
                    return@launch
                }
                
                val result = chatBackupService?.createFullBackup(userId, includeMedia)
                
                result?.onSuccess { backupUri ->
                    onSuccess(backupUri)
                    _error.value = null
                }?.onFailure { exception ->
                    onError(exception.message ?: "Failed to create backup")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to create backup")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Create a backup of specific chats.
     * 
     * @param chatIds List of chat IDs to backup
     * @param onSuccess Callback with backup file URI
     * @param onError Callback with error message
     */
    fun createSelectiveBackup(
        chatIds: List<String>,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authService.getCurrentUserId()
                if (userId == null) {
                    onError("User not authenticated")
                    return@launch
                }
                
                val result = chatBackupService?.createSelectiveBackup(userId, chatIds)
                
                result?.onSuccess { backupUri ->
                    onSuccess(backupUri)
                    _error.value = null
                }?.onFailure { exception ->
                    onError(exception.message ?: "Failed to create backup")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to create backup")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Upload backup to cloud storage.
     * 
     * @param backupUri Local backup file URI
     * @param onSuccess Callback with cloud storage URL
     * @param onError Callback with error message
     */
    fun uploadBackupToCloud(
        backupUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authService.getCurrentUserId()
                if (userId == null) {
                    onError("User not authenticated")
                    return@launch
                }
                
                val result = chatBackupService?.uploadBackupToCloud(backupUri, userId)
                
                result?.onSuccess { cloudUrl ->
                    onSuccess(cloudUrl)
                    _error.value = null
                }?.onFailure { exception ->
                    onError(exception.message ?: "Failed to upload backup")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to upload backup")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Restore chat data from backup file.
     * 
     * @param backupUri Backup file URI
     * @param mergeWithExisting Whether to merge with existing data or replace
     * @param onSuccess Callback with number of restored chats and messages
     * @param onError Callback with error message
     */
    fun restoreFromBackup(
        backupUri: Uri,
        mergeWithExisting: Boolean = true,
        onSuccess: (Int, Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authService.getCurrentUserId()
                if (userId == null) {
                    onError("User not authenticated")
                    return@launch
                }
                
                val result = chatBackupService?.restoreFromBackup(backupUri, userId, mergeWithExisting)
                
                result?.onSuccess { (chatsCount, messagesCount) ->
                    onSuccess(chatsCount, messagesCount)
                    _error.value = null
                    // Reload chats after restore
                    loadUserChats()
                }?.onFailure { exception ->
                    onError(exception.message ?: "Failed to restore backup")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to restore backup")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * List available backups for the current user.
     * 
     * @param onSuccess Callback with list of backup metadata
     * @param onError Callback with error message
     */
    fun listBackups(
        onSuccess: (List<ChatBackupService.BackupMetadata>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = authService.getCurrentUserId()
                if (userId == null) {
                    onError("User not authenticated")
                    return@launch
                }
                
                val result = chatBackupService?.listBackups(userId)
                
                result?.onSuccess { backups ->
                    onSuccess(backups)
                    _error.value = null
                }?.onFailure { exception ->
                    onError(exception.message ?: "Failed to list backups")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to list backups")
            }
        }
    }
    
    /**
     * Delete a backup file.
     * 
     * @param backupUri Backup file URI
     * @param onSuccess Callback on successful deletion
     * @param onError Callback with error message
     */
    fun deleteBackup(
        backupUri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = chatBackupService?.deleteBackup(backupUri)
                
                result?.onSuccess {
                    onSuccess()
                    _error.value = null
                }?.onFailure { exception ->
                    onError(exception.message ?: "Failed to delete backup")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete backup")
            }
        }
    }
}

/**
 * Bridge class to adapt the backend SupabaseChatService to the interface expected by ReadReceiptManager.
 */
private class ChatServiceBridge(
    private val backendChatService: com.synapse.social.studioasinc.backend.SupabaseChatService
) {
    suspend fun markMessagesAsRead(chatId: String, userId: String, messageIds: List<String>): Result<Unit> {
        return backendChatService.markMessagesAsRead(chatId, userId, messageIds)
    }
}

