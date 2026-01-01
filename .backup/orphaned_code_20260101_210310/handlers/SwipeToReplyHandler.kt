package com.synapse.social.studioasinc.chat.common.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import java.util.HashMap

class SwipeToReplyHandler(
    private val context: Context,
    private val chatMessagesList: ArrayList<HashMap<String, Any>>,
    private val onReply: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (position < 0 || position >= chatMessagesList.size) {
            return
        }

        val messageData = chatMessagesList[position]
        if (messageData["key"] == null) {
            viewHolder.itemView.animate().translationX(0f).setDuration(150).start()
            return
        }

        onReply(position)
        viewHolder.itemView.animate().translationX(0f).setDuration(150).start()
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return true
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
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
            val itemView = viewHolder.itemView
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_reply)
            if (icon != null) {
                icon.setColorFilter(0xFF616161.toInt(), PorterDuff.Mode.SRC_IN)

                val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + icon.intrinsicHeight

                val width = itemView.width.toFloat()
                val threshold = width * 0.25f
                val progress = Math.min(1f, Math.abs(dX) / threshold)
                icon.alpha = (Math.max(0.25f, progress) * 255).toInt()

                if (dX > 0) {
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon.draw(c)
                } else {
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon.draw(c)
                }
            }

            val dampedDx = dX * 0.75f
            itemView.translationX = dampedDx
            itemView.alpha = 1.0f
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.25f
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return defaultValue * 1.5f
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return defaultValue * 1.2f
    }

    fun attachToRecyclerView(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}
