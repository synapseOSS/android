package com.synapse.social.studioasinc.ui.chat.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.chat.animations.TypingDotsAnimation
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors

/**
 * Animated Typing Indicator Bubble
 * Shows when other user is typing
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) ChatColors.TypingBackgroundDark else ChatColors.TypingBackground
    
    // Bubble shape similar to received message (tail on left)
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomEnd = 16.dp,
        bottomStart = 4.dp
    )
    
    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        TypingDotsAnimation(
            dotColor = if (isDarkTheme) ChatColors.TypingDotLight else ChatColors.TypingDot
        )
    }
}

@Preview
@Composable
private fun TypingIndicatorPreview() {
    MaterialTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            TypingIndicator()
        }
    }
}
