package com.synapse.social.studioasinc.chat.common.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.ChatConstants
import java.util.HashMap

class ChatNavigator(
    private val activity: Activity,
    private val recyclerView: RecyclerView,
    private val messages: ArrayList<HashMap<String, Any>>
) {

    fun scrollToMessage(messageKey: String) {
        val position = findMessagePosition(messageKey)
        if (position != -1) {
            recyclerView.smoothScrollToPosition(position)
            Handler(Looper.getMainLooper()).postDelayed({
                if (!activity.isFinishing && !activity.isDestroyed) {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                    viewHolder?.itemView?.let { highlightMessage(it) }
                }
            }, 500)
        } else {
            Toast.makeText(activity, "Original message not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findMessagePosition(messageKey: String): Int {
        for (i in messages.indices) {
            if (messages[i][ChatConstants.KEY_KEY]?.toString() == messageKey) {
                return i
            }
        }
        return -1
    }

    private fun highlightMessage(messageView: View) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val originalBackground = messageView.background
        val highlightAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            addUpdateListener { animation ->
                if (activity.isFinishing || activity.isDestroyed) {
                    animation.cancel()
                    return@addUpdateListener
                }

                val progress = animation.animatedValue as Float
                val alpha = (100 * (1 - progress)).toInt()
                val color = Color.argb(alpha, 107, 76, 255)

                val highlightDrawable = GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = dpToPx(27).toFloat()
                }
                messageView.background = highlightDrawable
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        messageView.background = originalBackground
                    }
                }
            })
        }
        highlightAnimator.start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * activity.resources.displayMetrics.density).toInt()
    }
}
