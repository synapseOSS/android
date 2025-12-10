package com.synapse.social.studioasinc.util

import android.view.View
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

/**
 * Helper for smooth scrolling in RecyclerViews
 * Provides better UX with controlled scroll speeds and animations
 */
class SmoothScrollHelper(private val recyclerView: RecyclerView) {

    private val layoutManager: LinearLayoutManager?
        get() = recyclerView.layoutManager as? LinearLayoutManager

    /**
     * Scroll to position with smooth animation
     * 
     * @param position Target position
     * @param snapPreference Snap preference (START, CENTER, END)
     */
    fun smoothScrollToPosition(
        position: Int,
        snapPreference: Int = LinearSmoothScroller.SNAP_TO_START
    ) {
        val smoothScroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun getVerticalSnapPreference(): Int = snapPreference
            override fun getHorizontalSnapPreference(): Int = snapPreference

            // Adjust scroll speed (lower = faster)
            override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                return 100f / displayMetrics.densityDpi
            }
        }

        smoothScroller.targetPosition = position
        layoutManager?.startSmoothScroll(smoothScroller)
    }

    /**
     * Scroll to bottom of list smoothly
     */
    fun scrollToBottom() {
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        if (itemCount > 0) {
            smoothScrollToPosition(itemCount - 1, LinearSmoothScroller.SNAP_TO_END)
        }
    }

    /**
     * Scroll to top of list smoothly
     */
    fun scrollToTop() {
        smoothScrollToPosition(0, LinearSmoothScroller.SNAP_TO_START)
    }

    /**
     * Check if RecyclerView is at bottom
     * 
     * @param threshold Threshold in pixels to consider "at bottom"
     * @return True if at bottom
     */
    fun isAtBottom(threshold: Int = 50): Boolean {
        val layoutManager = this.layoutManager ?: return false
        val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        
        return lastVisiblePosition >= itemCount - 1 || 
               !recyclerView.canScrollVertically(1)
    }

    /**
     * Check if RecyclerView is at top
     * 
     * @return True if at top
     */
    fun isAtTop(): Boolean {
        return !recyclerView.canScrollVertically(-1)
    }

    /**
     * Scroll to position only if user is near bottom
     * Useful for auto-scrolling to new messages
     * 
     * @param position Target position
     * @param threshold Threshold to consider "near bottom"
     */
    fun scrollToPositionIfNearBottom(position: Int, threshold: Int = 200) {
        if (isNearBottom(threshold)) {
            smoothScrollToPosition(position, LinearSmoothScroller.SNAP_TO_END)
        }
    }

    /**
     * Check if user is near bottom of list
     * 
     * @param threshold Threshold in pixels
     * @return True if near bottom
     */
    fun isNearBottom(threshold: Int = 200): Boolean {
        val layoutManager = this.layoutManager ?: return false
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        
        // Check if last visible item is within threshold of last item
        return itemCount - lastVisiblePosition <= 3 || isAtBottom(threshold)
    }

    /**
     * Setup auto-scroll to bottom when new items are added
     * Only scrolls if user is already near bottom
     * 
     * @param threshold Threshold to trigger auto-scroll
     */
    fun setupAutoScrollToBottom(threshold: Int = 200) {
        recyclerView.adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                val adapter = recyclerView.adapter ?: return
                val totalItems = adapter.itemCount
                
                // Only auto-scroll if new items are at the end and user is near bottom
                if (positionStart + itemCount >= totalItems && isNearBottom(threshold)) {
                    recyclerView.post {
                        scrollToBottom()
                    }
                }
            }
        })
    }

    /**
     * Scroll to position with offset from top
     * 
     * @param position Target position
     * @param offset Offset from top in pixels
     */
    fun scrollToPositionWithOffset(position: Int, offset: Int = 0) {
        layoutManager?.scrollToPositionWithOffset(position, offset)
    }

    /**
     * Get current scroll position
     * 
     * @return Current first visible position
     */
    fun getCurrentPosition(): Int {
        return layoutManager?.findFirstVisibleItemPosition() ?: 0
    }

    /**
     * Save scroll position
     * 
     * @return Saved scroll state
     */
    fun saveScrollPosition(): ScrollPosition {
        val layoutManager = this.layoutManager ?: return ScrollPosition(0, 0)
        val position = layoutManager.findFirstVisibleItemPosition()
        val view = layoutManager.findViewByPosition(position)
        val offset = view?.top ?: 0
        return ScrollPosition(position, offset)
    }

    /**
     * Restore scroll position
     * 
     * @param scrollPosition Previously saved scroll position
     */
    fun restoreScrollPosition(scrollPosition: ScrollPosition) {
        layoutManager?.scrollToPositionWithOffset(scrollPosition.position, scrollPosition.offset)
    }

    /**
     * Data class to hold scroll position
     */
    data class ScrollPosition(val position: Int, val offset: Int)

    companion object {
        /**
         * Create SmoothScrollHelper for RecyclerView
         */
        fun create(recyclerView: RecyclerView): SmoothScrollHelper {
            return SmoothScrollHelper(recyclerView)
        }
    }
}

/**
 * Extension function for easy access to SmoothScrollHelper
 */
fun RecyclerView.smoothScrollHelper(): SmoothScrollHelper {
    return SmoothScrollHelper.create(this)
}
