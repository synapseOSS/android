package com.synapse.social.studioasinc.ui.chat.components.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors

/**
 * Main Chat Input Bar Component
 * Assembles attachment button, text field, and send/voice button
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onVoiceClick: () -> Unit,
    replyTo: MessageUiModel? = null,
    onClearReply: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ChatColors.InputBarBackground)
    ) {
        // Reply Preview Area
        AnimatedVisibility(
            visible = replyTo != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            if (replyTo != null) {
                ReplyBar(
                    message = replyTo,
                    onClear = onClearReply
                )
            }
        }

        // Input Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ChatColors.InputBarBackground,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // Attachment Button
                AttachmentButton(
                    onClick = onAttachmentClick,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = ChatColors.InputFieldBackground,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp), // Comfortable touch area
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Message",
                            style = TextStyle(
                                color = ChatColors.InputPlaceholder,
                                fontSize = 16.sp
                            )
                        )
                    }

                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        textStyle = TextStyle(
                            color = if (isDarkTheme) ChatColors.InputTextDark else ChatColors.InputText,
                            fontSize = 16.sp
                        ),
                        maxLines = 5,
                        cursorBrush = SolidColor(ChatColors.SentBubbleStart),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Send / Voice Button
                AnimatedSendButton(
                    showSend = text.isNotBlank(),
                    onSendClick = onSendClick,
                    onVoiceClick = onVoiceClick,
                    modifier = Modifier.padding(bottom = 0.dp) 
                )
            }
        }
    }
}
