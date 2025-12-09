package com.synapse.social.studioasinc.animations

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd

/**
 * Utility class for reaction-related animations
 */
object ReactionAnimations {

    /**
     * Animate reaction button when user selects a reaction
     */
    fun animateReactionSelection(view: View, callback: (() -> Unit)? = null) {
        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f)
        
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.3f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.3f, 1f)

        val scaleUp = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
            duration = 150
            interpolator = OvershootInterpolator()
        }

        val scaleDown = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY)
            duration = 150
            interpolator = OvershootInterpolator()
        }

        AnimatorSet().apply {
            playSequentially(scaleUp, scaleDown)
            doOnEnd { callback?.invoke() }
            start()
        }
    }

    /**
     * Bounce animation for reaction emoji
     */
    fun animateReactionBounce(view: View) {
        val bounce = ObjectAnimator.ofFloat(view, "translationY", 0f, -30f, 0f)
        bounce.duration = 400
        bounce.interpolator = BounceInterpolator()
        bounce.start()
    }

    /**
     * Fade in animation for reaction summary
     */
    fun animateReactionSummaryIn(view: View) {
        view.alpha = 0f
        view.scaleX =  0.8f
        view.scaleY = 0.8f
        view.visibility = View.VISIBLE

        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    /**
     * Fade out animation for reaction summary
     */
    fun animateReactionSummaryOut(view: View, callback: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(150)
            .withEndAction {
                view.visibility = View.GONE
                callback?.invoke()
            }
            .start()
    }

    /**
     * Ripple effect when reaction count changes
     */
    fun animateReactionCountChange(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 300
            start()
        }
    }

    /**
     * Pulse animation for reaction button
     */
    fun animatePulse(view: View, repeat: Int = 1) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 500
            this.startDelay = 0
            start()
        }
    }

    /**
     * Rotate animation for reaction icon change
     */
    fun animateReactionChange(view: View, callback: (() -> Unit)? = null) {
        view.animate()
            .rotationY(90f)
            .setDuration(150)
            .withEndAction {
                callback?.invoke()
                view.rotationY = -90f
                view.animate()
                    .rotationY(0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    /**
     * Cancel all animations on a view
     */
    fun cancelAnimations(view: View) {
        view.animate().cancel()
        view.clearAnimation()
    }
}
