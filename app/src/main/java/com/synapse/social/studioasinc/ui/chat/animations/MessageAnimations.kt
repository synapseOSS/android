package com.synapse.social.studioasinc.ui.chat.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.chat.theme.ChatAnimations
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors

/**
 * Premium message animations for the chat experience
 */

// =============================================
// MESSAGE ENTRANCE ANIMATIONS
// =============================================

/**
 * Animate a message bubble appearing with slide + scale + fade effect
 */
@Composable
fun Modifier.messageEntranceAnimation(
    visible: Boolean,
    isFromCurrentUser: Boolean,
    delayMillis: Int = 0
): Modifier {
    var animationPlayed by remember { mutableStateOf(false) }
    
    LaunchedEffect(visible) {
        if (visible && !animationPlayed) {
            animationPlayed = true
        }
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "messageAlpha"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "messageScale"
    )
    
    val animatedOffsetX by animateIntAsState(
        targetValue = if (animationPlayed) 0 else if (isFromCurrentUser) 50 else -50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "messageOffsetX"
    )
    
    return this
        .alpha(animatedAlpha)
        .scale(animatedScale)
        .offset { IntOffset(animatedOffsetX, 0) }
}

/**
 * Create message entrance animation for LazyColumn items
 */
fun messageEnterTransition(isFromCurrentUser: Boolean): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { if (isFromCurrentUser) it / 2 else -it / 2 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeIn(
        animationSpec = tween(300)
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessLow
        )
    )
}

/**
 * Create message exit animation
 */
fun messageExitTransition(isFromCurrentUser: Boolean): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { if (isFromCurrentUser) it / 2 else -it / 2 },
        animationSpec = tween(200)
    ) + fadeOut(
        animationSpec = tween(200)
    ) + scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(200)
    )
}

// =============================================
// SEND MESSAGE ENTRANCE ANIMATION (Per Spec)
// =============================================

/**
 * Premium send message entrance animation per spec:
 * - Fade-in + scale-up (150ms)
 * - Vertical translation from bottom (4dp float up)
 * - Custom cubic-bezier(0.25, 0.46, 0.45, 0.94) easing
 * 
 * Use on newly sent messages to create a smooth "float up" effect
 */
@Composable
fun Modifier.sendMessageEntranceAnimation(
    isNewlySent: Boolean
): Modifier {
    var hasAnimated by remember { mutableStateOf(!isNewlySent) }
    
    // Get density at composable level
    val density = androidx.compose.ui.platform.LocalDensity.current
    val offsetDp = with(density) { 4.dp.toPx() }
    
    LaunchedEffect(isNewlySent) {
        if (isNewlySent && !hasAnimated) {
            // Small delay to ensure composition is ready
            kotlinx.coroutines.delay(16)
            hasAnimated = true
        }
    }
    
    // Custom easing per spec: cubic-bezier(0.25, 0.46, 0.45, 0.94)
    val sendEasing = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)
    
    val alpha by animateFloatAsState(
        targetValue = if (hasAnimated) 1f else 0f,
        animationSpec = tween(
            durationMillis = ChatAnimations.SendBubbleEnterDuration,
            easing = sendEasing
        ),
        label = "sendMsgAlpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (hasAnimated) 1f else 0.95f,
        animationSpec = tween(
            durationMillis = ChatAnimations.SendBubbleEnterDuration,
            easing = sendEasing
        ),
        label = "sendMsgScale"
    )
    
    val offsetY by animateFloatAsState(
        targetValue = if (hasAnimated) 0f else offsetDp,
        animationSpec = tween(
            durationMillis = ChatAnimations.SendBubbleEnterDuration,
            easing = sendEasing
        ),
        label = "sendMsgOffsetY"
    )
    
    return this
        .graphicsLayer {
            this.alpha = alpha
            this.scaleX = scale
            this.scaleY = scale
            this.translationY = offsetY
        }
}

// =============================================
// TYPING INDICATOR ANIMATION
// =============================================

/**
 * Premium bouncing dots typing indicator
 */
@Composable
fun TypingDotsAnimation(
    modifier: Modifier = Modifier,
    dotColor: Color = ChatColors.TypingDot,
    dotSize: Float = 8f,
    dotCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typingDots")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(dotCount) { index ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = ChatAnimations.TypingDotDuration,
                        delayMillis = index * ChatAnimations.TypingDotDelayPerDot,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "typingDot$index"
            )
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = ChatAnimations.TypingDotDuration,
                        delayMillis = index * ChatAnimations.TypingDotDelayPerDot,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "typingDotAlpha$index"
            )
            
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .graphicsLayer {
                        translationY = offsetY
                        this.alpha = alpha
                    }
                    .background(dotColor, CircleShape)
            )
        }
    }
}

// =============================================
// SEND BUTTON ANIMATION
// =============================================

/**
 * Morphing animation state for send/voice button
 */
@Composable
fun rememberSendButtonAnimationState(showSend: Boolean): SendButtonAnimationState {
    val rotation by animateFloatAsState(
        targetValue = if (showSend) 0f else 360f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sendButtonRotation"
    )
    
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sendButtonScale"
    )
    
    return remember(rotation, scale) {
        SendButtonAnimationState(rotation, scale)
    }
}

data class SendButtonAnimationState(
    val rotation: Float,
    val scale: Float
)

// =============================================
// MESSAGE SELECTION ANIMATION
// =============================================

/**
 * Animate message selection state
 */
@Composable
fun Modifier.messageSelectionAnimation(
    isSelected: Boolean
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "selectionScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) ChatColors.SelectionOverlay else Color.Transparent,
        animationSpec = tween(ChatAnimations.SelectionScaleDuration),
        label = "selectionBackground"
    )
    
    return this
        .scale(scale)
        .background(backgroundColor)
}

// =============================================
// SCROLL TO BOTTOM FAB ANIMATION
// =============================================

/**
 * Animate scroll-to-bottom FAB visibility
 */
@Composable
fun scrollToBottomFabTransition(): Pair<EnterTransition, ExitTransition> {
    val enter = scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) + fadeIn(
        animationSpec = tween(ChatAnimations.FabScaleDuration)
    )
    
    val exit = scaleOut(
        animationSpec = tween(ChatAnimations.FabScaleDuration)
    ) + fadeOut(
        animationSpec = tween(ChatAnimations.FabScaleDuration)
    )
    
    return enter to exit
}

// =============================================
// REPLY BAR ANIMATION
// =============================================

/**
 * Reply bar enter/exit transitions
 */
fun replyBarEnterTransition(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(ChatAnimations.ReplyBarDuration)
    ) + fadeIn(
        animationSpec = tween(ChatAnimations.ReplyBarDuration)
    )
}

fun replyBarExitTransition(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(ChatAnimations.ReplyBarDuration)
    ) + fadeOut(
        animationSpec = tween(ChatAnimations.ReplyBarDuration)
    )
}

// =============================================
// VOICE WAVEFORM ANIMATION
// =============================================

/**
 * Animate voice message waveform during playback
 */
@Composable
fun rememberWaveformAnimationProgress(
    isPlaying: Boolean,
    duration: Long,
    currentPosition: Long
): Float {
    val progress by animateFloatAsState(
        targetValue = if (duration > 0) currentPosition.toFloat() / duration else 0f,
        animationSpec = tween(100),
        label = "waveformProgress"
    )
    
    return progress
}

// =============================================
// SHIMMER LOADING ANIMATION
// =============================================

/**
 * Premium shimmer effect for loading states
 */
@Composable
fun Modifier.chatShimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "chatShimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )
    
    return this.background(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset(
                translateAnim.value - 500f,
                translateAnim.value - 500f
            ),
            end = androidx.compose.ui.geometry.Offset(
                translateAnim.value,
                translateAnim.value
            )
        )
    )
}

// =============================================
// MESSAGE HIGHLIGHT ANIMATION
// =============================================

/**
 * Highlight animation when scrolling to a specific message
 */
@Composable
fun Modifier.messageHighlightAnimation(
    isHighlighted: Boolean,
    onAnimationEnd: () -> Unit = {}
): Modifier {
    var animationState by remember { mutableStateOf(0) }
    
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            animationState = 1
            kotlinx.coroutines.delay(300)
            animationState = 2
            kotlinx.coroutines.delay(300)
            animationState = 0
            onAnimationEnd()
        }
    }
    
    val backgroundColor by animateColorAsState(
        targetValue = when (animationState) {
            1, 2 -> ChatColors.SelectionOverlay.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "highlightBackground"
    )
    
    val scale by animateFloatAsState(
        targetValue = when (animationState) {
            1 -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "highlightScale"
    )
    
    return this
        .scale(scale)
        .background(backgroundColor)
}
