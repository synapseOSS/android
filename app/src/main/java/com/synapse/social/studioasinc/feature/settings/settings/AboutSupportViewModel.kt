package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.model.AppUpdateInfo
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.data.repository.FeedbackRepository
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

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _feedbackSubmitted = MutableStateFlow(false)
    val feedbackSubmitted: StateFlow<Boolean> = _feedbackSubmitted.asStateFlow()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val feedbackRepository = FeedbackRepository()

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
        return "https://synapse.social/terms"
    }

    /**
     * Handles navigation to Privacy Policy.
     * Returns the URL to be opened in a browser.
     * 
     * Requirements: 9.3
     */
    fun getPrivacyPolicyUrl(): String {
        return "https://synapse.social/privacy"
    }

    /**
     * Handles navigation to Help Center.
     * Returns the URL to be opened in a browser.
     * 
     * Requirements: 9.4
     */
    fun navigateToHelpCenter() {
        android.util.Log.d("AboutSupportViewModel", "Navigate to Help Center (placeholder)")
        // Navigation will be handled by the screen composable
    }

    /**
     * Gets the Help Center URL.
     * Returns the URL to be opened in a browser.
     * 
     * Requirements: 9.4
     */
    fun getHelpCenterUrl(): String {
        return "https://synapse.social/help"
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
            _error.value = null
            _message.value = null
            try {
                android.util.Log.d("AboutSupportViewModel", "Checking for updates...")
                
                val repository = SettingsRepositoryImpl.getInstance(getApplication())
                val result = repository.checkForUpdates()

                val context = getApplication<Application>()

                result.fold(
                    onSuccess = { updateInfo ->
                        if (updateInfo != null) {
                            val currentVersionCode = _buildNumber.value.toLongOrNull() ?: 0L

                            if (updateInfo.versionCode > currentVersionCode) {
                                _updateInfo.value = updateInfo
                                android.util.Log.d("AboutSupportViewModel", "Update available: ${updateInfo.versionName}")
                            } else {
                                android.util.Log.d("AboutSupportViewModel", "App is up to date")
                                _message.value = context.getString(R.string.app_is_up_to_date)
                            }
                        } else {
                            android.util.Log.d("AboutSupportViewModel", "No version info found on server")
                            _message.value = context.getString(R.string.app_is_up_to_date)
                        }
                    },
                    onFailure = { e ->
                        android.util.Log.e("AboutSupportViewModel", "Failed to check for updates", e)
                        _error.value = context.getString(R.string.update_check_failed)
                    }
                )
                
            } catch (e: Exception) {
                val context = getApplication<Application>()
                android.util.Log.e("AboutSupportViewModel", "Failed to check for updates", e)
                _error.value = context.getString(R.string.update_check_failed_short)
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

                val deviceInfo = "Manufacturer: ${Build.MANUFACTURER}, Model: ${Build.MODEL}, OS: ${Build.VERSION.RELEASE}, SDK: ${Build.VERSION.SDK_INT}"
                
                val result = feedbackRepository.submitFeedback(
                    category = category,
                    description = description,
                    appVersion = _appVersion.value,
                    buildNumber = _buildNumber.value,
                    deviceInfo = deviceInfo
                )

                if (result.isSuccess) {
                    _feedbackSubmitted.value = true
                    android.util.Log.d("AboutSupportViewModel", "Feedback submitted successfully")
                } else {
                    val exception = result.exceptionOrNull()
                    android.util.Log.e("AboutSupportViewModel", "Failed to submit feedback", exception)
                    _error.value = "Failed to submit feedback. Please try again."
                }
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
     * Clears any success messages.
     */
    fun clearMessage() {
        _message.value = null
    }

    /**
     * Dismisses the update dialog.
     */
    fun dismissUpdateDialog() {
        _updateInfo.value = null
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
