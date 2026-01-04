package com.synapse.social.studioasinc.core.util

import android.widget.TextView
import java.text.DecimalFormat

object CountUtils {
    fun setCount(textView: TextView, number: Double) {
        when {
            number < 10000 -> {
                textView.text = number.toLong().toString()
            }
            else -> {
                val decimalFormat = DecimalFormat("0.0")
                val (formattedNumber, suffix) = when {
                    number < 1000000 -> Pair(number / 1000, "K")
                    number < 1000000000 -> Pair(number / 1000000, "M")
                    number < 1000000000000L -> Pair(number / 1000000000, "B")
                    else -> Pair(number / 1000000000000L, "T")
                }
                textView.text = "${decimalFormat.format(formattedNumber)}$suffix"
            }
        }
    }
}
