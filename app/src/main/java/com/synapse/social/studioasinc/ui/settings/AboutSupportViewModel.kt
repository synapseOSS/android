package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the About and Support Settings screen.
 * 
 * Manages the state for app information, version details, and support-related
 * functionality including external link navigation and feedback submission.
 * 
 * Requirements: 9.1, 9.5
 */
class AboutSupportViewModel(
    application: Application
) : AndroidViewModel(application) {

    // ========================================================================
    // State
    // ========================================================================

    private val _appVersion = MutableStateFlow("")
    val appVersion: StateFlow<String> = _appVersion.asStateFlow()

    private val _buildNumber = MutableStateFlow("")
    val buildNumber: StateFlow<String> = _buildNumber.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _feedbackSubmitted = MutableStateFlow(false)
    val feedbackSubmitted: StateFlow<Boolean> = _feedbackSubmitted.asStateFlow()

    init {
        loadAppInfo()
    }

    // ========================================================================
    // App Information
    // ========================================================================

    /**
     * Loads app version and build information from PackageManager.
     * 
     * Requirements: 9.1
     */
    private fun loadAppInfo() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                )

                _appVersion.value = packageInfo.versionName ?: "Unknown"
                
                // Get version code based on API level
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toString()
                }
                
                _buildNumber.value = versionCode

                android.util.Log.d(
                    "AboutSupportViewModel",
                    "App version: ${_appVersion.value}, Build: ${_buildNumber.value}"
                )
            } catch (e: PackageManager.NameNotFoundException) {
                android.util.Log.e("AboutSupportViewModel", "Failed to load app info", e)
                _appVersion.value = "Unknown"
                _buildNumber.value = "Unknown"
            }
        }
    }

    // ========================================================================
    // External Link Navigation
    // ========================================================================

    /**
     * Handles navigation to Terms of Service.
     * Returns the URL to be opened in a browser.
     * 
     * Requirements: 9.2
     */
    fun getTermsOfServiceUrl(): String {
        // TODO: Replace with actual Terms of Service URL
        return "https://synapse.social/terms"
    }

    /**
     * Handles navigation to Privacy Policy.
     * Returns the URL to be opened in a browser.
     * 
     * Requirements: 9.3
     */
    fun getPrivacyPolicyUrl(): String {
        // TODO: Replace with actual Privacy Policy URL
        return "https://synapse.social/privacy"
    }

    /**
     * Handles navigation to Help Center.
     * This is a placeholder for future implementation.
     * 
     * Requirements: 9.4
     */
    fun navigateToHelpCenter() {
        android.util.Log.d("AboutSupportViewModel", "Navigate to Help Center (placeholder)")
        // Navigation will be handled by the screen composable
    }

    /**
     * Handles check for updates.
     * This is a placeholder for future implementation.
     * 
     * Requirements: 9.6
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("AboutSupportViewModel", "Checking for updates (placeholder)")
                // TODO: Implement actual update check logic
                // This would typically involve:
                // 1. Querying a backend API for latest version
                // 2. Comparing with current version
                // 3. Showing update dialog if available
                
                // For now, just simulate a check
                kotlinx.coroutines.delay(1000)
                
            } catch (e: Exception) {
                android.util.Log.e("AboutSupportViewModel", "Failed to check for updates", e)
                _error.value = "Failed to check for updates"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Handles navigation to Open Source Licenses screen.
     * This is a placeholder for future implementation.
     * 
     * Requirements: 9.7
     */
    fun navigateToLicenses() {
        android.util.Log.d("AboutSupportViewModel", "Navigate to Open Source Licenses (placeholder)")
        // Navigation will be handled by the screen composable
    }

    // ========================================================================
    // Feedback Submission
    // ========================================================================

    /**
     * Submits user feedback/problem report.
     * 
     * @param category The feedback category (Bug, Feature Request, Other)
     * @param description The detailed description of the feedback
     * 
     * Requirements: 9.5
     */
    fun submitFeedback(category: String, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _feedbackSubmitted.value = false
            
            try {
                // Validate input
                if (description.isBlank()) {
                    _error.value = "Please provide a description"
                    return@launch
                }

                android.util.Log.d(
                    "AboutSupportViewModel",
                    "Submitting feedback - Category: $category, Description length: ${description.length}"
                )

                // TODO: Implement actual feedback submission
                // This would typically involve:
                // 1. Sending feedback to backend API
                // 2. Including app version, device info, user ID
                // 3. Optionally attaching logs or screenshots
                
                // For now, just simulate submission
                kotlinx.coroutines.delay(1500)
                
                _feedbackSubmitted.value = true
                
                android.util.Log.d("AboutSupportViewModel", "Feedback submitted successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("AboutSupportViewModel", "Failed to submit feedback", e)
                _error.value = "Failed to submit feedback. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Resets the feedback submitted state.
     */
    fun resetFeedbackState() {
        _feedbackSubmitted.value = false
        _error.value = null
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

    /**
     * Gets the full app version string including build number.
     * 
     * @return Formatted version string (e.g., "1.0.0 (15)")
     */
    fun getFullVersionString(): String {
        return "${_appVersion.value} (${_buildNumber.value})"
    }
}
