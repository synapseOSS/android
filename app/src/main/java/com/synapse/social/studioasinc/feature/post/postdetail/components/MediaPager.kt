package com.synapse.social.studioasinc.ui.postdetail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.components.post.MediaContent

@Composable
fun MediaPager(
    mediaUrls: List<String>,
    isVideo: Boolean,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    MediaContent(
        mediaUrls = mediaUrls,
        isVideo = isVideo,
        onMediaClick = onMediaClick,
        modifier = modifier
    )
}
