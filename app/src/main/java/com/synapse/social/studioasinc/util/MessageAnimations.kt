package com.synapse.social.studioasinc.util

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R

/**
 * Utility object for applying animations to message views
 * Provides consistent animation behavior across the chat interface
 */
object MessageAnimations {

    /**
     * Apply fade animation for edited message updates
     * Used when a message content is updated after editing
     */
    fun applyEditedMessageAnimation(view: View) {
        val fadeAnimation = AnimationUtils.loadAnimation(view.context, R.anim.fade_message)
        view.startAnimation(fadeAnimation)
    }

    /**
     * Apply slide-out animation for deleted messages
     * Animates the message sliding out before removal
     */
    fun applyDeletedMessageAnimation(
        view: View,
        isOutgoing: Boolean,
        onAnimationEnd: () -> Unit
    ) {
        val slideOutAnimation = if (isOutgoing) {
            AnimationUtils.loadAnimation(view.context, R.anim.slide_out_right)
        } else {
            AnimationUtils.loadAnimation(view.context, R.anim.slide_out_left)
        }
        
        slideOutAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                onAnimationEnd()
            }
            
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
        
        view.startAnimation(slideOutAnimation)
    }

    /**
     * Apply highlight flash animation for scrolled-to messages
     * Used when navigating to a specific message (e.g., from reply)
     */
    fun applyHighlightFlashAnimation(view: View) {
        val highlightAnimation = AnimationUtils.loadAnimation(view.context, R.anim.highlight_flash)
        view.startAnimation(highlightAnimation)
    }

    /**
     * Apply ripple effect for action selections
     * Provides visual feedback when an action is selected
     */
    fun applyRippleEffect(view: View) {
        // Use the built-in ripple effect from Material Design
        view.isPressed = true
        view.postDelayed({
            view.isPressed = false
        }, 100)
    }

    /**
     * Scroll to a message with animation and highlight it
     * Used for "jump to message" functionality
     */
    fun scrollToMessageWithHighlight(
        recyclerView: RecyclerView,
        position: Int,
        smoothScroll: Boolean = true
    ) {
        if (smoothScroll) {
            recyclerView.smoothScrollToPosition(position)
        } else {
            recyclerView.scrollToPosition(position)
        }
        
        // Highlight the message after scrolling
        recyclerView.postDelayed({
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
            viewHolder?.itemView?.let { view ->
                applyHighlightFlashAnimation(view)
            }
        }, if (smoothScroll) 300 else 0)
    }

    /**
     * Apply pulse animation for pending actions
     * Used to indicate that an action is queued or in progress
     */
    fun applyPendingActionAnimation(view: View) {
        val pulseAnimation = AnimationUtils.loadAnimation(view.context, R.anim.pulse)
        view.startAnimation(pulseAnimation)
    }

    /**
     * Clear all animations from a view
     * Used when transitioning from loading to success/error states
     */
    fun clearAnimations(view: View) {
        view.clearAnimation()
    }

    /**
     * Apply smooth fade transition between states
     * Used for transitioning between loading, success, and error states
     */
    fun applyStateTransition(view: View, onTransitionComplete: () -> Unit) {
        val fadeOut = AnimationUtils.loadAnimation(view.context, R.anim.fade_out)
        fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                onTransitionComplete()
                val fadeIn = AnimationUtils.loadAnimation(view.context, R.anim.fade_in)
                view.startAnimation(fadeIn)
            }
            
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
        view.startAnimation(fadeOut)
    }
}
