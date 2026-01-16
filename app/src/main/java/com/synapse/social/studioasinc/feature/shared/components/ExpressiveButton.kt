package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    text: String,
    variant: ButtonVariant = ButtonVariant.Filled,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "button_scale"
    )
    
    val buttonColors = when (variant) {
        ButtonVariant.Filled -> ButtonDefaults.buttonColors()
        ButtonVariant.FilledTonal -> ButtonDefaults.filledTonalButtonColors()
        ButtonVariant.Outlined -> ButtonDefaults.outlinedButtonColors()
        ButtonVariant.Text -> ButtonDefaults.textButtonColors()
    }
    
    val animatedColors = ButtonDefaults.buttonColors(
        containerColor = animateColorAsState(
            targetValue = if (isPressed) 
                buttonColors.containerColor.copy(alpha = 0.8f) 
            else buttonColors.containerColor,
            animationSpec = tween(100),
            label = "container_color"
        ).value,
        contentColor = buttonColors.contentColor
    )
    
    when (variant) {
        ButtonVariant.Filled -> Button(
            onClick = onClick,
            modifier = modifier.scale(scale),
            enabled = enabled,
            colors = animatedColors,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ButtonContent(icon, text)
        }
        
        ButtonVariant.FilledTonal -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            enabled = enabled,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ButtonContent(icon, text)
        }
        
        ButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            enabled = enabled,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ButtonContent(icon, text)
        }
        
        ButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            enabled = enabled,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ButtonContent(icon, text)
        }
    }
}

@Composable
private fun ButtonContent(icon: ImageVector?, text: String) {
    icon?.let {
        Icon(
            imageVector = it,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text(text)
}

enum class ButtonVariant {
    Filled,
    FilledTonal,
    Outlined,
    Text
}
