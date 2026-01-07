package com.synapse.social.studioasinc.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.synapse.social.studioasinc.R

/**
 * Custom view that displays an animated three-dot typing indicator.
 * Shows a wave animation with dots bouncing up and down to indicate typing activity.
 */
class TypingAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dotPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.md_theme_primary)
    }

    private val animationDuration = 1200L
    private val dotCount = 3
    private val dotRadius = 4f
    private val dotSpacing = 12f
    private val bounceHeight = 8f

    private var animator: ValueAnimator? = null
    private var animationProgress = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = (dotCount * dotRadius * 2 + (dotCount - 1) * dotSpacing).toInt()
        val totalHeight = (dotRadius * 2 + bounceHeight).toInt()
        
        setMeasuredDimension(totalWidth, totalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerY = height / 2f
        val startX = dotRadius

        for (i in 0 until dotCount) {
            val x = startX + i * (dotRadius * 2 + dotSpacing)
            
            // Calculate bounce offset for each dot with phase delay
            val phase = (animationProgress + i * 0.33f) % 1f
            val bounceOffset = calculateBounceOffset(phase)
            val y = centerY - bounceOffset

            canvas.drawCircle(x, y, dotRadius, dotPaint)
        }
    }

    private fun calculateBounceOffset(phase: Float): Float {
        // Create a smooth wave animation using sine function
        return (kotlin.math.sin(phase * 2 * kotlin.math.PI) * bounceHeight / 2).toFloat()
    }

    /**
     * Starts the typing animation with a continuous wave effect
     */
    fun startAnimation() {
        stopAnimation() // Stop any existing animation

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float
                invalidate()
            }
            
            start()
        }
    }

    /**
     * Stops the typing animation and resets to initial state
     */
    fun stopAnimation() {
        animator?.cancel()
        animator = null
        animationProgress = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
