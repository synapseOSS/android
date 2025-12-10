package com.synapse.social.studioasinc.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility for providing haptic feedback throughout the app
 * Enhances UX by providing tactile responses to user interactions
 */
object HapticFeedbackUtil {

    /**
     * Feedback types for different interactions
     */
    enum class FeedbackType {
        LIGHT_CLICK,      // Light tap feedback (e.g., button press)
        MEDIUM_CLICK,     // Medium tap feedback (e.g., message send)
        HEAVY_CLICK,      // Heavy tap feedback (e.g., delete action)
        SUCCESS,          // Success feedback (e.g., message sent)
        ERROR,            // Error feedback (e.g., failed action)
        LONG_PRESS,       // Long press feedback
        SELECTION         // Selection feedback (e.g., multi-select)
    }

    /**
     * Perform haptic feedback on a view
     * 
     * @param view The view to perform feedback on
     * @param type The type of feedback to perform
     */
    fun performHapticFeedback(view: View, type: FeedbackType) {
        when (type) {
            FeedbackType.LIGHT_CLICK -> {
                view.performHapticFeedback(
                    HapticFeedbackConstants.CLOCK_TICK,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
            FeedbackType.MEDIUM_CLICK -> {
                view.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
            FeedbackType.HEAVY_CLICK -> {
                view.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
            FeedbackType.SUCCESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(
                        HapticFeedbackConstants.CONFIRM,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                } else {
                    performVibration(view.context, 50, VibrationEffect.DEFAULT_AMPLITUDE)
                }
            }
            FeedbackType.ERROR -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(
                        HapticFeedbackConstants.REJECT,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                } else {
                    performVibration(view.context, 100, 200)
                }
            }
            FeedbackType.LONG_PRESS -> {
                view.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
            FeedbackType.SELECTION -> {
                view.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        }
    }

    /**
     * Perform custom vibration pattern
     * 
     * @param context Android context
     * @param duration Duration in milliseconds
     * @param amplitude Vibration amplitude (1-255)
     */
    private fun performVibration(context: Context, duration: Long, amplitude: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createOneShot(duration, amplitude)
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duration)
            }
        }
    }

    /**
     * Perform success pattern vibration
     * Two short pulses indicating successful action
     */
    fun performSuccessPattern(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 50, 50, 50)
                val amplitudes = intArrayOf(0, 100, 0, 100)
                it.vibrate(
                    VibrationEffect.createWaveform(pattern, amplitudes, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 50, 50, 50)
                it.vibrate(pattern, -1)
            }
        }
    }

    /**
     * Perform error pattern vibration
     * Single long pulse indicating error
     */
    fun performErrorPattern(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createOneShot(200, 200)
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(200)
            }
        }
    }
}
