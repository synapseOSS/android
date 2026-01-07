package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel for the Language and Region Settings screen.
 * 
 * Manages the state for language and region-related settings including:
 * - Available languages list with native names
 * - Current language selection
 * - Language change handling
 * - Region preferences navigation
 * 
 * Requirements: 8.1, 8.4
 */
class LanguageRegionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository =
        SettingsRepositoryImpl.getInstance(application)

    // ========================================================================
    // State
    // ========================================================================

    private val _currentLanguage = MutableStateFlow("English")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<LanguageOption>>(emptyList())
    val availableLanguages: StateFlow<List<LanguageOption>> = _availableLanguages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAvailableLanguages()
    }

    // ========================================================================
    // Language Management
    // ========================================================================

    /**
     * Loads the list of available languages with their native names.
     * 
     * Requirements: 8.1, 8.4
     */
    private fun loadAvailableLanguages() {
        viewModelScope.launch {
            try {
                // Define available languages with native script names
                // Requirements: 8.4 - Language names in their native script
                val languages = listOf(
                    LanguageOption(
                        code = "en",
                        name = "English",
                        nativeName = "English"
                    ),
                    LanguageOption(
                        code = "es",
                        name = "Spanish",
                        nativeName = "Español"
                    ),
                    LanguageOption(
                        code = "fr",
                        name = "French",
                        nativeName = "Français"
                    ),
                    LanguageOption(
                        code = "de",
                        name = "German",
                        nativeName = "Deutsch"
                    ),
                    LanguageOption(
                        code = "it",
                        name = "Italian",
                        nativeName = "Italiano"
                    ),
                    LanguageOption(
                        code = "pt",
                        name = "Portuguese",
                        nativeName = "Português"
                    ),
                    LanguageOption(
                        code = "ru",
                        name = "Russian",
                        nativeName = "Русский"
                    ),
                    LanguageOption(
                        code = "ja",
                        name = "Japanese",
                        nativeName = "日本語"
                    ),
                    LanguageOption(
                        code = "ko",
                        name = "Korean",
                        nativeName = "한국어"
                    ),
                    LanguageOption(
                        code = "zh",
                        name = "Chinese (Simplified)",
                        nativeName = "简体中文"
                    ),
                    LanguageOption(
                        code = "zh-TW",
                        name = "Chinese (Traditional)",
                        nativeName = "繁體中文"
                    ),
                    LanguageOption(
                        code = "ar",
                        name = "Arabic",
                        nativeName = "العربية"
                    ),
                    LanguageOption(
                        code = "hi",
                        name = "Hindi",
                        nativeName = "हिन्दी"
                    ),
                    LanguageOption(
                        code = "bn",
                        name = "Bengali",
                        nativeName = "বাংলা"
                    ),
                    LanguageOption(
                        code = "tr",
                        name = "Turkish",
                        nativeName = "Türkçe"
                    ),
                    LanguageOption(
                        code = "vi",
                        name = "Vietnamese",
                        nativeName = "Tiếng Việt"
                    ),
                    LanguageOption(
                        code = "th",
                        name = "Thai",
                        nativeName = "ไทย"
                    ),
                    LanguageOption(
                        code = "pl",
                        name = "Polish",
                        nativeName = "Polski"
                    ),
                    LanguageOption(
                        code = "nl",
                        name = "Dutch",
                        nativeName = "Nederlands"
                    ),
                    LanguageOption(
                        code = "sv",
                        name = "Swedish",
                        nativeName = "Svenska"
                    )
                )
                _availableLanguages.value = languages

                // Set initial selection based on saved preference or system default
                settingsRepository.language.collect { savedCode ->
                    val selected = languages.find { it.code == savedCode }
                    if (selected != null) {
                        _currentLanguage.value = selected.nativeName
                    } else {
                        // If no saved preference or invalid, fallback to system if matched or English
                        val systemLocale = Locale.getDefault()
                        val systemCode = systemLocale.language
                        val match = languages.find { it.code.startsWith(systemCode) }
                        if (match != null) {
                            _currentLanguage.value = match.nativeName
                        }
                    }
                }

                android.util.Log.d("LanguageRegionViewModel", "Loaded ${_availableLanguages.value.size} languages")
            } catch (e: Exception) {
                android.util.Log.e("LanguageRegionViewModel", "Failed to load languages", e)
                _error.value = "Failed to load available languages"
            }
        }
    }

    /**
     * Sets the selected language.
     * 
     * @param languageOption The language option to select
     * Requirements: 8.1, 8.2
     */
    fun setLanguage(languageOption: LanguageOption) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // 1. Save the language preference to DataStore
                settingsRepository.setLanguage(languageOption.code)

                // 2. Update the app's locale configuration
                val locale = if (languageOption.code.contains("-")) {
                    val parts = languageOption.code.split("-")
                    Locale.Builder().setLanguage(parts[0]).setRegion(parts[1]).build()
                } else {
                    Locale.Builder().setLanguage(languageOption.code).build()
                }
                
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.create(locale)
                )

                _currentLanguage.value = languageOption.nativeName
                
                android.util.Log.d(
                    "LanguageRegionViewModel",
                    "Language changed to: ${languageOption.name} (${languageOption.code})"
                )
                
            } catch (e: Exception) {
                android.util.Log.e("LanguageRegionViewModel", "Failed to set language", e)
                _error.value = "Failed to change language"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Checks if a language is currently selected.
     * 
     * @param languageOption The language option to check
     * @return True if the language is currently selected
     */
    fun isLanguageSelected(languageOption: LanguageOption): Boolean {
        return _currentLanguage.value == languageOption.nativeName
    }

    // ========================================================================
    // Navigation Handlers
    // ========================================================================

    /**
     * Handles navigation to region preferences screen.
     * This is a placeholder for future implementation.
     * 
     * Requirements: 8.3
     */
    fun navigateToRegionPreferences() {
        android.util.Log.d("LanguageRegionViewModel", "Navigate to region preferences (placeholder)")
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

/**
 * Data class representing a language option.
 * 
 * @property code The ISO 639-1 language code (e.g., "en", "es", "ja")
 * @property name The English name of the language
 * @property nativeName The name of the language in its native script
 * 
 * Requirements: 8.4
 */
data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String
)
