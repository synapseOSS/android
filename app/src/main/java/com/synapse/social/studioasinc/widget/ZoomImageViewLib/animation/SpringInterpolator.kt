package com.synapse.social.studioasinc.widget.ZoomImageViewLib.animation

import android.view.animation.Interpolator
import kotlin.math.pow
import kotlin.math.sin

class SpringInterpolator(private val factor: Float) : Interpolator {
    override fun getInterpolation(input: Float): Float {
        return (2.0.pow((-10.0f * input).toDouble()) * sin(((input - factor / 4.0f) * 6.283185307179586).toDouble() / factor) + 1.0).toFloat()
    }
}
