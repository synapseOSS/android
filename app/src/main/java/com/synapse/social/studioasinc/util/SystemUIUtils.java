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

package com.synapse.social.studioasinc.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Utility class for managing system UI visibility and immersive modes.
 * Handles both legacy and modern APIs for backward compatibility.
 */
public class SystemUIUtils {

    /**
     * Enters immersive fullscreen mode, hiding status bar and navigation bar.
     * Uses modern WindowInsetsController for API 30+ and legacy method for older versions.
     * 
     * @param activity The activity to apply immersive mode to
     */
    public static void enterImmersiveMode(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Modern API (API 30+)
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.statusBars() | 
                               android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Legacy API (API 29 and below)
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.statusBars() | 
                               WindowInsetsCompat.Type.navigationBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
            
            // Additional legacy flags for older devices
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    /**
     * Exits immersive mode, showing status bar and navigation bar.
     * 
     * @param activity The activity to exit immersive mode from
     */
    public static void exitImmersiveMode(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Modern API (API 30+)
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.show(android.view.WindowInsets.Type.statusBars() | 
                               android.view.WindowInsets.Type.navigationBars());
            }
        } else {
            // Legacy API (API 29 and below)
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                controller.show(WindowInsetsCompat.Type.statusBars() | 
                               WindowInsetsCompat.Type.navigationBars());
            }
            
            // Additional legacy method
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    /**
     * Toggles between immersive and normal mode.
     * 
     * @param activity The activity to toggle immersive mode for
     * @param isImmersive Whether to enter (true) or exit (false) immersive mode
     */
    public static void toggleImmersiveMode(Activity activity, boolean isImmersive) {
        if (isImmersive) {
            enterImmersiveMode(activity);
        } else {
            exitImmersiveMode(activity);
        }
    }

    /**
     * Sets up the activity window for immersive experience.
     * Call this in onCreate() before setContentView().
     * 
     * @param activity The activity to set up
     */
    public static void setupImmersiveActivity(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false);
        
        // Set initial immersive mode
        enterImmersiveMode(activity);
    }

    /**
     * Hides only the status bar while keeping navigation bar visible.
     * 
     * @param activity The activity to hide status bar from
     */
    public static void hideStatusBar(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.statusBars());
            }
        } else {
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.statusBars());
            }
        }
    }

    /**
     * Hides only the navigation bar while keeping status bar visible.
     * 
     * @param activity The activity to hide navigation bar from
     */
    public static void hideNavigationBar(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.navigationBars());
            }
        } else {
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.navigationBars());
            }
        }
    }
}