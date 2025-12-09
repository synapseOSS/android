package com.synapse.social.studioasinc.chat

import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import kotlin.math.abs
import kotlin.math.min

/**
 * ItemTouchHelper callback for swipe-to-reply gesture in chat
 * Implements Material Design swipe gesture with visual feedback
 */
class SwipeToReplyCallback(
    private val context: Context,
    private val onSwipeToReply: (position: Int) -> Unit
) : ItemTouchHelper.Callback() {

    private var replyIcon: Drawable? = null
    private var currentItemViewHolder: RecyclerView.ViewHolder? = null
    private var swipeBack = false
    private var isVibrated = false
    private var startTracking = false

    companion object {
        private const val SWIPE_THRESHOLD = 0.3f // 30% of view width to trigger reply
        private const val ICON_HORIZONTAL_MARGIN = 32f
        private const val HAPTIC_FEEDBACK_THRESHOLD = 0.25f // 25% of view width
    }

    init {
        replyIcon = ContextCompat.getDrawable(context, R.drawable.ic_reply)
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Enable swipe from right to left (for both incoming and outgoing messages)
        val swipeFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        return makeMovementFlags(0, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false // We don't support move
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // This is called when swipe threshold is reached
        // We handle the reply action in onChildDraw instead
    }

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
        if (swipeBack) {
            swipeBack = false
            return 0
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            setTouchListener(recyclerView, viewHolder, dX)
        }

        // Draw reply icon
        drawReplyIcon(c, viewHolder, dX)

        // Limit swipe distance
        val maxSwipe = viewHolder.itemView.width * SWIPE_THRESHOLD
        val limitedDx = when {
            dX > 0 -> min(dX, maxSwipe)
            dX < 0 -> kotlin.math.max(dX, -maxSwipe)
            else -> dX
        }

        super.onChildDraw(c, recyclerView, viewHolder, limitedDx, dY, actionState, isCurrentlyActive)
    }

    private fun drawReplyIcon(c: Canvas, viewHolder: RecyclerView.ViewHolder, dX: Float) {
        val itemView = viewHolder.itemView
        val icon = replyIcon ?: return

        val iconSize = 24 * context.resources.displayMetrics.density.toInt()
        val iconMargin = (ICON_HORIZONTAL_MARGIN * context.resources.displayMetrics.density).toInt()

        val iconTop = itemView.top + (itemView.height - iconSize) / 2
        val iconBottom = iconTop + iconSize

        // Calculate icon position and alpha based on swipe distance
        val swipeProgress = abs(dX) / (itemView.width * SWIPE_THRESHOLD)
        val alpha = (min(swipeProgress, 1f) * 255).toInt()

        if (dX > 0) {
            // Swipe right - icon on left
            val iconLeft = itemView.left + iconMargin
            val iconRight = iconLeft + iconSize
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        } else if (dX < 0) {
            // Swipe left - icon on right
            val iconRight = itemView.right - iconMargin
            val iconLeft = iconRight - iconSize
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        }

        // Apply alpha and color
        icon.alpha = alpha
        icon.setColorFilter(
            ContextCompat.getColor(context, R.color.md_theme_primary),
            PorterDuff.Mode.SRC_IN
        )
        icon.draw(c)
    }

    private fun setTouchListener(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float
    ) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack = event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP

            if (swipeBack) {
                val swipeThreshold = viewHolder.itemView.width * SWIPE_THRESHOLD
                
                if (abs(dX) >= swipeThreshold) {
                    // Trigger reply action
                    onSwipeToReply(viewHolder.bindingAdapterPosition)
                    
                    // Trigger haptic feedback
                    viewHolder.itemView.performHapticFeedback(
                        HapticFeedbackConstants.CONTEXT_CLICK,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                }
                
                // Reset state
                currentItemViewHolder = null
                isVibrated = false
                startTracking = false
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                // Trigger haptic feedback when reaching threshold
                val hapticThreshold = viewHolder.itemView.width * HAPTIC_FEEDBACK_THRESHOLD
                if (abs(dX) >= hapticThreshold && !isVibrated) {
                    viewHolder.itemView.performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                    isVibrated = true
                }
                
                if (abs(dX) < 10) {
                    isVibrated = false
                }
            }

            false
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return SWIPE_THRESHOLD
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return defaultValue * 10 // Make it harder to accidentally trigger
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return defaultValue * 5 // Require faster swipe to trigger
    }
}
