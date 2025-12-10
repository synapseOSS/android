package com.synapse.social.studioasinc.animations

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.synapse.social.studioasinc.R

class ShimmerFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val shimmerPaint = Paint()
    private var valueAnimator: ValueAnimator? = null
    private val viewRect = RectF()

    private var isShimmering = false
    private var shimmerDuration = 1200L
    private var shader: Shader? = null
    private var shimmerColor = Color.argb(68, 255, 255, 255) // 44 in hex = 68 in decimal

    init {
        setWillNotDraw(false)
        val a = context.obtainStyledAttributes(attrs, R.styleable.ShimmerFrameLayout, 0, 0)
        shimmerDuration = a.getInt(R.styleable.ShimmerFrameLayout_shimmer_duration, 1200).toLong()
        shimmerColor = a.getColor(R.styleable.ShimmerFrameLayout_shimmer_color, shimmerColor)
        val autoStart = a.getBoolean(R.styleable.ShimmerFrameLayout_shimmer_auto_start, false)
        a.recycle()

        if (autoStart) {
            post { startShimmer() }
        }
        shimmerPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    private fun createShader(): Shader {
        val transparentColor = Color.TRANSPARENT
        return LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(transparentColor, shimmerColor, transparentColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun startShimmer() {
        if (isShimmering) return
        isShimmering = true
        if (valueAnimator == null) {
            valueAnimator = createAnimator()
        }
        valueAnimator?.start()
        invalidate()
    }

    fun stopShimmer() {
        if (!isShimmering) return
        isShimmering = false
        valueAnimator?.cancel()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopShimmer()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            viewRect.set(0f, 0f, width.toFloat(), height.toFloat())
            shader = null // Recreate shader on size change
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (!isShimmering || width <= 0 || height <= 0) {
            super.dispatchDraw(canvas)
            return
        }

        // Save the canvas to a new layer. This is crucial for the xfermode to work correctly.
        val saveCount = canvas.saveLayer(viewRect, null)

        // First, draw the children of this FrameLayout normally onto the layer.
        super.dispatchDraw(canvas)

        // Create the shader if it doesn't exist for the current view dimensions.
        if (shader == null) {
            shader = createShader()
            shimmerPaint.shader = shader
        }

        // Now, draw a rectangle over the whole layer with the shimmer paint.
        // The SRC_IN mode means the shimmer gradient will only be drawn where the
        // destination (the children) already has pixels.
        canvas.drawRect(viewRect, shimmerPaint)

        // Restore the layer, effectively blending it onto the main canvas.
        canvas.restoreToCount(saveCount)
    }

    private fun createAnimator(): ValueAnimator {
        val animator = ValueAnimator.ofFloat(-1.5f, 1.5f) // Animate from off-screen left to off-screen right
        animator.interpolator = LinearInterpolator()
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = ValueAnimator.INFINITE
        animator.duration = shimmerDuration
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            val translate = width * animatedValue
            val matrix = Matrix()
            matrix.setTranslate(translate, 0f)
            shader?.setLocalMatrix(matrix)
            invalidate()
        }
        return animator
    }
}
