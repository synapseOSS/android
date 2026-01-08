package com.synapse.social.studioasinc.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Utility object for handling window insets in edge-to-edge layouts.
 * Provides consistent padding values for status bar and navigation bar areas.
 */
object EdgeToEdgeUtils {
    
    /**
     * Returns the system bars insets as padding values.
     * Use this to add padding to your content to avoid overlap with system bars.
     */
    @Composable
    fun systemBarsPadding() = WindowInsets.systemBars.asPaddingValues()
    
    /**
     * Returns the top inset (status bar height) as a Dp value.
     */
    @Composable
    fun statusBarHeight(): Dp = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    
    /**
     * Returns the bottom inset (navigation bar height) as a Dp value.
     */
    @Composable
    fun navigationBarHeight(): Dp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
}
