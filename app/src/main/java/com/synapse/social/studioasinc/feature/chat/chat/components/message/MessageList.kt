package com.synapse.social.studioasinc.ui.chat.components.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.chat.MessagePosition
import com.synapse.social.studioasinc.ui.chat.MessageType
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.ui.chat.animations.messageEnterTransition
import com.synapse.social.studioasinc.ui.chat.animations.messageExitTransition
import com.synapse.social.studioasinc.ui.chat.theme.ChatSpacing

/**
 * Main Message List Composable
 * Renders the chat history with date headers, grouping, and animations
 */
@Composable
fun MessageList(
    messages: List<MessageUiModel>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    typingUsers: List<String> = emptyList(),
    onMessageClick: (MessageUiModel) -> Unit = {},
    onMessageLongClick: (MessageUiModel) -> Unit = {},
    onReplyClick: (MessageUiModel) -> Unit = {},
    onMediaClick: (messageId: String, mediaIndex: Int) -> Unit = { _, _ -> }
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        reverseLayout = true, // Chat usually scrolls from bottom
        contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp), // Space for input bar
        verticalArrangement = Arrangement.Top
    ) {
        // Typing indicator is at the bottom (which is index 0 in reverse layout)
        if (typingUsers.isNotEmpty()) {
            item(key = "typing_indicator") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 8.dp)
                        .animateItem(fadeInSpec = null, fadeOutSpec = null) // AnimatedVisibility wrapper handled inside if needed
                ) {
                    TypingIndicator()
                    // Could add "User is typing..." text here if needed
                }
            }
        }

        // Messages
        itemsIndexed(
            items = messages,
            key = { _, msg -> msg.id },
            contentType = { _, msg -> msg.messageType }
        ) { index, message ->
            // Date Header (Reverse layout: Check if date changed from next item (which is strictly older))
            // BUT wait, if we reverse the list in UI, we should receive the list in reverse chronological order (newest first)?
            // Assuming `messages` is ordered Newest -> Oldest for ReverseLayout.
            
            val isLastItem = index == messages.lastIndex
            val nextMessage = if (!isLastItem) messages[index + 1] else null
            
            // Logic: inner composable handles standard bubble wrapping
            // We just need to determine if we show date header *above* this message (which means below in code because reverse?)
            // No, in reverse layout:
            // Item 0 (Newest)
            // Item 1
            // ...
            // Item N (Oldest)
            
            // Date header should appear "above" a group of messages from the same day.
            // In reverse list, "above" means *after* the last message of that day (which is earlier index? No wait).
            // Visual:
            // [Date Today]  <-- This item comes AFTER the messages of today in a reverse column
            // Msg 1 (10:00 PM)
            // Msg 2 (9:00 PM)
            
            // So we need to check if the NEXT message (index + 1, older) has a different date.
            
            val showDateHeader = shouldShowDateHeader(message, nextMessage)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = null
                    ) // Modifier.animateItemPlacement() equivalent
            ) {
                Column {
                    // Actual message content
                    MessageItem(
                        message = message,
                        onMessageClick = { onMessageClick(message) },
                        onLongClick = { onMessageLongClick(message) },
                        onReplyClick = { onReplyClick(message) },
                        onMediaClick = { index -> onMediaClick(message.id, index) }
                    )
                    
                    if (showDateHeader) {
                        DateHeader(date = message.dateHeaderText ?: "Date")
                    }
                }
            }
        }
    }
}

/**
 * Message Item Dispatcher
 * Selects the content type to render
 */
@Composable
fun MessageItem(
    message: MessageUiModel,
    onMessageClick: () -> Unit,
    onLongClick: () -> Unit,
    onReplyClick: () -> Unit,
    onMediaClick: (Int) -> Unit
) {
    // Determine which bubble wrapper to use
    if (message.isFromCurrentUser) {
        SentMessageBubble(
            message = message,
            onClick = onMessageClick,
            onLongClick = onLongClick
        ) {
            MessageContentDispatcher(
                message = message,
                onMediaClick = onMediaClick
            )
        }
    } else {
        ReceivedMessageBubble(
            message = message,
            showAvatar = false, // Disabled avatar completely
            avatarUrl = message.senderAvatarUrl,
            onClick = onMessageClick,
            onLongClick = onLongClick
        ) {
            MessageContentDispatcher(
                message = message,
                onMediaClick = onMediaClick
            )
        }
    }
}

@Composable
fun MessageContentDispatcher(
    message: MessageUiModel,
    onMediaClick: (Int) -> Unit
) {
    when {
        message.isDeleted -> DeletedMessageContent(message)
        message.messageType == MessageType.Text -> TextMessageContent(message)
        message.messageType == MessageType.Image -> MediaMessageContent(message, onMediaClick = onMediaClick)
        message.messageType == MessageType.Video -> VideoMessageContent(message, onVideoClick = { onMediaClick(0) })
        message.messageType == MessageType.Voice -> VoiceMessageContent(message)
        else -> TextMessageContent(message) // Fallback
    }
}

private fun shouldShowDateHeader(current: MessageUiModel, next: MessageUiModel?): Boolean {
    if (next == null) return true // Show for oldest message
    return current.showDateHeader // Use pre-calculated flag if available, valid assumption from Plan
    // Or compare dates here if needed. Assuming ViewModel handles it.
}
