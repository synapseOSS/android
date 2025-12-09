package com.synapse.social.studioasinc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText

/**
 * Modified by StudioAs Inc. 2024
 * Enhanced with dynamic background switching based on line count
 */
class FadeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var horiz: Int = 0
    private var fadeTextColor: Int = 0xFF000000.toInt()
    private var currentLineCount: Int = 1
    private var parentContainer: ViewGroup? = null
    private var isUpdatingBackground = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Cache parent reference once
        parentContainer = parent as? ViewGroup
    }

    override fun onScrollChanged(horiz: Int, vert: Int, oldHoriz: Int, oldVert: Int) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert)
        this.horiz = horiz
        requestLayout()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // Only update on actual layout changes
        if (changed) {
            updateBackgroundBasedOnLines()
        }
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        // Check line count immediately without post
        updateBackgroundBasedOnLines()
    }

    /**
     * Dynamically switch background based on line count
     * Single line = pill shape (300dp radius)
     * Multi-line = rounded rectangle (24dp radius)
     * Optimized to only update when line count actually changes
     */
    private fun updateBackgroundBasedOnLines() {
        // Prevent recursive calls
        if (isUpdatingBackground) return
        
        val lineCount = lineCount
        
        // Only update if line count changed
        if (lineCount != currentLineCount) {
            isUpdatingBackground = true
            currentLineCount = lineCount
            
            // Use cached parent reference
            parentContainer?.let { parent ->
                if (lineCount <= 1) {
                    // Single line: use pill shape
                    parent.setBackgroundResource(R.drawable.bg_message_input)
                } else {
                    // Multi-line: use rounded rectangle
                    parent.setBackgroundResource(R.drawable.bg_message_input_expanded)
                }
            }
            
            isUpdatingBackground = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        val width = measuredWidth
        fadeTextColor = currentTextColor

        val text = text
        if (text != null && text.length > 1 && layout.getLineWidth(0) > width) {
            val widthRight = measuredWidth + horiz
            val percent = measuredWidth * 20 / 100

            val widthPreLeft = horiz
            val widthLeft = horiz

            val stopPreLeft = widthPreLeft.toFloat() / widthRight.toFloat()

            val stopLeft = if (widthPreLeft > 0) {
                (widthLeft + percent).toFloat() / widthRight.toFloat()
            } else {
                0f
            }

            val stopRight = if (layout.getLineWidth(0) > widthRight) {
                (widthRight - percent).toFloat() / widthRight.toFloat()
            } else {
                widthRight.toFloat() / widthRight.toFloat()
            }

            val gradient = LinearGradient(
                0f, 0f, widthRight.toFloat(), 0f,
                intArrayOf(fadeTextColor, Color.TRANSPARENT, fadeTextColor, fadeTextColor, Color.TRANSPARENT),
                floatArrayOf(0f, stopPreLeft, stopLeft, stopRight, 1.0f),
                Shader.TileMode.CLAMP
            )

            paint.shader = gradient
        } else {
            paint.shader = null
        }

        super.onDraw(canvas)
    }
}
