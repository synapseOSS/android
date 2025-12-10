package com.synapse.social.studioasinc.util

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Smart scroll helper for chat RecyclerView
 * Provides intelligent scrolling behavior with smooth animations
 */
class SmartScrollHelper(
    private val recyclerView: RecyclerView,
    private val coroutineScope: CoroutineScope
) {

    private var autoScrollJob: Job? = null
    private var isUserScrolling = false
    private var shouldAutoScroll = true
    private val scrollThreshold = 3 // Number of items from bottom to trigger auto-scroll

    /**
     * Scroll to bottom with smooth animation
     */
    fun scrollToBottom(smooth: Boolean = true) {
        val adapter = recyclerView.adapter ?: return
        val itemCount = adapter.itemCount
        
        if (itemCount == 0) return
        
        if (smooth) {
            recyclerView.smoothScrollToPosition(itemCount - 1)
        } else {
            recyclerView.scrollToPosition(itemCount - 1)
        }
    }

    /**
     * Scroll to specific position with animation
     */
    fun scrollToPosition(position: Int, smooth: Boolean = true, offset: Int = 0) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        
        if (smooth) {
            recyclerView.smoothScrollToPosition(position)
            // Apply offset after scroll completes
            if (offset != 0) {
                coroutineScope.launch {
                    delay(300) // Wait for smooth scroll to complete
                    layoutManager.scrollToPositionWithOffset(position, offset)
                }
            }
        } else {
            layoutManager.scrollToPositionWithOffset(position, offset)
        }
    }

    /**
     * Check if user is near bottom of chat
     */
    fun isNearBottom(): Boolean {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return false
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        
        return itemCount - lastVisiblePosition <= scrollThreshold
    }

    /**
     * Auto-scroll to bottom when new message arrives (only if near bottom)
     */
    fun autoScrollOnNewMessage() {
        if (shouldAutoScroll && isNearBottom()) {
            scrollToBottom(smooth = true)
        }
    }

    /**
     * Setup scroll listener to detect user scrolling
     */
    fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        isUserScrolling = true
                        shouldAutoScroll = false
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        isUserScrolling = false
                        // Re-enable auto-scroll if user scrolled to bottom
                        if (isNearBottom()) {
                            shouldAutoScroll = true
                        }
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Check if scrolled to bottom
                if (isNearBottom()) {
                    shouldAutoScroll = true
                }
            }
        })
    }

    /**
     * Scroll to message with highlight animation
     */
    fun scrollToMessageWithHighlight(position: Int, highlightView: View?) {
        scrollToPosition(position, smooth = true)
        
        // Highlight after scroll completes
        highlightView?.let { view ->
            coroutineScope.launch {
                delay(400) // Wait for scroll animation
                highlightMessage(view)
            }
        }
    }

    /**
     * Highlight message with pulse animation
     */
    private fun highlightMessage(view: View) {
        val originalAlpha = view.alpha
        
        coroutineScope.launch(Dispatchers.Main) {
            // Pulse animation
            repeat(3) {
                view.animate()
                    .alpha(0.5f)
                    .setDuration(200)
                    .withEndAction {
                        view.animate()
                            .alpha(originalAlpha)
                            .setDuration(200)
                            .start()
                    }
                    .start()
                delay(400)
            }
        }
    }

    /**
     * Save current scroll position
     */
    fun saveScrollPosition(): Pair<Int, Int> {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        val position = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val view = layoutManager?.findViewByPosition(position)
        val offset = view?.top ?: 0
        
        return Pair(position, offset)
    }

    /**
     * Restore scroll position
     */
    fun restoreScrollPosition(position: Int, offset: Int) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        layoutManager?.scrollToPositionWithOffset(position, offset)
    }

    /**
     * Smooth scroll to top (for loading older messages)
     */
    fun scrollToTop(smooth: Boolean = true) {
        if (smooth) {
            recyclerView.smoothScrollToPosition(0)
        } else {
            recyclerView.scrollToPosition(0)
        }
    }

    /**
     * Check if at top of list
     */
    fun isAtTop(): Boolean {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return false
        return layoutManager.findFirstVisibleItemPosition() == 0
    }

    /**
     * Enable/disable auto-scroll
     */
    fun setAutoScrollEnabled(enabled: Boolean) {
        shouldAutoScroll = enabled
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        autoScrollJob?.cancel()
    }
}
