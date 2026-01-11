package com.synapse.social.studioasinc.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.StoryWithUser
import com.synapse.social.studioasinc.ui.components.post.PostActions
import com.synapse.social.studioasinc.ui.components.post.SharedPostItem
import com.synapse.social.studioasinc.ui.components.post.PostOptionsBottomSheet
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.feature.stories.tray.StoryTray
import com.synapse.social.studioasinc.feature.stories.tray.StoryTrayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel(),
    storyTrayViewModel: StoryTrayViewModel = hiltViewModel(),
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onMediaClick: (Int) -> Unit,
    onStoryClick: (String) -> Unit = { _ -> },
    onAddStoryClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsState()
    val posts = viewModel.posts.collectAsLazyPagingItems()
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    
    // Story tray state
    val storyTrayState by storyTrayViewModel.storyTrayState.collectAsState()
    val currentUser by storyTrayViewModel.currentUser.collectAsState()

    var isUserRefreshing by remember { mutableStateOf(false) }
    
    LaunchedEffect(posts.loadState.refresh) {
        if (posts.loadState.refresh !is LoadState.Loading) {
            isUserRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isUserRefreshing,
        onRefresh = {
            isUserRefreshing = true
            posts.refresh()
            viewModel.refresh()
            storyTrayViewModel.refresh()
        },
        indicator = {
            ExpressiveLoadingIndicator(
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = contentPadding
            ) {
                // Story Tray at the top
                item(key = "story_tray") {
                    StoryTray(
                        currentUser = currentUser,
                        myStory = storyTrayState.myStory,
                        friendStories = storyTrayState.friendStories,
                        onMyStoryClick = {
                            storyTrayState.myStory?.let { myStory ->
                                onStoryClick(myStory.user.uid)
                            }
                        },
                        onAddStoryClick = onAddStoryClick,
                        onStoryClick = { storyWithUser ->
                            onStoryClick(storyWithUser.user.uid)
                        },
                        isLoading = storyTrayState.isLoading
                    )
                }
                
                // Posts
                items(
                    count = posts.itemCount,
                    key = posts.itemKey { it.id },
                    contentType = posts.itemContentType { "post" }
                ) { index ->
                    val post = posts[index]
                    if (post != null) {
                        SharedPostItem(
                            post = post,
                            postViewStyle = uiState.postViewStyle,
                            actions = PostActions(
                                onLike = { viewModel.likePost(post) },
                                onComment = { onCommentClick(post.id) },
                                onShare = { viewModel.sharePost(post) },
                                onBookmark = { viewModel.bookmarkPost(post) },
                                onOptionClick = { selectedPost = post },
                                onPollVote = { p, idx -> viewModel.votePoll(p, idx) },
                                onUserClick = { onUserClick(post.authorUid) },
                                onMediaClick = onMediaClick
                            )
                        )
                    }
                }

                if (posts.loadState.append is LoadState.Loading) {
                    item { PostShimmer() }
                }

                if (posts.loadState.append is LoadState.Error) {
                    item {
                        val e = posts.loadState.append as LoadState.Error
                        FeedError(
                            message = "Error loading more",
                            onRetry = { posts.retry() },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
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
