package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil

/**
 * Button for OAuth providers (Google, Facebook, Apple).
 *
 * Design tokens:
 * - Height: 48dp
 * - Haptic feedback: On press
 * - Styling: Outlined with icon
 */
@Composable
fun OAuthButton(
    provider: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale = AnimationUtil.animatedScale(pressed = isPressed)

    val (iconRes, label) = when (provider.lowercase()) {
        "google" -> R.drawable.ic_google_logo to "Continue with Google"
        "facebook" -> R.drawable.ic_facebook_logo to "Continue with Facebook"
        "apple" -> R.drawable.ic_apple_logo to "Continue with Apple"
        else -> R.drawable.ic_google_logo to "Continue with $provider" // Fallback
    }

    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .scale(scale),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
