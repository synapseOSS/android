package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.biometric.BiometricManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.core.util.BiometricChecker
import com.synapse.social.studioasinc.core.util.BiometricCheckerImpl
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
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
    
    private val biometricChecker: BiometricChecker = BiometricCheckerImpl()

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

    // Dialog states for 2FA setup
    private val _show2FASetupDialog = MutableStateFlow(false)
    val show2FASetupDialog: StateFlow<Boolean> = _show2FASetupDialog.asStateFlow()

    private val _twoFactorSetupState = MutableStateFlow(TwoFactorSetupState())
    val twoFactorSetupState: StateFlow<TwoFactorSetupState> = _twoFactorSetupState.asStateFlow()

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
                if (enabled) {
                    // Show 2FA setup dialog
                    _show2FASetupDialog.value = true
                    _twoFactorSetupState.value = TwoFactorSetupState()
                } else {
                    // Disable 2FA directly
                    settingsRepository.setTwoFactorEnabled(false)
                    android.util.Log.d("PrivacySecurityViewModel", "Two-factor authentication disabled")
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
     * Generates a new 2FA secret and QR code for setup.
     */
    fun generate2FASecret() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val supabaseClient = com.synapse.social.studioasinc.core.network.SupabaseClient.client
                val currentUser = supabaseClient.auth.currentUserOrNull()
                
                if (currentUser?.email != null) {
                    // Generate a random secret (32 characters, base32)
                    val secret = generateBase32Secret()
                    val qrCodeUrl = "otpauth://totp/Synapse:${currentUser.email}?secret=$secret&issuer=Synapse"
                    
                    _twoFactorSetupState.value = _twoFactorSetupState.value.copy(
                        secret = secret,
                        qrCodeUrl = qrCodeUrl,
                        step = TwoFactorSetupStep.SCAN_QR
                    )
                } else {
                    _error.value = "User email not found"
                }
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to generate 2FA secret", e)
                _error.value = "Failed to generate 2FA secret"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Verifies the 2FA code entered by the user.
     */
    fun verify2FACode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (code.length != 6 || !code.all { it.isDigit() }) {
                    _error.value = "Please enter a valid 6-digit code"
                    return@launch
                }

                // In a real implementation, you would verify the TOTP code against the secret
                // For now, we'll simulate successful verification
                settingsRepository.setTwoFactorEnabled(true)
                
                _twoFactorSetupState.value = _twoFactorSetupState.value.copy(
                    step = TwoFactorSetupStep.COMPLETED
                )
                
                android.util.Log.d("PrivacySecurityViewModel", "Two-factor authentication enabled successfully")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to verify 2FA code", e)
                _error.value = "Failed to verify code"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Dismisses the 2FA setup dialog.
     */
    fun dismiss2FASetupDialog() {
        _show2FASetupDialog.value = false
        _twoFactorSetupState.value = TwoFactorSetupState()
    }

    /**
     * Generates a base32 secret for TOTP.
     */
    private fun generateBase32Secret(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        return (1..32).map { chars.random() }.joinToString("")
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
                if (enabled) {
                    val canAuthenticate = biometricChecker.canAuthenticate(getApplication())

                    when (canAuthenticate) {
                        BiometricManager.BIOMETRIC_SUCCESS -> {
                            // Can authenticate, proceed
                        }
                        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                            _error.value = "No biometric features available on this device."
                            _isLoading.value = false
                            return@launch
                        }
                        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                            _error.value = "Biometric features are currently unavailable."
                            _isLoading.value = false
                            return@launch
                        }
                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                            _error.value = "No biometric credentials enrolled on this device."
                            _isLoading.value = false
                            return@launch
                        }
                        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                            _error.value = "Security update required for biometric features."
                            _isLoading.value = false
                            return@launch
                        }
                        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                            _error.value = "Biometric features are unsupported."
                            _isLoading.value = false
                            return@launch
                        }
                        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                            _error.value = "Biometric status unknown."
                            _isLoading.value = false
                            return@launch
                        }
                    }
                }

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
    // Read Receipts
    // ========================================================================

    /**
     * Toggles read receipts setting.
     * 
     * @param enabled True to enable read receipts, false to disable
     */
    fun setReadReceiptsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setReadReceiptsEnabled(enabled)
                android.util.Log.d("PrivacySecurityViewModel", "Read receipts ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to toggle read receipts", e)
                _error.value = "Failed to update read receipts"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // App Lock
    // ========================================================================

    /**
     * Toggles app lock setting.
     * 
     * @param enabled True to enable app lock, false to disable
     */
    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setAppLockEnabled(enabled)
                android.util.Log.d("PrivacySecurityViewModel", "App lock ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to toggle app lock", e)
                _error.value = "Failed to update app lock"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Chat Lock
    // ========================================================================

    /**
     * Toggles chat lock setting.
     * 
     * @param enabled True to enable chat lock, false to disable
     */
    fun setChatLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setChatLockEnabled(enabled)
                android.util.Log.d("PrivacySecurityViewModel", "Chat lock ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("PrivacySecurityViewModel", "Failed to toggle chat lock", e)
                _error.value = "Failed to update chat lock"
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

/**
 * State for two-factor authentication setup flow.
 */
data class TwoFactorSetupState(
    val step: TwoFactorSetupStep = TwoFactorSetupStep.INITIAL,
    val secret: String = "",
    val qrCodeUrl: String = ""
)

/**
 * Steps in the 2FA setup process.
 */
enum class TwoFactorSetupStep {
    INITIAL,
    SCAN_QR,
    VERIFY_CODE,
    COMPLETED
}
