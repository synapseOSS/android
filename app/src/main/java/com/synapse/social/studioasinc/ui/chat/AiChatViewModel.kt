package com.synapse.social.studioasinc.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.AI.Gemini
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.data.repository.AiRepository
import com.synapse.social.studioasinc.domain.model.AiChatResponse
import com.synapse.social.studioasinc.domain.model.AiChatSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiChatUiState(
    val messages: List<AiChatMessageUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = ""
)

data class AiChatMessageUiModel(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AiChatViewModel(application: Application) : AndroidViewModel(application) {

    private val aiRepository = AiRepository()
    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var currentSessionId: String? = null
    private var gemini: Gemini? = null

    init {
        initializeGemini()
        initializeSession()
    }

    private fun initializeGemini() {
         gemini = Gemini.Builder(getApplication())
            .model("gemini-1.5-flash")
            .systemInstruction("You are a helpful AI assistant integrated into the Synapse social media app.")
            .build()
    }

    private fun initializeSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val user = UserProfileManager.getCurrentUserProfile()

                if (user == null || user.uid.isBlank()) {
                     _uiState.update { it.copy(error = "You must be logged in to use AI chat", isLoading = false) }
                     return@launch
                }

                val userId = user.uid

                // Try to get existing active session or create new
                val sessions = aiRepository.getChatSessions(userId)
                val activeSession = sessions.firstOrNull { it.isActive == true }

                if (activeSession != null) {
                    currentSessionId = activeSession.id
                    loadChatHistory(activeSession.id)
                } else {
                    val newSession = aiRepository.createChatSession(userId)
                    currentSessionId = newSession.id
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to initialize chat: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadChatHistory(sessionId: String) {
        val history = aiRepository.getChatHistory(sessionId)
        val uiMessages = history.flatMap {
            listOf(
                AiChatMessageUiModel(it.id + "_user", it.userMessage, true),
                AiChatMessageUiModel(it.id + "_ai", it.aiResponse, false)
            )
        }
        _uiState.update { it.copy(messages = uiMessages) }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText
        if (text.isBlank()) return

        val sessionId = currentSessionId ?: return

        // Optimistic update
        val tempUserMsg = AiChatMessageUiModel("temp_user", text, true)
        _uiState.update {
            it.copy(
                messages = it.messages + tempUserMsg,
                inputText = "",
                isLoading = true
            )
        }

        gemini?.sendPrompt(text, object : Gemini.GeminiCallback {
            override fun onSuccess(response: String) {
                viewModelScope.launch {
                    // Save to DB
                    try {
                        val savedResponse = aiRepository.saveChatResponse(sessionId, text, response)

                        // Update UI with real data
                        val userMsg = AiChatMessageUiModel(savedResponse.id + "_user", text, true)
                        val aiMsg = AiChatMessageUiModel(savedResponse.id + "_ai", response, false)

                        _uiState.update { state ->
                            val newMessages = state.messages.filter { it.id != "temp_user" } + userMsg + aiMsg
                            state.copy(messages = newMessages, isLoading = false)
                        }
                    } catch (e: Exception) {
                         _uiState.update { it.copy(error = "Failed to save chat: ${e.message}", isLoading = false) }
                    }
                }
            }

            override fun onError(error: String) {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            messages = it.messages.filter { msg -> msg.id != "temp_user" },
                            inputText = text, // Restore input on error
                            error = "AI Error: $error",
                            isLoading = false
                        )
                    }
                }
            }

            override fun onThinking() {
                // Could show a thinking indicator
            }
        })
    }
}
