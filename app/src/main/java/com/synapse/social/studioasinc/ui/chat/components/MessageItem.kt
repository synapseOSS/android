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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.ui.chat.AttachmentType
import com.synapse.social.studioasinc.ui.chat.MessagePosition
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.ui.chat.MessageType
import com.synapse.social.studioasinc.ui.chat.DeliveryStatus
import com.synapse.social.studioasinc.ui.components.mentions.MentionTextFormatter
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.roundToInt

/**
 * Color for selected message overlay (#E0F7FA)
 */
private val SelectionOverlayColor = Color(0xFFE0F7FA)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: MessageUiModel,
    isSelectionMode: Boolean,
    onSelect: (MessageUiModel) -> Unit,
    onReply: (MessageUiModel) -> Unit,
    onLongClick: (MessageUiModel) -> Unit,
    onAttachmentClick: (String, AttachmentType) -> Unit,
    onProfileClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Swipe to reply state
    var offsetX by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 60.dp.toPx() }
    val replyIconAlpha by animateFloatAsState(targetValue = if (offsetX > threshold / 2) 1f else 0f, label = "alpha")
    
    var showMentionDialogForUser by remember { mutableStateOf<String?>(null) }
    
    if (showMentionDialogForUser != null) {
        AlertDialog(
            onDismissRequest = { showMentionDialogForUser = null },
            title = { Text("Open Profile") },
            text = { Text("Are you sure you want to open the account @${showMentionDialogForUser}?") },
            confirmButton = {
                TextButton(onClick = {
                    onProfileClick(showMentionDialogForUser!!)
                    showMentionDialogForUser = null
                }) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMentionDialogForUser = null }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                contentDescription = stringResource(R.string.action_reply),
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
                .padding(horizontal = 8.dp, vertical = 2.dp),
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
                Spacer(modifier = Modifier.width(4.dp))
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

                // Selection indicator row wrapper
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkmark for selection mode
                    if (isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (message.isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                // Empty circle placeholder
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.size(24.dp)
                                ) {}
                            }
                        }
                    }

                Surface(
                    color = when {
                        message.isSelected -> SelectionOverlayColor
                        message.isFromCurrentUser -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = getMessageShape(message.position, message.isFromCurrentUser),
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .combinedClickable(
                            onClick = { 
                                if (isSelectionMode) onSelect(message) 
                                // else normal click handler if needed
                            },
                            onLongClick = { 
                                if (!isSelectionMode) onLongClick(message)
                                else onSelect(message)
                            }
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
                            val textColor = if (message.isFromCurrentUser)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                            
                            val pillColor = if(message.isFromCurrentUser) 
                                MaterialTheme.colorScheme.surface.copy(alpha=0.3f) 
                            else 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.3f)

                            // Get colors outside remember block (Composable context)
                            val mentionColor = MaterialTheme.colorScheme.primary
                            val annotatedText = remember(message.content, mentionColor, pillColor) {
                                MentionTextFormatter.buildMentionText(
                                    text = message.content,
                                    mentionColor = mentionColor,
                                    pillColor = pillColor
                                )
                            }
                            
                            // Use BasicText with pointerInput to allow long-press to propagate
                            // This fixes the issue where ClickableText consumes long-press events
                            var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
                            
                            androidx.compose.foundation.text.BasicText(
                                text = annotatedText,
                                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                                onTextLayout = { textLayoutResult = it },
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            // Only handle taps on mentions, don't consume long-press
                                            textLayoutResult?.let { layoutResult ->
                                                val position = layoutResult.getOffsetForPosition(offset)
                                                val annotations = annotatedText.getStringAnnotations(
                                                    tag = "MENTION", 
                                                    start = position, 
                                                    end = position
                                                )
                                                if (annotations.isNotEmpty()) {
                                                    showMentionDialogForUser = annotations.first().item
                                                } else if (isSelectionMode) {
                                                    // Forward tap to parent for selection
                                                    onSelect(message)
                                                }
                                            }
                                        },
                                        onLongPress = {
                                            // Forward long-press to parent handler
                                            if (!isSelectionMode) {
                                                onLongClick(message)
                                            } else {
                                                onSelect(message)
                                            }
                                        }
                                    )
                                }
                            )
                        }

                        // Link Preview
                        message.linkPreview?.let { preview ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    preview.imageUrl?.let { imgUrl ->
                                        AsyncImage(
                                            model = imgUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 120.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    preview.title?.let { title ->
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    preview.description?.let { desc ->
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = preview.domain,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Timestamp & Status
                        Row(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = message.formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                            )
                            
                            // Read Receipt Ticks (only for current user's messages)
                            if (message.isFromCurrentUser) {
                                val (icon, tint) = when (message.deliveryStatus) {
                                    DeliveryStatus.Sending -> Icons.Default.Schedule to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    DeliveryStatus.Sent -> Icons.Default.Done to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    DeliveryStatus.Delivered -> Icons.Default.DoneAll to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    DeliveryStatus.Read -> Icons.Default.DoneAll to MaterialTheme.colorScheme.primary
                                    DeliveryStatus.Failed -> Icons.Default.Schedule to MaterialTheme.colorScheme.error
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = message.deliveryStatus.name,
                                    modifier = Modifier.size(14.dp),
                                    tint = tint
                                )
                            }
                        }
                    }
                }
                } // Close Row wrapper for selection indicator
            }
        }
    }
}

@Composable
fun AttachmentView(
    attachment: com.synapse.social.studioasinc.ui.chat.AttachmentUiModel,
    onClick: (String, AttachmentType) -> Unit
) {
    // DEBUG: Log attachment rendering
    android.util.Log.d("MessageItem", "=== RENDERING ATTACHMENT ===")
    android.util.Log.d("MessageItem", "Attachment type: ${attachment.type}")
    android.util.Log.d("MessageItem", "Attachment URL: ${attachment.url}")
    android.util.Log.d("MessageItem", "Thumbnail URL: ${attachment.thumbnailUrl}")
    
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
                    contentDescription = stringResource(R.string.cd_play_video),
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
                    .heightIn(min = 48.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { onClick(attachment.url, attachment.type) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = stringResource(R.string.cd_media_attachment)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = attachment.fileName ?: stringResource(R.string.label_attachment),
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
