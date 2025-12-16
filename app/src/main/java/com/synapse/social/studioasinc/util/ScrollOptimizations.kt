package com.synapse.social.studioasinc.util

import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Scroll performance optimizations for chat screens
 */
object ScrollOptimizations {
    
    /**
     * Optimized scroll-to-bottom detection with debouncing
     */
    @Composable
    fun rememberScrollToBottomState(
        listState: LazyListState,
        threshold: Int = 3
    ): State<Boolean> {
        return remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > threshold
            }
        }
    }
    
    /**
     * Smooth scroll animation spec optimized for chat
     */
    fun chatScrollAnimationSpec(): AnimationSpec<Float> = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )
    
    /**
     * Entrance animation spec for new messages
     */
    fun messageEntranceAnimationSpec(): AnimationSpec<Float> = tween(
        durationMillis = 150,
        easing = FastOutSlowInEasing
    )
    
    /**
     * Item placement animation for smooth reordering
     */
    fun itemPlacementAnimationSpec(): FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    /**
     * Debounced scroll position tracking
     */
    @Composable
    fun rememberDebouncedScrollPosition(
        listState: LazyListState,
        debounceMs: Long = 100L
    ): State<Int> {
        val scrollPosition = remember { mutableIntStateOf(0) }
        
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { position ->
                    kotlinx.coroutines.delay(debounceMs)
                    scrollPosition.intValue = position
                }
        }
        
        return scrollPosition
    }
    
    /**
     * Optimized content padding calculation
     */
    @Composable
    fun rememberOptimizedContentPadding(
        inputBarHeight: Dp,
        additionalBottomPadding: Dp = 16.dp,
        topPadding: Dp = 16.dp
    ): androidx.compose.foundation.layout.PaddingValues {
        return remember(inputBarHeight, additionalBottomPadding, topPadding) {
            androidx.compose.foundation.layout.PaddingValues(
                bottom = inputBarHeight + additionalBottomPadding,
                top = topPadding
            )
        }
    }
    
    /**
     * Performance-optimized message visibility tracking
     */
    @Composable
    fun rememberVisibleMessageRange(
        listState: LazyListState,
        totalMessages: Int
    ): IntRange {
        return remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                
                // Add buffer for smooth scrolling
                val bufferSize = 5
                val start = maxOf(0, firstVisible - bufferSize)
                val end = minOf(totalMessages - 1, lastVisible + bufferSize)
                
                start..end
            }
        }.value
    }
}

/**
 * Extension functions for LazyListState optimization
 */
suspend fun LazyListState.animateScrollToItemOptimized(
    index: Int,
    scrollOffset: Int = 0
) {
    animateScrollToItem(
        index = index,
        scrollOffset = scrollOffset
    )
}

/**
 * Composable for handling scroll performance in chat lists
 */
@Composable
fun rememberChatScrollBehavior(
    listState: LazyListState,
    messages: List<Any>,
    autoScrollThreshold: Int = 2
): ChatScrollBehavior {
    val isNearBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex <= autoScrollThreshold
        }
    }
    
    val shouldAutoScroll by remember {
        derivedStateOf {
            isNearBottom && messages.isNotEmpty()
        }
    }
    
    return remember(listState, shouldAutoScroll) {
        ChatScrollBehavior(
            listState = listState,
            isNearBottom = isNearBottom,
            shouldAutoScroll = shouldAutoScroll
        )
    }
}

data class ChatScrollBehavior(
    val listState: LazyListState,
    val isNearBottom: Boolean,
    val shouldAutoScroll: Boolean
) {
    suspend fun scrollToBottom(animated: Boolean = true) {
        if (animated) {
            listState.animateScrollToItemOptimized(0)
        } else {
            listState.scrollToItem(0)
        }
    }
}

/**
 * Performance monitoring for scroll operations
 */
object ScrollPerformanceMonitor {
    private var scrollStartTime = 0L
    private var frameDropCount = 0
    
    fun startScrollTracking() {
        scrollStartTime = System.currentTimeMillis()
        frameDropCount = 0
    }
    
    fun recordFrameDrop() {
        frameDropCount++
    }
    
    fun endScrollTracking(): ScrollPerformanceMetrics {
        val duration = System.currentTimeMillis() - scrollStartTime
        return ScrollPerformanceMetrics(
            durationMs = duration,
            frameDrops = frameDropCount
        )
    }
}

data class ScrollPerformanceMetrics(
    val durationMs: Long,
    val frameDrops: Int
) {
    val isPerformant: Boolean
        get() = frameDrops < 3 && durationMs < 1000
}
