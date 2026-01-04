package com.synapse.social.studioasinc.ui.inbox.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.inbox.theme.InboxColors

/**
 * Animated typing indicator with bouncing dots.
 * Shows when the other user is typing a message.
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    dotColor: androidx.compose.ui.graphics.Color = InboxColors.TypingDot
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typingDots")
    
    // Staggered bounce animations for each dot
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0f at 0 using LinearEasing
                -5f at 150 using FastOutSlowInEasing
                0f at 300 using FastOutSlowInEasing
                0f at 900 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1Offset"
    )
    
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0f at 0 using LinearEasing
                0f at 150 using LinearEasing
                -5f at 300 using FastOutSlowInEasing
                0f at 450 using FastOutSlowInEasing
                0f at 900 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2Offset"
    )
    
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0f at 0 using LinearEasing
                0f at 300 using LinearEasing
                -5f at 450 using FastOutSlowInEasing
                0f at 600 using FastOutSlowInEasing
                0f at 900 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3Offset"
    )
    
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TypingDot(offset = dot1Offset, color = dotColor)
        TypingDot(offset = dot2Offset, color = dotColor)
        TypingDot(offset = dot3Offset, color = dotColor)
    }
}

@Composable
private fun TypingDot(
    offset: Float,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(6.dp)
            .graphicsLayer {
                translationY = offset
            }
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Inline typing text indicator.
 * Shows "typing..." with animated dots.
 */
@Composable
fun TypingText(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typingText")
    
    val dotCount by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotCount"
    )
    
    val dots = ".".repeat(dotCount.coerceIn(0, 3))
    
    Text(
        text = "typing$dots",
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier
    )
}

/**
 * Compact typing indicator for chat list items.
 * Shows just the animated dots in a row.
 */
@Composable
fun CompactTypingIndicator(
    modifier: Modifier = Modifier,
    dotColor: androidx.compose.ui.graphics.Color = InboxColors.TypingDot
) {
    val infiniteTransition = rememberInfiniteTransition(label = "compactTyping")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.3f at 0
                1f at 200
                0.3f at 400
                0.3f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1Alpha"
    )
    
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.3f at 0
                0.3f at 200
                1f at 400
                0.3f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2Alpha"
    )
    
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.3f at 0
                0.3f at 400
                1f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3Alpha"
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .graphicsLayer { alpha = dot1Alpha }
                .clip(CircleShape)
                .background(dotColor)
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .graphicsLayer { alpha = dot2Alpha }
                .clip(CircleShape)
                .background(dotColor)
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .graphicsLayer { alpha = dot3Alpha }
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}
