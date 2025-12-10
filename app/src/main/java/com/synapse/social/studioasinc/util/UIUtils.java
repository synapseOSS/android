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

package com.synapse.social.studioasinc.util;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;

/**
 * Utility class for common UI operations.
 * Centralizes reusable UI helper methods to follow DRY principle.
 */
public class UIUtils {

    /**
     * Converts density-independent pixels (dp) to pixels (px).
     * 
     * @param context The context to get display metrics from
     * @param dp The value in dp to convert
     * @return The equivalent value in pixels
     */
    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            dp, 
            context.getResources().getDisplayMetrics()
        );
    }

    /**
     * Converts density-independent pixels (dp) to pixels (px) using a view's context.
     * 
     * @param view The view to get context from
     * @param dp The value in dp to convert
     * @return The equivalent value in pixels
     */
    public static int dpToPx(View view, int dp) {
        return dpToPx(view.getContext(), dp);
    }

    /**
     * Converts pixels (px) to density-independent pixels (dp).
     * 
     * @param context The context to get display metrics from
     * @param px The value in px to convert
     * @return The equivalent value in dp
     */
    public static int pxToDp(Context context, int px) {
        return (int) (px / context.getResources().getDisplayMetrics().density);
    }

    /**
     * Converts scale-independent pixels (sp) to pixels (px).
     * 
     * @param context The context to get display metrics from
     * @param sp The value in sp to convert
     * @return The equivalent value in pixels
     */
    public static int spToPx(Context context, int sp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 
            sp, 
            context.getResources().getDisplayMetrics()
        );
    }
}