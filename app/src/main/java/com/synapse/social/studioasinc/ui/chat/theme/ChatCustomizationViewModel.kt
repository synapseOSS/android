package com.synapse.social.studioasinc.ui.chat.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.domain.model.ChatThemePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatCustomizationViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedTheme = MutableStateFlow(ChatThemePreset.DEFAULT)
    val selectedTheme: StateFlow<ChatThemePreset> = _selectedTheme.asStateFlow()

    init {
        loadCurrentTheme()
    }

    private fun loadCurrentTheme() {
        viewModelScope.launch {
            val settings = settingsRepository.chatSettings.first()
            _selectedTheme.value = settings.themePreset
        }
    }

    fun selectTheme(preset: ChatThemePreset) {
        viewModelScope.launch {
            _selectedTheme.value = preset
            settingsRepository.setChatThemePreset(preset)
        }
    }
}
