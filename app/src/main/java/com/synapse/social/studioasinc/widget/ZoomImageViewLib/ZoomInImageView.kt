package com.synapse.social.studioasinc.widget.ZoomImageViewLib

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class ZoomInImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val attacher: ZoomInImageViewAttacher = ZoomInImageViewAttacher(this)

}
