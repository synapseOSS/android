package com.synapse.social.studioasinc.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.synapse.social.studioasinc.ui.components.CircularAvatar

// Simple notification model for UI
data class UiNotification(
    val id: String,
    val type: String, // like, comment, follow
    val actorName: String,
    val actorAvatar: String?,
    val message: String,
    val timestamp: String,
    val isRead: Boolean,
    val targetId: String? = null // postId or userId
)

@Composable
fun NotificationItem(
    notification: UiNotification,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularAvatar(
            imageUrl = notification.actorAvatar,
            contentDescription = "Avatar",
            size = 48.dp,
            onClick = onUserClick
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = notification.actorName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onUserClick)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = notification.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            // Unread indicator dot
             androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
