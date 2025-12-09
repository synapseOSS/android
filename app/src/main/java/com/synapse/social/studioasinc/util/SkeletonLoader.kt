package com.synapse.social.studioasinc.util

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.synapse.social.studioasinc.R

/**
 * Skeleton loader for showing loading states
 * Provides smooth shimmer effect while content is loading
 */
class SkeletonLoader(private val view: View) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shimmerAnimator: ValueAnimator? = null
    private var shimmerTranslate = 0f
    private val shimmerWidth = view.width * 2f

    private val baseColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.md_theme_surfaceContainerLow)
    }

    private val shimmerColor: Int by lazy {
        ContextCompat.getColor(view.context, R.color.md_theme_surfaceContainerHigh)
    }

    /**
     * Start showing skeleton loading state
     */
    fun show() {
        view.isVisible = true
        startShimmerAnimation()
        view.setWillNotDraw(false)
        view.invalidate()
    }

    /**
     * Hide skeleton loading state
     */
    fun hide() {
        stopShimmerAnimation()
        view.isVisible = false
        view.setWillNotDraw(true)
    }

    /**
     * Draw skeleton on canvas
     */
    fun draw(canvas: Canvas) {
        val width = view.width.toFloat()
        val height = view.height.toFloat()

        // Create shimmer gradient
        val gradient = LinearGradient(
            shimmerTranslate - shimmerWidth,
            0f,
            shimmerTranslate,
            0f,
            intArrayOf(baseColor, shimmerColor, baseColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient

        // Draw skeleton rectangles
        drawSkeletonRects(canvas, width, height)
    }

    private fun drawSkeletonRects(canvas: Canvas, width: Float, height: Float) {
        val cornerRadius = 12f
        val spacing = 16f

        // Draw multiple skeleton lines
        val lineHeight = 16f
        val numLines = (height / (lineHeight + spacing)).toInt().coerceAtLeast(1)

        for (i in 0 until numLines) {
            val top = i * (lineHeight + spacing)
            val lineWidth = if (i == numLines - 1) width * 0.6f else width * 0.9f
            
            val rect = RectF(0f, top, lineWidth, top + lineHeight)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }
    }

    private fun startShimmerAnimation() {
        shimmerAnimator?.cancel()
        shimmerAnimator = ValueAnimator.ofFloat(0f, view.width.toFloat() + shimmerWidth).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                shimmerTranslate = animation.animatedValue as Float
                view.invalidate()
            }
            start()
        }
    }

    private fun stopShimmerAnimation() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
    }

    companion object {
        /**
         * Create and show skeleton loader for a view
         */
        fun showSkeleton(view: View): SkeletonLoader {
            return SkeletonLoader(view).apply { show() }
        }

        /**
         * Show skeleton for multiple views
         */
        fun showSkeletons(vararg views: View): List<SkeletonLoader> {
            return views.map { showSkeleton(it) }
        }

        /**
         * Hide skeleton loaders
         */
        fun hideSkeletons(loaders: List<SkeletonLoader>) {
            loaders.forEach { it.hide() }
        }
    }
}

/**
 * Extension function to easily show skeleton on any view
 */
fun View.showSkeleton(): SkeletonLoader {
    return SkeletonLoader.showSkeleton(this)
}

/**
 * Extension function to show skeleton on ViewGroup children
 */
fun ViewGroup.showSkeletonChildren(): List<SkeletonLoader> {
    val loaders = mutableListOf<SkeletonLoader>()
    for (i in 0 until childCount) {
        loaders.add(getChildAt(i).showSkeleton())
    }
    return loaders
}
