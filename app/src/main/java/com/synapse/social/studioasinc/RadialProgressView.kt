package com.synapse.social.studioasinc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.Keep
import kotlin.math.abs
import kotlin.math.max

class RadialProgressView(context: Context) : View(context) {

    private var lastUpdateTime: Long = 0
    private var radOffset: Float = 0f
    private var currentCircleLength: Float = 0f
    private var risingCircleLength: Boolean = false
    private var currentProgressTime: Float = 0f
    private val cicleRect = RectF()
    private var useSelfAlpha: Boolean = false
    private var drawingCircleLenght: Float = 0f

    private var progressColor: Int = 0xff6B7278.toInt()

    private val decelerateInterpolator = DecelerateInterpolator()
    private val accelerateInterpolator = AccelerateInterpolator()
    private val progressPaint: Paint
    private var size: Int

    private var currentProgress: Float = 0f
    private var progressAnimationStart: Float = 0f
    private var progressTime: Int = 0
    private var animatedProgress: Float = 0f
    private var toCircle: Boolean = false
    private var toCircleProgress: Float = 0f

    private var noProgress = true

    init {
        size = dp(45f)

        progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = dp(3f).toFloat()
            color = progressColor
        }
    }

    fun setUseSelfAlpha(value: Boolean) {
        useSelfAlpha = value
    }

    @Keep
    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
        if (useSelfAlpha) {
            val background: Drawable? = background
            val a = (alpha * 255).toInt()
            background?.alpha = a
            progressPaint.alpha = a
        }
    }

    fun setNoProgress(value: Boolean) {
        noProgress = value
    }

    fun setProgress(value: Float) {
        currentProgress = value
        if (animatedProgress > value) {
            animatedProgress = value
        }
        progressAnimationStart = animatedProgress
        progressTime = 0
    }

    private fun updateAnimation() {
        val newTime = System.currentTimeMillis()
        var dt = newTime - lastUpdateTime
        if (dt > 17) {
            dt = 17
        }
        lastUpdateTime = newTime

        radOffset += 360 * dt / ROTATION_TIME
        val count = (radOffset / 360).toInt()
        radOffset -= count * 360

        if (toCircle && toCircleProgress != 1f) {
            toCircleProgress += 16 / 220f
            if (toCircleProgress > 1f) {
                toCircleProgress = 1f
            }
        } else if (!toCircle && toCircleProgress != 0f) {
            toCircleProgress -= 16 / 400f
            if (toCircleProgress < 0) {
                toCircleProgress = 0f
            }
        }

        if (noProgress) {
            if (toCircleProgress == 0f) {
                currentProgressTime += dt
                if (currentProgressTime >= RISING_TIME) {
                    currentProgressTime = RISING_TIME
                }
                currentCircleLength = if (risingCircleLength) {
                    4 + 266 * accelerateInterpolator.getInterpolation(currentProgressTime / RISING_TIME)
                } else {
                    4 - 270 * (1.0f - decelerateInterpolator.getInterpolation(currentProgressTime / RISING_TIME))
                }

                if (currentProgressTime == RISING_TIME) {
                    if (risingCircleLength) {
                        radOffset += 270
                        currentCircleLength = -266f
                    }
                    risingCircleLength = !risingCircleLength
                    currentProgressTime = 0f
                }
            } else {
                if (risingCircleLength) {
                    val old = currentCircleLength
                    currentCircleLength = 4 + 266 * accelerateInterpolator.getInterpolation(
                        currentProgressTime / RISING_TIME
                    )
                    currentCircleLength += 360 * toCircleProgress
                    val dx = old - currentCircleLength
                    if (dx > 0) {
                        radOffset += old - currentCircleLength
                    }
                } else {
                    val old = currentCircleLength
                    currentCircleLength = 4 - 270 * (1.0f - decelerateInterpolator.getInterpolation(
                        currentProgressTime / RISING_TIME
                    ))
                    currentCircleLength -= 364 * toCircleProgress
                    val dx = old - currentCircleLength
                    if (dx > 0) {
                        radOffset += old - currentCircleLength
                    }
                }
            }
        } else {
            val progressDiff = currentProgress - progressAnimationStart
            if (progressDiff > 0) {
                progressTime += dt.toInt()
                if (progressTime >= 200) {
                    animatedProgress = currentProgress
                    progressAnimationStart = currentProgress
                    progressTime = 0
                } else {
                    animatedProgress = progressAnimationStart + progressDiff *
                            decelerateInterpolator.getInterpolation(progressTime / 200.0f)
                }
            }
            currentCircleLength = max(4f, 360 * animatedProgress)
        }
        invalidate()
    }

    fun setSize(value: Int) {
        size = value
        invalidate()
    }

    fun setStrokeWidth(value: Float) {
        progressPaint.strokeWidth = dp(value).toFloat()
    }

    fun setProgressColor(color: Int) {
        progressColor = color
        progressPaint.color = progressColor
    }

    fun toCircle(toCircle: Boolean, animated: Boolean) {
        this.toCircle = toCircle
        if (!animated) {
            toCircleProgress = if (toCircle) 1f else 0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        val x = (measuredWidth - size) / 2
        val y = (measuredHeight - size) / 2
        cicleRect.set(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat())
        drawingCircleLenght = currentCircleLength
        canvas.drawArc(cicleRect, radOffset, drawingCircleLenght, false, progressPaint)
        updateAnimation()
    }

    fun draw(canvas: Canvas, cx: Float, cy: Float) {
        cicleRect.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)
        drawingCircleLenght = currentCircleLength
        canvas.drawArc(cicleRect, radOffset, drawingCircleLenght, false, progressPaint)
        updateAnimation()
    }

    fun isCircle(): Boolean {
        return abs(drawingCircleLenght) >= 360
    }

    private fun dp(px: Float): Int {
        return (context.resources.displayMetrics.density * px).toInt()
    }

    companion object {
        private const val ROTATION_TIME = 2000f
        private const val RISING_TIME = 500f
    }
}
