package com.synapse.social.studioasinc.util

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar

fun Activity.setStateColor(statusColor: Int, navigationColor: Int) {
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    window.statusBarColor = statusColor
    window.navigationBarColor = navigationColor
}

fun ImageView.setImageColor(color: Int) {
    setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
}

fun View.setViewGraphics(onFocus: Int, onRipple: Int, radius: Double, stroke: Double, strokeColor: Int) {
    val gradientDrawable = GradientDrawable()
    gradientDrawable.setColor(onFocus)
    gradientDrawable.cornerRadius = radius.toFloat()
    gradientDrawable.setStroke(stroke.toInt(), strokeColor)
    val rippleDrawable = RippleDrawable(ColorStateList(arrayOf(intArrayOf()), intArrayOf(onRipple)), gradientDrawable, null)
    background = rippleDrawable
}

fun View.setGradientDrawable(color: Int, cornerRadius: Float, strokeWidth: Int = 0, strokeColor: Int = Color.TRANSPARENT) {
    val gradientDrawable = GradientDrawable()
    gradientDrawable.setColor(color)
    gradientDrawable.cornerRadius = cornerRadius
    if (strokeWidth > 0) {
        gradientDrawable.setStroke(strokeWidth, strokeColor)
    }
    background = gradientDrawable
}

fun ProgressBar.setProgressBarColor(color: Int) {
    val colorStateList = ColorStateList.valueOf(color)
    indeterminateTintList = colorStateList
    progressTintList = colorStateList
}
