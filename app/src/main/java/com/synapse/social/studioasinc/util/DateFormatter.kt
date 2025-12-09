package com.synapse.social.studioasinc.util

import android.content.Context
import com.synapse.social.studioasinc.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * A utility object for formatting dates.
 */
object DateFormatter {

    /**
     * Formats a timestamp into a human-readable "time ago" format.
     *
     * @param context The context.
     * @param timestamp The timestamp to format.
     * @return The formatted string.
     */
    fun format(context: Context, timestamp: Long): String {
        val now = Calendar.getInstance()
        val time = Calendar.getInstance().apply { timeInMillis = timestamp }

        val diff = now.timeInMillis - time.timeInMillis
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> context.getString(R.string.seconds_ago, seconds)
            minutes < 60 -> context.getString(R.string.minutes_ago, minutes)
            hours < 24 -> context.getString(R.string.hours_ago, hours)
            days < 7 -> context.getString(R.string.days_ago, days)
            else -> SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(time.time)
        }
    }
}
