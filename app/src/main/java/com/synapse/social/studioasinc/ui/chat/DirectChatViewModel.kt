package com.synapse.social.studioasinc.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.domain.usecase.ObserveMessagesUseCase
import com.synapse.social.studioasinc.model.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    val messages: StateFlow<List<MessageUiModel>> = combine(_dbMessages, _optimisticMessages) { db, optimistic ->
        // Merge DB messages with optimistic ones.
        // If an optimistic message matches a DB message by content and recent timestamp, we assume it's synced.

        val merged = db.toMutableList()
        val now = System.currentTimeMillis()

        optimistic.forEach { optMsg ->
             // Heuristic: If DB has a message with same content from "Me" within last 10 seconds, ignore optimistic
             val isSynced = db.any { dbMsg ->
                 dbMsg.isFromCurrentUser &&
                 dbMsg.content == optMsg.content &&
                 kotlin.math.abs(dbMsg.timestamp - optMsg.timestamp) < 10000 // 10s window
             }

             if (!isSynced) {
                 merged.add(optMsg)
             }
        }
        // Ensure sorted by timestamp
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
                         // TODO: Fetch profile
                         _uiState.update {
                             it.copy(otherUser = ChatUserInfo(
                                 id = otherId,
                                 username = "User", // Placeholder
                                 displayName = null,
                                 avatarUrl = null
                             ))
                         }
                     }
                 }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    private fun observeRealtimeMessages(chatId: String) {
        realtimeJob?.cancel()
        realtimeJob = observeMessagesUseCase(chatId)
            .onEach { messageList ->
                // Map new list to UI models
                val uiMessages = messageList.map { it.toUiModel(currentUserId) }
                _dbMessages.value = uiMessages
            }
            .catch { e ->
                _uiState.update { it.copy(error = "Realtime error: ${e.message}") }
            }
            .launchIn(viewModelScope)
    }
    
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
            // Handle other intents as needed
            else -> { /* TODO: Implement other intents */ }
        }
    }
    
    private fun sendMessage(content: String) {
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
                messageType = MessageType.Text,
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
                messageType = "text",
                replyToId = replyToId
            )
            
            result.onSuccess {
                // Success: Remove optimistic message strictly by tempId to avoid leaks
                // We rely on the combine logic to prevent display duplication if it's already in DB
                _optimisticMessages.update { list -> list.filter { it.id != tempId } }
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
    
    // Mapper function
    private fun Message.toUiModel(currentUserId: String?): MessageUiModel {
        val isMe = this.senderId == currentUserId

        // Map Reply Preview if exists
        val replyPreview = if (this.replyToId != null) {
            // Placeholder
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
}
