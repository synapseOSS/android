package com.synapse.social.studioasinc.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.ui.chat.AttachmentType
import com.synapse.social.studioasinc.ui.chat.MessagePosition
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.ui.chat.MessageType
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: MessageUiModel,
    onReply: (MessageUiModel) -> Unit,
    onLongClick: (MessageUiModel) -> Unit,
    onAttachmentClick: (String, AttachmentType) -> Unit,
    modifier: Modifier = Modifier
) {
    // Swipe to reply state
    var offsetX by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 60.dp.toPx() }
    val replyIconAlpha by animateFloatAsState(targetValue = if (offsetX > threshold / 2) 1f else 0f, label = "alpha")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    // Only allow dragging right (positive delta)
                    val newValue = offsetX + delta
                    if (newValue >= 0 && newValue <= threshold * 1.5) {
                        offsetX = newValue
                    }
                },
                onDragStopped = {
                    if (offsetX > threshold) {
                        onReply(message)
                    }
                    offsetX = 0f
                }
            )
    ) {
        // Reply Icon Indicator (visible when swiping)
        if (offsetX > 0) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = "Reply",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .alpha(replyIconAlpha),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Message Content with offset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = if (message.isFromCurrentUser) Arrangement.End else Arrangement.Start
        ) {
            if (!message.isFromCurrentUser) {
                // Avatar for other user
                AsyncImage(
                    model = message.senderAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .align(Alignment.Bottom),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message Bubble
            Column(
                horizontalAlignment = if (message.isFromCurrentUser) Alignment.End else Alignment.Start
            ) {
                // Reply Preview Bubble (if this message is a reply)
                message.replyTo?.let { reply ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .widthIn(max = 240.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = reply.senderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = reply.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = when {
                        message.isFromCurrentUser -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = getMessageShape(message.position, message.isFromCurrentUser),
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .combinedClickable(
                            onClick = { /* Handle click */ },
                            onLongClick = { onLongClick(message) }
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Attachments
                        message.attachments?.forEach { attachment ->
                            AttachmentView(attachment, onAttachmentClick)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Text Content
                        if (message.content.isNotEmpty()) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (message.isFromCurrentUser)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Timestamp & Status
                        Row(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message.formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentView(
    attachment: com.synapse.social.studioasinc.ui.chat.AttachmentUiModel,
    onClick: (String, AttachmentType) -> Unit
) {
    when (attachment.type) {
        AttachmentType.Image -> {
            AsyncImage(
                model = attachment.url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick(attachment.url, AttachmentType.Image) },
                contentScale = ContentScale.Crop
            )
        }
        AttachmentType.Video -> {
            // Placeholder for video thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .clickable { onClick(attachment.url, AttachmentType.Video) },
                contentAlignment = Alignment.Center
            ) {
                 AsyncImage(
                    model = attachment.thumbnailUrl, // Assuming thumbnail exists
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                // Play icon
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        else -> {
            // Document or other types
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .clickable { onClick(attachment.url, attachment.type) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = attachment.fileName ?: "Attachment",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun getMessageShape(position: MessagePosition, isCurrentUser: Boolean): RoundedCornerShape {
    val cornerRadius = 20.dp
    val smallRadius = 4.dp

    return if (isCurrentUser) {
        when (position) {
            MessagePosition.Single -> RoundedCornerShape(cornerRadius, cornerRadius, cornerRadius, cornerRadius)
            MessagePosition.First -> RoundedCornerShape(cornerRadius, cornerRadius, smallRadius, cornerRadius)
            MessagePosition.Middle -> RoundedCornerShape(cornerRadius, smallRadius, smallRadius, cornerRadius)
            MessagePosition.Last -> RoundedCornerShape(cornerRadius, smallRadius, cornerRadius, cornerRadius)
        }
    } else {
        when (position) {
            MessagePosition.Single -> RoundedCornerShape(cornerRadius, cornerRadius, cornerRadius, cornerRadius)
            MessagePosition.First -> RoundedCornerShape(cornerRadius, cornerRadius, cornerRadius, smallRadius)
            MessagePosition.Middle -> RoundedCornerShape(smallRadius, cornerRadius, cornerRadius, smallRadius)
            MessagePosition.Last -> RoundedCornerShape(smallRadius, cornerRadius, cornerRadius, cornerRadius)
        }
    }
}
