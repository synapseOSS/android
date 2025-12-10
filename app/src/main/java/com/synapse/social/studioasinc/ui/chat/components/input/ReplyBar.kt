package com.synapse.social.studioasinc.ui.chat.components.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.ui.chat.components.message.ReplyPreview
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors

/**
 * Reply preview bar shown above the input field
 */
@Composable
fun ReplyBar(
    message: MessageUiModel,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ChatColors.InputBarBackground) // Same as input bar or slightly different
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // We reuse the ReplyPreview component but style it for the input area
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(
                            color = ChatColors.ReplyAccent,
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Replying to ${message.senderName ?: "Unknown"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ChatColors.ReplyAccent
                    )
                    Text(
                        text = message.content, // Should handle media types description too if needed
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Cancel reply",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
