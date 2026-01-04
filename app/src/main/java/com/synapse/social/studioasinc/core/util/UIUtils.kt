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

package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.util.TypedValue
import android.view.View

/**
 * Utility class for common UI operations.
 * Centralizes reusable UI helper methods to follow DRY principle.
 */
object UIUtils {

    /**
     * Converts density-independent pixels (dp) to pixels (px).
     * 
     * @param context The context to get display metrics from
     * @param dp The value in dp to convert
     * @return The equivalent value in pixels
     */
    fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * Converts density-independent pixels (dp) to pixels (px) using a view's context.
     * 
     * @param view The view to get context from
     * @param dp The value in dp to convert
     * @return The equivalent value in pixels
     */
    fun dpToPx(view: View, dp: Int): Int {
        return dpToPx(view.context, dp)
    }

    /**
     * Converts pixels (px) to density-independent pixels (dp).
     * 
     * @param context The context to get display metrics from
     * @param px The value in px to convert
     * @return The equivalent value in dp
     */
    fun pxToDp(context: Context, px: Int): Int {
        return (px / context.resources.displayMetrics.density).toInt()
    }

    /**
     * Converts scale-independent pixels (sp) to pixels (px).
     * 
     * @param context The context to get display metrics from
     * @param sp The value in sp to convert
     * @return The equivalent value in pixels
     */
    fun spToPx(context: Context, sp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 
            sp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
