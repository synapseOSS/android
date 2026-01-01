package com.synapse.social.studioasinc.widget.ZoomImageViewLib

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.Interpolator
import android.widget.ImageView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.widget.ZoomImageViewLib.animation.AnimCompat
import com.synapse.social.studioasinc.widget.ZoomImageViewLib.animation.SpringInterpolator
import com.synapse.social.studioasinc.widget.ZoomImageViewLib.gestures.OnScaleAndMoveGestureListener
import com.synapse.social.studioasinc.widget.ZoomImageViewLib.gestures.ScaleAndMoveDetector
import com.synapse.social.studioasinc.widget.ZoomImageViewLib.window.WindowManagerUtil
import java.lang.ref.WeakReference
import kotlin.math.pow
import kotlin.math.sqrt

class ZoomInImageViewAttacher(imageView: ImageView) : View.OnTouchListener {

    private var zoomDuration = 1000
    private var hasInterruptParentNotToHandleTouchEvent = false
    private var mAnimInterpolator: Interpolator = SpringInterpolator(1.0f)
    private val mBaseMatrix = Matrix()
    private val mDrawMatrix = Matrix()
    private var mImageView: WeakReference<ImageView>? = null
    private val mMatrixValues = FloatArray(9)
    private var mOffsetToLeft = 0
    private var mOffsetToTop = 0
    private var mScaleGestureDetector: ScaleAndMoveDetector? = null
    private val mSuppMatrix = Matrix()
    private var mWindowLayout: View? = null
    private var mWindowLayoutParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    private var mZoomBitmap: Bitmap? = null
    private var mZoomEnabled = true
    private var mZoomIV: ImageView? = null

    init {
        attachImageView(imageView)
    }

    fun attachImageView(imageView: ImageView?) {
        if (imageView == null) {
            throw NullPointerException("imageview is null")
        }
        mImageView = WeakReference(imageView)
        imageView.setOnTouchListener(this)
        initGestureDectector()
    }

    @Synchronized
    fun initGestureDectector() {
        val imageView = getImageView() ?: return

        mScaleGestureDetector = ScaleAndMoveDetector(imageView.context, object : OnScaleAndMoveGestureListener {
            override fun onScaleAndMove(detector: ScaleGestureDetector, currentScale: Float, moveDistanceFromX: Float, moveDistanceFromY: Float) {
                if (mZoomIV == null) {
                    mScaleGestureDetector?.setStartScale(detector.scaleFactor)
                    return
                }
                mSuppMatrix.reset()

                val currentImageView = getImageView() ?: return
                val centerX = mOffsetToLeft + currentImageView.width / 2.0f
                val centerY = mOffsetToTop + currentImageView.height / 2.0f

                var scale = currentScale
                if (scale >= 1.0) {
                    mSuppMatrix.postScale(scale, scale, centerX, centerY)
                } else {
                    scale = 1.0f
                    mSuppMatrix.postScale(1.0f, 1.0f, centerX, centerY)
                }
                mSuppMatrix.postTranslate(moveDistanceFromX, moveDistanceFromY)

                if (mWindowLayout != null) {
                    if (scale < 1.0) {
                        mWindowLayout?.setBackgroundColor(Color.argb(0, 0, 0, 0))
                    } else if (scale > 3.0) {
                        mWindowLayout?.setBackgroundColor(Color.argb(200, 0, 0, 0))
                    } else {
                        mWindowLayout?.setBackgroundColor(Color.argb(((200.0 * (scale - 1.0)) / 2.0).toInt(), 0, 0, 0))
                    }
                }
                setImageViewMatrix(getDrawMatrix())
            }

            override fun onScaleBegin(detector: ScaleGestureDetector) {
                if (mZoomBitmap == null) {
                    val currentImageView = getImageView() ?: return
                    currentImageView.isDrawingCacheEnabled = true
                    if (currentImageView.drawingCache != null) {
                        mZoomBitmap = Bitmap.createBitmap(currentImageView.drawingCache)
                    }
                    currentImageView.destroyDrawingCache()
                }

                mZoomBitmap?.let {
                    createZoomImage(it, mOffsetToLeft, mOffsetToTop)
                }
            }

            override fun onScaleEnd(detector: ScaleGestureDetector, moveDistanceFromX: Float, moveDistanceFromY: Float) {
                onReleaseZoom(moveDistanceFromX, moveDistanceFromY)
            }
        })
    }

    fun setZoomReleaseAnimInterpolator(animInterpolator: Interpolator?) {
        if (animInterpolator != null) {
            mAnimInterpolator = animInterpolator
        }
    }

    fun setZoomReleaseAnimDuration(duration: Int) {
        if (duration > 0) {
            zoomDuration = duration
        }
    }

    fun setZoomable(zoomable: Boolean) {
        mZoomEnabled = zoomable
    }

    fun detach() {
        clean()
    }

    private fun clean() {
        if (mImageView != null) {
            mImageView?.get()?.setOnTouchListener(null)
            mImageView = null
        }
        mWindowLayoutParams = null
        mWindowLayout = null
        mWindowManager = null
        mScaleGestureDetector = null
        recycleZoomBitmap()
    }

    private fun onReleaseZoom(disx: Float, disy: Float) {
        if (getImageView() != null && mZoomIV != null && mZoomBitmap != null) {
            mZoomIV?.post(
                AnimatedZoomRunnable(
                    scale, 1.0f,
                    mOffsetToLeft + mZoomBitmap!!.width / 2.0f,
                    mOffsetToTop + mZoomBitmap!!.height / 2.0f,
                    disx, disy
                )
            )
        }
    }

    private fun removeZoomImage() {
        hasInterruptParentNotToHandleTouchEvent = false
        if (mZoomIV != null) {
            mZoomIV?.visibility = View.INVISIBLE
            if (mWindowManager != null && mWindowLayout != null) {
                WindowManagerUtil.removeViewSafety(mWindowManager, mWindowLayout)
            }
            mZoomIV = null
            recycleZoomBitmap()
        }
    }

    private fun recycleZoomBitmap() {
        if (mZoomBitmap != null && !mZoomBitmap!!.isRecycled) {
            mZoomBitmap!!.recycle()
            mZoomBitmap = null
        }
    }

    @Synchronized
    private fun createZoomImage(bitmap: Bitmap, offsetToLeft: Int, offsetToTop: Int) {
        val imageView = getImageView() ?: return

        if (mWindowLayoutParams == null || mWindowLayout == null) {
            mWindowLayoutParams = WindowManager.LayoutParams()
            mWindowLayoutParams?.format = 1
            mWindowLayoutParams?.alpha = 1.0f
            mWindowLayoutParams?.width = -1
            mWindowLayoutParams?.height = -1
            mWindowLayoutParams?.type = 1000 // TYPE_APPLICATION_PANEL or similar depending on needs, originally 1000
            mWindowLayout = LayoutInflater.from(imageView.context).inflate(R.layout.layout_zoominimage, null as ViewGroup?)
        }

        mWindowLayout?.isClickable = true
        mZoomIV = mWindowLayout?.findViewById(R.id.iv_zoominpic)
        mZoomIV?.visibility = View.VISIBLE
        mZoomIV?.setImageBitmap(bitmap)
        mBaseMatrix.reset()
        mSuppMatrix.reset()
        mBaseMatrix.postTranslate(offsetToLeft.toFloat(), offsetToTop.toFloat())
        mDrawMatrix.set(mBaseMatrix)
        mZoomIV?.scaleType = ImageView.ScaleType.MATRIX
        setImageViewMatrix(mDrawMatrix)

        mWindowLayout?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (mImageView?.get() != null) {
                    getImageView()?.postDelayed({
                        getImageView()?.visibility = View.INVISIBLE
                    }, 300)
                }
                mWindowLayout?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            }
        })

        Log.e("ivd", "show drag image")
        if (mWindowManager == null) {
            mWindowManager = imageView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }
        WindowManagerUtil.addViewSafety(mWindowManager, mWindowLayout, mWindowLayoutParams)
    }

    fun getDrawMatrix(): Matrix {
        mDrawMatrix.set(mBaseMatrix)
        mDrawMatrix.postConcat(mSuppMatrix)
        return mDrawMatrix
    }

    private fun setImageViewMatrix(matrix: Matrix) {
        mZoomIV?.imageMatrix = matrix
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (mZoomEnabled) {
            if (event.pointerCount < 2) {
                hasInterruptParentNotToHandleTouchEvent = false
                view.parent.requestDisallowInterceptTouchEvent(false)
            } else if (!hasInterruptParentNotToHandleTouchEvent) {
                view.parent.requestDisallowInterceptTouchEvent(true)
                hasInterruptParentNotToHandleTouchEvent = true
            }
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mOffsetToTop = (event.rawY - event.y).toInt()
                mOffsetToLeft = (event.rawX - event.x).toInt()
            }
        }

        var handled = false
        if (mZoomEnabled && mScaleGestureDetector != null && mScaleGestureDetector!!.onTouchEvent(event)) {
            handled = true
        }

        // Return true if handled or if the view handles it, but match original Java logic
        if (view.onTouchEvent(event)) {
            return true
        }
        return handled
    }

    fun getImageView(): ImageView? {
        if (mImageView != null) {
            return mImageView?.get()
        }
        clean()
        return null
    }

    val scale: Float
        get() = sqrt(
            (getValue(mSuppMatrix, 0).toDouble().pow(2.0) +
                    getValue(mSuppMatrix, 3).toDouble().pow(2.0))
        ).toFloat()

    private fun getValue(matrix: Matrix, whichValue: Int): Float {
        matrix.getValues(mMatrixValues)
        return mMatrixValues[whichValue]
    }

    private inner class AnimatedZoomRunnable(
        private val mZoomStartScale: Float,
        private val mZoomEndScale: Float,
        private val mFocalX: Float,
        private val mFocalY: Float,
        private val mTranslateDistanceX: Float,
        private val mTranslateDistanceY: Float
    ) : Runnable {
        private val mStartTime = System.currentTimeMillis()

        init {
            mWindowLayout?.setBackgroundColor(Color.argb(0, 0, 0, 0))
        }

        override fun run() {
            val imageView = mZoomIV
            if (imageView != null) {
                val time = (1.0f * (System.currentTimeMillis() - mStartTime)) / zoomDuration
                val t = mAnimInterpolator.getInterpolation(time)
                val scales = mZoomStartScale + (mZoomEndScale - mZoomStartScale) * t
                mSuppMatrix.reset()
                mSuppMatrix.postScale(scales, scales, mFocalX, mFocalY)
                mSuppMatrix.postTranslate(
                    mTranslateDistanceX + (0.0f - mTranslateDistanceX) * t,
                    mTranslateDistanceY + (0.0f - mTranslateDistanceY) * t
                )
                setImageViewMatrix(getDrawMatrix())

                var stopTime = 1.0f
                if (mAnimInterpolator is SpringInterpolator) {
                    stopTime = 0.8f
                }

                if (time < stopTime) {
                    AnimCompat.postOnAnimation(imageView, this)
                } else {
                    getImageView()?.post {
                        getImageView()?.visibility = View.VISIBLE
                        removeZoomImage()
                    }
                }
            }
        }
    }
}
