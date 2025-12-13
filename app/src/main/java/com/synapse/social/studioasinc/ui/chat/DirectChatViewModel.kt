package com.synapse.social.studioasinc.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.chat.service.SupabaseRealtimeService
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.domain.usecase.ObserveMessagesUseCase
import com.synapse.social.studioasinc.model.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import io.github.jan.supabase.realtime.broadcastFlow

/**
 * ViewModel for DirectChatScreen
 * Adapter for existing ChatRepository to Compose UI State
 */
class DirectChatViewModel(application: Application) : AndroidViewModel(application) {

    // Dependencies
    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    private val chatRepository = ChatRepository(chatDao)
    private val authService = SupabaseAuthenticationService(application)
    private val observeMessagesUseCase = ObserveMessagesUseCase(chatDao)
    
    // UI State
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // Messages list (Source of Truth: Realtime/DB)
    private val _dbMessages = MutableStateFlow<List<MessageUiModel>>(emptyList())

    // Optimistic Messages (Temporary local state)
    private val _optimisticMessages = MutableStateFlow<List<MessageUiModel>>(emptyList())

    // Combined Messages Flow
    // Combined Messages Flow
    val messages: StateFlow<List<MessageUiModel>> = combine(_dbMessages, _optimisticMessages) { db, optimistic ->
        // Merge DB messages with optimistic ones.
        // We prioritize DB messages. Optimistic messages are only shown if they haven't been "seen" in the DB yet.

        val merged = ArrayList<MessageUiModel>(db.size + optimistic.size)
        merged.addAll(db)

        val dbIds = db.map { it.id }.toSet()

        optimistic.forEach { optMsg ->
             // 1. Check if the optimistic message ID (tempId) is already in DB (unlikely, but good for safety)
             if (dbIds.contains(optMsg.id)) return@forEach

             // 2. Check for "content sync": mismatched IDs but same content, sender, and recent time
             // This happens when Realtime returns the NEW message with a server-generated ID, but our local one has temp_id.
             val isSynced = db.any { dbMsg ->
                 dbMsg.isFromCurrentUser &&
                 dbMsg.content == optMsg.content &&
                 kotlin.math.abs(dbMsg.timestamp - optMsg.timestamp) < 5000 // 5s window
             }

             if (!isSynced) {
                 merged.add(optMsg)
             }
        }
        // Ensure sorted by timestamp descending (newest first) ?? No, UI expects newest at bottom usually, but LazyColumn is reversed. 
        // The original code passed `items(messages.reversed())` to a reversed LazyColumn.
        // That means `messages` should be sorted Oldest -> Newest (ascending timestamp).
        merged.sortedBy { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private var currentChatId: String? = null
    private var currentUserId: String? = null
    private var realtimeJob: Job? = null
    
    init {
        loadCurrentUser()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            currentUserId = authService.getCurrentUserId()
        }
    }

    fun loadChat(chatId: String) {
        if (currentChatId == chatId) return
        currentChatId = chatId
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Initial fetch (restores from DB / Network)
                val result = chatRepository.getMessagesPage(chatId, null, 50)
                
                result.onSuccess { domainMessages ->
                    val uiMessages = domainMessages.map { it.toUiModel(currentUserId) }
                    _dbMessages.value = uiMessages
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    
                    // Start realtime observation
                    observeRealtimeMessages(chatId)
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
                
                // Load other user info
                 chatRepository.getChatParticipants(chatId).onSuccess { userIds ->
                     val otherId = userIds.firstOrNull { it != currentUserId }
                     if (otherId != null) {
                         // Fetch actual user profile from UserProfileManager
                         val userProfile = com.synapse.social.studioasinc.UserProfileManager.getUserProfile(otherId)
                         _uiState.update {
                             it.copy(otherUser = ChatUserInfo(
                                 id = otherId,
                                 username = userProfile?.username ?: "User",
                                 displayName = userProfile?.displayName,
                                 avatarUrl = userProfile?.profileImageUrl
                             ))
                         }
                     }
                 }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    // Realtime Service
    private val realtimeService = SupabaseRealtimeService()

    /**
     * Start observing everything: Messages and Typing
     */
    private fun observeRealtimeMessages(chatId: String) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            // 1. Subscribe to channel (used for both)
            val channel = realtimeService.subscribeToChat(chatId)
            
            // 2. Observe DB/Messages Flow from UseCase
            launch {
                observeMessagesUseCase(chatId)
                    .onEach { messageList ->
                        val uiMessages = messageList.map { it.toUiModel(currentUserId) }
                        _dbMessages.value = uiMessages
                    }
                    .catch { e ->
                        _uiState.update { it.copy(error = "Realtime error: ${e.message}") }
                    }
                    .collect()
            }
            
            // 3. Observe Typing Events
            launch {
                 channel.broadcastFlow<JsonObject>(event = "typing")
                     .collect { event ->
                         val payload = event
                         val userId = payload["user_id"]?.toString()?.replace("\"", "") ?: return@collect
                         val isTyping = payload["is_typing"]?.toString()?.toBoolean() ?: false
                         
                         // Ignore self
                         if (userId == currentUserId) return@collect
                         
                         _uiState.update { state ->
                             val currentTyping = state.typingUsers.toMutableSet()
                             if (isTyping) {
                                 // Add user
                                 currentTyping.add(state.otherUser?.username ?: "User")
                             } else {
                                 currentTyping.remove(state.otherUser?.username ?: "User")
                             }
                             state.copy(typingUsers = currentTyping.toList())
                         }
                         
                         // Auto-remove typing status after 3 seconds (debounce safety)
                         if (isTyping) {
                             launch {
                                 kotlinx.coroutines.delay(3000)
                                 _uiState.update { state ->
                                     val currentTyping = state.typingUsers.toMutableSet()
                                     currentTyping.remove(state.otherUser?.username ?: "User")
                                     state.copy(typingUsers = currentTyping.toList())
                                 }
                             }
                         }
                     }
            }
        }
    }
    
    // Effects Channel
    private val _effects = kotlinx.coroutines.channels.Channel<ChatEffect>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage(intent.content)
            is ChatIntent.UpdateInputText -> {
                _uiState.update { it.copy(inputText = intent.text) }
            }
            is ChatIntent.SetReplyTo -> {
                _uiState.update { it.copy(replyTo = intent.message) }
            }
            is ChatIntent.ClearReply -> {
                _uiState.update { it.copy(replyTo = null) }
            }
            is ChatIntent.DeleteMessage -> deleteMessage(intent.messageId)
            is ChatIntent.EditMessage -> editMessage(intent.messageId, intent.newContent)
            is ChatIntent.CopyToClipboard -> {
                viewModelScope.launch {
                    _effects.send(ChatEffect.CopyToClipboard(intent.content))
                    _effects.send(ChatEffect.ShowSnackbar("Copied to clipboard"))
                }
            }
            else -> { /* TODO: Implement other intents */ }
        }
    }
    
    private fun sendMessage(content: String, type: String = "text") {
        val chatId = currentChatId ?: return
        val senderId = currentUserId ?: return
        
        if (content.isBlank()) return
        
        val replyToId = _uiState.value.replyTo?.id
        val tempId = "temp_${System.currentTimeMillis()}"

        viewModelScope.launch {
            // Optimistic Update: Add to optimistic state
            val optimisticMessage = MessageUiModel(
                id = tempId,
                content = content,
                messageType = mapMessageType(type),
                senderId = senderId,
                senderName = "Me",
                senderAvatarUrl = null,
                timestamp = System.currentTimeMillis(),
                formattedTime = formatTime(System.currentTimeMillis()),
                isFromCurrentUser = true,
                deliveryStatus = DeliveryStatus.Sending,
                position = MessagePosition.Single,
                replyTo = _uiState.value.replyTo?.let {
                    ReplyPreviewData(
                        messageId = it.id,
                        senderName = it.senderName ?: "User",
                        content = it.content
                    )
                }
            )
            
            _optimisticMessages.update { it + optimisticMessage }
            _uiState.update { it.copy(inputText = "", replyTo = null) }
            
            val result = chatRepository.sendMessage(
                chatId = chatId,
                senderId = senderId,
                content = content,
                messageType = type,
                replyToId = replyToId
            )
            
            result.onSuccess { realMessageId ->
                // Success: Update the optimistic message with the Real ID.
                _optimisticMessages.update { list -> 
                    list.map { 
                        if (it.id == tempId) it.copy(id = realMessageId, deliveryStatus = DeliveryStatus.Sent) else it 
                    } 
                }
            }.onFailure { error ->
                // Mark optimistic message as failed
                _optimisticMessages.update { list ->
                    list.map {
                        if (it.id == tempId) it.copy(deliveryStatus = DeliveryStatus.Failed) else it
                    }
                }
                _uiState.update { it.copy(error = "Failed to send: ${error.message}") }
            }
        }
    }

    private fun deleteMessage(messageId: String) {
        viewModelScope.launch {
             chatRepository.deleteMessage(messageId).onSuccess {
                 // Optimistic update handled by Realtime if available, or force refresh
                 // For now, let's wait for realtime. Or we can manually remove from _dbMessages if needed.
                 _effects.send(ChatEffect.ShowSnackbar("Message deleted"))
             }.onFailure { e ->
                 _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
             }
        }
    }

    private fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            chatRepository.editMessage(messageId, newContent).onSuccess {
                _effects.send(ChatEffect.ShowSnackbar("Message edited"))
            }.onFailure { e ->
                 _uiState.update { it.copy(error = "Failed to edit: ${e.message}") }
            }
        }
    }

    // Mapper function
    private fun Message.toUiModel(currentUserId: String?): MessageUiModel {
        val isMe = this.senderId == currentUserId

        // Map Reply Preview if exists
        val replyPreview = if (this.replyToId != null) {
            // In a real app, we'd need to fetch the original message or look it up in cache
            // For now, simplistic approximation or null
             null
        } else null

        return MessageUiModel(
            id = this.id,
            content = this.content,
            messageType = mapMessageType(this.messageType),
            senderId = this.senderId,
            senderName = this.senderName ?: "User",
            senderAvatarUrl = this.senderAvatarUrl,
            timestamp = this.createdAt,
            formattedTime = formatTime(this.createdAt),
            isFromCurrentUser = isMe,
            deliveryStatus = DeliveryStatus.Read, // Placeholder
            position = MessagePosition.Single,
            replyTo = replyPreview,
            attachments = this.attachments?.map {
                AttachmentUiModel(
                    id = it.id,
                    url = it.url,
                    type = mapAttachmentType(it.type),
                    thumbnailUrl = it.thumbnailUrl,
                    fileName = it.fileName,
                    fileSize = it.fileSize
                )
            }
        )
    }

    private fun mapMessageType(type: String): MessageType {
        return when (type) {
            "text" -> MessageType.Text
            "image" -> MessageType.Image
            "video" -> MessageType.Video
            "audio" -> MessageType.Voice
            else -> MessageType.Text
        }
    }

    private fun mapAttachmentType(type: String): AttachmentType {
        return when (type) {
            "image" -> AttachmentType.Image
            "video" -> AttachmentType.Video
            "audio" -> AttachmentType.Audio
            "document" -> AttachmentType.Document
            else -> AttachmentType.Unknown
        }
    }

    private fun formatTime(timestamp: Long): String {
        // Simple formatter
        val date = java.util.Date(timestamp)
        return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(date)
    }

    // ============================================================================================
    // TODO: FUTURE FEATURES IMPLEMENTATION
    // ============================================================================================

    /**
     * Block User
     */
    fun blockUser(userId: String) {
        val currentUserId = currentUserId ?: return
        viewModelScope.launch {
            _effects.send(ChatEffect.ShowSnackbar("Blocking user..."))
            val result = chatRepository.blockUser(blockerId = currentUserId, blockedId = userId)
            result.onSuccess {
                _effects.send(ChatEffect.ShowSnackbar("User blocked"))
                // Optionally navigate back or refresh
            }.onFailure {
                _effects.send(ChatEffect.ShowSnackbar("Failed to block: ${it.message}"))
            }
        }
    }

    /**
     * Report User
     */
    /**
     * Report User
     */
    fun reportUser(userId: String) {
        val currentUserId = currentUserId ?: return
        viewModelScope.launch {
             _effects.send(ChatEffect.ShowSnackbar("Reporting user..."))
             // Hardcoded reason for now, could be passed from UI dialog
             val result = chatRepository.reportUser(reporterId = currentUserId, reportedId = userId, reason = "Spam/Abuse")
             result.onSuccess {
                 _effects.send(ChatEffect.ShowSnackbar("User reported"))
             }.onFailure {
                 _effects.send(ChatEffect.ShowSnackbar("Failed to report: ${it.message}"))
             }
        }
    }

    /**
     * Delete Chat
     */
    fun deleteChat() {
        val chatId = currentChatId ?: return
        val currentUserId = currentUserId ?: return
        
        viewModelScope.launch {
            _effects.send(ChatEffect.ShowSnackbar("Deleting chat..."))
            val result = chatRepository.deleteChat(chatId, currentUserId)
            result.onSuccess {
                _effects.send(ChatEffect.ShowSnackbar("Chat deleted"))
                _effects.send(ChatEffect.NavigateBack) 
            }.onFailure {
                _effects.send(ChatEffect.ShowSnackbar("Failed to delete chat: ${it.message}"))
            }
        }
    }

    /**
     * TODO: Implement Attachment Upload
     * Backend Context:
     * 1. Supabase Storage: Use bucket 'chat-attachments'.
     * 2. Path structure: '{chat_id}/{timestamp}_{filename}'.
     * 3. Process:
     *    a. uploadFile(bucket, path, fileBytes)
     *    b. getPublicUrl(bucket, path)
     *    c. sendMessage(type="image/video", content=publicUrl)
     * 4. RLS: Storage bucket needs 'authenticated' role permissions.
     */
    private val storageService = com.synapse.social.studioasinc.backend.SupabaseStorageService()

    /**
     * Upload and Send Attachment
     */
    fun sendAttachment(uri: Any, type: String) {
         val contentUri = uri as? android.net.Uri ?: return
         val chatId = currentChatId ?: return
         
         viewModelScope.launch {
             try {
                 _effects.send(ChatEffect.ShowSnackbar("Uploading..."))
                 
                 val bytes = getApplication<Application>().contentResolver.openInputStream(contentUri)?.use { 
                     it.readBytes() 
                 } ?: throw Exception("Failed to read file")
                 
                 // Generate path: chat_id/timestamp_filename
                 val filename = "upload_${System.currentTimeMillis()}" // Simple filename for now
                 val path = storageService.generateStoragePath(chatId, filename)
                 
                 val result = storageService.uploadFileBytes(bytes, path)
                 
                 result.onSuccess { publicUrl ->
                     sendMessage(content = publicUrl) // Send as text url for now? Or update sendMessage to support types?
                     // Verify: sendMessage implementation hardcodes "text" currently.
                     // I need to update sendMessage to accept messageType.
                     // But for now, let's just send the URL. 
                     // Wait, the UI needs to know it's an image.
                     // I should call a modified sendMessage or update sendMessage signature.
                     
                     // Let's check sendMessage signature below. 
                     // It calls repository.sendMessage(..., messageType = "text", ...)
                     // I need to overload sendMessage or change it.
                     
                     // Direct call to repository for now to bypass the hardcoded "text" in the private helper
                     // Actually, better to refactor the private helper `sendMessage` to take `type`.
                     
                     sendMessage(content = publicUrl, type = "image") 
                 }.onFailure {
                     _uiState.update { state -> state.copy(error = "Upload failed: ${it.message}") }
                 }
                 
             } catch (e: Exception) {
                 _uiState.update { state -> state.copy(error = "Upload error: ${e.message}") }
             }
         }
    }

    /**
     * Set Typing Status
     * Backend: Broadcasts 'typing' event to 'chat:{id}' channel
     */
    fun setTypingStatus(isTyping: Boolean) {
        val chatId = currentChatId ?: return
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            realtimeService.broadcastTyping(chatId, userId, isTyping)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeService.cleanup()
        }
    }
}
