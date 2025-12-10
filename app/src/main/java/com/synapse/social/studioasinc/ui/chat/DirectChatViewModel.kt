package com.synapse.social.studioasinc.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.domain.usecase.ObserveMessagesUseCase
import com.synapse.social.studioasinc.model.Message
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for DirectChatComposeActivity
 * Adapter for existing ChatRepository to Compose UI State
 */
class DirectChatViewModel(application: Application) : AndroidViewModel(application) {

    // Dependencies
    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    private val chatRepository = ChatRepository(chatDao)
    private val authService = SupabaseAuthenticationService()
    private val observeMessagesUseCase = ObserveMessagesUseCase(chatDao)
    
    // UI State
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // Messages list (separate flow for efficiency)
    private val _messages = MutableStateFlow<List<MessageUiModel>>(emptyList())
    val messages: StateFlow<List<MessageUiModel>> = _messages.asStateFlow()
    
    private var currentChatId: String? = null
    private var currentUserId: String? = null
    
    init {
        loadCurrentUser()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            currentUserId = authService.getCurrentUserId()
        }
    }

    fun loadChat(chatId: String) {
        currentChatId = chatId
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Initial fetch (restores from DB / Network)
                val result = chatRepository.getMessagesPage(chatId, null, 50)
                
                result.onSuccess { domainMessages ->
                    val uiMessages = domainMessages.map { it.toUiModel(currentUserId) }
                    _messages.value = uiMessages
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    
                    // Start realtime observation
                    observeRealtimeMessages(chatId)
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
                
                // TODO: Load other user info here using ChatRepository/ProfileRepository
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    private fun observeRealtimeMessages(chatId: String) {
        observeMessagesUseCase(chatId)
            .onEach { messageList ->
                // Map new list to UI models
                val uiMessages = messageList.map { it.toUiModel(currentUserId) }
                _messages.value = uiMessages
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
            is ChatIntent.AddAttachment -> {
                _uiState.update { it.copy(attachments = it.attachments + intent.attachment) }
            }
            is ChatIntent.RemoveAttachment -> {
                _uiState.update { it.copy(attachments = it.attachments.filter { item -> item.id != intent.attachmentId }) }
            }
            is ChatIntent.SetReplyTo -> {
                _uiState.update { it.copy(replyTo = intent.message) }
            }
            is ChatIntent.ClearReply -> {
                _uiState.update { it.copy(replyTo = null) }
            }
            // Handle other intents...
            else -> { /* TODO */ }
        }
    }
    
    private fun sendMessage(content: String) {
        val chatId = currentChatId ?: return
        val senderId = currentUserId ?: return
        
        if (content.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(inputText = "") } // Clear input immediately
            
            // Optimistic update handled by Realtime observation usually, 
            // but we can add temp message if we want immediate feedback before DB write.
            // For now, reliance on ObserveMessagesUseCase (which observers DB) is standard if DB write is fast.
            
            val result = chatRepository.sendMessage(chatId, senderId, content)
            
            result.onFailure {
               // Show error
               _uiState.update { it.copy(error = "Failed to send") }
            }
        }
    }
    
    // Mapper function
    private fun Message.toUiModel(currentUserId: String?): MessageUiModel {
        val isMe = this.senderId == currentUserId
        return MessageUiModel(
            id = this.id.toString(),
            content = this.content,
            messageType = mapMessageType(this.messageType),
            senderId = this.senderId,
            senderName = this.senderName,
            senderAvatarUrl = this.senderAvatarUrl,
            timestamp = this.createdAt,
            formattedTime = "12:00 PM", // Replace with real formatter
            isFromCurrentUser = isMe,
            deliveryStatus = DeliveryStatus.Read,
            position = MessagePosition.Single 
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
}
