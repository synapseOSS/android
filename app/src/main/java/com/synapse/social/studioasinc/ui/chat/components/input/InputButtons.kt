package com.synapse.social.studioasinc.ui.chat.components.input

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.chat.animations.rememberSendButtonAnimationState
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors

/**
 * Animated Send / Voice Button
 * Morphs between Mic icon (when empty) and Send icon (when text exists)
 */
@Composable
fun AnimatedSendButton(
    showSend: Boolean,
    onSendClick: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationState = rememberSendButtonAnimationState(showSend)
    
    val backgroundColor by animateColorAsState(
        targetValue = if (showSend) ChatColors.SendButtonActive else ChatColors.InputBarBackground,
        animationSpec = tween(300),
        label = "sendButtonColor"
    )

    val iconColor = if (showSend) Color.White else ChatColors.SendButtonInactive

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(animationState.scale)
            .background(backgroundColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true, radius = 24.dp),
                onClick = if (showSend) onSendClick else onVoiceClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = showSend,
            animationSpec = tween(300),
            label = "sendIconCrossfade"
        ) { isSend ->
            Icon(
                imageVector = if (isSend) Icons.Rounded.Send else Icons.Rounded.Mic,
                contentDescription = if (isSend) "Send" else "Voice Message",
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(animationState.rotation)
            )
        }
    }
}

/**
 * Attachment Button with rotation on click effect
 */
@Composable
fun AttachmentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false
) {
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(200),
        label = "attachmentRotation"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Attach",
            tint = ChatColors.InputPlaceholder, // Or primary color
            modifier = Modifier
                .size(28.dp)
                .rotate(rotation)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .padding(4.dp)
        )
    }
}
