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
import com.synapse.social.studioasinc.model.CommentWithUser
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.ui.components.CircularAvatar
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.util.TimeUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentItem(
    comment: CommentWithUser,
    replies: List<CommentWithUser> = emptyList(),
    isRepliesLoading: Boolean = false,
    onReplyClick: (CommentWithUser) -> Unit, // Changed to pass the comment being replied to
    onLikeClick: (String) -> Unit, // Changed to pass ID
    onShowReactions: (CommentWithUser) -> Unit,
    onShowOptions: (CommentWithUser) -> Unit,
    onUserClick: (String) -> Unit,
    onViewReplies: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            CircularAvatar(
                imageUrl = comment.user?.profileImageUrl,
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
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                            onClick = { onLikeClick(comment.id) },
                            onLongClick = { onShowReactions(comment) }
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
                    ExpressiveLoadingIndicator(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(24.dp)
                    )
                }
            }

            IconButton(
                onClick = { onShowOptions(comment) },
                modifier = Modifier.size(24.dp)
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
                    replies = emptyList(), // Max depth 1 for now (replies to replies shown flat or not supported yet in UI tree)
                    isRepliesLoading = false,
                    onReplyClick = onReplyClick, // Reply to the reply (which replies to parent usually, or nested)
                    onLikeClick = onLikeClick,
                    onShowReactions = onShowReactions,
                    onShowOptions = onShowOptions,
                    onUserClick = onUserClick,
                    modifier = Modifier.padding(start = 48.dp) // Indent replies
                )
            }
        }
    }
}
