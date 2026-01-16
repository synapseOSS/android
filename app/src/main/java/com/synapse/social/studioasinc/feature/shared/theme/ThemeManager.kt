package com.synapse.social.studioasinc.ui.theme

import android.app.Activity
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.synapse.social.studioasinc.ui.settings.ThemeMode

/**
 * Manages app-wide theme application for both XML and Compose layouts.
 * 
 * This singleton applies theme settings to the entire app, not just individual screens.
 * It handles:
 * - System-wide dark/light mode via AppCompatDelegate
 * - Activity-specific theme application
 * - Dynamic color support (Android 12+)
 */
object ThemeManager {
    
    /**
     * Applies the theme mode to the entire app.
     * 
     * @param themeMode The theme mode to apply (Light, Dark, or System)
     */
    fun applyThemeMode(themeMode: ThemeMode) {
        val nightMode = when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                } else {
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                }
            }
        }
        
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
    
    /**
     * Applies dynamic color theme to an activity (Android 12+ only).
     * 
     * @param activity The activity to apply dynamic color to
     * @param enabled Whether dynamic color should be enabled
     */
    fun applyDynamicColor(activity: Activity, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && enabled) {
            // Dynamic color is automatically applied by Material 3 when using
            // dynamicDarkColorScheme/dynamicLightColorScheme in SynapseTheme
            // This method is a placeholder for future enhancements
        }
    }
}
