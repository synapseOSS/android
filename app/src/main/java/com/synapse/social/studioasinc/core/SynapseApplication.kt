package com.synapse.social.studioasinc.core

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.remote.services.AuthDevelopmentUtils
import com.synapse.social.studioasinc.core.util.MediaCacheCleanupManager
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.ui.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class SynapseApplication : Application() {
    
    private lateinit var mediaCacheCleanupManager: MediaCacheCleanupManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize authentication service
        SupabaseAuthenticationService.initialize(this)
        
        // Initialize background maintenance services
        initializeMaintenanceServices()
        
        // Apply saved theme on app startup
        applyThemeOnStartup()
        
        // Log authentication configuration in development builds
        if (AuthDevelopmentUtils.isDevelopmentBuild()) {
            AuthDevelopmentUtils.logAuthConfig(this)
        }
    }
    
    private fun applyThemeOnStartup() {
        val settingsRepository = SettingsRepositoryImpl.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                settingsRepository.appearanceSettings.collect { settings ->
                    ThemeManager.applyThemeMode(settings.themeMode)
                }
            } catch (e: Exception) {
                android.util.Log.e("SynapseApplication", "Failed to apply theme on startup", e)
            }
        }
    }
    
    private fun initializeMaintenanceServices() {
        // Initialize media cache cleanup
        mediaCacheCleanupManager = MediaCacheCleanupManager(this)
        mediaCacheCleanupManager.initialize()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Clean up maintenance services
        if (::mediaCacheCleanupManager.isInitialized) {
            mediaCacheCleanupManager.shutdown()
        }
    }
}
