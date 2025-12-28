package com.synapse.social.studioasinc.widget.ZoomImageViewLib.gestures

import android.view.ScaleGestureDetector

interface OnScaleAndMoveGestureListener {
    fun onScaleAndMove(scaleGestureDetector: ScaleGestureDetector, currentScale: Float, moveDistanceFromX: Float, moveDistanceFromY: Float)
    fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector)
    fun onScaleEnd(scaleGestureDetector: ScaleGestureDetector, moveDistanceFromX: Float, moveDistanceFromY: Float)
}
