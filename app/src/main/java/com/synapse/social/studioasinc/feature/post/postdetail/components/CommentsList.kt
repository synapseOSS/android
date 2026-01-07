package com.synapse.social.studioasinc.ui.postdetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.CommentAction
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator

@Composable
fun CommentsList(
    comments: LazyPagingItems<CommentWithUser>,
    repliesState: Map<String, List<CommentWithUser>> = emptyMap(),
    replyLoadingState: Set<String> = emptySet(),
    commentActionsLoading: Set<String> = emptySet(),
    onReplyClick: (CommentWithUser) -> Unit,
    onLikeClick: (String) -> Unit,
    onViewReplies: (String) -> Unit = {},
    onShowReactions: (CommentWithUser) -> Unit,
    onShowOptions: (CommentWithUser) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            headerContent()
        }

        if (comments.loadState.refresh is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                        contentAlignment = Alignment.Center
                ) {
                    ExpressiveLoadingIndicator()
                }
            }
        }

        if (comments.loadState.refresh is LoadState.Error) {
             item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error loading comments")
                }
            }
        }

        if (comments.itemCount == 0 && comments.loadState.refresh !is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No comments yet. Be the first!")
                }
            }
        }

        items(comments.itemCount) { index ->
            val comment = comments[index]
            if (comment != null) {
                CommentItem(
                    comment = comment,
                    replies = repliesState[comment.id] ?: emptyList(),
                    isRepliesLoading = replyLoadingState.contains(comment.id),
                    loadingIds = commentActionsLoading,
                    onReplyClick = onReplyClick,
                    onLikeClick = onLikeClick,
                    onViewReplies = { onViewReplies(comment.id) },
                    onShowReactions = onShowReactions,
                    onShowOptions = onShowOptions,
                    onUserClick = onUserClick,
                    modifier = Modifier // No indent for flat list
                )
            }
        }

        if (comments.loadState.append is LoadState.Loading) {
             item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveLoadingIndicator()
                }
            }
        }
    }
}
