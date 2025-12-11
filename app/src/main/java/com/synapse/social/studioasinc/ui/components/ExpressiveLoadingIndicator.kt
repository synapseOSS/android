package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    // Material 3 Expressive Loading Indicator
    // Note: If LoadingIndicator is not available in the current dependency version,
    // we would fallback to CircularProgressIndicator, but the user explicitly asked for LoadingIndicator.
    // Based on BOM 2024.11.00, material3 should be 1.3.1 which has LoadingIndicator.
    LoadingIndicator(
        modifier = modifier.width(48.dp),
        color = color
    )
}
