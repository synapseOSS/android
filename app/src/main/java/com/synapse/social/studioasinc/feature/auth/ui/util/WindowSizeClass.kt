package com.synapse.social.studioasinc.feature.auth.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size classes for adaptive layouts.
 */
sealed class WindowWidthSizeClass {
    object Compact : WindowWidthSizeClass()
    object Medium : WindowWidthSizeClass()
    object Expanded : WindowWidthSizeClass()
}

/**
 * Calculates the window size class based on screen width.
 */
@Composable
fun calculateWindowSizeClass(): WindowWidthSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    return remember(screenWidth) {
        when {
            screenWidth < 600.dp -> WindowWidthSizeClass.Compact
            screenWidth < 840.dp -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        }
    }
}
