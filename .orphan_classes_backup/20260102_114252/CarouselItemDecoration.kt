/**
 * CONFIDENTIAL AND PROPRIETARY
 * 
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 * 
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.util.UIUtils

class CarouselItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        val spacingPx = UIUtils.dpToPx(view, spacing)

        // Add spacing to start of first item
        if (position == 0) {
            outRect.left = spacingPx
        }

        // Add spacing to end of all items
        outRect.right = spacingPx

        // Add extra spacing to end of last item for better UX
        if (position == itemCount - 1) {
            outRect.right = spacingPx * 2
        }
    }

    companion object {
        /**
         * Creates decoration with standard carousel spacing from dimens.xml
         */
        fun createWithStandardSpacing(view: View): CarouselItemDecoration {
            val spacingDp = (view.resources.getDimension(R.dimen.chat_carousel_spacing) /
                    view.resources.displayMetrics.density).toInt()
            return CarouselItemDecoration(spacingDp)
        }
    }
}
