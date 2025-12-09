package com.synapse.social.studioasinc

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.roundToInt

class CenterCropLinearLayoutNoEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var backgroundDrawable: Drawable? = null

    init {
        if (attrs != null) {
            val attrsArray = intArrayOf(android.R.attr.background)
            val ta = context.obtainStyledAttributes(attrs, attrsArray)
            backgroundDrawable = ta.getDrawable(0)
            ta.recycle()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        backgroundDrawable?.let { drawable ->
            val viewWidth = width
            val viewHeight = height
            val drawableWidth = drawable.intrinsicWidth
            val drawableHeight = drawable.intrinsicHeight

            val scale = max(
                viewWidth.toFloat() / drawableWidth,
                viewHeight.toFloat() / drawableHeight
            )

            val scaledWidth = (scale * drawableWidth).roundToInt()
            val scaledHeight = (scale * drawableHeight).roundToInt()

            val dx = (viewWidth - scaledWidth) / 2
            val dy = (viewHeight - scaledHeight) / 2

            drawable.setBounds(dx, dy, dx + scaledWidth, dy + scaledHeight)
            drawable.draw(canvas)
        }

        super.dispatchDraw(canvas)
    }

    override fun setBackground(background: Drawable?) {
        backgroundDrawable = background
        invalidate()
    }

    override fun setBackgroundResource(resid: Int) {
        backgroundDrawable = ContextCompat.getDrawable(context, resid)
        invalidate()
    }

    @Deprecated("Use setBackground instead", ReplaceWith("setBackground(background)"))
    override fun setBackgroundDrawable(background: Drawable?) {
        backgroundDrawable = background
        invalidate()
    }
}

// codes by @studioasinc
// https://t.me/studioasinc
