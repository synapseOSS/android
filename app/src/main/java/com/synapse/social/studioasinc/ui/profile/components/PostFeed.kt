package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostFeed(
    posts: List<Post>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    currentUserId: String,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onUserClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onReportPost: (String, String) -> Unit,
    onEditPost: (String) -> Unit,
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        if (posts.isEmpty() && !isLoading) {
            EmptyPostsState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        isLiked = post.id in likedPostIds,
                        isSaved = post.id in savedPostIds,
                        onUserClick = { onUserClick(post.authorUid) },
                        onLikeClick = { onLikeClick(post.id) },
                        onCommentClick = { onCommentClick(post.id) },
                        onShareClick = {
                            selectedPostId = post.id
                            showShareSheet = true
                        },
                        onSaveClick = { onSaveClick(post.id) },
                        onMenuClick = {
                            selectedPostId = post.id
                            showMenuSheet = true
                        },
                        onMediaClick = { onMediaClick(post.id) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
    
    // Share bottom sheet
    if (showShareSheet && selectedPostId != null) {
        SharePostBottomSheet(
            onDismiss = { showShareSheet = false },
            onCopyLink = { onShareClick(selectedPostId!!) },
            onShareToStory = { /* TODO: Implement */ },
            onShareViaMessage = { /* TODO: Implement */ },
            onShareExternal = { /* TODO: Implement */ }
        )
    }
    
    // Menu bottom sheet
    if (showMenuSheet && selectedPostId != null) {
        val post = posts.find { it.id == selectedPostId }
        val isOwnPost = post?.authorUid == currentUserId
        
        PostMenuBottomSheet(
            isOwnPost = isOwnPost,
            onDismiss = { showMenuSheet = false },
            onEdit = { onEditPost(selectedPostId!!) },
            onDelete = { onDeletePost(selectedPostId!!) },
            onReport = { showReportDialog = true }
        )
    }
    
    // Report dialog
    if (showReportDialog && selectedPostId != null) {
        ReportPostDialog(
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                onReportPost(selectedPostId!!, reason)
                showReportDialog = false
                showMenuSheet = false
            }
        )
    }
}

@Composable
private fun EmptyPostsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No posts yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
