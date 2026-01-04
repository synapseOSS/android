package com.synapse.social.studioasinc.ui.inbox.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.animation.core.*
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapse.social.studioasinc.ui.inbox.theme.InboxColors
import com.synapse.social.studioasinc.ui.inbox.theme.InboxDimens

/**
 * Animated unread message count badge.
 * Shows a pop animation when count changes.
 */
@Composable
fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = InboxColors.UnreadAccent,
    textColor: Color = Color.White
) {
    if (count <= 0) return
    
    // Pop animation when count changes
    var previousCount by remember { mutableIntStateOf(count) }
    var animateTrigger by remember { mutableStateOf(false) }
    
    LaunchedEffect(count) {
        if (count != previousCount) {
            animateTrigger = !animateTrigger
            previousCount = count
        }
    }
    
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "badgeScale"
    )
    
    // Entrance animation
    val entranceScale by animateFloatAsState(
        targetValue = if (count > 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "entranceScale"
    )
    
    val displayText = if (count > 99) "99+" else count.toString()
    val badgeSize = if (count > 99) InboxDimens.UnreadBadgeSize + 8.dp else InboxDimens.UnreadBadgeSize
    
    Box(
        modifier = modifier
            .scale(entranceScale * scale)
            .height(InboxDimens.UnreadBadgeSize)
            .widthIn(min = badgeSize)
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = displayText,
            transitionSpec = {
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) togetherWith scaleOut(targetScale = 0.8f)
            },
            label = "badgeCount"
        ) { text ->
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MutedBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(InboxDimens.UnreadBadgeSizeSmall)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Muted",
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PinnedIndicator(
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = "Pinned",
        modifier = modifier.size(14.dp),
        tint = InboxColors.PinnedIcon
    )
}

@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = "Verified",
        modifier = modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}
