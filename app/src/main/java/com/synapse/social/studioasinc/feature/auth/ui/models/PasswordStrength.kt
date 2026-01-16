package com.synapse.social.studioasinc.feature.auth.ui.models

import androidx.compose.ui.graphics.Color

/**
 * Sealed class representing password strength levels with associated UI properties.
 * Each level has a progress value (0.0-1.0), a label, and a color for visual feedback.
 */
sealed class PasswordStrength(
    val progress: Float,
    val label: String,
    val color: Color
) {
    /**
     * Weak password strength (0-33% progress)
     * Typically for passwords that are too short or lack complexity
     */
    object Weak : PasswordStrength(
        progress = 0.33f,
        label = "Weak",
        color = Color.Red
    )

    /**
     * Fair password strength (34-66% progress)
     * Typically for passwords with moderate length and some complexity
     */
    object Fair : PasswordStrength(
        progress = 0.66f,
        label = "Fair",
        color = Color(0xFFFFA500) // Orange
    )

    /**
     * Strong password strength (67-100% progress)
     * Typically for passwords with good length and high complexity
     */
    object Strong : PasswordStrength(
        progress = 1.0f,
        label = "Strong",
        color = Color.Green
    )
}
