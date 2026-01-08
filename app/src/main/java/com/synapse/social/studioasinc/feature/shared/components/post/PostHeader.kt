package com.synapse.social.studioasinc.ui.components.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.ui.components.CircularAvatar
import com.synapse.social.studioasinc.ui.components.GenderBadge
import com.synapse.social.studioasinc.ui.components.VerifiedBadge

@Composable
fun PostHeader(
    user: User,
    timestamp: String,
    onUserClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularAvatar(
            imageUrl = user.avatar,
            contentDescription = "Avatar of ${user.username}",
            onClick = onUserClick,
            size = 40.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onUserClick)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.username ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.verify) {
                    VerifiedBadge()
                }
                user.gender?.let {
                     GenderBadge(gender = it)
                }
            }
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.padding(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options"
            )
        }
    }
}
