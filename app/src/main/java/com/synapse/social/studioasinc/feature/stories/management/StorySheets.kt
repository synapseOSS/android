package com.synapse.social.studioasinc.feature.stories.management

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.domain.model.StoryViewWithUser
import com.synapse.social.studioasinc.domain.model.User
import java.time.Duration
import java.time.Instant

/**
 * Bottom sheet showing the list of users who viewed a story segment.
 * Only visible to the story creator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerListSheet(
    viewers: List<StoryViewWithUser>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUserClick: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Seen by ${viewers.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                viewers.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No views yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(viewers) { viewWithUser ->
                            ViewerListItem(
                                viewer = viewWithUser.viewer,
                                viewedAt = viewWithUser.storyView.viewedAt,
                                onClick = { viewWithUser.viewer?.let { onUserClick(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerListItem(
    viewer: User?,
    viewedAt: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (viewer?.avatar != null) {
            AsyncImage(
                model = viewer.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(
                        Modifier.padding(0.dp) // Placeholder background would go here
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewer?.displayName?.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = viewer?.displayName ?: viewer?.username ?: "User",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            viewer?.username?.let { username ->
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Time ago
        Text(
            text = formatTimeAgo(viewedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Bottom sheet with story options (delete, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryOptionsSheet(
    isOwnStory: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            if (isOwnStory) {
                // Options for own story
                ListItem(
                    headlineContent = { Text("Delete story") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { onDelete() }
                )
            } else {
                // Options for others' stories
                ListItem(
                    headlineContent = { Text("Mute this person's stories") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { onMute() }
                )
                
                ListItem(
                    headlineContent = { Text("Report story") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { onReport() }
                )
            }
        }
    }
}

private fun formatTimeAgo(timestamp: String?): String {
    if (timestamp == null) return ""
    
    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val duration = Duration.between(instant, now)
        
        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
            duration.toHours() < 24 -> "${duration.toHours()}h"
            else -> "${duration.toDays()}d"
        }
    } catch (e: Exception) {
        ""
    }
}
