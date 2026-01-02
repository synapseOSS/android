package com.synapse.social.studioasinc.ui.chat.components.message

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.chat.DeliveryStatus
import com.synapse.social.studioasinc.ui.chat.MessagePosition
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.ui.chat.animations.messageSelectionAnimation
import com.synapse.social.studioasinc.ui.chat.theme.ChatBubbleCorners
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors
import com.synapse.social.studioasinc.ui.chat.theme.ChatSpacing
import com.synapse.social.studioasinc.ui.chat.theme.ChatTheme

/**
 * Base message bubble composable with premium styling
 * 
 * Features:
 * - Dynamic corner radius based on message grouping position
 * - Gradient background for sent messages
 * - Glassmorphism effect for received messages
 * - Selection animation support
 * - Max width constraint (75% of screen)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val isFromCurrentUser = message.isFromCurrentUser
    val isDarkTheme = isSystemInDarkTheme()
    
    // Calculate max bubble width (75% of screen)
    val configuration = LocalConfiguration.current
    val maxBubbleWidth = (configuration.screenWidthDp * 0.75f).dp
    
    // Get corner shape based on position
    val bubbleShape = getBubbleShape(message.position, isFromCurrentUser)
    
    // Get background
    val bubbleBackground = if (isFromCurrentUser) {
        Brush.linearGradient(
            colors = listOf(ChatColors.SentBubbleStart, ChatColors.SentBubbleEnd)
        )
    } else {
        SolidColor(
            if (isDarkTheme) ChatColors.ReceivedBubbleDark else ChatColors.ReceivedBubbleLight
        )
    }
    
    // Calculate vertical spacing based on position
    val topPadding = when (message.position) {
        MessagePosition.First, MessagePosition.Single -> ChatSpacing.MessageUngrouped
        MessagePosition.Middle, MessagePosition.Last -> ChatSpacing.MessageGrouped
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ChatSpacing.MessageHorizontalPadding,
                end = ChatSpacing.MessageHorizontalPadding,
                top = topPadding
            )
            .messageSelectionAnimation(message.isSelected),
        contentAlignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val clickLabel = stringResource(id = R.string.cd_message_options)
        // Wrapper for accessibility touch target size (48dp)
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(bubbleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    role = Role.Button,
                    onClickLabel = clickLabel
                )
                .animateContentSize(),
            contentAlignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            // Visual bubble (can be smaller than 48dp)
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .background(bubbleBackground, bubbleShape)
                    .padding(
                        horizontal = ChatSpacing.BubblePaddingHorizontal,
                        vertical = ChatSpacing.BubblePaddingVertical
                    )
            ) {
                content()
            }
        }
    }
}

/**
 * Get the appropriate corner shape based on message position and sender
 */
@Composable
private fun getBubbleShape(
    position: MessagePosition,
    isFromCurrentUser: Boolean
): RoundedCornerShape {
    val corners = when (position) {
        MessagePosition.Single -> ChatBubbleCorners.Single
        MessagePosition.First -> {
            if (isFromCurrentUser) ChatBubbleCorners.SentFirst else ChatBubbleCorners.ReceivedFirst
        }
        MessagePosition.Middle -> {
            if (isFromCurrentUser) ChatBubbleCorners.SentMiddle else ChatBubbleCorners.ReceivedMiddle
        }
        MessagePosition.Last -> {
            if (isFromCurrentUser) ChatBubbleCorners.SentLast else ChatBubbleCorners.ReceivedLast
        }
    }
    return corners.toShape()
}

/**
 * Sent message bubble wrapper with right alignment
 */
@Composable
fun SentMessageBubble(
    message: MessageUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    MessageBubble(
        message = message.copy(isFromCurrentUser = true),
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        content = content
    )
}

/**
 * Received message bubble wrapper with left alignment
 */
@Composable
fun ReceivedMessageBubble(
    message: MessageUiModel,
    modifier: Modifier = Modifier,
    showAvatar: Boolean = false,
    avatarUrl: String? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar space (for group chats, shown on last message of group)
        if (showAvatar) {
            Box(
                modifier = Modifier
                    .size(ChatSpacing.AvatarSize)
                    .padding(end = 8.dp)
            ) {
                // Avatar composable would go here
                // UserAvatar(url = avatarUrl, size = ChatSpacing.AvatarSize)
            }
        } else {
            // Minimal spacer for alignment when avatar not shown
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        MessageBubble(
            message = message.copy(isFromCurrentUser = false),
            modifier = Modifier.weight(1f, fill = false),
            onClick = onClick,
            onLongClick = onLongClick,
            content = content
        )
        
        // Right padding to balance
        Spacer(modifier = Modifier.width(16.dp))
    }
}

// =============================================
// BUBBLE CONTENT LAYOUTS
// =============================================

/**
 * Standard message content layout with text, time, and status
 */
@Composable
fun MessageContentLayout(
    message: MessageUiModel,
    modifier: Modifier = Modifier,
    repliedContent: @Composable (() -> Unit)? = null,
    mainContent: @Composable () -> Unit
) {
    val isFromCurrentUser = message.isFromCurrentUser
    val isDarkTheme = isSystemInDarkTheme()
    
    Column(modifier = modifier) {
        // Forwarded indicator
        if (message.forwardedFrom != null) {
            ForwardedIndicator()
        }
        
        // Reply preview
        repliedContent?.let {
            Box(modifier = Modifier.padding(bottom = 4.dp)) {
                it()
            }
        }
        
        // Main content
        mainContent()
        
        // Time and status row
        Row(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Edited indicator
            if (message.isEdited) {
                Text(
                    text = stringResource(id = R.string.msg_edited),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFromCurrentUser) {
                        ChatColors.SentBubbleSecondaryText
                    } else {
                        if (isDarkTheme) ChatColors.ReceivedBubbleSecondaryTextDark
                        else ChatColors.ReceivedBubbleSecondaryText
                    }
                )
            }
            
            // Timestamp
            Text(
                text = message.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = if (isFromCurrentUser) {
                    ChatColors.SentBubbleSecondaryText
                } else {
                    if (isDarkTheme) ChatColors.ReceivedBubbleSecondaryTextDark
                    else ChatColors.ReceivedBubbleSecondaryText
                }
            )
            
            // Delivery status (only for sent messages)
            if (isFromCurrentUser) {
                MessageStatusIcon(status = message.deliveryStatus)
            }
        }
    }
}

/**
 * Forwarded message indicator
 */
@Composable
private fun ForwardedIndicator() {
    Row(
        modifier = Modifier.padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Forward icon would go here
        Text(
            text = stringResource(id = R.string.msg_forwarded),
            style = MaterialTheme.typography.labelSmall,
            color = ChatColors.ForwardedAccent
        )
    }
}

// =============================================
// PREVIEWS
// =============================================

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun SentMessageBubblePreview() {
    MaterialTheme {
        ChatTheme {
            Column(modifier = Modifier.padding(16.dp)) {
                MessageBubble(
                    message = previewSentMessage(),
                    onClick = {},
                    onLongClick = {}
                ) {
                    Text(
                        text = "Hello! This is a sent message.",
                        color = ChatColors.SentBubbleText
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ReceivedMessageBubblePreview() {
    MaterialTheme {
        ChatTheme {
            Column(modifier = Modifier.padding(16.dp)) {
                MessageBubble(
                    message = previewReceivedMessage(),
                    onClick = {},
                    onLongClick = {}
                ) {
                    Text(
                        text = "Hi there! This is a received message.",
                        color = ChatColors.ReceivedBubbleText
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun MessageGroupPreview() {
    MaterialTheme {
        ChatTheme {
            Column(modifier = Modifier.padding(16.dp)) {
                // First message in group
                MessageBubble(
                    message = previewSentMessage(position = MessagePosition.First),
                    onClick = {},
                    onLongClick = {}
                ) {
                    Text("First message", color = ChatColors.SentBubbleText)
                }
                
                // Middle message
                MessageBubble(
                    message = previewSentMessage(position = MessagePosition.Middle),
                    onClick = {},
                    onLongClick = {}
                ) {
                    Text("Middle message", color = ChatColors.SentBubbleText)
                }
                
                // Last message
                MessageBubble(
                    message = previewSentMessage(position = MessagePosition.Last),
                    onClick = {},
                    onLongClick = {}
                ) {
                    Text("Last message", color = ChatColors.SentBubbleText)
                }
            }
        }
    }
}

// Preview helper functions
private fun previewSentMessage(
    position: MessagePosition = MessagePosition.Single
) = MessageUiModel(
    id = "1",
    content = "Hello!",
    messageType = com.synapse.social.studioasinc.ui.chat.MessageType.Text,
    senderId = "user1",
    senderName = "Me",
    senderAvatarUrl = null,
    timestamp = System.currentTimeMillis(),
    formattedTime = "12:34 PM",
    isFromCurrentUser = true,
    deliveryStatus = DeliveryStatus.Read,
    position = position
)

private fun previewReceivedMessage(
    position: MessagePosition = MessagePosition.Single
) = MessageUiModel(
    id = "2",
    content = "Hi there!",
    messageType = com.synapse.social.studioasinc.ui.chat.MessageType.Text,
    senderId = "user2",
    senderName = "John",
    senderAvatarUrl = null,
    timestamp = System.currentTimeMillis(),
    formattedTime = "12:35 PM",
    isFromCurrentUser = false,
    deliveryStatus = DeliveryStatus.Read,
    position = position
)
