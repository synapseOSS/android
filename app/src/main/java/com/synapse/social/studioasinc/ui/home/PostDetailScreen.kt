package com.synapse.social.studioasinc.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String?,
    onBackClick: () -> Unit,
    viewModel: PostDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    androidx.compose.runtime.LaunchedEffect(postId) {
        if (postId != null) {
            viewModel.loadPost(postId)
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBackClick) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                FeedLoading()
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.error}")
                }
            } else if (uiState.post != null) {
                com.synapse.social.studioasinc.ui.components.post.PostCard(
                    state = uiState.post!!,
                    onLikeClick = { /* Handle like */ },
                    onCommentClick = { /* Handle comment */ },
                    onShareClick = { /* Handle share */ },
                    onBookmarkClick = { /* Handle bookmark */ },
                    onUserClick = { /* Handle user click */ },
                    onPostClick = { },
                    onMediaClick = { },
                    onOptionsClick = { },
                    onPollVote = { }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Post not found")
                }
            }
        }
    }
}
