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

    /**
     * Handles navigation to chat customization screen.
     * This allows users to customize chat bubble colors and themes.
     */
    fun navigateToChatCustomization() {
        android.util.Log.d("ChatSettingsViewModel", "Navigate to chat customization")
        // Navigation will be handled by the screen composable
    }

    /**
     * Handles navigation to chat wallpapers screen.
     * This allows users to set custom backgrounds for conversations.
     */
    fun navigateToChatWallpapers() {
        android.util.Log.d("ChatSettingsViewModel", "Navigate to chat wallpapers")
        // Navigation will be handled by the screen composable
    }

    /**
     * Handles navigation to chat history deletion screen.
     * This allows users to delete their chat history from all devices.
     */
    fun navigateToChatHistoryDeletion() {
        android.util.Log.d("ChatSettingsViewModel", "Navigate to chat history deletion")
        // Navigation will be handled by the screen composable
    }

    // ========================================================================
    // Chat Font Size
    // ========================================================================

    /**
     * Sets the chat font scale for message text.
     * 
     * @param scale The font scale multiplier (0.8f to 1.4f)
     */
    fun setChatFontScale(scale: Float) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setChatFontScale(scale)
                android.util.Log.d("ChatSettingsViewModel", "Chat font scale set to $scale")
            } catch (e: Exception) {
                android.util.Log.e("ChatSettingsViewModel", "Failed to set chat font scale", e)
                _error.value = "Failed to update chat font size"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Converts chat font scale to slider value (0-3).
     */
    fun getChatFontSizeSliderValue(scale: Float): Float {
        return when {
            scale <= 0.8f -> 0f
            scale <= 1.0f -> 1f
            scale <= 1.2f -> 2f
            else -> 3f
        }
    }

    /**
     * Converts slider value (0-3) to chat font scale.
     */
    fun getChatFontScaleFromSliderValue(value: Float): Float {
        return when (value.toInt()) {
            0 -> 0.8f
            1 -> 1.0f
            2 -> 1.2f
            3 -> 1.4f
            else -> 1.0f
        }
    }

    /**
     * Gets preview text for chat font scale.
     */
    fun getChatFontScalePreviewText(scale: Float): String {
        return when {
            scale <= 0.8f -> "Small"
            scale <= 1.0f -> "Default"
            scale <= 1.2f -> "Large"
            else -> "Extra Large"
        }
    }

    // ========================================================================
    // Enter is Send
    // ========================================================================

    /**
     * Toggles enter key send setting.
     * 
     * When enabled, pressing Enter sends the message instead of creating a new line.
     * 
     * @param enabled True to enable enter key send, false to disable
     */
    fun setEnterIsSend(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setEnterIsSendEnabled(enabled)
                android.util.Log.d("ChatSettingsViewModel", "Enter is send ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("ChatSettingsViewModel", "Failed to toggle enter is send", e)
                _error.value = "Failed to update enter is send"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Media Visibility
    // ========================================================================

    /**
     * Toggles media visibility setting.
     * 
     * When enabled, media files are visible in device gallery.
     * 
     * @param enabled True to show media in gallery, false to hide
     */
    fun setMediaVisibility(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setMediaVisibilityEnabled(enabled)
                android.util.Log.d("ChatSettingsViewModel", "Media visibility ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("ChatSettingsViewModel", "Failed to toggle media visibility", e)
                _error.value = "Failed to update media visibility"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Voice Transcripts
    // ========================================================================

    /**
     * Toggles voice transcripts setting.
     * 
     * When enabled, voice messages are automatically transcribed to text.
     * 
     * @param enabled True to enable voice transcripts, false to disable
     */
    fun setVoiceTranscripts(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setVoiceTranscriptsEnabled(enabled)
                android.util.Log.d("ChatSettingsViewModel", "Voice transcripts ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("ChatSettingsViewModel", "Failed to toggle voice transcripts", e)
                _error.value = "Failed to update voice transcripts"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Auto Backup
    // ========================================================================

    /**
     * Toggles auto backup setting.
     * 
     * When enabled, chat history is automatically backed up to cloud storage.
     * 
     * @param enabled True to enable auto backup, false to disable
     */
    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setAutoBackupEnabled(enabled)
                android.util.Log.d("ChatSettingsViewModel", "Auto backup ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("ChatSettingsViewModel", "Failed to toggle auto backup", e)
                _error.value = "Failed to update auto backup"
            } finally {
                _isLoading.value = false
            }
        }
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
