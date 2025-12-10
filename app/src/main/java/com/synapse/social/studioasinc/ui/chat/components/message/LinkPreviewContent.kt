package com.synapse.social.studioasinc.ui.chat.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.synapse.social.studioasinc.ui.chat.LinkPreviewData
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors

/**
 * Link Preview Content Composable
 * Renders a rich preview for URLs shared in chat
 */
@Composable
fun LinkPreviewContent(
    message: MessageUiModel,
    modifier: Modifier = Modifier
) {
    val linkPreview = message.linkPreview ?: return
    val uriHandler = LocalUriHandler.current
    val isDarkTheme = isSystemInDarkTheme()

    MessageContentLayout(
        message = message,
        modifier = modifier,
        repliedContent = if (message.replyTo != null) {
            { ReplyPreview(replyData = message.replyTo, isFromCurrentUser = message.isFromCurrentUser) }
        } else null
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Main text content (the URL itself or message)
            TextMessageContent(
                message = message.copy(replyTo = null, linkPreview = null) // Avoid recursion
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // The Preview Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable { uriHandler.openUri(linkPreview.url) }
            ) {
                // Preview Image
                if (linkPreview.imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(linkPreview.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Link preview image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
                
                // Preview Text Info
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    if (!linkPreview.title.isNullOrBlank()) {
                        Text(
                            text = linkPreview.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isDarkTheme) ChatColors.ReceivedBubbleTextDark else ChatColors.ReceivedBubbleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    if (!linkPreview.description.isNullOrBlank()) {
                        Text(
                            text = linkPreview.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkTheme) ChatColors.ReceivedBubbleSecondaryTextDark else ChatColors.ReceivedBubbleSecondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    Text(
                        text = linkPreview.domain,
                        style = MaterialTheme.typography.labelSmall,
                        color = ChatColors.LinkPreviewAccent.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
