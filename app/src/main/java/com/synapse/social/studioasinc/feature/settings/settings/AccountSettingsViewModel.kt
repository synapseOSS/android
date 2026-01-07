package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * ViewModel for the Account Settings screen.
 * 
 * Manages the state for account-related settings including linked accounts,
 * email changes, password changes, and account deletion flows.
 * 
 * Requirements: 2.1, 2.3, 2.4, 2.5, 2.6
 */
class AccountSettingsViewModel(application: Application) : AndroidViewModel(application) {

    // ========================================================================
    // State
    // ========================================================================

    private val _linkedAccounts = MutableStateFlow<LinkedAccountsState>(LinkedAccountsState())
    val linkedAccounts: StateFlow<LinkedAccountsState> = _linkedAccounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Dialog states
    private val _showChangeEmailDialog = MutableStateFlow(false)
    val showChangeEmailDialog: StateFlow<Boolean> = _showChangeEmailDialog.asStateFlow()

    private val _showChangePasswordDialog = MutableStateFlow(false)
    val showChangePasswordDialog: StateFlow<Boolean> = _showChangePasswordDialog.asStateFlow()

    private val _showDeleteAccountDialog = MutableStateFlow(false)
    val showDeleteAccountDialog: StateFlow<Boolean> = _showDeleteAccountDialog.asStateFlow()

    init {
        loadLinkedAccounts()
    }

    // ========================================================================
    // Linked Accounts
    // ========================================================================

    /**
     * Loads the current state of linked social accounts.
     * 
     * Requirements: 2.5
     */
    private fun loadLinkedAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch identities from the database view for the most up-to-date information
                val supabaseClient = com.synapse.social.studioasinc.core.network.SupabaseClient.client
                
                // We use the 'user_identities' view which is filtered to only show the current user's identities
                val identities = supabaseClient.from("user_identities")
                    .select()
                    .decodeList<JsonObject>()

                _linkedAccounts.value = LinkedAccountsState(
                    googleLinked = identities.any { it["provider"]?.jsonPrimitive?.contentOrNull == "google" },
                    facebookLinked = identities.any { it["provider"]?.jsonPrimitive?.contentOrNull == "facebook" },
                    appleLinked = identities.any { it["provider"]?.jsonPrimitive?.contentOrNull == "apple" }
                )
                
                // Log for debugging
                android.util.Log.d("AccountSettingsViewModel", "Loaded ${identities.size} linked accounts")
            } catch (e: Exception) {
                android.util.Log.e("AccountSettingsViewModel", "Failed to load linked accounts", e)
                _error.value = "Failed to load linked accounts: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Connects a social account provider.
     * 
     * @param provider The social provider to connect (google, facebook, apple)
     * Requirements: 2.5
     */
    fun connectSocialAccount(provider: SocialProvider) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val supabaseClient = com.synapse.social.studioasinc.core.network.SupabaseClient.client
                
                when (provider) {
                    SocialProvider.GOOGLE -> {
                        supabaseClient.auth.linkIdentity(io.github.jan.supabase.auth.providers.Google)
                    }
                    SocialProvider.FACEBOOK -> {
                        supabaseClient.auth.linkIdentity(io.github.jan.supabase.auth.providers.Facebook)
                    }
                    SocialProvider.APPLE -> {
                        // Apple linking would require platform-specific implementation
                        throw UnsupportedOperationException("Apple linking not yet supported")
                    }
                }
                
                // Reload linked accounts to reflect changes
                loadLinkedAccounts()
            } catch (e: Exception) {
                android.util.Log.e("AccountSettingsViewModel", "Failed to connect $provider", e)
                _error.value = "Failed to connect ${provider.displayName}: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Disconnects a social account provider.
     * 
     * @param provider The social provider to disconnect
     * Requirements: 2.5
     */
    fun disconnectSocialAccount(provider: SocialProvider) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val supabaseClient = com.synapse.social.studioasinc.core.network.SupabaseClient.client
                val currentUser = supabaseClient.auth.currentUserOrNull()
                
                if (currentUser != null) {
                    val identities = currentUser.identities ?: emptyList()
                    val targetIdentity = identities.find { 
                        it.provider == provider.name.lowercase() 
                    }
                    
                    if (targetIdentity != null) {
                        supabaseClient.auth.unlinkIdentity(targetIdentity.id)
                        // Reload linked accounts to reflect changes
                        loadLinkedAccounts()
                    } else {
                        _error.value = "${provider.displayName} account is not linked"
                    }
                } else {
                    _error.value = "User not authenticated"
                }
            } catch (e: Exception) {
                android.util.Log.e("AccountSettingsViewModel", "Failed to disconnect $provider", e)
                _error.value = "Failed to disconnect ${provider.displayName}: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Email Change
    // ========================================================================

    /**
     * Shows the change email dialog.
     * 
     * Requirements: 2.3
     */
    fun showChangeEmailDialog() {
        _showChangeEmailDialog.value = true
    }

    /**
     * Dismisses the change email dialog.
     */
    fun dismissChangeEmailDialog() {
        _showChangeEmailDialog.value = false
        _error.value = null
    }

    /**
     * Handles email change request.
     * 
     * @param newEmail The new email address
     * @param password Current password for verification
     * Requirements: 2.3
     */
    fun changeEmail(newEmail: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Validate email format
                if (!isValidEmail(newEmail)) {
                    _error.value = "Invalid email format"
                    return@launch
                }

                if (password.isBlank()) {
                    _error.value = "Password is required"
                    return@launch
                }

                // Get current user email
                val supabaseClient = com.synapse.social.studioasinc.core.network.SupabaseClient.client
                val currentUser = supabaseClient.auth.currentUserOrNull()
                val currentEmail = currentUser?.email

                if (currentEmail == null) {
                    _error.value = "User email not found. Please sign in again."
                    return@launch
                }

                // Verify current password by attempting to sign in
                try {
                    supabaseClient.auth.signInWith(Email) {
                        this.email = currentEmail
                        this.password = password
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AccountSettingsViewModel", "Failed to verify password", e)
                    if (e.message?.contains("invalid", ignoreCase = true) == true ||
                        e.message?.contains("credential", ignoreCase = true) == true) {
                        _error.value = "Incorrect password"
                    } else {
                        _error.value = "Failed to verify password: ${e.message}"
                    }
                    return@launch
                }

                // Update email with backend
                android.util.Log.d("AccountSettingsViewModel", "Changing email to: $newEmail")
                supabaseClient.auth.updateUser {
                    email = newEmail
                }
                
                _showChangeEmailDialog.value = false
            } catch (e: Exception) {
                android.util.Log.e("AccountSettingsViewModel", "Failed to change email", e)
                _error.value = "Failed to change email: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Password Change
    // ========================================================================

    /**
     * Shows the change password dialog.
     * 
     * Requirements: 2.4
     */
    fun showChangePasswordDialog() {
        _showChangePasswordDialog.value = true
    }

    /**
     * Dismisses the change password dialog.
     */
    fun dismissChangePasswordDialog() {
        _showChangePasswordDialog.value = false
        _error.value = null
    }

    /**
     * Handles password change request.
     * 
     * @param currentPassword Current password for verification
     * @param newPassword New password
     * @param confirmPassword Confirmation of new password
     * Requirements: 2.4
     */
    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Validate inputs
                if (currentPassword.isBlank()) {
                    _error.value = "Current password is required"
                    return@launch
                }

                if (newPassword.length < 8) {
                    _error.value = "New password must be at least 8 characters"
                    return@launch
                }

                if (newPassword != confirmPassword) {
                    _error.value = "Passwords do not match"
                    return@launch
                }

                // Get current user email
                val supabaseClient = com.synapse.social.studioasinc.core.network.SupabaseClient.client
                val currentUser = supabaseClient.auth.currentUserOrNull()
                val email = currentUser?.email

                if (email == null) {
                    _error.value = "User email not found. Please sign in again."
                    return@launch
                }

                // Verify current password by attempting to sign in
                try {
                    supabaseClient.auth.signInWith(Email) {
                        this.email = email
                        this.password = currentPassword
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AccountSettingsViewModel", "Failed to verify password", e)
                    // Basic check to distinguish auth failure from other errors
                    if (e.message?.contains("invalid", ignoreCase = true) == true ||
                        e.message?.contains("credential", ignoreCase = true) == true) {
                        _error.value = "Incorrect current password"
                    } else {
                        _error.value = "Failed to verify password: ${e.message}"
                    }
                    return@launch
                }

                // Update password
                supabaseClient.auth.updateUser {
                    password = newPassword
                }

                android.util.Log.d("AccountSettingsViewModel", "Password changed successfully")
                
                _showChangePasswordDialog.value = false
            } catch (e: Exception) {
                android.util.Log.e("AccountSettingsViewModel", "Failed to change password", e)
                _error.value = "Failed to change password: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculates password strength for visual indicator.
     * 
     * @param password The password to evaluate
     * @return Strength level from 0 (weak) to 4 (very strong)
     */
    fun calculatePasswordStrength(password: String): Int {
        var strength = 0
        
        if (password.length >= 8) strength++
        if (password.length >= 12) strength++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) strength++
        if (password.any { it.isDigit() }) strength++
        if (password.any { !it.isLetterOrDigit() }) strength++
        
        return strength.coerceIn(0, 4)
    }

    // ========================================================================
    // Account Deletion
    // ========================================================================

    /**
     * Shows the delete account confirmation dialog.
     * 
     * Requirements: 2.6
     */
    fun showDeleteAccountDialog() {
        _showDeleteAccountDialog.value = true
    }

    /**
     * Dismisses the delete account dialog.
     */
    fun dismissDeleteAccountDialog() {
        _showDeleteAccountDialog.value = false
        _error.value = null
    }

    /**
     * Handles account deletion request.
     * 
     * @param confirmationText User must type exact confirmation phrase
     * Requirements: 2.6
     */
    fun deleteAccount(confirmationText: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Validate confirmation text
                if (confirmationText != DELETE_ACCOUNT_CONFIRMATION) {
                    _error.value = "Please type the exact confirmation phrase"
                    return@launch
                }

                android.util.Log.d("AccountSettingsViewModel", "Deleting account")
                
                val supabaseClient = com.synapse.social.studioasinc.core.network.SupabaseClient.client

                // Call Edge Function to delete user
                supabaseClient.functions.invoke(function = "delete-account")

                // Sign out locally
                supabaseClient.auth.signOut()

                _showDeleteAccountDialog.value = false
                // Note: Navigation to login screen is typically observed via auth state changes
                // in the main activity or navigation graph
            } catch (e: Exception) {
                android.util.Log.e("AccountSettingsViewModel", "Failed to delete account", e)
                _error.value = "Failed to delete account: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Validates email format.
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Clears any error messages.
     */
    fun clearError() {
        _error.value = null
    }

    companion object {
        const val DELETE_ACCOUNT_CONFIRMATION = "DELETE MY ACCOUNT"
    }
}

/**
 * State class for linked social accounts.
 */
data class LinkedAccountsState(
    val googleLinked: Boolean = false,
    val facebookLinked: Boolean = false,
    val appleLinked: Boolean = false
)

/**
 * Enum for social account providers.
 */
enum class SocialProvider {
    GOOGLE,
    FACEBOOK,
    APPLE;

    val displayName: String
        get() = when (this) {
            GOOGLE -> "Google"
            FACEBOOK -> "Facebook"
            APPLE -> "Apple"
        }
}
