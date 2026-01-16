package com.synapse.social.studioasinc.feature.auth.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil

/**
 * Overlay component for loading states.
 * Applies a blur effect to the content (on supported devices) and shows a centered spinner.
 * Blocks user interaction when active.
 *
 * Design tokens:
 * - Blur radius: 10dp (Android 12+)
 * - Scrim: Black with 50% opacity
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = AnimationUtil.rememberReducedMotion()

    Box(modifier = modifier.fillMaxSize()) {
        // Content with conditional blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isLoading && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                         Modifier.blur(radius = 10.dp)
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }

        // Overlay
        AnimatedVisibility(
            visible = isLoading,
            enter = if (reducedMotion) androidx.compose.animation.EnterTransition.None else fadeIn(),
            exit = if (reducedMotion) androidx.compose.animation.ExitTransition.None else fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    // Block clicks
                    .pointerInput(Unit) {}
            ) {
                ExpressiveLoadingIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
