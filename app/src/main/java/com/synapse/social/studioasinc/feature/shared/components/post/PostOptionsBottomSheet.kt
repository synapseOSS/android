package com.synapse.social.studioasinc.ui.components.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.Post

data class PostOption(
    val label: String,
    val iconRes: Int,
    val isDangerous: Boolean = false,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    post: Post,
    isOwner: Boolean,
    commentsDisabled: Boolean = false,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onBookmark: () -> Unit,
    onReshare: () -> Unit = {},
    onToggleComments: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onRevokeVote: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Quick Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAction(
                    icon = R.drawable.ic_forward, // Using forward icon for reshare
                    label = "Reshare",
                    onClick = {
                        onReshare()
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = R.drawable.ic_send,
                    label = "Share",
                    onClick = {
                        onShare()
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = R.drawable.ic_link,
                    label = "Copy Link",
                    onClick = {
                        onCopyLink()
                        onDismiss()
                    }
                )
                QuickAction(
                    icon = R.drawable.ic_bookmark,
                    label = "Save",
                    onClick = {
                        onBookmark()
                        onDismiss()
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Options List
            LazyColumn {
                items(buildOptions(
                    isOwner = isOwner,
                    commentsDisabled = commentsDisabled,
                    post = post,
                    onEdit = { onEdit(); onDismiss() },
                    onDelete = { showDeleteDialog = true },
                    onToggleComments = { onToggleComments(); onDismiss() },
                    onReport = { onReport(); onDismiss() },
                    onBlock = { onBlock(); onDismiss() },
                    onShare = { onShare(); onDismiss() },
                    onRevokeVote = { onRevokeVote(); onDismiss() }
                )) { option ->
                    OptionItem(option = option)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuickAction(
    icon: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun OptionItem(option: PostOption) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = option.action)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(option.iconRes),
            contentDescription = option.label,
            tint = if (option.isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (option.isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun buildOptions(
    isOwner: Boolean,
    commentsDisabled: Boolean,
    post: Post,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComments: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onShare: () -> Unit,
    onRevokeVote: () -> Unit
): List<PostOption> {
    val options = mutableListOf<PostOption>()

    if (isOwner) {
        options.add(PostOption("Edit", R.drawable.ic_edit_note_48px, action = onEdit))
        options.add(PostOption(
            if (commentsDisabled) "Turn on commenting" else "Turn off commenting",
            R.drawable.ic_comments_disabled,
            action = onToggleComments
        ))
        options.add(PostOption("Delete", R.drawable.ic_delete_48px, isDangerous = true, action = onDelete))
    } else {
        options.add(PostOption("Report", R.drawable.ic_report_48px, isDangerous = true, action = onReport))
        options.add(PostOption("Block", R.drawable.mobile_block_24px, isDangerous = true, action = onBlock))
    }

    if (post.userPollVote != null) {
        options.add(PostOption("Revoke vote", R.drawable.ic_delete_48px, action = onRevokeVote))
    }

    options.add(PostOption("Share via...", R.drawable.ic_send, action = onShare))

    return options
}
