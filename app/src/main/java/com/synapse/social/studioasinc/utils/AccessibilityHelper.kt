package com.synapse.social.studioasinc.utils

import android.view.View
import android.widget.ImageView
import androidx.core.view.ViewCompat

/**
 * Helper object for accessibility improvements
 */
object AccessibilityHelper {
    
    /**
     * Sets content description for a view
     */
    fun setContentDescription(view: View, description: String) {
        view.contentDescription = description
    }
    
    /**
     * Sets minimum touch target size (48dp)
     */
    fun ensureMinTouchTarget(view: View) {
        view.minimumWidth = 48.dpToPx(view.context)
        view.minimumHeight = 48.dpToPx(view.context)
    }
    
    /**
     * Announces state change for accessibility services
     */
    fun announceForAccessibility(view: View, message: String) {
        view.announceForAccessibility(message)
    }
    
    /**
     * Sets up profile image accessibility
     */
    fun setupProfileImageAccessibility(imageView: ImageView, username: String) {
        imageView.contentDescription = "Profile picture of $username"
        imageView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    
    /**
     * Sets up cover image accessibility
     */
    fun setupCoverImageAccessibility(imageView: ImageView, username: String) {
        imageView.contentDescription = "Cover photo of $username"
        imageView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    
    /**
     * Sets up button accessibility with state
     */
    fun setupButtonAccessibility(view: View, label: String, isEnabled: Boolean = true) {
        view.contentDescription = label
        view.isEnabled = isEnabled
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    
    /**
     * Sets up stats accessibility (followers, following, posts)
     */
    fun setupStatsAccessibility(view: View, count: Int, type: String) {
        view.contentDescription = "$count $type"
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
}

/**
 * Extension function to convert dp to px
 */
private fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}
