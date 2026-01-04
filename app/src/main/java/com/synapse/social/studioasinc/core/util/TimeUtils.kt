package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.widget.TextView
import com.synapse.social.studioasinc.R
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun setTime(currentTime: Double, textView: TextView, context: Context) {
        val c1 = Calendar.getInstance()
        val c2 = Calendar.getInstance()
        val timeDiff = c1.timeInMillis - currentTime
        
        when {
            timeDiff < 60000 -> {
                val seconds = (timeDiff / 1000).toLong()
                if (seconds < 2) {
                    textView.text = "1s"
                } else {
                    textView.text = "${seconds}s"
                }
            }
            timeDiff < (60 * 60000) -> {
                val minutes = (timeDiff / 60000).toLong()
                textView.text = "${minutes}m"
            }
            timeDiff < (24 * 60 * 60000) -> {
                val hours = (timeDiff / (60 * 60000)).toLong()
                textView.text = "${hours}h"
            }
            timeDiff < (7 * 24 * 60 * 60000) -> {
                val days = (timeDiff / (24 * 60 * 60000)).toLong()
                textView.text = "${days}d"
            }
            else -> {
                val weeks = (timeDiff / (7 * 24 * 60 * 60000)).toLong()
                textView.text = "${weeks}w"
            }
        }
    }

    /**
     * Format timestamp to relative time string (e.g., "2h", "3d")
     */
    fun formatTimestamp(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - timestamp

        return when {
            diff < 0 -> "1s" // Future time? treat as just now
            diff < 60_000 -> "${(diff / 1000).coerceAtLeast(1)}s"
            diff < 3600_000 -> "${diff / 60_000}m"
            diff < 86400_000 -> "${diff / 3600_000}h"
            diff < 604800_000 -> "${diff / 86400_000}d"
            else -> "${diff / 604800_000}w"
        }
    }

    fun getTimeAgo(isoTimestamp: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val timestamp = sdf.parse(isoTimestamp.substringBefore('+').substringBefore('Z'))?.time ?: return "1s"
            formatTimestamp(timestamp)
        } catch (e: Exception) {
            "1s"
        }
    }
}
