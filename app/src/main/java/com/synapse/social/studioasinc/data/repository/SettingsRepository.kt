package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.ui.settings.AppearanceSettings
import com.synapse.social.studioasinc.ui.settings.ChatSettings
import com.synapse.social.studioasinc.ui.settings.ContentVisibility
import com.synapse.social.studioasinc.ui.settings.FontScale
import com.synapse.social.studioasinc.ui.settings.MediaAutoDownload
import com.synapse.social.studioasinc.ui.settings.NotificationCategory
import com.synapse.social.studioasinc.ui.settings.NotificationPreferences
import com.synapse.social.studioasinc.ui.settings.PrivacySettings
import com.synapse.social.studioasinc.ui.settings.ProfileVisibility
import com.synapse.social.studioasinc.ui.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user settings.
 * 
 * This interface defines the contract for persisting and retrieving all user
 * settings using DataStore. It provides Flow-based reactive access to settings
 * and suspend functions for updating values.
 * 
 * Requirements: 10.1, 10.2, 10.5
 */
interface SettingsRepository {
    
    // ========================================================================
    // Theme Settings (Device-level - preserved on logout)
    // ========================================================================
    
    /**
     * Flow of the current theme mode setting.
     * Emits updates whenever the theme mode changes.
     */
    val themeMode: Flow<ThemeMode>
    
    /**
     * Flow of the dynamic color enabled state.
     * Only applicable on Android 12+ devices.
     */
    val dynamicColorEnabled: Flow<Boolean>
    
    /**
     * Flow of the current font scale setting.
     */
    val fontScale: Flow<FontScale>
    
    /**
     * Flow of combined appearance settings.
     */
    val appearanceSettings: Flow<AppearanceSettings>
    
    /**
     * Sets the theme mode (Light, Dark, or System).
     * @param mode The theme mode to apply
     */
    suspend fun setThemeMode(mode: ThemeMode)
    
    /**
     * Enables or disables dynamic color theming.
     * @param enabled True to enable dynamic color, false to disable
     */
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    
    /**
     * Sets the font scale for text sizing.
     * @param scale The font scale to apply
     */
    suspend fun setFontScale(scale: FontScale)
    
    // ========================================================================
    // Privacy Settings (User-level - cleared on logout)
    // ========================================================================
    
    /**
     * Flow of the profile visibility setting.
     */
    val profileVisibility: Flow<ProfileVisibility>
    
    /**
     * Flow of the content visibility setting.
     */
    val contentVisibility: Flow<ContentVisibility>
    
    /**
     * Flow of the biometric lock enabled state.
     */
    val biometricLockEnabled: Flow<Boolean>
    
    /**
     * Flow of the two-factor authentication enabled state.
     */
    val twoFactorEnabled: Flow<Boolean>
    
    /**
     * Flow of combined privacy settings.
     */
    val privacySettings: Flow<PrivacySettings>
    
    /**
     * Sets the profile visibility level.
     * @param visibility The visibility level to apply
     */
    suspend fun setProfileVisibility(visibility: ProfileVisibility)
    
    /**
     * Sets the content visibility level.
     * @param visibility The visibility level to apply
     */
    suspend fun setContentVisibility(visibility: ContentVisibility)
    
    /**
     * Enables or disables biometric lock for app access.
     * @param enabled True to enable biometric lock, false to disable
     */
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    
    /**
     * Enables or disables two-factor authentication.
     * @param enabled True to enable 2FA, false to disable
     */
    suspend fun setTwoFactorEnabled(enabled: Boolean)
    
    // ========================================================================
    // Notification Settings (User-level - cleared on logout)
    // ========================================================================
    
    /**
     * Flow of notification preferences for all categories.
     */
    val notificationPreferences: Flow<NotificationPreferences>
    
    /**
     * Updates the notification preference for a specific category.
     * @param category The notification category to update
     * @param enabled True to enable notifications, false to disable
     */
    suspend fun updateNotificationPreference(category: NotificationCategory, enabled: Boolean)
    
    /**
     * Enables or disables in-app notifications.
     * @param enabled True to enable in-app notifications, false to disable
     */
    suspend fun setInAppNotificationsEnabled(enabled: Boolean)
    
    // ========================================================================
    // Chat Settings (User-level - cleared on logout)
    // ========================================================================
    
    /**
     * Flow of chat settings.
     */
    val chatSettings: Flow<ChatSettings>
    
    /**
     * Enables or disables read receipts.
     * @param enabled True to show read receipts, false to hide
     */
    suspend fun setReadReceiptsEnabled(enabled: Boolean)
    
    /**
     * Enables or disables typing indicators.
     * @param enabled True to show typing indicators, false to hide
     */
    suspend fun setTypingIndicatorsEnabled(enabled: Boolean)
    
    /**
     * Sets the media auto-download preference.
     * @param setting The auto-download setting to apply
     */
    suspend fun setMediaAutoDownload(setting: MediaAutoDownload)
    
    // ========================================================================
    // Storage and Cache Management
    // ========================================================================
    
    /**
     * Flow of the current cache size in bytes.
     */
    val cacheSize: Flow<Long>
    
    /**
     * Clears the app cache and returns the amount of space freed.
     * @return The number of bytes freed by clearing the cache
     */
    suspend fun clearCache(): Long
    
    /**
     * Calculates and returns the current cache size.
     * @return The current cache size in bytes
     */
    suspend fun calculateCacheSize(): Long
    
    // ========================================================================
    // Data Saver Settings
    // ========================================================================
    
    /**
     * Flow of the data saver mode enabled state.
     */
    val dataSaverEnabled: Flow<Boolean>
    
    /**
     * Enables or disables data saver mode.
     * @param enabled True to enable data saver, false to disable
     */
    suspend fun setDataSaverEnabled(enabled: Boolean)
    
    // ========================================================================
    // Settings Lifecycle Management
    // ========================================================================
    
    /**
     * Clears all user-specific settings while preserving device-level preferences.
     * Called on user logout to reset user data while keeping theme preferences.
     * 
     * Device-level settings preserved:
     * - Theme mode
     * - Dynamic color
     * - Font scale
     * 
     * User-level settings cleared:
     * - Privacy settings
     * - Notification preferences
     * - Chat settings
     */
    suspend fun clearUserSettings()
    
    /**
     * Clears all settings including device-level preferences.
     * Use with caution - typically only for complete app reset.
     */
    suspend fun clearAllSettings()
    
    /**
     * Restores default values for all settings.
     */
    suspend fun restoreDefaults()
}
