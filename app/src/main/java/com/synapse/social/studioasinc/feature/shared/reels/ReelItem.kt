package com.synapse.social.studioasinc.feature.shared.reels

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.ui.components.CircularAvatar

@Composable
fun ReelItem(
    reel: Post,
    isActive: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        VideoPlayer(
            videoUrl = reel.mediaItems?.firstOrNull()?.url ?: reel.postImage ?: "",
            isActive = isActive,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                        startY = 500f
                    )
                )
        )

        // Right side controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (reel.hasUserReacted()) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (reel.hasUserReacted()) Color.Red else Color.White
                )
            }
            Text(text = "${reel.likesCount}", color = Color.White)
            Spacer(modifier = Modifier.size(16.dp))

            IconButton(onClick = onCommentClick) {
                @Suppress("DEPRECATION")
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = "Comment",
                    tint = Color.White
                )
            }
            Text(text = "${reel.commentsCount}", color = Color.White)
            Spacer(modifier = Modifier.size(16.dp))

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.size(16.dp))

            IconButton(onClick = { /* More options */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White
                )
            }
        }

        // Bottom User Info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 20.dp, start = 16.dp, end = 80.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularAvatar(
                    imageUrl = reel.avatarUrl,
                    contentDescription = null,
                    size = 32.dp,
                    onClick = onUserClick
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = reel.username ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.clickable(onClick = onUserClick)
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            if (!reel.postText.isNullOrEmpty()) {
                Text(
                    text = reel.postText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(videoUrl) {
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ONE

        onDispose {
            exoPlayer.release()
        }
    }

    DisposableEffect(isActive) {
        if (isActive) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
        onDispose {
            exoPlayer.pause()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
    )
}
