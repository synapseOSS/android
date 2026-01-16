package com.synapse.social.studioasinc.core.util

/**
 * Data class to store scroll position state for restoration
 * 
 * @param position The scroll position (item index)
 * @param offset The offset within the item
 * @param timestamp The timestamp when the position was saved
 */
data class ScrollPositionState(
    val position: Int,
    val offset: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        // Default timeout for scroll position expiration (5 minutes)
        const val DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L
    }
    
    /**
     * Check if the scroll position has expired
     * 
     * @param timeout The timeout in milliseconds (default: 5 minutes)
     * @return true if the position is expired, false otherwise
     */
    fun isExpired(timeout: Long = DEFAULT_TIMEOUT_MS): Boolean {
        return System.currentTimeMillis() - timestamp > timeout
    }
}
