package com.synapse.social.studioasinc.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.chat.service.MessageSelectionService
import com.synapse.social.studioasinc.chat.service.SyraAiChatService
import com.synapse.social.studioasinc.moderation.AiModerationService
import com.synapse.social.studioasinc.moderation.ContentType
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Refactored DirectChatViewModel using new BaaS AI services
 */
class DirectChatViewModelRefactored(application: Application) : AndroidViewModel(application) {

    // Services
    private val messageSelectionService = MessageSelectionService()
    private val syraAiService = SyraAiChatService()
    private val moderationService = AiModerationService()
    private val authService = SupabaseAuthenticationService(application)
    
    // UI State
    private val _uiState = MutableStateFlow(ChatUiStateRefactored())
    val uiState: StateFlow<ChatUiStateRefactored> = _uiState.asStateFlow()
    
    // Combined state from services
    val isSelectionMode = messageSelectionService.isSelectionMode
    val selectedMessageIds = messageSelectionService.selectedMessageIds
    val smartReplies = syraAiService.smartReplies
    val isLoadingReplies = syraAiService.isLoadingReplies
    val isModerating = moderationService.isAnalyzing
    
    private var currentChatId: String = ""
    private var currentUserId: String = ""
    
    fun initialize(chatId: String, otherUserId: String) {
        currentChatId = chatId
        currentUserId = authService.getCurrentUserId() ?: ""
        
        // Initialize existing chat functionality
        // ... existing initialization code ...
    }
    
    fun handleIntent(intent: ChatIntentRefactored) {
        when (intent) {
            // Message Selection Intents
            is ChatIntentRefactored.StartMessageSelection -> {
                viewModelScope.launch {
                    messageSelectionService.startSelectionSession(currentUserId, currentChatId)
                }
            }
            
            is ChatIntentRefactored.ToggleMessageSelection -> {
                viewModelScope.launch {
                    messageSelectionService.toggleMessageSelection(intent.messageId)
                }
            }
            
            is ChatIntentRefactored.ExitSelectionMode -> {
                viewModelScope.launch {
                    messageSelectionService.endSelectionSession()
                }
            }
            
            is ChatIntentRefactored.DeleteSelectedMessages -> {
                viewModelScope.launch {
                    messageSelectionService.executeAction(
                        action = com.synapse.social.studioasinc.chat.service.MessageAction.DELETE,
                        userId = currentUserId
                    )
                }
            }
            
            is ChatIntentRefactored.CopySelectedMessages -> {
                viewModelScope.launch {
                    messageSelectionService.executeAction(
                        action = com.synapse.social.studioasinc.chat.service.MessageAction.COPY,
                        userId = currentUserId
                    )
                }
            }
            
            is ChatIntentRefactored.ForwardSelectedMessages -> {
                viewModelScope.launch {
                    messageSelectionService.executeAction(
                        action = com.synapse.social.studioasinc.chat.service.MessageAction.FORWARD,
                        userId = currentUserId,
                        additionalData = mapOf("target_chats" to intent.targetChatIds)
                    )
                }
            }
            
            // Syra AI Intents
            is ChatIntentRefactored.SendMessageToSyra -> {
                viewModelScope.launch {
                    val response = syraAiService.sendToSyra(
                        userId = currentUserId,
                        message = intent.message,
                        chatId = currentChatId
                    )
                    
                    response?.let {
                        // Handle Syra response - add to chat
                        _uiState.update { state ->
                            state.copy(lastSyraResponse = it.response)
                        }
                    }
                }
            }
            
            is ChatIntentRefactored.GenerateSmartReplies -> {
                viewModelScope.launch {
                    syraAiService.generateSmartReplies(
                        userId = currentUserId,
                        message = intent.message,
                        chatContext = intent.context
                    )
                }
            }
            
            is ChatIntentRefactored.UseSmartReply -> {
                syraAiService.clearSmartReplies()
                _uiState.update { it.copy(inputText = intent.reply) }
            }
            
            // Content Moderation Intents
            is ChatIntentRefactored.AnalyzeMessage -> {
                viewModelScope.launch {
                    val result = moderationService.analyzeContent(
                        content = intent.content,
                        contentId = intent.messageId,
                        contentType = ContentType.MESSAGE,
                        userId = currentUserId
                    )
                    
                    result?.let {
                        _uiState.update { state ->
                            state.copy(
                                moderationResult = it,
                                showModerationWarning = it.flagged
                            )
                        }
                    }
                }
            }
            
            is ChatIntentRefactored.GetRealtimeModerationFeedback -> {
                viewModelScope.launch {
                    val feedback = moderationService.getRealtimeFeedback(intent.content)
                    _uiState.update { it.copy(moderationFeedback = feedback) }
                }
            }
            
            is ChatIntentRefactored.ReportMessage -> {
                viewModelScope.launch {
                    val success = moderationService.reportContent(
                        contentId = intent.messageId,
                        contentType = ContentType.MESSAGE,
                        reason = intent.reason,
                        description = intent.description,
                        reporterId = currentUserId
                    )
                    
                    _uiState.update { 
                        it.copy(showReportSuccess = success)
                    }
                }
            }
            
            // Input handling with AI features
            is ChatIntentRefactored.UpdateInputText -> {
                _uiState.update { it.copy(inputText = intent.text) }
                
                // Check for @syra mentions
                if (syraAiService.containsSyraMention(intent.text)) {
                    _uiState.update { it.copy(showSyraSuggestion = true) }
                }
                
                // Real-time moderation feedback
                viewModelScope.launch {
                    delay(500) // Debounce
                    if (_uiState.value.inputText == intent.text) {
                        val feedback = moderationService.getRealtimeFeedback(intent.text)
                        _uiState.update { it.copy(moderationFeedback = feedback) }
                    }
                }
            }
        }
    }
}

// Refactored UI State
data class ChatUiStateRefactored(
    val isLoading: Boolean = false,
    val inputText: String = "",
    val lastSyraResponse: String? = null,
    val showSyraSuggestion: Boolean = false,
    val moderationResult: com.synapse.social.studioasinc.moderation.ModerationResult? = null,
    val moderationFeedback: com.synapse.social.studioasinc.moderation.ModerationFeedback = com.synapse.social.studioasinc.moderation.ModerationFeedback.Safe,
    val showModerationWarning: Boolean = false,
    val showReportSuccess: Boolean = false
)

// Refactored Intents
sealed class ChatIntentRefactored {
    // Message Selection
    object StartMessageSelection : ChatIntentRefactored()
    data class ToggleMessageSelection(val messageId: String) : ChatIntentRefactored()
    object ExitSelectionMode : ChatIntentRefactored()
    object DeleteSelectedMessages : ChatIntentRefactored()
    object CopySelectedMessages : ChatIntentRefactored()
    data class ForwardSelectedMessages(val targetChatIds: List<String>) : ChatIntentRefactored()
    
    // Syra AI
    data class SendMessageToSyra(val message: String) : ChatIntentRefactored()
    data class GenerateSmartReplies(val message: String, val context: List<String> = emptyList()) : ChatIntentRefactored()
    data class UseSmartReply(val reply: String) : ChatIntentRefactored()
    
    // Content Moderation
    data class AnalyzeMessage(val content: String, val messageId: String) : ChatIntentRefactored()
    data class GetRealtimeModerationFeedback(val content: String) : ChatIntentRefactored()
    data class ReportMessage(val messageId: String, val reason: String, val description: String? = null) : ChatIntentRefactored()
    
    // Input
    data class UpdateInputText(val text: String) : ChatIntentRefactored()
}
