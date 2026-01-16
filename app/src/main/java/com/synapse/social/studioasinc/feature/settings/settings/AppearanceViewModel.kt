package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Appearance Settings screen.
 * 
 * Manages the state for appearance-related settings including:
 * - Theme mode (Light, Dark, System)
 * - Dynamic color (Android 12+ wallpaper-based theming)
 * - Font scale (text size customization)
 * - Chat customization navigation
 * 
 * Theme changes are applied immediately with preview support.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
class AppearanceViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepositoryImpl.getInstance(application)

    // ========================================================================
    // State
    // ========================================================================

    private val _appearanceSettings = MutableStateFlow(AppearanceSettings())
    val appearanceSettings: StateFlow<AppearanceSettings> = _appearanceSettings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Whether dynamic color is supported on this device.
     * Dynamic color requires Android 12 (SDK 31) or higher.
     * 
     * Requirements: 4.4
     */
    val isDynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    init {
        loadAppearanceSettings()
    }

    // ========================================================================
    // Appearance Settings
    // ========================================================================

    /**
     * Loads appearance settings from the repository.
     * 
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     */
    private fun loadAppearanceSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.appearanceSettings.collect { settings ->
                    _appearanceSettings.value = settings
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellation - this is expected when scope is cancelled
                throw e
            } catch (e: Exception) {
                android.util.Log.e("AppearanceViewModel", "Failed to load appearance settings", e)
                _error.value = "Failed to load appearance settings"
            }
        }
    }

    // ========================================================================
    // Theme Mode
    // ========================================================================

    /**
     * Sets the theme mode (Light, Dark, or System).
     * Theme changes are applied immediately app-wide.
     * 
     * @param mode The theme mode to apply
     * Requirements: 4.1, 4.2
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setThemeMode(mode)
                // Apply theme app-wide
                com.synapse.social.studioasinc.ui.theme.ThemeManager.applyThemeMode(mode)
                android.util.Log.d("AppearanceViewModel", "Theme mode set to: $mode")
            } catch (e: Exception) {
                android.util.Log.e("AppearanceViewModel", "Failed to set theme mode", e)
                _error.value = "Failed to update theme mode"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Returns the display name for a theme mode.
     * 
     * @param mode The theme mode
     * @return Human-readable display name
     */
    fun getThemeModeDisplayName(mode: ThemeMode): String = when (mode) {
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.SYSTEM -> "System Default"
    }

    /**
     * Returns all available theme mode options.
     * 
     * @return List of theme mode display names
     */
    fun getThemeModeOptions(): List<String> = listOf(
        getThemeModeDisplayName(ThemeMode.LIGHT),
        getThemeModeDisplayName(ThemeMode.DARK),
        getThemeModeDisplayName(ThemeMode.SYSTEM)
    )

    /**
     * Converts a display name back to a ThemeMode enum.
     * 
     * @param displayName The display name
     * @return The corresponding ThemeMode
     */
    fun getThemeModeFromDisplayName(displayName: String): ThemeMode = when (displayName) {
        "Light" -> ThemeMode.LIGHT
        "Dark" -> ThemeMode.DARK
        "System Default" -> ThemeMode.SYSTEM
        else -> ThemeMode.SYSTEM
    }

    // ========================================================================
    // Dynamic Color
    // ========================================================================

    /**
     * Enables or disables dynamic color theming.
     * Only applicable on Android 12+ devices.
     * 
     * @param enabled True to enable dynamic color, false to disable
     * Requirements: 4.3, 4.4
     */
    fun setDynamicColorEnabled(enabled: Boolean) {
        if (!isDynamicColorSupported) {
            android.util.Log.w("AppearanceViewModel", "Dynamic color not supported on this device")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setDynamicColorEnabled(enabled)
                android.util.Log.d("AppearanceViewModel", "Dynamic color ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("AppearanceViewModel", "Failed to toggle dynamic color", e)
                _error.value = "Failed to update dynamic color"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Font Scale
    // ========================================================================

    /**
     * Sets the font scale for text sizing.
     * 
     * @param scale The font scale to apply
     * Requirements: 4.5
     */
    fun setFontScale(scale: FontScale) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setFontScale(scale)
                android.util.Log.d("AppearanceViewModel", "Font scale set to: $scale")
            } catch (e: Exception) {
                android.util.Log.e("AppearanceViewModel", "Failed to set font scale", e)
                _error.value = "Failed to update font scale"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Converts a slider value (0-3) to a FontScale enum.
     * 
     * @param value The slider value (0 = Small, 1 = Medium, 2 = Large, 3 = Extra Large)
     * @return The corresponding FontScale
     */
    fun getFontScaleFromSliderValue(value: Float): FontScale = when (value.toInt()) {
        0 -> FontScale.SMALL
        1 -> FontScale.MEDIUM
        2 -> FontScale.LARGE
        3 -> FontScale.EXTRA_LARGE
        else -> FontScale.MEDIUM
    }

    /**
     * Converts a FontScale enum to a slider value (0-3).
     * 
     * @param scale The font scale
     * @return The slider value (0-3)
     */
    fun getSliderValueFromFontScale(scale: FontScale): Float = when (scale) {
        FontScale.SMALL -> 0f
        FontScale.MEDIUM -> 1f
        FontScale.LARGE -> 2f
        FontScale.EXTRA_LARGE -> 3f
    }

    /**
     * Returns a preview text string for the current font scale.
     * 
     * @param scale The font scale
     * @return Preview text showing the scale name
     */
    fun getFontScalePreviewText(scale: FontScale): String = scale.displayName()

    // ========================================================================
    // Post View Style
    // ========================================================================

    /**
     * Sets the post view style (Swipe or Grid).
     *
     * @param style The post view style to apply
     */
    fun setPostViewStyle(style: PostViewStyle) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setPostViewStyle(style)
                android.util.Log.d("AppearanceViewModel", "Post view style set to: $style")
            } catch (e: Exception) {
                android.util.Log.e("AppearanceViewModel", "Failed to set post view style", e)
                _error.value = "Failed to update post view style"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Returns all available post view style options.
     */
    fun getPostViewStyleOptions(): List<String> = listOf(
        PostViewStyle.SWIPE.displayName(),
        PostViewStyle.GRID.displayName()
    )

    /**
     * Converts a display name back to a PostViewStyle enum.
     */
    fun getPostViewStyleFromDisplayName(displayName: String): PostViewStyle = when (displayName) {
        "Swipe" -> PostViewStyle.SWIPE
        "Grid" -> PostViewStyle.GRID
        else -> PostViewStyle.SWIPE
    }

    // ========================================================================
    // Navigation Handlers
    // ========================================================================

    /**
     * Handles navigation to chat customization screen.
     * This is a placeholder for future implementation.
     * 
     * Requirements: 4.6
     */
    fun navigateToChatCustomization() {
        android.util.Log.d("AppearanceViewModel", "Navigate to chat customization (placeholder)")
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
