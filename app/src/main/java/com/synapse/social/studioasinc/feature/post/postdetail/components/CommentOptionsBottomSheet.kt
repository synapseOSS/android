package com.synapse.social.studioasinc.ui.postdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.CommentAction
import com.synapse.social.studioasinc.domain.model.CommentWithUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentOptionsBottomSheet(
    comment: CommentWithUser,
    isOwnComment: Boolean,
    isPostAuthor: Boolean,
    onDismiss: () -> Unit,
    onAction: (CommentAction) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {

            ListItem(
                headlineContent = { Text("Reply") },
                leadingContent = {
                    Icon(
                        painterResource(R.drawable.ic_reply),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.clickable {
                    onAction(CommentAction.Reply(comment.id, comment.userId))
                    onDismiss()
                }
            )

            ListItem(
                headlineContent = { Text("Copy") },
                leadingContent = {
                    Icon(
                        painterResource(R.drawable.ic_content_copy_48px),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.clickable {
                    onAction(CommentAction.Copy(comment.content))
                    onDismiss()
                }
            )

            ListItem(
                headlineContent = { Text("Share") },
                leadingContent = {
                    Icon(
                        painterResource(R.drawable.ic_send),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.clickable {
                    onAction(CommentAction.Share(comment.id, comment.content, comment.postId))
                    onDismiss()
                }
            )

            if (isOwnComment && !comment.isDeleted) {
                ListItem(
                    headlineContent = { Text("Edit") },
                    leadingContent = {
                        Icon(
                            painterResource(R.drawable.ic_edit_note_48px),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.clickable {
                        onAction(CommentAction.Edit(comment.id, comment.content))
                        onDismiss()
                    }
                )
            }

            if (isPostAuthor && !comment.isDeleted) {
                ListItem(
                    headlineContent = { Text(if (comment.isPinned) "Unpin" else "Pin") },
                    leadingContent = {
                        Icon(
                            painterResource(R.drawable.ic_bookmark),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.clickable {
                        onAction(CommentAction.Pin(comment.id, comment.postId))
                        onDismiss()
                    }
                )
            }

            if (!isOwnComment) {
                 ListItem(
                    headlineContent = { Text("Report") },
                    leadingContent = {
                        Icon(
                            painterResource(R.drawable.ic_report_48px),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.clickable {
                        onAction(CommentAction.Report(comment.id, "spam", null))
                        onDismiss()
                    }
                )
            }

            if (isOwnComment) {
                ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Icon(
                            painterResource(R.drawable.ic_delete_48px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.clickable {
                        onAction(CommentAction.Delete(comment.id))
                        onDismiss()
                    }
                )
            }
        }
    }
}
