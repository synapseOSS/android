/**
 * CONFIDENTIAL AND PROPRIETARY
 *
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 *
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc.core.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Utility class for managing system UI visibility and immersive modes.
 * Handles both legacy and modern APIs for backward compatibility.
 */
object SystemUIUtils {

    /**
     * Enters immersive fullscreen mode, hiding status bar and navigation bar.
     * Uses modern WindowInsetsController for API 30+ and legacy method for older versions.
     *
     * @param activity The activity to apply immersive mode to
     */
    fun enterImmersiveMode(activity: Activity?) {
        if (activity == null) return

        val window = activity.window ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Modern API (API 30+)
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.statusBars() or
                               android.view.WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Legacy API (API 29 and below)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars() or
                           WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Additional legacy flags for older devices
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    /**
     * Exits immersive mode, showing status bar and navigation bar.
     *
     * @param activity The activity to exit immersive mode from
     */
    fun exitImmersiveMode(activity: Activity?) {
        if (activity == null) return

        val window = activity.window ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Modern API (API 30+)
            val controller = window.insetsController
            if (controller != null) {
                controller.show(android.view.WindowInsets.Type.statusBars() or
                               android.view.WindowInsets.Type.navigationBars())
            }
        } else {
            // Legacy API (API 29 and below)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.statusBars() or
                           WindowInsetsCompat.Type.navigationBars())

            // Additional legacy method
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    /**
     * Toggles between immersive and normal mode.
     *
     * @param activity The activity to toggle immersive mode for
     * @param isImmersive Whether to enter (true) or exit (false) immersive mode
     */
    fun toggleImmersiveMode(activity: Activity?, isImmersive: Boolean) {
        if (isImmersive) {
            enterImmersiveMode(activity)
        } else {
            exitImmersiveMode(activity)
        }
    }

    /**
     * Sets up the activity window for immersive experience.
     * Call this in onCreate() before setContentView().
     *
     * @param activity The activity to set up
     */
    fun setupImmersiveActivity(activity: Activity?) {
        if (activity == null) return

        val window = activity.window ?: return

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set initial immersive mode
        enterImmersiveMode(activity)
    }

    /**
     * Hides only the status bar while keeping navigation bar visible.
     *
     * @param activity The activity to hide status bar from
     */
    fun hideStatusBar(activity: Activity?) {
        if (activity == null) return

        val window = activity.window ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.statusBars())
            }
        } else {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    /**
     * Hides only the navigation bar while keeping status bar visible.
     *
     * @param activity The activity to hide navigation bar from
     */
    fun hideNavigationBar(activity: Activity?) {
        if (activity == null) return

        val window = activity.window ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.navigationBars())
            }
        } else {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
