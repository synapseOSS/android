package com.synapse.social.studioasinc.data.repository

import android.content.Context
import android.util.Log
import com.synapse.social.studioasinc.data.local.database.SettingsDataStore
import com.synapse.social.studioasinc.ui.settings.AppearanceSettings
import com.synapse.social.studioasinc.ui.settings.ChatSettings
import com.synapse.social.studioasinc.ui.settings.ContentVisibility
import com.synapse.social.studioasinc.ui.settings.FontScale
import com.synapse.social.studioasinc.ui.settings.MediaAutoDownload
import com.synapse.social.studioasinc.ui.settings.NotificationCategory
import com.synapse.social.studioasinc.ui.settings.NotificationPreferences
import com.synapse.social.studioasinc.ui.settings.PrivacySettings
import com.synapse.social.studioasinc.ui.settings.ProfileVisibility
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.model.AppUpdateInfo
import com.synapse.social.studioasinc.ui.settings.ThemeMode
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Implementation of SettingsRepository that uses SettingsDataStore for persistence.
 * 
 * This class provides the concrete implementation of all settings operations,
 * delegating persistence to SettingsDataStore and handling cache management.
 * 
 * Requirements: 7.2, 10.1, 10.3
 */
class SettingsRepositoryImpl private constructor(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    companion object {
        private const val TAG = "SettingsRepositoryImpl"
        
        @Volatile
        private var INSTANCE: SettingsRepositoryImpl? = null

        fun getInstance(context: Context): SettingsRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                val dataStore = SettingsDataStore.getInstance(context)
                INSTANCE ?: SettingsRepositoryImpl(
                    context.applicationContext,
                    dataStore
                ).also { INSTANCE = it }
            }
        }
    }


    // Cache size state for reactive updates
    private val _cacheSize = MutableStateFlow(0L)

    // ========================================================================
    // Theme Settings (Device-level - preserved on logout)
    // ========================================================================
    
    override val themeMode: Flow<ThemeMode> = settingsDataStore.themeMode
    
    override val dynamicColorEnabled: Flow<Boolean> = settingsDataStore.dynamicColorEnabled
    
    override val fontScale: Flow<FontScale> = settingsDataStore.fontScale
    
    override val appearanceSettings: Flow<AppearanceSettings> = settingsDataStore.appearanceSettings
    
    override suspend fun setThemeMode(mode: ThemeMode) {
        settingsDataStore.setThemeMode(mode)
    }
    
    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        settingsDataStore.setDynamicColorEnabled(enabled)
    }
    
    override suspend fun setFontScale(scale: FontScale) {
        settingsDataStore.setFontScale(scale)
    }

    override suspend fun setPostViewStyle(style: com.synapse.social.studioasinc.ui.settings.PostViewStyle) {
        settingsDataStore.setPostViewStyle(style)
    }

    override val language: Flow<String> = settingsDataStore.language

    override suspend fun setLanguage(languageCode: String) {
        settingsDataStore.setLanguage(languageCode)
    }

    // ========================================================================
    // Privacy Settings (User-level - cleared on logout)
    // ========================================================================
    
    override val profileVisibility: Flow<ProfileVisibility> = settingsDataStore.profileVisibility
    
    override val contentVisibility: Flow<ContentVisibility> = settingsDataStore.contentVisibility
    
    override val biometricLockEnabled: Flow<Boolean> = settingsDataStore.biometricLockEnabled
    
    override val twoFactorEnabled: Flow<Boolean> = settingsDataStore.twoFactorEnabled
    
    override val privacySettings: Flow<PrivacySettings> = settingsDataStore.privacySettings
    
    override suspend fun setProfileVisibility(visibility: ProfileVisibility) {
        settingsDataStore.setProfileVisibility(visibility)
    }
    
    override suspend fun setContentVisibility(visibility: ContentVisibility) {
        settingsDataStore.setContentVisibility(visibility)
    }
    
    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        settingsDataStore.setBiometricLockEnabled(enabled)
    }
    
    override suspend fun setTwoFactorEnabled(enabled: Boolean) {
        settingsDataStore.setTwoFactorEnabled(enabled)
    }

    // ========================================================================
    // Notification Settings (User-level - cleared on logout)
    // ========================================================================
    
    override val notificationPreferences: Flow<NotificationPreferences> = 
        settingsDataStore.notificationPreferences
    
    override suspend fun updateNotificationPreference(category: NotificationCategory, enabled: Boolean) {
        settingsDataStore.updateNotificationPreference(category, enabled)
    }
    
    override suspend fun setInAppNotificationsEnabled(enabled: Boolean) {
        settingsDataStore.setInAppNotificationsEnabled(enabled)
    }

    // ========================================================================
    // Chat Settings (User-level - cleared on logout)
    // ========================================================================
    
    override val chatSettings: Flow<ChatSettings> = settingsDataStore.chatSettings
    
    override suspend fun setReadReceiptsEnabled(enabled: Boolean) {
        settingsDataStore.setReadReceiptsEnabled(enabled)
    }
    
    override suspend fun setTypingIndicatorsEnabled(enabled: Boolean) {
        settingsDataStore.setTypingIndicatorsEnabled(enabled)
    }
    
    override suspend fun setMediaAutoDownload(setting: MediaAutoDownload) {
        settingsDataStore.setMediaAutoDownload(setting)
    }
    
    override suspend fun setChatFontScale(scale: Float) {
        settingsDataStore.setChatFontScale(scale)
    }

    override suspend fun setChatThemePreset(preset: com.synapse.social.studioasinc.domain.model.ChatThemePreset) {
        settingsDataStore.setChatThemePreset(preset)
    }

    override suspend fun setChatWallpaper(wallpaper: com.synapse.social.studioasinc.domain.model.ChatWallpaper) {
        settingsDataStore.setChatWallpaper(wallpaper)
    }


    // ========================================================================
    // Storage and Cache Management
    // Requirements: 7.2
    // ========================================================================
    
    override val cacheSize: Flow<Long> = _cacheSize.asStateFlow()
    
    /**
     * Clears the app cache and returns the amount of space freed.
     * 
     * This method clears:
     * - Internal cache directory
     * - External cache directory (if available)
     * - Code cache directory
     * 
     * @return The number of bytes freed by clearing the cache
     */
    override suspend fun clearCache(): Long {
        val sizeBefore = calculateCacheSize()
        
        try {
            // Clear internal cache
            context.cacheDir?.let { cacheDir ->
                deleteDirectory(cacheDir)
            }
            
            // Clear external cache if available
            context.externalCacheDir?.let { externalCacheDir ->
                deleteDirectory(externalCacheDir)
            }
            
            // Clear code cache
            context.codeCacheDir?.let { codeCacheDir ->
                deleteDirectory(codeCacheDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
        
        val sizeAfter = calculateCacheSize()
        val freedSpace = sizeBefore - sizeAfter
        
        // Update the cache size state
        _cacheSize.value = sizeAfter
        
        Log.d(TAG, "Cache cleared: freed ${freedSpace / 1024}KB")
        return freedSpace
    }
    
    /**
     * Calculates and returns the current cache size.
     * 
     * @return The current cache size in bytes
     */
    override suspend fun calculateCacheSize(): Long {
        var totalSize = 0L
        
        try {
            // Internal cache
            context.cacheDir?.let { cacheDir ->
                totalSize += getDirectorySize(cacheDir)
            }
            
            // External cache
            context.externalCacheDir?.let { externalCacheDir ->
                totalSize += getDirectorySize(externalCacheDir)
            }
            
            // Code cache
            context.codeCacheDir?.let { codeCacheDir ->
                totalSize += getDirectorySize(codeCacheDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size", e)
        }
        
        // Update the cache size state
        _cacheSize.value = totalSize
        
        return totalSize
    }
    
    /**
     * Recursively calculates the size of a directory.
     */
    private fun getDirectorySize(directory: File): Long {
        var size = 0L
        
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        
        return size
    }
    
    /**
     * Recursively deletes all files in a directory without deleting the directory itself.
     */
    private fun deleteDirectory(directory: File): Boolean {
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                }
                file.delete()
            }
        }
        return true
    }


    // ========================================================================
    // Data Saver Settings
    // ========================================================================
    
    override val dataSaverEnabled: Flow<Boolean> = settingsDataStore.dataSaverEnabled
    
    override suspend fun setDataSaverEnabled(enabled: Boolean) {
        settingsDataStore.setDataSaverEnabled(enabled)
    }

    override suspend fun setEnterIsSendEnabled(enabled: Boolean) {
        settingsDataStore.setEnterIsSendEnabled(enabled)
    }

    override suspend fun setMediaVisibilityEnabled(enabled: Boolean) {
        settingsDataStore.setMediaVisibilityEnabled(enabled)
    }

    override suspend fun setVoiceTranscriptsEnabled(enabled: Boolean) {
        settingsDataStore.setVoiceTranscriptsEnabled(enabled)
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        settingsDataStore.setAutoBackupEnabled(enabled)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        settingsDataStore.setRemindersEnabled(enabled)
    }

    override suspend fun setHighPriorityEnabled(enabled: Boolean) {
        settingsDataStore.setHighPriorityEnabled(enabled)
    }

    override suspend fun setReactionNotificationsEnabled(enabled: Boolean) {
        settingsDataStore.setReactionNotificationsEnabled(enabled)
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        settingsDataStore.setAppLockEnabled(enabled)
    }

    override suspend fun setChatLockEnabled(enabled: Boolean) {
        settingsDataStore.setChatLockEnabled(enabled)
    }

    // ========================================================================
    // Settings Lifecycle Management
    // Requirements: 10.3
    // ========================================================================
    
    /**
     * Clears user-specific settings while preserving device-level preferences.
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
    override suspend fun clearUserSettings() {
        settingsDataStore.clearUserSettings()
    }
    
    /**
     * Clears all settings including device-level preferences.
     * Use with caution - typically only for complete app reset.
     */
    override suspend fun clearAllSettings() {
        settingsDataStore.clearAllSettings()
    }
    
    /**
     * Restores default values for all settings.
     */
    override suspend fun restoreDefaults() {
        settingsDataStore.restoreDefaults()
    }

    override suspend fun checkForUpdates(): Result<AppUpdateInfo?> {
        return try {
            val latestVersion = SupabaseClient.client
                .from("app_versions")
                .select() {
                    order("version_code", Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<AppUpdateInfo>()

            Result.success(latestVersion)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            Result.failure(e)
        }
    }
}
