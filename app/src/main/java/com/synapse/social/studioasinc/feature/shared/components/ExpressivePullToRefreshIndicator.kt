package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Custom pull-to-refresh indicator using Material 3 LoadingIndicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressivePullToRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                val progress = state.distanceFraction
                scaleX = min(1f, progress)
                scaleY = min(1f, progress)
                alpha = min(1f, progress)
            },
        contentAlignment = Alignment.Center
    ) {
        if (isRefreshing || state.distanceFraction > 0f) {
            ExpressiveLoadingIndicator(
                color = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
