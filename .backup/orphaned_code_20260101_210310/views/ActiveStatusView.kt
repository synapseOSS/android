package com.synapse.social.studioasinc.chat

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.synapse.social.studioasinc.R

/**
 * Custom view component for displaying user active status indicators
 */
class ActiveStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val ONLINE_THRESHOLD = 60000L // 1 minute
        private const val RECENTLY_ACTIVE_THRESHOLD = 300000L // 5 minutes
    }

    private var isOnline = false
    private var lastSeen = 0L
    private var activityStatus = "offline"

    init {
        setupView()
    }

    private fun setupView() {
        // Set default size
        val size = context.resources.getDimensionPixelSize(R.dimen.active_status_indicator_size)
        layoutParams = layoutParams?.apply {
            width = size
            height = size
        } ?: ViewGroup.LayoutParams(size, size)
        
        // Set default appearance
        updateStatusIndicator()
    }

    /**
     * Update user status
     */
    fun updateStatus(isOnline: Boolean, lastSeen: Long, activityStatus: String = "online") {
        this.isOnline = isOnline
        this.lastSeen = lastSeen
        this.activityStatus = activityStatus
        updateStatusIndicator()
    }

    /**
     * Set user as online
     */
    fun setOnline() {
        updateStatus(true, System.currentTimeMillis(), "online")
    }

    /**
     * Set user as offline
     */
    fun setOffline(lastSeen: Long = System.currentTimeMillis()) {
        updateStatus(false, lastSeen, "offline")
    }

    /**
     * Set user as away
     */
    fun setAway(lastSeen: Long = System.currentTimeMillis()) {
        updateStatus(true, lastSeen, "away")
    }

    /**
     * Set user as typing
     */
    fun setTyping() {
        updateStatus(true, System.currentTimeMillis(), "typing")
    }

    /**
     * Update the visual indicator based on current status
     */
    private fun updateStatusIndicator() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSeen = currentTime - lastSeen

        when {
            // User is actively online
            isOnline && timeSinceLastSeen < ONLINE_THRESHOLD -> {
                when (activityStatus) {
                    "typing" -> showTypingIndicator()
                    "away" -> showAwayIndicator()
                    else -> showOnlineIndicator()
                }
            }
            
            // User was recently active
            timeSinceLastSeen < RECENTLY_ACTIVE_THRESHOLD -> {
                showRecentlyActiveIndicator()
            }
            
            // User is offline
            else -> {
                showOfflineIndicator()
            }
        }
    }

    /**
     * Show online indicator (green dot)
     */
    private fun showOnlineIndicator() {
        visibility = View.VISIBLE
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.status_online))
        contentDescription = context.getString(R.string.status_online)
    }

    /**
     * Show away indicator (yellow dot)
     */
    private fun showAwayIndicator() {
        visibility = View.VISIBLE
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.status_away))
        contentDescription = context.getString(R.string.status_away)
    }

    /**
     * Show typing indicator (pulsing green dot)
     */
    private fun showTypingIndicator() {
        visibility = View.VISIBLE
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.status_typing))
        contentDescription = context.getString(R.string.status_typing)
        
        // Add pulsing animation
        animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(500)
            .withEndAction {
                animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(500)
                    .withEndAction {
                        if (activityStatus == "typing") {
                            showTypingIndicator() // Continue pulsing
                        }
                    }
                    .start()
            }
            .start()
    }

    /**
     * Show recently active indicator (gray dot)
     */
    private fun showRecentlyActiveIndicator() {
        visibility = View.VISIBLE
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.status_recently_active))
        contentDescription = context.getString(R.string.status_recently_active)
    }

    /**
     * Show offline indicator (hidden)
     */
    private fun showOfflineIndicator() {
        visibility = View.GONE
        contentDescription = context.getString(R.string.status_offline)
    }

    /**
     * Get current status as string
     */
    fun getCurrentStatusText(): String {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSeen = currentTime - lastSeen

        return when {
            isOnline && timeSinceLastSeen < ONLINE_THRESHOLD -> {
                when (activityStatus) {
                    "typing" -> context.getString(R.string.status_typing)
                    "away" -> context.getString(R.string.status_away)
                    else -> context.getString(R.string.status_online)
                }
            }
            timeSinceLastSeen < RECENTLY_ACTIVE_THRESHOLD -> {
                context.getString(R.string.status_recently_active)
            }
            else -> {
                val minutes = timeSinceLastSeen / 60000
                val hours = minutes / 60
                val days = hours / 24

                when {
                    days > 0 -> context.getString(R.string.last_seen_days_ago, days)
                    hours > 0 -> context.getString(R.string.last_seen_hours_ago, hours)
                    minutes > 0 -> context.getString(R.string.last_seen_minutes_ago, minutes)
                    else -> context.getString(R.string.last_seen_just_now)
                }
            }
        }
    }

    /**
     * Check if user is currently online
     */
    fun isUserOnline(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSeen = currentTime - lastSeen
        return isOnline && timeSinceLastSeen < ONLINE_THRESHOLD
    }

    /**
     * Check if user is recently active
     */
    fun isUserRecentlyActive(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSeen = currentTime - lastSeen
        return timeSinceLastSeen < RECENTLY_ACTIVE_THRESHOLD
    }

    /**
     * Force refresh the status indicator
     */
    fun refresh() {
        updateStatusIndicator()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Clear any running animations
        clearAnimation()
    }
}
