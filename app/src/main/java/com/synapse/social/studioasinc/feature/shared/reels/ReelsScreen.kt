package com.synapse.social.studioasinc.feature.shared.reels

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel = viewModel(),
    onUserClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsState()
    val reels = uiState.reels.collectAsLazyPagingItems()

    // Note: VerticalPager with LazyPagingItems can be tricky.
    // We use reels.itemCount for pageCount.
    val pagerState = rememberPagerState(pageCount = { reels.itemCount })

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) { page ->
        // Accessing the item triggers Paging load
        val reel = reels[page]
        if (reel != null) {
            ReelItem(
                reel = reel,
                isActive = page == pagerState.currentPage,
                onLikeClick = { viewModel.likeReel(reel.id) },
                onCommentClick = { onCommentClick(reel.id) },
                onShareClick = { /* Share */ },
                onUserClick = { onUserClick(reel.authorUid) }
            )
        } else {
            // Handle placeholder if enablePlaceholders is true, or loading state
            // For now, we might show a loading indicator or nothing.
        }
    }
}
