package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.chat.service.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for ChatPrivacyScreen.
 */
data class ChatPrivacyUiState(
    val sendReadReceipts: Boolean = true,
    val showTypingIndicators: Boolean = true,
    val isLoading: Boolean = false
)

/**
 * ViewModel for managing Chat Privacy Settings.
 */
class ChatPrivacyViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatPrivacyUiState(isLoading = true))
    val uiState: StateFlow<ChatPrivacyUiState> = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatPrivacyUiState(isLoading = true)
    )

    init {
        loadSettings()
        observeSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val preferences = preferencesManager.getChatPreferences()
            _uiState.update {
                it.copy(
                    sendReadReceipts = preferences.sendReadReceipts,
                    showTypingIndicators = preferences.showTypingIndicators,
                    isLoading = false
                )
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            preferencesManager.getChatPreferencesFlow().collect { preferences ->
                _uiState.update {
                    it.copy(
                        sendReadReceipts = preferences.sendReadReceipts,
                        showTypingIndicators = preferences.showTypingIndicators
                    )
                }
            }
        }
    }

    fun toggleReadReceipts(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setReadReceiptsEnabled(enabled)
            // State update will happen via observeSettings
        }
    }

    fun toggleTypingIndicators(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setTypingIndicatorsEnabled(enabled)
            // State update will happen via observeSettings
        }
    }
}
