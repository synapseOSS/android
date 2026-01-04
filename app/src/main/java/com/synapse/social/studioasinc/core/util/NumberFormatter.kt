package com.synapse.social.studioasinc.core.util

import java.text.DecimalFormat

/**
 * A utility object for formatting numbers.
 */
object NumberFormatter {

    /**
     * Formats a number into a shorthand form (e.g., 1K, 1M, 1B, 1T).
     *
     * @param number The number to format.
     * @return The formatted string.
     */
    fun format(number: Double): String {
        if (number < 10000) {
            return number.toLong().toString()
        }

        val decimalFormat = DecimalFormat("0.0")
        return when {
            number < 1_000_000 -> decimalFormat.format(number / 1_000) + "K"
            number < 1_000_000_000 -> decimalFormat.format(number / 1_000_000) + "M"
            number < 1_000_000_000_000L -> decimalFormat.format(number / 1_000_000_000) + "B"
            else -> decimalFormat.format(number / 1_000_000_000_000L) + "T"
        }
    }

    /**
     * Format large numbers for display (e.g., 1.2K, 3.5M)
     */
    fun formatCount(count: Int): String {
        return when {
            count < 1000 -> count.toString()
            count < 1_000_000 -> String.format("%.1fK", count / 1000.0)
            else -> String.format("%.1fM", count / 1_000_000.0)
        }
    }
}
