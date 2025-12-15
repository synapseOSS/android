package com.synapse.social.studioasinc.ui.postdetail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.PostDetailViewModel

@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: PostDetailViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    Text("Post Detail Screen - Under Construction")
}
