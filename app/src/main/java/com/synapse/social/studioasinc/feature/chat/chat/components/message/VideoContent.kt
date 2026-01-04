package com.synapse.social.studioasinc.ui.chat.components.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.synapse.social.studioasinc.ui.chat.MessageUiModel

/**
 * Video Message Content Composable
 * Wraps Video display logic. Usually delegates to MediaContent but kept for completeness
 */
@Composable
fun VideoMessageContent(
    message: MessageUiModel,
    modifier: Modifier = Modifier,
    onVideoClick: () -> Unit = {}
) {
    // In our design, MediaMessageContent handles both, so this is a wrapper
    // or specific implementation for standalone video messages
    MediaMessageContent(
        message = message,
        modifier = modifier,
        onMediaClick = { onVideoClick() }
    )
}
