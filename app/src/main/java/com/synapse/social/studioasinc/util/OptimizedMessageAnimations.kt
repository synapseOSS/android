package com.synapse.social.studioasinc.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Optimized message animations for better scroll performance
 * Reduces animation complexity during fast scrolling
 */
object OptimizedMessageAnimations {
    
    private const val ENTRANCE_DURATION = 200L
    private const val EXIT_DURATION = 150L
    private const val HIGHLIGHT_DURATION = 300L
    
    private var isScrolling = false
    
    /**
     * Set scroll state to optimize animations
     */
    fun setScrolling(scrolling: Boolean) {
        isScrolling = scrolling
    }
    
    /**
     * Optimized entrance animation for new messages
     * Simplified during scrolling for better performance
     */
    fun animateMessageEntrance(
        view: View,
        isMyMessage: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        if (isScrolling) {
            // Skip animation during scroll for performance
            view.alpha = 1f
            view.scaleX = 1f
            view.scaleY = 1f
            view.translationY = 0f
            onComplete?.invoke()
            return
        }
        
        // Initial state
        view.alpha = 0f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        view.translationY = if (isMyMessage) 20f else -20f
        
        // Animate to final state
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
        val slideIn = ObjectAnimator.ofFloat(view, "translationY", view.translationY, 0f)
        
        fadeIn.duration = ENTRANCE_DURATION
        scaleX.duration = ENTRANCE_DURATION
        scaleY.duration = ENTRANCE_DURATION
        slideIn.duration = ENTRANCE_DURATION
        
        fadeIn.interpolator = FastOutSlowInInterpolator()
        scaleX.interpolator = OvershootInterpolator(1.1f)
        scaleY.interpolator = OvershootInterpolator(1.1f)
        slideIn.interpolator = FastOutSlowInInterpolator()
        
        fadeIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onComplete?.invoke()
            }
        })
        
        fadeIn.start()
        scaleX.start()
        scaleY.start()
        slideIn.start()
    }
    
    /**
     * Optimized exit animation for deleted messages
     */
    fun animateMessageExit(
        view: View,
        isMyMessage: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        if (isScrolling) {
            // Skip animation during scroll
            view.visibility = View.GONE
            onComplete?.invoke()
            return
        }
        
        val slideOut = if (isMyMessage) {
            ObjectAnimator.ofFloat(view, "translationX", 0f, view.width.toFloat())
        } else {
            ObjectAnimator.ofFloat(view, "translationX", 0f, -view.width.toFloat())
        }
        
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 0.9f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 0.9f)
        )
        
        slideOut.duration = EXIT_DURATION
        fadeOut.duration = EXIT_DURATION
        scaleDown.duration = EXIT_DURATION
        
        slideOut.interpolator = AccelerateDecelerateInterpolator()
        fadeOut.interpolator = AccelerateDecelerateInterpolator()
        scaleDown.interpolator = AccelerateDecelerateInterpolator()
        
        slideOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
                onComplete?.invoke()
            }
        })
        
        slideOut.start()
        fadeOut.start()
        scaleDown.start()
    }
    
    /**
     * Highlight animation for message selection or navigation
     */
    fun animateMessageHighlight(
        view: View,
        highlightColor: Int = 0x33FFD700, // Semi-transparent gold
        onComplete: (() -> Unit)? = null
    ) {
        if (isScrolling) {
            // Skip animation during scroll
            onComplete?.invoke()
            return
        }
        
        val originalBackground = view.background
        
        // Create highlight effect
        view.setBackgroundColor(highlightColor)
        
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f)
        fadeOut.duration = HIGHLIGHT_DURATION
        fadeOut.interpolator = AccelerateDecelerateInterpolator()
        
        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.background = originalBackground
                onComplete?.invoke()
            }
        })
        
        fadeOut.start()
    }
    
    /**
     * Optimized state change animation (for read receipts, etc.)
     */
    fun animateStateChange(
        view: View,
        onComplete: (() -> Unit)? = null
    ) {
        if (isScrolling) {
            // Skip animation during scroll
            onComplete?.invoke()
            return
        }
        
        val pulse = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f)
        )
        
        pulse.duration = 200L
        pulse.interpolator = OvershootInterpolator(1.2f)
        
        pulse.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onComplete?.invoke()
            }
        })
        
        pulse.start()
    }
    
    /**
     * Batch animation for multiple messages (e.g., bulk operations)
     */
    fun animateMessageBatch(
        views: List<View>,
        animationType: BatchAnimationType,
        staggerDelay: Long = 50L,
        onComplete: (() -> Unit)? = null
    ) {
        if (isScrolling || views.isEmpty()) {
            onComplete?.invoke()
            return
        }
        
        var completedCount = 0
        val totalCount = views.size
        
        views.forEachIndexed { index, view ->
            view.postDelayed({
                when (animationType) {
                    BatchAnimationType.FADE_IN -> {
                        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
                        fadeIn.duration = 150L
                        fadeIn.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                completedCount++
                                if (completedCount == totalCount) {
                                    onComplete?.invoke()
                                }
                            }
                        })
                        fadeIn.start()
                    }
                    BatchAnimationType.SLIDE_UP -> {
                        view.translationY = 50f
                        val slideUp = ObjectAnimator.ofFloat(view, "translationY", 50f, 0f)
                        slideUp.duration = 200L
                        slideUp.interpolator = FastOutSlowInInterpolator()
                        slideUp.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                completedCount++
                                if (completedCount == totalCount) {
                                    onComplete?.invoke()
                                }
                            }
                        })
                        slideUp.start()
                    }
                }
            }, index * staggerDelay)
        }
    }
    
    enum class BatchAnimationType {
        FADE_IN,
        SLIDE_UP
    }
}

/**
 * Extension functions for easy animation usage
 */
fun View.animateEntranceOptimized(
    isMyMessage: Boolean,
    onComplete: (() -> Unit)? = null
) {
    OptimizedMessageAnimations.animateMessageEntrance(this, isMyMessage, onComplete)
}

fun View.animateExitOptimized(
    isMyMessage: Boolean,
    onComplete: (() -> Unit)? = null
) {
    OptimizedMessageAnimations.animateMessageExit(this, isMyMessage, onComplete)
}

fun View.animateHighlightOptimized(
    highlightColor: Int = 0x33FFD700,
    onComplete: (() -> Unit)? = null
) {
    OptimizedMessageAnimations.animateMessageHighlight(this, highlightColor, onComplete)
}

fun View.animateStateChangeOptimized(onComplete: (() -> Unit)? = null) {
    OptimizedMessageAnimations.animateStateChange(this, onComplete)
}
