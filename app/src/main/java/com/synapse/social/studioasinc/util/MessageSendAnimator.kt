package com.synapse.social.studioasinc.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible

/**
 * Animator for message send actions
 * Provides smooth visual feedback when sending messages
 */
object MessageSendAnimator {

    /**
     * Animate send button when message is sent
     * 
     * @param sendButton The send button view
     * @param onAnimationEnd Callback when animation completes
     */
    fun animateSendButton(sendButton: View, onAnimationEnd: (() -> Unit)? = null) {
        // Scale down and back up with rotation
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(sendButton, "scaleX", 1f, 0.8f),
                ObjectAnimator.ofFloat(sendButton, "scaleY", 1f, 0.8f),
                ObjectAnimator.ofFloat(sendButton, "rotation", 0f, -15f)
            )
            duration = 100
            interpolator = AccelerateDecelerateInterpolator()
        }

        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(sendButton, "scaleX", 0.8f, 1f),
                ObjectAnimator.ofFloat(sendButton, "scaleY", 0.8f, 1f),
                ObjectAnimator.ofFloat(sendButton, "rotation", -15f, 0f)
            )
            duration = 150
            interpolator = OvershootInterpolator()
        }

        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Animate message input clearing
     * 
     * @param inputView The message input view
     */
    fun animateInputClear(inputView: View) {
        ObjectAnimator.ofFloat(inputView, "alpha", 1f, 0.5f, 1f).apply {
            duration = 200
            start()
        }
    }

    /**
     * Animate new message appearing in list
     * 
     * @param messageView The message view to animate
     */
    fun animateMessageAppear(messageView: View) {
        messageView.alpha = 0f
        messageView.translationY = 50f
        messageView.scaleX = 0.9f
        messageView.scaleY = 0.9f

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(messageView, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(messageView, "translationY", 50f, 0f),
                ObjectAnimator.ofFloat(messageView, "scaleX", 0.9f, 1f),
                ObjectAnimator.ofFloat(messageView, "scaleY", 0.9f, 1f)
            )
            duration = 300
            interpolator = OvershootInterpolator()
            start()
        }
    }

    /**
     * Animate message sending state (loading indicator)
     * 
     * @param loadingView The loading indicator view
     * @param show Whether to show or hide the loading indicator
     */
    fun animateLoadingIndicator(loadingView: View, show: Boolean) {
        if (show) {
            loadingView.isVisible = true
            loadingView.alpha = 0f
            loadingView.scaleX = 0.5f
            loadingView.scaleY = 0.5f

            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(loadingView, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(loadingView, "scaleX", 0.5f, 1f),
                    ObjectAnimator.ofFloat(loadingView, "scaleY", 0.5f, 1f)
                )
                duration = 200
                interpolator = OvershootInterpolator()
                start()
            }
        } else {
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(loadingView, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(loadingView, "scaleX", 1f, 0.5f),
                    ObjectAnimator.ofFloat(loadingView, "scaleY", 1f, 0.5f)
                )
                duration = 150
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        loadingView.isVisible = false
                    }
                })
                start()
            }
        }
    }

    /**
     * Animate reply preview appearing
     * 
     * @param replyView The reply preview view
     */
    fun animateReplyPreview(replyView: View, show: Boolean) {
        if (show) {
            replyView.isVisible = true
            replyView.translationY = -50f
            replyView.alpha = 0f

            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(replyView, "translationY", -50f, 0f),
                    ObjectAnimator.ofFloat(replyView, "alpha", 0f, 1f)
                )
                duration = 250
                interpolator = OvershootInterpolator()
                start()
            }
        } else {
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(replyView, "translationY", 0f, -50f),
                    ObjectAnimator.ofFloat(replyView, "alpha", 1f, 0f)
                )
                duration = 200
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        replyView.isVisible = false
                    }
                })
                start()
            }
        }
    }

    /**
     * Pulse animation for attention-grabbing elements
     * 
     * @param view The view to pulse
     * @param repeatCount Number of times to repeat (-1 for infinite)
     */
    fun pulseAnimation(view: View, repeatCount: Int = 2) {
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f)
            )
            duration = 300
        }

        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1.1f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 1.1f, 1f)
            )
            duration = 300
        }

        AnimatorSet().apply {
            playSequentially(scaleUp, scaleDown)
            if (repeatCount != 0) {
                addListener(object : AnimatorListenerAdapter() {
                    private var currentCount = 0

                    override fun onAnimationEnd(animation: Animator) {
                        currentCount++
                        if (repeatCount == -1 || currentCount < repeatCount) {
                            start()
                        }
                    }
                })
            }
            start()
        }
    }

    /**
     * Shake animation for error states
     * 
     * @param view The view to shake
     */
    fun shakeAnimation(view: View) {
        val shake = ObjectAnimator.ofFloat(
            view,
            "translationX",
            0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f
        ).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
        }
        shake.start()
    }
}
