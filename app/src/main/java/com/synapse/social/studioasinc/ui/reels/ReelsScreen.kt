package com.synapse.social.studioasinc.ui.reels

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

@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel = viewModel(),
    onUserClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { uiState.reels.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) { page ->
        val reel = uiState.reels.getOrNull(page)
        if (reel != null) {
            ReelItem(
                reel = reel,
                isActive = page == pagerState.currentPage,
                onLikeClick = { viewModel.likeReel(reel.id) },
                onCommentClick = { onCommentClick(reel.id) },
                onShareClick = { /* Share */ },
                onUserClick = { onUserClick(reel.authorUid) }
            )
        }

        // Load more when reaching end
        androidx.compose.runtime.LaunchedEffect(page) {
            if (page >= uiState.reels.size - 2) {
                 viewModel.loadMoreReels()
            }
        }
    }
}
