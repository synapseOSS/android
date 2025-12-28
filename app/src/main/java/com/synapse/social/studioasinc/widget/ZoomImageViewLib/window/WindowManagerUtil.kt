package com.synapse.social.studioasinc.widget.ZoomImageViewLib.window

import android.os.Build
import android.os.Looper
import android.view.View
import android.view.WindowManager

object WindowManagerUtil {
    @Synchronized
    fun removeViewSafety(windowManager: WindowManager?, viewNeedRemove: View?) {
        if (windowManager == null || viewNeedRemove == null) return

        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (isAttachedToWindow(viewNeedRemove)) {
                try {
                    windowManager.removeView(viewNeedRemove)
                } catch (e: Exception) {
                    // Ignore
                }
            } else {
                try {
                    windowManager.removeView(viewNeedRemove)
                } catch (e2: Exception) {
                    // Ignore
                }
            }
        }
    }

    @Synchronized
    fun addViewSafety(windowManager: WindowManager?, viewNeedAdd: View?, params: WindowManager.LayoutParams?) {
        if (windowManager == null || viewNeedAdd == null || params == null) return

        if (Looper.myLooper() == Looper.getMainLooper() && !isAttachedToWindow(viewNeedAdd)) {
            try {
                windowManager.addView(viewNeedAdd, params)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun isAttachedToWindow(view: View): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.isAttachedToWindow
        } else {
            view.windowToken != null
        }
    }
}
