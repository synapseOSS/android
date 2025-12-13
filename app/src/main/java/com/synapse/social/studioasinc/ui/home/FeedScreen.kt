package com.synapse.social.studioasinc.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.ui.components.post.PostCard
import com.synapse.social.studioasinc.ui.components.post.PostOptionsBottomSheet
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onMediaClick: (Int) -> Unit // Added missing parameter
) {
    val uiState by viewModel.uiState.collectAsState()
    val posts = viewModel.posts.collectAsLazyPagingItems()
    var selectedPost by remember { mutableStateOf<Post?>(null) }

    val isRefreshing = posts.loadState.refresh is LoadState.Loading
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = {
            posts.refresh()
            viewModel.refresh()
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (posts.loadState.refresh is LoadState.Loading && posts.itemCount == 0) {
                FeedLoading()
            } else if (posts.loadState.refresh is LoadState.Error) {
                val e = posts.loadState.refresh as LoadState.Error
                FeedError(
                    message = e.error.localizedMessage ?: "Unknown error",
                    onRetry = { posts.retry() }
                )
        } else if (posts.itemCount == 0 && posts.loadState.refresh is LoadState.NotLoading) {
            FeedEmpty()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = posts.itemCount,
                    key = posts.itemKey { it.id },
                    contentType = posts.itemContentType { "post" }
                ) { index ->
                    val post = posts[index]
                    if (post != null) {
                        PostCard(
                            state = viewModel.mapPostToState(post),
                            onLikeClick = { viewModel.likePost(post) },
                            onCommentClick = { onCommentClick(post.id) },
                            onShareClick = { viewModel.sharePost(post) },
                            onBookmarkClick = { viewModel.bookmarkPost(post) },
                            onUserClick = { onUserClick(post.authorUid) },
                            onPostClick = { onPostClick(post.id) },
                            onMediaClick = onMediaClick,
                            onOptionsClick = { selectedPost = post },
                            onPollVote = { optionId -> viewModel.votePoll(post, optionId.toIntOrNull() ?: 0) },
                            onReactionSelected = { reaction -> viewModel.reactToPost(post, reaction) }
                        )
                    }
                }

                if (posts.loadState.append is LoadState.Loading) {
                    item { PostShimmer() }
                }

                if (posts.loadState.append is LoadState.Error) {
                    item {
                         // Small retry button at bottom
                        val e = posts.loadState.append as LoadState.Error
                        FeedError(
                            message = "Error loading more",
                            onRetry = { posts.retry() },
                            modifier = Modifier.fillMaxWidth().height(100.dp) // Restrict height for list footer
                        )
                    }
                }
            }
        }
    }
    }

    selectedPost?.let { post ->
        PostOptionsBottomSheet(
            post = post,
            isOwner = viewModel.isPostOwner(post),
            commentsDisabled = viewModel.areCommentsDisabled(post),
            onDismiss = { selectedPost = null },
            onEdit = { viewModel.editPost(post) },
            onDelete = { viewModel.deletePost(post) },
            onShare = { viewModel.sharePost(post) },
            onCopyLink = { viewModel.copyPostLink(post) },
            onBookmark = { viewModel.bookmarkPost(post) },
            onToggleComments = { viewModel.toggleComments(post) },
            onReport = { viewModel.reportPost(post) },
            onBlock = { viewModel.blockUser(post.authorUid) },
            onRevokeVote = { viewModel.revokeVote(post) }
        )
    }
}
