package com.synapse.social.studioasinc.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Helper class for message animations in chat
 * Provides smooth, delightful animations for various message interactions
 */
object MessageAnimationHelper {

    /**
     * Animate message send with scale and fade
     * Creates a satisfying "pop" effect when sending
     */
    fun animateMessageSend(view: View, onComplete: (() -> Unit)? = null) {
        view.alpha = 0f
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.05f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.05f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 300
            interpolator = OvershootInterpolator(1.2f)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Animate message receive with slide and fade
     * Smooth entrance for incoming messages
     */
    fun animateMessageReceive(view: View, isMyMessage: Boolean, onComplete: (() -> Unit)? = null) {
        val startX = if (isMyMessage) 100f else -100f
        view.translationX = startX
        view.alpha = 0f
        
        val translateX = ObjectAnimator.ofFloat(view, "translationX", startX, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        AnimatorSet().apply {
            playTogether(translateX, alpha)
            duration = 250
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Animate message deletion with fade and scale
     * Smooth exit animation for deleted messages
     */
    fun animateMessageDelete(view: View, isMyMessage: Boolean, onComplete: (() -> Unit)? = null) {
        val endX = if (isMyMessage) 100f else -100f
        
        val translateX = ObjectAnimator.ofFloat(view, "translationX", 0f, endX)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.8f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f)
        
        AnimatorSet().apply {
            playTogether(translateX, alpha, scaleX, scaleY)
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Animate message selection with scale pulse
     * Visual feedback for multi-select mode
     */
    fun animateMessageSelection(view: View, isSelected: Boolean) {
        val targetScale = if (isSelected) 0.95f else 1f
        
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", view.scaleX, targetScale)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", view.scaleY, targetScale)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 150
            interpolator = FastOutSlowInInterpolator()
            start()
        }
    }

    /**
     * Animate reply preview appearance
     * Smooth slide up animation for reply UI
     */
    fun animateReplyPreviewShow(view: View, onComplete: (() -> Unit)? = null) {
        view.translationY = view.height.toFloat()
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        val translateY = ObjectAnimator.ofFloat(view, "translationY", view.height.toFloat(), 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        AnimatorSet().apply {
            playTogether(translateY, alpha)
            duration = 250
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Animate reply preview dismissal
     * Smooth slide down animation
     */
    fun animateReplyPreviewHide(view: View, onComplete: (() -> Unit)? = null) {
        val translateY = ObjectAnimator.ofFloat(view, "translationY", 0f, view.height.toFloat())
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        
        AnimatorSet().apply {
            playTogether(translateY, alpha)
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Animate typing indicator appearance
     * Gentle fade and slide for typing indicator
     */
    fun animateTypingIndicatorShow(view: View) {
        view.alpha = 0f
        view.translationY = 20f
        view.visibility = View.VISIBLE
        
        val translateY = ObjectAnimator.ofFloat(view, "translationY", 20f, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        AnimatorSet().apply {
            playTogether(translateY, alpha)
            duration = 200
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /**
     * Animate typing indicator dismissal
     */
    fun animateTypingIndicatorHide(view: View, onComplete: (() -> Unit)? = null) {
        val translateY = ObjectAnimator.ofFloat(view, "translationY", 0f, 20f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        
        AnimatorSet().apply {
            playTogether(translateY, alpha)
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Animate message state change (sent -> delivered -> read)
     * Subtle pulse animation for status icon
     */
    fun animateMessageStateChange(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 300
            interpolator = OvershootInterpolator()
            start()
        }
    }

    /**
     * Animate scroll to message highlight
     * Pulse animation to draw attention to specific message
     */
    fun animateMessageHighlight(view: View, highlightColor: Int, originalColor: Int) {
        val colorAnimator = ValueAnimator.ofArgb(originalColor, highlightColor, originalColor)
        colorAnimator.duration = 1000
        colorAnimator.repeatCount = 2
        colorAnimator.addUpdateListener { animator ->
            view.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimator.start()
    }

    /**
     * Animate connection status banner
     * Smooth slide down from top
     */
    fun animateConnectionBannerShow(view: View) {
        view.translationY = -view.height.toFloat()
        view.visibility = View.VISIBLE
        
        ObjectAnimator.ofFloat(view, "translationY", -view.height.toFloat(), 0f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /**
     * Animate connection status banner dismissal
     */
    fun animateConnectionBannerHide(view: View, onComplete: (() -> Unit)? = null) {
        ObjectAnimator.ofFloat(view, "translationY", 0f, -view.height.toFloat()).apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Animate upload progress indicator
     * Smooth appearance for media upload UI
     */
    fun animateUploadProgressShow(view: View) {
        view.alpha = 0f
        view.scaleY = 0f
        view.visibility = View.VISIBLE
        
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleY, alpha)
            duration = 200
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /**
     * Animate upload completion with success indicator
     */
    fun animateUploadSuccess(view: View, onComplete: (() -> Unit)? = null) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 400
            interpolator = OvershootInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            start()
        }
    }
}
