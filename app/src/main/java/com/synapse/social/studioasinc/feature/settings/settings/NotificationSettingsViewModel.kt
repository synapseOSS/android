package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Notification Settings screen.
 * 
 * Manages the state for notification-related settings including:
 * - Push notification preferences for all categories (Likes, Comments, Follows, Messages, Mentions)
 * - In-app notification toggle
 * - Notification sound settings (placeholder)
 * - Do Not Disturb schedule (placeholder)
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.6
 */
class NotificationSettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ========================================================================
    // State
    // ========================================================================

    private val _notificationPreferences = MutableStateFlow(NotificationPreferences())
    val notificationPreferences: StateFlow<NotificationPreferences> = _notificationPreferences.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadNotificationPreferences()
    }

    // ========================================================================
    // Notification Preferences
    // ========================================================================

    /**
     * Loads notification preferences from the repository.
     * 
     * Requirements: 5.1, 5.2, 5.3, 5.6
     */
    private fun loadNotificationPreferences() {
        viewModelScope.launch {
            try {
                settingsRepository.notificationPreferences.collect { preferences ->
                    _notificationPreferences.value = preferences
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsViewModel", "Failed to load notification preferences", e)
                _error.value = "Failed to load notification preferences"
            }
        }
    }

    // ========================================================================
    // Category Toggles
    // ========================================================================

    /**
     * Toggles notification preference for a specific category.
     * 
     * @param category The notification category to toggle
     * @param enabled True to enable notifications, false to disable
     * Requirements: 5.2, 5.3
     */
    fun toggleNotificationCategory(category: NotificationCategory, enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.updateNotificationPreference(category, enabled)
                android.util.Log.d(
                    "NotificationSettingsViewModel",
                    "${category.displayName()} notifications ${if (enabled) "enabled" else "disabled"}"
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsViewModel", "Failed to toggle notification category", e)
                _error.value = "Failed to update notification preference"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Returns whether notifications are enabled for a specific category.
     * 
     * @param category The notification category to check
     * @return True if notifications are enabled for this category
     */
    fun isCategoryEnabled(category: NotificationCategory): Boolean {
        return _notificationPreferences.value.isEnabled(category)
    }

    // ========================================================================
    // In-App Notifications
    // ========================================================================

    /**
     * Toggles in-app notifications.
     * 
     * @param enabled True to enable in-app notifications, false to disable
     * Requirements: 5.6
     */
    fun toggleInAppNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setInAppNotificationsEnabled(enabled)
                android.util.Log.d(
                    "NotificationSettingsViewModel",
                    "In-app notifications ${if (enabled) "enabled" else "disabled"}"
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsViewModel", "Failed to toggle in-app notifications", e)
                _error.value = "Failed to update in-app notifications"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Navigation Handlers (Placeholders)
    // ========================================================================

    /**
     * Handles navigation to notification sound settings.
     * This is a placeholder for future implementation.
     * 
     * Requirements: 5.4
     */
    fun navigateToNotificationSound() {
        android.util.Log.d("NotificationSettingsViewModel", "Navigate to notification sound (placeholder)")
        // Navigation will be handled by the screen composable
    }

    /**
     * Handles navigation to Do Not Disturb settings.
     * This is a placeholder for future implementation.
     * 
     * Requirements: 5.5
     */
    fun navigateToDoNotDisturb() {
        android.util.Log.d("NotificationSettingsViewModel", "Navigate to Do Not Disturb (placeholder)")
        // Navigation will be handled by the screen composable
    }

    // ========================================================================
    // Reminders
    // ========================================================================

    /**
     * Toggles reminder notifications.
     * 
     * @param enabled True to enable reminders, false to disable
     */
    fun setReminders(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setRemindersEnabled(enabled)
                android.util.Log.d("NotificationSettingsViewModel", "Reminders ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsViewModel", "Failed to toggle reminders", e)
                _error.value = "Failed to update reminders"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // High Priority Notifications
    // ========================================================================

    /**
     * Toggles high priority notifications.
     * 
     * @param enabled True to enable high priority notifications, false to disable
     */
    fun setHighPriority(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setHighPriorityEnabled(enabled)
                android.util.Log.d("NotificationSettingsViewModel", "High priority ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsViewModel", "Failed to toggle high priority", e)
                _error.value = "Failed to update high priority"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Reaction Notifications
    // ========================================================================

    /**
     * Toggles reaction notifications.
     * 
     * @param enabled True to enable reaction notifications, false to disable
     */
    fun setReactionNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setReactionNotificationsEnabled(enabled)
                android.util.Log.d("NotificationSettingsViewModel", "Reaction notifications ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsViewModel", "Failed to toggle reaction notifications", e)
                _error.value = "Failed to update reaction notifications"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Returns all notification categories for iteration.
     * 
     * @return List of all notification categories
     */
    fun getAllCategories(): List<NotificationCategory> {
        return NotificationCategory.values().toList()
    }

    /**
     * Clears any error messages.
     */
    fun clearError() {
        _error.value = null
    }
}
