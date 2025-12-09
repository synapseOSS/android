package com.synapse.social.studioasinc.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Enhanced haptic feedback utility with rich patterns
 * Provides contextual haptic feedback for various chat interactions
 */
object EnhancedHapticFeedback {

    /**
     * Haptic feedback types for different interactions
     */
    enum class FeedbackType {
        LIGHT_TAP,          // Light tap for button presses
        MEDIUM_TAP,         // Medium tap for message send
        HEAVY_TAP,          // Heavy tap for important actions
        SUCCESS,            // Success pattern (double pulse)
        ERROR,              // Error pattern (long pulse)
        WARNING,            // Warning pattern (triple short pulse)
        SELECTION,          // Selection in multi-select mode
        LONG_PRESS,         // Long press detection
        SWIPE,              // Swipe gesture
        SCROLL_LIMIT,       // Reached scroll limit
        MESSAGE_RECEIVED,   // New message received
        TYPING_START,       // Started typing
        TYPING_STOP         // Stopped typing
    }

    /**
     * Perform haptic feedback with specified type
     */
    fun perform(view: View, type: FeedbackType) {
        when (type) {
            FeedbackType.LIGHT_TAP -> performLightTap(view)
            FeedbackType.MEDIUM_TAP -> performMediumTap(view)
            FeedbackType.HEAVY_TAP -> performHeavyTap(view)
            FeedbackType.SUCCESS -> performSuccessPattern(view.context)
            FeedbackType.ERROR -> performErrorPattern(view.context)
            FeedbackType.WARNING -> performWarningPattern(view.context)
            FeedbackType.SELECTION -> performSelection(view)
            FeedbackType.LONG_PRESS -> performLongPress(view)
            FeedbackType.SWIPE -> performSwipe(view)
            FeedbackType.SCROLL_LIMIT -> performScrollLimit(view)
            FeedbackType.MESSAGE_RECEIVED -> performMessageReceived(view.context)
            FeedbackType.TYPING_START -> performTypingStart(view.context)
            FeedbackType.TYPING_STOP -> performTypingStop(view.context)
        }
    }

    private fun performLightTap(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun performMediumTap(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun performHeavyTap(view: View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    private fun performSelection(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun performLongPress(view: View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    private fun performSwipe(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun performScrollLimit(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    private fun performSuccessPattern(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = getVibrator(context)
            val pattern = longArrayOf(0, 50, 50, 50)
            val amplitudes = intArrayOf(0, 100, 0, 150)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        }
    }

    private fun performErrorPattern(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = getVibrator(context)
            val pattern = longArrayOf(0, 100, 50, 100)
            val amplitudes = intArrayOf(0, 200, 0, 200)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        }
    }

    private fun performWarningPattern(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = getVibrator(context)
            val pattern = longArrayOf(0, 30, 30, 30, 30, 30)
            val amplitudes = intArrayOf(0, 100, 0, 100, 0, 100)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        }
    }

    private fun performMessageReceived(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = getVibrator(context)
            vibrator?.vibrate(VibrationEffect.createOneShot(30, 80))
        }
    }

    private fun performTypingStart(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = getVibrator(context)
            vibrator?.vibrate(VibrationEffect.createOneShot(10, 50))
        }
    }

    private fun performTypingStop(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = getVibrator(context)
            vibrator?.vibrate(VibrationEffect.createOneShot(15, 40))
        }
    }

    @Suppress("DEPRECATION")
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
