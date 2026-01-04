package com.synapse.social.studioasinc.ui.inbox.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.inbox.theme.InboxColors
import com.synapse.social.studioasinc.ui.inbox.theme.InboxDimens

/**
 * Online status indicator with optional pulse animation.
 * Shows a green dot when user is online with subtle breathing effect.
 */
@Composable
fun OnlineIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    showPulse: Boolean = true,
    size: androidx.compose.ui.unit.Dp = InboxDimens.OnlineIndicatorSize
) {
    if (!isOnline) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "onlinePulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (showPulse) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (showPulse) 0.7f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow (visible when pulsing)
        if (showPulse) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        color = InboxColors.OnlineGreen.copy(alpha = pulseAlpha * 0.3f)
                    )
            )
        }
        
        // Inner solid circle
        Box(
            modifier = Modifier
                .size(size - 2.dp)
                .clip(CircleShape)
                .background(color = InboxColors.OnlineGreen)
                .border(
                    width = InboxDimens.OnlineIndicatorBorder,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
        )
    }
}

/**
 * Story ring that wraps around an avatar.
 * Shows a gradient ring when user has an active story.
 */
@Composable
fun StoryRing(
    hasStory: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!hasStory) {
        Box(modifier = modifier) {
            content()
        }
        return
    }
    
    // Animated rotation for the gradient
    val infiniteTransition = rememberInfiniteTransition(label = "storyRing")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        InboxColors.StoryGradientStart,
                        InboxColors.StoryGradientMiddle,
                        InboxColors.StoryGradientEnd,
                        InboxColors.StoryGradientStart
                    )
                ),
                shape = CircleShape
            )
            .padding(InboxDimens.StoryRingWidth),
        contentAlignment = Alignment.Center
    ) {
        // White gap between ring and avatar
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
                .padding(2.dp)
        ) {
            content()
        }
    }
}

/**
 * Avatar with optional story ring and online indicator.
 * Combines StoryRing and OnlineIndicator around the avatar.
 */
@Composable
fun ChatAvatar(
    avatarUrl: String?,
    displayName: String,
    isOnline: Boolean,
    hasStory: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = InboxDimens.AvatarSize
) {
    Box(
        modifier = modifier.size(size + if (hasStory) InboxDimens.StoryRingWidth * 2 + 4.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        StoryRing(
            hasStory = hasStory,
            modifier = Modifier.size(size + if (hasStory) InboxDimens.StoryRingWidth * 2 + 4.dp else 0.dp)
        ) {
            // Actual avatar image
            AvatarImage(
                avatarUrl = avatarUrl,
                displayName = displayName,
                modifier = Modifier.size(size)
            )
        }
        
        // Online indicator positioned at bottom-right
        if (isOnline) {
            OnlineIndicator(
                isOnline = true,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
            )
        }
    }
}

/**
 * Simple avatar image with fallback.
 */
@Composable
private fun AvatarImage(
    avatarUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier
) {
    if (!avatarUrl.isNullOrEmpty() && avatarUrl != "null") {
        // Use Coil for image loading
        coil.compose.AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar of $displayName",
            modifier = modifier.clip(CircleShape),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            placeholder = coil.compose.rememberAsyncImagePainter(
                model = com.synapse.social.studioasinc.R.drawable.avatar
            ),
            error = coil.compose.rememberAsyncImagePainter(
                model = com.synapse.social.studioasinc.R.drawable.avatar
            )
        )
    } else {
        // Fallback avatar with initials
        AvatarFallback(
            displayName = displayName,
            modifier = modifier
        )
    }
}

/**
 * Fallback avatar showing initials.
 */
@Composable
private fun AvatarFallback(
    displayName: String,
    modifier: Modifier = Modifier
) {
    val initials = displayName
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { "?" }
    
    val backgroundColor = remember(displayName) {
        // Generate consistent color from name
        val hash = displayName.hashCode()
        val hue = (hash and 0xFF) / 255f * 360f
        Color.hsl(hue, 0.5f, 0.6f)
    }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}
