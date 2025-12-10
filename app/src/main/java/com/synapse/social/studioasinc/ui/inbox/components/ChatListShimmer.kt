package com.synapse.social.studioasinc.ui.inbox.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.inbox.theme.InboxDimens

/**
 * Shimmer effect modifier with theme-aware colors.
 */
fun Modifier.inboxShimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    val translateX by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    )
    
    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateX - 500f, translateX - 500f),
            end = Offset(translateX, translateX)
        )
    )
}

/**
 * Full chat list shimmer loading state.
 */
@Composable
fun ChatListShimmer(
    itemCount: Int = 8,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(itemCount) { index ->
            ChatItemShimmer(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                delay = index * 50
            )
        }
    }
}

/**
 * Individual chat item shimmer.
 */
@Composable
private fun ChatItemShimmer(
    modifier: Modifier = Modifier,
    delay: Int = 0
) {
    val transition = rememberInfiniteTransition(label = "itemShimmer")
    
    val translateX by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing,
                delayMillis = delay
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "itemTranslate"
    )
    
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.5f),
            Color.LightGray.copy(alpha = 0.3f)
        ),
        start = Offset(translateX - 500f, 0f),
        end = Offset(translateX, 0f)
    )
    
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(InboxDimens.AvatarSize)
                .clip(CircleShape)
                .background(shimmerBrush)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            // Name placeholder
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Time placeholder
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
    }
}

/**
 * Top bar shimmer for loading state.
 */
@Composable
fun TopBarShimmer(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "topBarShimmer")
    
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "topBarTranslate"
    )
    
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.5f),
            Color.LightGray.copy(alpha = 0.3f)
        ),
        start = Offset(translateX - 300f, 0f),
        end = Offset(translateX, 0f)
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
        }
    }
}
