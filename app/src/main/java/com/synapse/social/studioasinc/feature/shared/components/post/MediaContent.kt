package com.synapse.social.studioasinc.ui.components.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import com.synapse.social.studioasinc.ui.settings.PostViewStyle

@Composable
fun MediaContent(
    mediaUrls: List<String>,
    isVideo: Boolean,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    postViewStyle: PostViewStyle = PostViewStyle.SWIPE
) {
    if (mediaUrls.isEmpty()) return

    if (mediaUrls.size == 1) {
        val url = mediaUrls.first()
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onMediaClick(0) }
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Post Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isVideo) 16f / 9f else 4f / 3f) // Adjust aspect ratio as needed
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            if (isVideo) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Video",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    } else if (postViewStyle == PostViewStyle.GRID) {
        // Grid Layout logic
        PostMediaGrid(mediaUrls, onMediaClick, modifier)
    } else {
        // Horizontal Pager for multiple images
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { mediaUrls.size })
        Box(modifier = modifier.fillMaxWidth()) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMediaClick(page) }
                        .padding(vertical = 8.dp)
                ) {
                    AsyncImage(
                        model = mediaUrls[page],
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(if (isVideo) 16f / 9f else 4f / 3f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Page Indicator
            if (mediaUrls.size > 1) {
                androidx.compose.material3.Text(
                    text = "${pagerState.currentPage + 1}/${mediaUrls.size}",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun PostMediaGrid(
    mediaUrls: List<String>,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = mediaUrls.size
    val spacing = 2.dp
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        when (count) {
            2 -> {
                Row(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    MediaGridItem(url = mediaUrls[0], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(0) })
                    Spacer(modifier = Modifier.width(spacing))
                    MediaGridItem(url = mediaUrls[1], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(1) })
                }
            }
            3 -> {
                // One big top, two small bottom
                MediaGridItem(url = mediaUrls[0], modifier = Modifier.fillMaxWidth().height(300.dp), onClick = { onMediaClick(0) })
                Spacer(modifier = Modifier.height(spacing))
                Row(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    MediaGridItem(url = mediaUrls[1], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(1) })
                    Spacer(modifier = Modifier.width(spacing))
                    MediaGridItem(url = mediaUrls[2], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(2) })
                }
            }
            else -> {
                // 4 or more: 2x2 grid
                Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    MediaGridItem(url = mediaUrls[0], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(0) })
                    Spacer(modifier = Modifier.width(spacing))
                    MediaGridItem(url = mediaUrls[1], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(1) })
                }
                Spacer(modifier = Modifier.height(spacing))
                Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    MediaGridItem(url = mediaUrls[2], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(2) })
                    Spacer(modifier = Modifier.width(spacing))
                    
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        MediaGridItem(url = mediaUrls[3], modifier = Modifier.fillMaxSize(), onClick = { onMediaClick(3) })
                        if (count > 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { onMediaClick(3) }, // Usually clicking "more" opens the gallery at that index
                                contentAlignment = Alignment.Center
                            ) {
                                // Show "+N" where N is the number of items remaining
                                // For 5 items: indices 0,1,2 are shown, slot [3] shows item 3 with overlay "+2" (items 4 and 5 remaining)
                                Text(
                                    text = "+${count - 3}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaGridItem(
    url: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AsyncImage(
        model = url,
        contentDescription = "Post Image",
        modifier = modifier
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}
