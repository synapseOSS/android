package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Enhanced Profile Top App Bar with scroll-based visibility.
 * 
 * Features:
 * - Transparent when at top, solid when scrolled
 * - Username appears on scroll
 * - Smooth color and alpha transitions
 * - Scale animation on icons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(
    username: String,
    scrollProgress: Float,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (scrollProgress > 0.5f) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "backgroundColor"
    )

    val elevation by animateDpAsState(
        targetValue = if (scrollProgress > 0.5f) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "elevation"
    )
    
    val titleAlpha by animateFloatAsState(
        targetValue = if (scrollProgress > 0.7f) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "titleAlpha"
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (scrollProgress > 0.5f || scrollProgress < 0.1f) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color.White
        },
        animationSpec = tween(durationMillis = 200),
        label = "iconTint"
    )

    TopAppBar(
        title = {
            Text(
                text = "@$username",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha }
            )
        },
        navigationIcon = {
            AnimatedIconButton(
                onClick = onBackClick,
                contentDescription = "Back"
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = iconTint
                )
            }
        },
        actions = {
            AnimatedIconButton(
                onClick = onMoreClick,
                contentDescription = "More options"
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = iconTint
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor
        ),
        modifier = modifier
    )
}

/**
 * Icon button with press animation.
 */
@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "iconScale"
    )
    
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.scale(scale)
    ) {
        content()
    }
}

@Preview
@Composable
private fun ProfileTopAppBarPreview() {
    MaterialTheme {
        ProfileTopAppBar(
            username = "johndoe",
            scrollProgress = 0.7f,
            onBackClick = {},
            onMoreClick = {}
        )
    }
}

@Preview
@Composable
private fun ProfileTopAppBarTransparentPreview() {
    MaterialTheme {
        ProfileTopAppBar(
            username = "johndoe",
            scrollProgress = 0f,
            onBackClick = {},
            onMoreClick = {}
        )
    }
}
