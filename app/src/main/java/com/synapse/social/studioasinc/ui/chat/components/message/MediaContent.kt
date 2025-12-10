package com.synapse.social.studioasinc.ui.chat.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.synapse.social.studioasinc.ui.chat.AttachmentType
import com.synapse.social.studioasinc.ui.chat.AttachmentUiModel
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.ui.chat.animations.chatShimmer

/**
 * Media message content (Images & Video thumbnails)
 * Supports:
 * - Single image
 * - Multiple images (grid layout)
 * - Video with play button
 */
@Composable
fun MediaMessageContent(
    message: MessageUiModel,
    modifier: Modifier = Modifier,
    onMediaClick: (Int) -> Unit = {}
) {
    val attachments = message.attachments ?: return
    
    MessageContentLayout(
        message = message,
        modifier = modifier,
        repliedContent = if (message.replyTo != null) {
            { ReplyPreview(replyData = message.replyTo, isFromCurrentUser = message.isFromCurrentUser) }
        } else null
    ) {
        if (attachments.size == 1) {
            SingleMediaItem(
                attachment = attachments.first(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(
                        // Try to respect aspect ratio if available, otherwise default to 4:3
                        if (attachments.first().width != null && attachments.first().height != null) {
                            attachments.first().width!!.toFloat() / attachments.first().height!!.toFloat()
                        } else {
                            1.33f
                        }
                    )
                    .clip(RoundedCornerShape(12.dp)),
                onClick = { onMediaClick(0) }
            )
        } else {
            MediaGrid(
                attachments = attachments,
                onMediaClick = onMediaClick
            )
        }
        
        // Caption text if present (appended to content of the message model)
        if (message.content.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            TextMessageContent(
                message = message.copy(attachments = null, replyTo = null) // Render just text part
            )
        }
    }
}

/**
 * Single media item (Image or Video thumbnail)
 */
@Composable
fun SingleMediaItem(
    attachment: AttachmentUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(attachment.thumbnailUrl ?: attachment.url)
                .crossfade(true)
                .build(),
            contentDescription = "Media attachment",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Show play button overlay for videos
        if (attachment.type == AttachmentType.Video) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play video",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Duration overlay
                attachment.duration?.let { durationMs ->
                    Text(
                        text = formatDuration(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        // Loading shimmer overlay
        if (attachment.isUploading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .chatShimmer()
            )
        }
    }
}

/**
 * Grid layout for multiple media items (max 4 shown, rest with +N overlay)
 */
@Composable
fun MediaGrid(
    attachments: List<AttachmentUiModel>,
    onMediaClick: (Int) -> Unit
) {
    val displayCount = minOf(attachments.size, 4)
    val hasMore = attachments.size > 4
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        val row1Count = if (displayCount <= 2) displayCount else 2
        val row2Count = if (displayCount > 2) displayCount - 2 else 0
        
        // First Row
        Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 0 until row1Count) {
                SingleMediaItem(
                    attachment = attachments[i],
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onMediaClick(i) }
                )
            }
        }
        
        if (row2Count > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            // Second Row
            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (i in 0 until row2Count) {
                    val index = i + 2
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        SingleMediaItem(
                            attachment = attachments[index],
                            modifier = Modifier.fillMaxSize(),
                            onClick = { onMediaClick(index) }
                        )
                        
                        // Show +N overlay on the last item if there are more
                        if (i == row2Count - 1 && hasMore) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { onMediaClick(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${attachments.size - 4}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format duration in mm:ss
 */
private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}
