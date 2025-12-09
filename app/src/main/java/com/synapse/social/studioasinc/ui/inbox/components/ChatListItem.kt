package com.synapse.social.studioasinc.ui.inbox.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.inbox.models.ChatItemUiModel
import com.synapse.social.studioasinc.ui.inbox.theme.InboxColors
import com.synapse.social.studioasinc.ui.inbox.theme.InboxDimens
import com.synapse.social.studioasinc.ui.inbox.theme.InboxShapes

/**
 * Premium chat list item with all visual features.
 * 
 * Features:
 * - Avatar with story ring and online indicator
 * - Display name with verified badge
 * - Last message preview (or typing indicator)
 * - Relative time badge
 * - Unread count with animation
 * - Muted/Pinned indicators
 * - Press scale animation
 * - Long press support
 */
@Composable
fun ChatListItem(
    chat: ChatItemUiModel,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Press scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(
            durationMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "pressScale"
    )
    
    // Background color for pinned items
    val backgroundColor by animateColorAsState(
        targetValue = if (chat.isPinned) {
            InboxColors.PinnedBackground.copy(alpha = 0.5f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "backgroundColor"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(InboxShapes.ChatItemCard)
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(color = MaterialTheme.colorScheme.primary),
                    onClick = onClick
                )
                .padding(InboxDimens.ChatItemPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with story ring and online indicator
            ChatAvatar(
                avatarUrl = chat.avatarUrl,
                displayName = chat.displayName,
                isOnline = chat.isOnline,
                hasStory = chat.hasStory,
                size = InboxDimens.AvatarSize
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content column (name, message)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                // Name row with verified badge
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (chat.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedBadge()
                    }
                    
                    if (chat.isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        PinnedIndicator()
                    }
                }
                
                Spacer(modifier = Modifier.height(InboxDimens.ChatItemVerticalSpacing))
                
                // Last message or typing indicator
                if (chat.isTyping) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactTypingIndicator()
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "typing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = chat.getMessagePreview(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = if (chat.unreadCount > 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Right column (time, badges)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Time
                Text(
                    text = chat.getFormattedTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (chat.unreadCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Badges row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (chat.isMuted) {
                        MutedBadge()
                        if (chat.unreadCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                    
                    if (chat.unreadCount > 0) {
                        UnreadBadge(count = chat.unreadCount)
                    }
                }
            }
        }
    }
}

/**
 * Shimmer loading placeholder for chat list item.
 */
@Composable
fun ChatListItemShimmer(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(InboxDimens.ChatItemPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar shimmer
        com.synapse.social.studioasinc.ui.components.ShimmerCircle(
            size = InboxDimens.AvatarSize
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content shimmer
        Column(
            modifier = Modifier.weight(1f)
        ) {
            com.synapse.social.studioasinc.ui.components.ShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            com.synapse.social.studioasinc.ui.components.ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Time shimmer
        com.synapse.social.studioasinc.ui.components.ShimmerBox(
            modifier = Modifier
                .width(40.dp)
                .height(12.dp)
        )
    }
}
