package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Privacy and Security Settings screen.
 * 
 * Manages the state for privacy and security-related settings including:
 * - Profile visibility
 * - Content visibility
 * - Two-factor authentication
 * - Biometric lock
 * - Blocked/muted users navigation
 * - Active sessions
 * 
 * Requirements: 3.1, 3.2, 3.8
 */
class PrivacySecurityViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepositoryImpl.getInstance(application)

    // ========================================================================
    // State
    // ========================================================================

    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings: StateFlow<PrivacySettings> = _privacySettings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPrivacySettings()
    }

    // ========================================================================
    // Privacy Settings
    // ========================================================================

    /**
     * Loads privacy settings from the repository.
     * 
     * Requirements: 3.1, 3.2, 3.8
     */
    private fun loadPrivacySettings() {
        viewModelScope.launch {
            try {
                settingsRepository.privacySettings.collect { settings ->
                    _privacySettings.value = settings
                }
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to load privacy settings", e)
                _error.value = "Failed to load privacy settings"
            }
        }
    }

    /**
     * Sets the profile visibility level.
     * 
     * @param visibility The new profile visibility setting
     * Requirements: 3.2
     */
    fun setProfileVisibility(visibility: ProfileVisibility) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setProfileVisibility(visibility)
                android.util.Log.d("PrivacySecurityViewModel", "Profile visibility set to: $visibility")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to set profile visibility", e)
                _error.value = "Failed to update profile visibility"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sets the content visibility level.
     * 
     * @param visibility The new content visibility setting
     * Requirements: 3.8
     */
    fun setContentVisibility(visibility: ContentVisibility) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setContentVisibility(visibility)
                android.util.Log.d("PrivacySecurityViewModel", "Content visibility set to: $visibility")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to set content visibility", e)
                _error.value = "Failed to update content visibility"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Security Settings
    // ========================================================================

    /**
     * Enables or disables two-factor authentication.
     * 
     * @param enabled True to enable 2FA, false to disable
     * Requirements: 3.3
     */
    fun setTwoFactorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setTwoFactorEnabled(enabled)
                android.util.Log.d("PrivacySecurityViewModel", "Two-factor authentication ${if (enabled) "enabled" else "disabled"}")
                
                // TODO: If enabling, guide user through 2FA setup with authenticator app or SMS
                if (enabled) {
                    // Show 2FA setup flow
                }
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to toggle 2FA", e)
                _error.value = "Failed to update two-factor authentication"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Enables or disables biometric lock for app access.
     * 
     * @param enabled True to enable biometric lock, false to disable
     * Requirements: 3.4
     */
    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // TODO: Verify biometric capability before enabling
                settingsRepository.setBiometricLockEnabled(enabled)
                android.util.Log.d("PrivacySecurityViewModel", "Biometric lock ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to toggle biometric lock", e)
                _error.value = "Failed to update biometric lock"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Navigation Handlers
    // ========================================================================

    /**
     * Handles navigation to blocked users list.
     * 
     * Requirements: 3.5
     */
    fun navigateToBlockedUsers() {
        android.util.Log.d("PrivacySecurityViewModel", "Navigate to blocked users")
        // Navigation will be handled by the screen composable
    }

    /**
     * Handles navigation to muted users list.
     * 
     * Requirements: 3.6
     */
    fun navigateToMutedUsers() {
        android.util.Log.d("PrivacySecurityViewModel", "Navigate to muted users")
        // Navigation will be handled by the screen composable
    }

    /**
     * Handles navigation to active sessions.
     * 
     * Requirements: 3.7
     */
    fun navigateToActiveSessions() {
        android.util.Log.d("PrivacySecurityViewModel", "Navigate to active sessions")
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
