package com.synapse.social.studioasinc.ui.chat.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.chat.DeliveryStatus
import com.synapse.social.studioasinc.ui.chat.MessagePosition
import com.synapse.social.studioasinc.ui.chat.MessageType
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors
import com.synapse.social.studioasinc.ui.chat.theme.ChatTheme

/**
 * Text message content composable
 * 
 * Features:
 * - Selectable text for copy functionality
 * - Proper text styling based on sender
 * - Emoji-only detection for larger font
 * - Link highlighting support (future)
 */
@Composable
fun TextMessageContent(
    message: MessageUiModel,
    modifier: Modifier = Modifier
) {
    val isFromCurrentUser = message.isFromCurrentUser
    val isDarkTheme = isSystemInDarkTheme()
    
    val textColor = if (isFromCurrentUser) {
        ChatColors.SentBubbleText
    } else {
        if (isDarkTheme) ChatColors.ReceivedBubbleTextDark else ChatColors.ReceivedBubbleText
    }
    
    // Check if message is emoji-only for larger font
    val isEmojiOnly = isEmojiOnlyMessage(message.content)
    val textStyle = if (isEmojiOnly) {
        MaterialTheme.typography.headlineLarge
    } else {
        MaterialTheme.typography.bodyLarge
    }
    
    MessageContentLayout(
        message = message,
        modifier = modifier,
        repliedContent = if (message.replyTo != null) {
            { ReplyPreview(replyData = message.replyTo, isFromCurrentUser = isFromCurrentUser) }
        } else null
    ) {
        SelectionContainer {
            Text(
                text = message.content,
                style = textStyle,
                color = textColor,
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Visible
            )
        }
    }
}

/**
 * Check if a message contains only emojis
 */
private fun isEmojiOnlyMessage(text: String): Boolean {
    if (text.isBlank()) return false
    if (text.length > 10) return false // Limit for emoji-only detection
    
    // Simple emoji detection - can be enhanced with Unicode ranges
    val emojiRegex = Regex("[\\p{So}\\p{Cn}]+")
    val withoutSpaces = text.replace(" ", "")
    return withoutSpaces.matches(emojiRegex) || 
           withoutSpaces.all { it.isSurrogate() || matchesEmoji(it.code) }
}

/**
 * Extension to check if a character is an emoji
 */
private fun matchesEmoji(codePoint: Int): Boolean {
    return when (codePoint) {
        in 0x1F600..0x1F64F, // Emoticons
        in 0x1F300..0x1F5FF, // Misc Symbols and Pictographs
        in 0x1F680..0x1F6FF, // Transport and Map
        in 0x1F1E0..0x1F1FF, // Flags
        in 0x2600..0x26FF,   // Misc symbols
        in 0x2700..0x27BF,   // Dingbats
        in 0xFE00..0xFE0F,   // Variation Selectors
        in 0x1F900..0x1F9FF, // Supplemental Symbols and Pictographs
        in 0x1FA00..0x1FA6F, // Chess Symbols
        in 0x1FA70..0x1FAFF  // Symbols and Pictographs Extended-A
        -> true
        else -> false
    }
}

/**
 * Deleted message content placeholder
 */
@Composable
fun DeletedMessageContent(
    message: MessageUiModel,
    modifier: Modifier = Modifier
) {
    val isFromCurrentUser = message.isFromCurrentUser
    val isDarkTheme = isSystemInDarkTheme()
    
    val textColor = if (isFromCurrentUser) {
        ChatColors.SentBubbleSecondaryText
    } else {
        if (isDarkTheme) ChatColors.ReceivedBubbleSecondaryTextDark 
        else ChatColors.ReceivedBubbleSecondaryText
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Deleted icon
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Filled.Close,
            contentDescription = "Deleted message",
            modifier = Modifier.size(16.dp),
            tint = textColor
        )
        
        Text(
            text = "This message was deleted",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            ),
            color = textColor
        )
    }
}

/**
 * Reply preview shown above message content
 */
@Composable
fun ReplyPreview(
    replyData: com.synapse.social.studioasinc.ui.chat.ReplyPreviewData,
    isFromCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    val backgroundColor = if (isFromCurrentUser) {
        ChatColors.SentBubbleStart.copy(alpha = 0.3f)
    } else {
        if (isDarkTheme) ChatColors.ReplyAccent.copy(alpha = 0.2f)
        else ChatColors.ReplyAccentLight
    }
    
    val accentColor = if (isFromCurrentUser) {
        ChatColors.SentBubbleText.copy(alpha = 0.8f)
    } else {
        ChatColors.ReplyAccent
    }
    
    val textColor = if (isFromCurrentUser) {
        ChatColors.SentBubbleText
    } else {
        if (isDarkTheme) ChatColors.ReceivedBubbleTextDark else ChatColors.ReceivedBubbleText
    }
    
    val secondaryTextColor = if (isFromCurrentUser) {
        ChatColors.SentBubbleSecondaryText
    } else {
        if (isDarkTheme) ChatColors.ReceivedBubbleSecondaryTextDark 
        else ChatColors.ReceivedBubbleSecondaryText
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(
                    color = accentColor,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Sender name
            Text(
                text = replyData.senderName,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Message preview
            Text(
                text = when (replyData.messageType) {
                    MessageType.Image -> "📷 Photo"
                    MessageType.Video -> "🎥 Video"
                    MessageType.Voice -> "🎵 Voice message"
                    MessageType.File -> "📎 File"
                    else -> replyData.content
                },
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Thumbnail for media replies
        replyData.thumbnailUrl?.let { url ->
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
            ) {
                // Image loading would go here
                // AsyncImage(url = url, ...)
            }
        }
    }
}

// =============================================
// PREVIEWS
// =============================================

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun TextMessagePreview() {
    MaterialTheme {
        ChatTheme {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sent message
                MessageBubble(
                    message = previewTextMessage(isFromCurrentUser = true),
                    onClick = {},
                    onLongClick = {}
                ) {
                    TextMessageContent(
                        message = previewTextMessage(isFromCurrentUser = true)
                    )
                }
                
                // Received message
                MessageBubble(
                    message = previewTextMessage(isFromCurrentUser = false),
                    onClick = {},
                    onLongClick = {}
                ) {
                    TextMessageContent(
                        message = previewTextMessage(isFromCurrentUser = false)
                    )
                }
                
                // Emoji only message
                MessageBubble(
                    message = previewEmojiMessage(),
                    onClick = {},
                    onLongClick = {}
                ) {
                    TextMessageContent(
                        message = previewEmojiMessage()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ReplyPreviewDemo() {
    MaterialTheme {
        ChatTheme {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReplyPreview(
                    replyData = com.synapse.social.studioasinc.ui.chat.ReplyPreviewData(
                        messageId = "1",
                        senderName = "John",
                        content = "This is the message being replied to",
                        messageType = MessageType.Text
                    ),
                    isFromCurrentUser = false
                )
                
                ReplyPreview(
                    replyData = com.synapse.social.studioasinc.ui.chat.ReplyPreviewData(
                        messageId = "2",
                        senderName = "You",
                        content = "",
                        messageType = MessageType.Image
                    ),
                    isFromCurrentUser = true
                )
            }
        }
    }
}

private fun previewTextMessage(
    isFromCurrentUser: Boolean
) = MessageUiModel(
    id = "1",
    content = "Hey! How are you doing today? I hope you're having a great day!",
    messageType = MessageType.Text,
    senderId = if (isFromCurrentUser) "me" else "other",
    senderName = if (isFromCurrentUser) "Me" else "John",
    senderAvatarUrl = null,
    timestamp = System.currentTimeMillis(),
    formattedTime = "12:34 PM",
    isFromCurrentUser = isFromCurrentUser,
    deliveryStatus = DeliveryStatus.Read,
    isEdited = false
)

private fun previewEmojiMessage() = MessageUiModel(
    id = "2",
    content = "😊👍",
    messageType = MessageType.Text,
    senderId = "me",
    senderName = "Me",
    senderAvatarUrl = null,
    timestamp = System.currentTimeMillis(),
    formattedTime = "12:35 PM",
    isFromCurrentUser = true,
    deliveryStatus = DeliveryStatus.Read
)


