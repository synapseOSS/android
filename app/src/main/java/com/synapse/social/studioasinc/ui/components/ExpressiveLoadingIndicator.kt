package com.synapse.social.studioasinc.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A custom Expressive Loading Indicator.
 * This implements a 3-dot pulsating animation which is "expressive" and matches modern UI standards,
 * replacing the standard CircularProgressIndicator for indeterminate loading states.
 *
 * It avoids relying on `androidx.compose.material3.LoadingIndicator` which may be unavailable
 * or have version conflicts.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotSize: androidx.compose.ui.unit.Dp = 12.dp,
    spacing: androidx.compose.ui.unit.Dp = 6.dp,
    animationDelay: Int = 100
) {
    val dots = listOf(
        remember { Animatable(0f) },
        remember { Animatable(0f) },
        remember { Animatable(0f) }
    )

    dots.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(index * animationDelay.toLong())
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.0f at 0 with LinearOutSlowInEasing
                        1.0f at 300 with LinearOutSlowInEasing
                        0.0f at 600 with LinearOutSlowInEasing
                        0.0f at 1200 with LinearOutSlowInEasing
                    },
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        dots.forEachIndexed { index, animatable ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(spacing))
            }
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale = 0.5f + (animatable.value * 0.5f)) // Scale between 0.5 and 1.0
                    .background(
                        color = color.copy(alpha = 0.3f + (animatable.value * 0.7f)), // Fade opacity
                        shape = CircleShape
                    )
            )
        }
    }
}
