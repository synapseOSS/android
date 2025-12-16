package com.synapse.social.studioasinc.util

import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.ChatAdapter

/**
 * Optimized scroll listener for chat RecyclerView
 * Improves performance during fast scrolling by reducing heavy operations
 */
class ChatScrollListener(
    private val adapter: ChatAdapter,
    private val onScrollStateChanged: ((isScrolling: Boolean) -> Unit)? = null
) : RecyclerView.OnScrollListener() {
    
    private var isScrolling = false
    private var scrollStartTime = 0L
    private var lastScrollY = 0
    private var scrollVelocity = 0f
    
    companion object {
        private const val FAST_SCROLL_THRESHOLD = 20 // pixels per frame
        private const val SCROLL_DEBOUNCE_MS = 150L
    }
    
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        
        when (newState) {
            RecyclerView.SCROLL_STATE_DRAGGING -> {
                if (!isScrolling) {
                    isScrolling = true
                    scrollStartTime = System.currentTimeMillis()
                    adapter.setScrolling(true)
                    onScrollStateChanged?.invoke(true)
                }
            }
            RecyclerView.SCROLL_STATE_SETTLING -> {
                // Continue optimizations during settling
                if (!isScrolling) {
                    isScrolling = true
                    adapter.setScrolling(true)
                    onScrollStateChanged?.invoke(true)
                }
            }
            RecyclerView.SCROLL_STATE_IDLE -> {
                if (isScrolling) {
                    // Debounce the idle state to avoid flickering
                    recyclerView.postDelayed({
                        if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                            isScrolling = false
                            adapter.setScrolling(false)
                            onScrollStateChanged?.invoke(false)
                            
                            // Refresh visible items after scroll ends
                            refreshVisibleItems(recyclerView)
                        }
                    }, SCROLL_DEBOUNCE_MS)
                }
            }
        }
    }
    
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        
        // Calculate scroll velocity for performance adjustments
        scrollVelocity = kotlin.math.abs(dy - lastScrollY).toFloat()
        lastScrollY = dy
        
        // Enable aggressive optimizations for fast scrolling
        val isFastScrolling = scrollVelocity > FAST_SCROLL_THRESHOLD
        if (isFastScrolling && !isScrolling) {
            isScrolling = true
            adapter.setScrolling(true)
            onScrollStateChanged?.invoke(true)
        }
    }
    
    /**
     * Refresh visible items after scrolling ends to ensure proper display
     */
    private fun refreshVisibleItems(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        
        if (firstVisible != RecyclerView.NO_POSITION && lastVisible != RecyclerView.NO_POSITION) {
            // Refresh visible range with a small buffer
            val start = maxOf(0, firstVisible - 2)
            val end = minOf(adapter.itemCount - 1, lastVisible + 2)
            
            for (i in start..end) {
                adapter.notifyItemChanged(i)
            }
        }
    }
    
    /**
     * Get current scroll performance metrics
     */
    fun getScrollMetrics(): ScrollMetrics {
        return ScrollMetrics(
            isScrolling = isScrolling,
            scrollVelocity = scrollVelocity,
            scrollDuration = if (isScrolling) System.currentTimeMillis() - scrollStartTime else 0L
        )
    }
}

data class ScrollMetrics(
    val isScrolling: Boolean,
    val scrollVelocity: Float,
    val scrollDuration: Long
) {
    val isPerformant: Boolean
        get() = scrollVelocity < 50f && scrollDuration < 2000L
}
