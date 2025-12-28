package com.synapse.social.studioasinc.widget.ZoomImageViewLib.gestures

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class ScaleAndMoveDetector(context: Context, private val mDetectorListener: OnScaleAndMoveGestureListener?) : ScaleGestureDetector.OnScaleGestureListener {
    private var lastX = 0.0f
    private var lastY = 0.0f
    private var mCurrentScale = 1.0f
    private val mDetector: ScaleGestureDetector = ScaleGestureDetector(context, this)
    private var moveDisFromX = 0.0f
    private var moveDisFromY = 0.0f
    private var scaleStart = 1.0f

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val tempx = detector.focusX
        val tempy = detector.focusY
        moveDisFromX = tempx - lastX
        moveDisFromY = tempy - lastY

        mDetectorListener?.onScaleAndMove(detector, getCurrentScale(detector.scaleFactor), moveDisFromX, moveDisFromY)
        return false
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        lastX = detector.focusX
        lastY = detector.focusY
        moveDisFromX = 0.0f
        moveDisFromY = 0.0f
        scaleStart = detector.scaleFactor

        mDetectorListener?.onScaleBegin(detector)
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        mDetectorListener?.onScaleEnd(detector, moveDisFromX, moveDisFromY)
    }

    fun setStartScale(scaleFactor: Float) {
        scaleStart = scaleFactor
    }

    private fun getCurrentScale(scaleFactor: Float): Float {
        mCurrentScale = (1.0f / scaleStart) * scaleFactor
        return mCurrentScale
    }

    fun onTouchEvent(ev: MotionEvent): Boolean {
        return mDetector.onTouchEvent(ev)
    }
}
