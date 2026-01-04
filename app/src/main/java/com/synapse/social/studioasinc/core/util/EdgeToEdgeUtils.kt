package com.synapse.social.studioasinc.core.util

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Utility functions for handling edge-to-edge display and status bar colors in Compose.
 */
object EdgeToEdgeUtils {
    
    /**
     * Sets up edge-to-edge display with appropriate status bar colors.
     * Should be called in Compose content to automatically adjust status bar colors
     * based on the current theme.
     */
    @Composable
    fun SetupEdgeToEdge(
        colorScheme: ColorScheme,
        darkTheme: Boolean
    ) {
        val view = LocalView.current
        
        LaunchedEffect(colorScheme, darkTheme) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            
            // Enable edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Set status bar color to transparent for edge-to-edge effect
            window.statusBarColor = Color.Transparent.toArgb()
            
            // Set navigation bar color to transparent for edge-to-edge effect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.navigationBarColor = Color.Transparent.toArgb()
            }
            
            // Configure status bar content color (light/dark icons)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            
            // Configure navigation bar content color (light/dark icons)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    
    /**
     * Sets up edge-to-edge display for an Activity.
     * Call this in onCreate() before setContent().
     */
    fun setupEdgeToEdgeActivity(activity: Activity) {
        val window = activity.window
        
        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Set transparent status and navigation bars
        window.statusBarColor = Color.Transparent.toArgb()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }
    
    /**
     * Updates status bar appearance based on theme.
     * Use this when theme changes dynamically.
     */
    fun updateStatusBarAppearance(activity: Activity, darkTheme: Boolean) {
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        
        // Set status bar icons to be dark on light theme, light on dark theme
        insetsController.isAppearanceLightStatusBars = !darkTheme
        
        // Set navigation bar icons to be dark on light theme, light on dark theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
