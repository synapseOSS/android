package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil

/**
 * Primary button component for authentication actions.
 * Supports loading state, haptic feedback, and press animations.
 *
 * Design tokens:
 * - Height: 48dp (minimum touch target)
 * - Press animation: Scale to 0.95 over 100ms
 * - Haptic feedback: On press
 */
@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // Scale animation
    val scale = AnimationUtil.animatedScale(pressed = isPressed)

    // Trigger haptic feedback on press
    if (isPressed) {
        // We use a side effect here or just rely on the click handler.
        // But the requirement says "WHEN buttons are pressed", which implies the down event.
        // However, Composable side-effects in if-blocks are tricky.
        // Usually Button handles click haptics. But let's be explicit if needed.
        // For simplicity and correctness in Compose, we can add it to onClick or use a specialized modifier.
        // But standard Button onClick is on release.
        // Let's trust the standard behavior + maybe an extra one if really needed, but standard is usually fine.
        // Requirement 1.4: "WHEN the user taps ... provide haptic feedback". "Tap" usually means up.
        // Requirement 11.2: "WHEN a user taps an OAuth button ... haptic feedback".
        // Let's add it to the onClick wrapper.
    }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Using TextHandleMove as a generic "tick"
            onClick()
        },
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp) // Requirement 8.4
            .scale(scale)
    ) {
        if (loading) {
            ExpressiveLoadingIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
