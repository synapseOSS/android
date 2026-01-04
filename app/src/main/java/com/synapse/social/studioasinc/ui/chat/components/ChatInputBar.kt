package com.synapse.social.studioasinc.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.domain.model.SearchResult
import com.synapse.social.studioasinc.ui.components.mentions.MentionSuggestions
import com.synapse.social.studioasinc.ui.chat.theme.ChatAnimations

@Composable
fun ChatInputBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    onSendVoiceNote: (String) -> Unit = {},
    replyingTo: MessageUiModel? = null,
    onCancelReply: () -> Unit,
    typingUsers: List<String> = emptyList(),
    suggestions: List<SearchResult.User> = emptyList(),
    onInsertMention: (SearchResult.User) -> Unit = {},
    isSending: Boolean = false,  // Animation trigger
    modifier: Modifier = Modifier
) {
    var showVoiceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp) // Floating padding
    ) {
        // Typing Indicator
        AnimatedVisibility(
            visible = typingUsers.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            TypingIndicatorView(typingUsers)
        }
        
        // Mention Suggestions
        if (suggestions.isNotEmpty()) {
             MentionSuggestions(
                 suggestions = suggestions,
                 onUserSelected = onInsertMention
             )
        }

        // Reply Preview
        AnimatedVisibility(
            visible = replyingTo != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            replyingTo?.let { message ->
                ReplyPreviewBar(
                    message = message,
                    onCancel = onCancelReply
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Field
        Surface(
            shape = ShapeDefaults.ExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach Button
                IconButton(onClick = onAttachClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.action_attach),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Text Input
                Box(modifier = Modifier.weight(1f)) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = "Message...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    // Animate text fade on send
                    val textAlpha by animateFloatAsState(
                        targetValue = if (isSending) 0f else 1f,
                        animationSpec = tween(ChatAnimations.SendInputClearDuration),
                        label = "inputTextAlpha"
                    )
                    
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp),
                        maxLines = 4
                    )
                }

                // Send / Mic Button
                if (value.text.isNotBlank()) {
                    IconButton(
                        onClick = onSendClick,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(48.dp) // Enforce touch target size
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = stringResource(R.string.action_send_message),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    IconButton(onClick = { showVoiceDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.action_record_audio),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Voice Recording Dialog
        if (showVoiceDialog) {
            VoiceRecordingDialog(
                onDismiss = { showVoiceDialog = false },
                onSendVoiceNote = { audioPath ->
                    onSendVoiceNote(audioPath)
                    showVoiceDialog = false
                }
            )
        }
    }
}

@Composable
fun ReplyPreviewBar(
    message: MessageUiModel,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent Line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Replying to ${message.senderName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel_reply),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TypingIndicatorView(typingUsers: List<String>) {
    val infiniteTransition = rememberInfiniteTransition(label = "typingDots")
    
    // Create staggered animations for each dot
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )
    
    Row(
        modifier = Modifier.padding(bottom = 4.dp, start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val name = when {
            typingUsers.isEmpty() -> ""
            typingUsers.size == 1 -> typingUsers.first()
            else -> "${typingUsers.size} people"
        }
        
        if (name.isNotEmpty()) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        // Animated bouncing dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(dot1Offset, dot2Offset, dot3Offset).forEach { offset ->
                Box(
                    modifier = Modifier
                        .offset(y = offset.dp)
                        .size(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
