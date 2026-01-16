package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onActionClick) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun EmptyPostsState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.PostAdd,
        title = "No posts yet",
        message = "When you share posts, they'll appear here",
        modifier = modifier
    )
}

@Composable
fun EmptyPhotosState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.PhotoLibrary,
        title = "No photos to show",
        message = "Photos you share will appear here",
        modifier = modifier
    )
}

@Composable
fun EmptyReelsState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.VideoLibrary,
        title = "No reels yet",
        message = "Reels you create will appear here",
        modifier = modifier
    )
}

@Composable
fun EmptyFollowingState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.PersonAdd,
        title = "Not following anyone",
        message = "Find people to follow and connect with",
        modifier = modifier
    )
}
