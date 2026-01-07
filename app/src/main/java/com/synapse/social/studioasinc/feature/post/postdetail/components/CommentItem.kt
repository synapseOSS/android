package com.synapse.social.studioasinc.ui.postdetail.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.ui.components.CircularAvatar
import com.synapse.social.studioasinc.core.util.TimeUtils
import com.synapse.social.studioasinc.ui.components.mentions.MentionTextFormatter
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentItem(
    comment: CommentWithUser,
    replies: List<CommentWithUser> = emptyList(),
    isRepliesLoading: Boolean = false,
    loadingIds: Set<String> = emptySet(),
    onReplyClick: (CommentWithUser) -> Unit,
    onLikeClick: (String) -> Unit,
    onShowReactions: (CommentWithUser) -> Unit,
    onShowOptions: (CommentWithUser) -> Unit,
    onUserClick: (String) -> Unit,
    onViewReplies: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLoading = loadingIds.contains(comment.id)
    var showMentionDialogForUser by remember { mutableStateOf<String?>(null) }
    
    if (showMentionDialogForUser != null) {
        AlertDialog(
            onDismissRequest = { showMentionDialogForUser = null },
            title = { Text("Open Profile") },
            text = { Text("Are you sure you want to open the account @${showMentionDialogForUser}?") },
            confirmButton = {
                TextButton(onClick = {
                    onUserClick(showMentionDialogForUser!!)
                    showMentionDialogForUser = null
                }) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMentionDialogForUser = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            CircularAvatar(
                imageUrl = comment.user?.avatar,
                contentDescription = "Avatar",
                size = 32.dp,
                onClick = { comment.userId?.let { onUserClick(it) } }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.user?.username ?: "User",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { comment.userId?.let { onUserClick(it) } }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = TimeUtils.getTimeAgo(comment.createdAt ?: "") ?: "Just now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                val mentionColor = MaterialTheme.colorScheme.primary
                val pillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    val annotatedText = remember(comment.content, mentionColor, pillColor) {
                        MentionTextFormatter.buildMentionText(
                            text = comment.content,
                            mentionColor = mentionColor,
                            pillColor = pillColor
                        )
                    }
                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clickable { 
                                // Handle mention clicks if needed
                            }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onReplyClick(comment) }
                            .padding(end = 16.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.combinedClickable(
                            onClick = { if (!isLoading) onLikeClick(comment.id) },
                            onLongClick = { if (!isLoading) onShowReactions(comment) }
                        )
                    ) {
                        val userReaction = comment.userReaction
                        if (userReaction != null) {
                            Image(
                                painter = painterResource(id = userReaction.iconRes),
                                contentDescription = userReaction.displayName,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (userReaction == ReactionType.LIKE) "Like" else userReaction.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                        } else {
                            Icon(
                                imageVector = Icons.Default.ThumbUpOffAlt,
                                contentDescription = "Like",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Like",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (comment.likesCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = comment.likesCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Replies Section
                if (comment.repliesCount > 0 && replies.isEmpty() && !isRepliesLoading) {
                    Text(
                        text = "View ${comment.repliesCount} replies",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable(onClick = onViewReplies)
                    )
                }

                 if (isRepliesLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            IconButton(
                onClick = { onShowOptions(comment) },
                modifier = Modifier.size(24.dp),
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Render nested replies
        if (replies.isNotEmpty()) {
            replies.forEach { reply ->
                CommentItem(
                    comment = reply,
                    replies = emptyList(),
                    isRepliesLoading = false,
                    loadingIds = loadingIds,
                    onReplyClick = onReplyClick,
                    onLikeClick = onLikeClick,
                    onShowReactions = onShowReactions,
                    onShowOptions = onShowOptions,
                    onUserClick = onUserClick,
                    modifier = Modifier.padding(start = 48.dp)
                )
            }
        }
    }
}
