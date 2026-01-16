package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a shimmer effect brush for loading placeholders.
 * The shimmer smoothly animates across the component to indicate loading state.
 */
@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        )
        
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )
        
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnimation - 200f, translateAnimation - 200f),
            end = Offset(translateAnimation, translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }
}

/**
 * A shimmer placeholder box with customizable shape.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

/**
 * A circular shimmer placeholder, ideal for profile images.
 */
@Composable
fun ShimmerCircle(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(shimmerBrush())
    )
}

/**
 * A shimmer placeholder for text lines.
 */
@Composable
fun ShimmerText(
    width: Dp,
    height: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    ShimmerBox(
        modifier = modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(4.dp)
    )
}

/**
 * Profile skeleton screen showing shimmer placeholders for all profile elements.
 * Displays while the actual profile data is loading.
 */
@Composable
fun ProfileSkeletonScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Cover Photo Skeleton
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(0.dp)
        )
        
        Spacer(modifier = Modifier.height((-50).dp))
        
        // Profile Image Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Spacer(modifier = Modifier.width(16.dp))
            ShimmerCircle(size = 100.dp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Name skeleton
        ShimmerText(width = 150.dp, height = 20.dp)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Username skeleton
        ShimmerText(width = 100.dp, height = 14.dp)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Bio skeleton
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerText(width = 280.dp, height = 14.dp)
            ShimmerText(width = 200.dp, height = 14.dp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stats skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    ShimmerText(width = 40.dp, height = 18.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerText(width = 60.dp, height = 12.dp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Buttons skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(20.dp)
            )
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShimmerPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerCircle(size = 48.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerText(width = 120.dp)
                    ShimmerText(width = 80.dp, height = 12.dp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSkeletonPreview() {
    MaterialTheme {
        ProfileSkeletonScreen()
    }
}
