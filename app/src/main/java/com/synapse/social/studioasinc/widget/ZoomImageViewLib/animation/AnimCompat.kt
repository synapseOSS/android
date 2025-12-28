package com.synapse.social.studioasinc.widget.ZoomImageViewLib.animation

import android.os.Build
import android.view.View

object AnimCompat {
    private const val SIXTY_FPS_INTERVAL = 16L

    fun postOnAnimation(view: View, runnable: Runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.postOnAnimation(runnable)
        } else {
            view.postDelayed(runnable, SIXTY_FPS_INTERVAL)
        }
    }
}
