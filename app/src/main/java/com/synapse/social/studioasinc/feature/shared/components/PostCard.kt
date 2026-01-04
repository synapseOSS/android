package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.Post

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    isSaved: Boolean,
    onUserClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onMenuClick: () -> Unit,
    onMediaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            PostHeader(
                username = post.username ?: "Unknown",
                avatarUrl = post.avatarUrl,
                isVerified = post.isVerified,
                timestamp = formatTimestamp(post.publishDate),
                onUserClick = onUserClick,
                onMenuClick = onMenuClick
            )
            
            PostContent(
                text = post.postText,
                mediaItems = post.mediaItems,
                onMediaClick = { onMediaClick() }
            )
            
            PostActionBar(
                isLiked = isLiked,
                isSaved = isSaved,
                likesCount = post.likesCount,
                commentsCount = post.commentsCount,
                onLikeClick = onLikeClick,
                onCommentClick = onCommentClick,
                onShareClick = onShareClick,
                onSaveClick = onSaveClick,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

private fun formatTimestamp(timestamp: String?): String {
    return timestamp ?: "Just now"
}

@Preview
@Composable
private fun PostCardPreview() {
    MaterialTheme {
        PostCard(
            post = Post(
                id = "1",
                authorUid = "user1",
                postText = "This is a sample post",
                username = "john_doe",
                isVerified = true,
                likesCount = 42,
                commentsCount = 8
            ),
            isLiked = false,
            isSaved = false,
            onUserClick = {},
            onLikeClick = {},
            onCommentClick = {},
            onShareClick = {},
            onSaveClick = {},
            onMenuClick = {},
            onMediaClick = {}
        )
    }
}
