package com.synapse.social.studioasinc.ui.chat.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.chat.MessageUiModel
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors
import com.synapse.social.studioasinc.ui.chat.theme.ChatTheme

/**
 * Voice Message Content Composable
 * Display a waveform (simulated), play/pause button, and duration
 */
@Composable
fun VoiceMessageContent(
    message: MessageUiModel,
    modifier: Modifier = Modifier,
    onPlayPauseClick: () -> Unit = {}
) {
    val voiceData = message.voiceData ?: return
    val isFromCurrentUser = message.isFromCurrentUser
    
    val playButtonColor = if (isFromCurrentUser) Color.White else ChatColors.VoicePlayButton
    val waveformColor = if (isFromCurrentUser) Color.White.copy(alpha = 0.7f) else ChatColors.VoiceWaveformActive
    val inactiveWaveformColor = if (isFromCurrentUser) Color.White.copy(alpha = 0.3f) else ChatColors.VoiceWaveformInactive
    
    MessageContentLayout(
        message = message,
        modifier = modifier,
        repliedContent = if (message.replyTo != null) {
            { ReplyPreview(replyData = message.replyTo, isFromCurrentUser = isFromCurrentUser) }
        } else null
    ) {
        Row(
            modifier = Modifier
                .width(200.dp) // Fixed width for voice messages
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFromCurrentUser) Color.White.copy(alpha = 0.2f) 
                        else ChatColors.VoicePlayButton.copy(alpha = 0.1f)
                    )
                    .clickable(onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (voiceData.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (voiceData.isPlaying) stringResource(id = R.string.cd_pause_audio) else stringResource(id = R.string.cd_play_audio),
                    tint = playButtonColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Waveform visualization
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Simulate 30 bars
                    repeat(30) { index ->
                        // Generate fake heights based on index to look like a wave
                        val height = 4.dp + (index % 5 * 3).dp
                        val color = if (index < 10) waveformColor else inactiveWaveformColor // Fake progress
                        
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(height)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Duration text
                Text(
                    text = formatDuration(voiceData.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFromCurrentUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}
