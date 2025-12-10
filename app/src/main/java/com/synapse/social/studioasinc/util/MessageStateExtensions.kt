package com.synapse.social.studioasinc.util

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.MessageDeliveryStatus

/**
 * Extension function to set message state icon based on delivery status
 */
fun ImageView.setMessageState(state: MessageDeliveryStatus) {
    when (state) {
        MessageDeliveryStatus.SENDING -> {
            setImageResource(R.drawable.ic_check_single)
            alpha = 0.5f // Show as semi-transparent while sending
            clearColorFilter()
        }
        MessageDeliveryStatus.SENT -> {
            setImageResource(R.drawable.ic_check_single)
            alpha = 1.0f
            clearColorFilter()
        }
        MessageDeliveryStatus.DELIVERED -> {
            setImageResource(R.drawable.ic_check_double)
            alpha = 1.0f
            clearColorFilter()
        }
        MessageDeliveryStatus.READ -> {
            setImageResource(R.drawable.ic_check_double)
            alpha = 1.0f
            setColorFilter(ContextCompat.getColor(context, R.color.md_theme_primary))
        }
        MessageDeliveryStatus.FAILED -> {
            setImageResource(R.drawable.ic_error)
            alpha = 1.0f
            setColorFilter(ContextCompat.getColor(context, R.color.md_theme_error))
        }
    }
}

/**
 * Extension function to set message state icon based on string state
 * This provides compatibility with string-based state systems
 */
fun ImageView.setMessageState(state: String) {
    val deliveryStatus = when (state.lowercase()) {
        "sending" -> MessageDeliveryStatus.SENDING
        "sent" -> MessageDeliveryStatus.SENT
        "delivered" -> MessageDeliveryStatus.DELIVERED
        "read" -> MessageDeliveryStatus.READ
        "failed" -> MessageDeliveryStatus.FAILED
        else -> MessageDeliveryStatus.SENT // Default fallback
    }
    setMessageState(deliveryStatus)
}
