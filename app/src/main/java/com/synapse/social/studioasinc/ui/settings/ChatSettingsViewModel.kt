package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Chat Settings screen.
 * 
 * Manages the state for chat-related settings including:
 * - Read receipts (showing when messages are read)
 * - Typing indicators (showing typing status to others)
 * - Media auto-download preferences (Always, WiFi Only, Never)
 * - Message requests navigation
 * - Chat privacy navigation
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4
 */
class ChatSettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ========================================================================
    // State
    // ========================================================================

    private val _chatSettings = MutableStateFlow(ChatSettings())
    val chatSettings: StateFlow<ChatSettings> = _chatSettings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadChatSettings()
    }

    // ========================================================================
    // Chat Settings Loading
    // ========================================================================

    /**
     * Loads chat settings from the repository.
     * 
     * Requirements: 6.1, 6.2, 6.3, 6.4
     */
    private fun loadChatSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.chatSettings.collect { settings ->
                    _chatSettings.value = settings
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatSettingsViewModel", "Failed to load chat settings", e)
                _error.value = "Failed to load chat settings"
            }
        }
    }

    // ========================================================================
    // Read Receipts
    // ========================================================================

    /**
     * Toggles read receipts setting.
     * 
     * When enabled, other users can see when you've read their messages.
     * 
     * @param enabled True to show read receipts, false to hide
     * Requirements: 6.2
     */
    fun toggleReadReceipts(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setReadReceiptsEnabled(enabled)
                android.util.Log.d(
                    "ChatSettingsViewModel",
                    "Read receipts ${if (enabled) "enabled" else "disabled"}"
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatSettingsViewModel", "Failed to toggle read receipts", e)
                _error.value = "Failed to update read receipts"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Typing Indicators
    // ========================================================================

    /**
     * Toggles typing indicators setting.
     * 
     * When enabled, other users can see when you're typing a message.
     * 
     * @param enabled True to show typing indicators, false to hide
     * Requirements: 6.3
     */
    fun toggleTypingIndicators(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setTypingIndicatorsEnabled(enabled)
                android.util.Log.d(
                    "ChatSettingsViewModel",
                    "Typing indicators ${if (enabled) "enabled" else "disabled"}"
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatSettingsViewModel", "Failed to toggle typing indicators", e)
                _error.value = "Failed to update typing indicators"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Media Auto-Download
    // ========================================================================

    /**
     * Sets the media auto-download preference.
     * 
     * Controls when media (images, videos) should be automatically downloaded
     * in chat conversations.
     * 
     * @param setting The auto-download setting (Always, WiFi Only, Never)
     * Requirements: 6.4
     */
    fun setMediaAutoDownload(setting: MediaAutoDownload) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setMediaAutoDownload(setting)
                android.util.Log.d(
                    "ChatSettingsViewModel",
                    "Media auto-download set to ${setting.displayName()}"
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatSettingsViewModel", "Failed to set media auto-download", e)
                _error.value = "Failed to update media auto-download"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Returns all media auto-download options for selection.
     * 
     * @return List of all MediaAutoDownload enum values
     */
    fun getMediaAutoDownloadOptions(): List<MediaAutoDownload> {
        return MediaAutoDownload.values().toList()
    }

    // ========================================================================
    // Navigation Handlers
    // ========================================================================

    /**
     * Handles navigation to message requests screen.
     * This is a placeholder for future implementation.
     * 
     * Requirements: 6.5
     */
    fun navigateToMessageRequests() {
        android.util.Log.d("ChatSettingsViewModel", "Navigate to message requests (placeholder)")
        // Navigation will be handled by the screen composable
    }

    /**
     * Handles navigation to chat privacy settings.
     * This will navigate to the existing ChatPrivacySettingsActivity.
     * 
     * Requirements: 6.6
     */
    fun navigateToChatPrivacy() {
        android.util.Log.d("ChatSettingsViewModel", "Navigate to chat privacy")
        // Navigation will be handled by the screen composable
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Clears any error messages.
     */
    fun clearError() {
        _error.value = null
    }
}
