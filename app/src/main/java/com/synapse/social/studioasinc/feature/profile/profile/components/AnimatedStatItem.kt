package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Animated stat item displaying a count with label.
 * 
 * Features:
 * - Counting animation on first appearance
 * - Scale animation on press
 * - Number formatting (K, M, B)
 * - Staggered entrance animation
 * 
 * @param label The label text (e.g., "Posts", "Followers")
 * @param count The numeric count to display
 * @param onClick Callback when stat is clicked
 * @param animationDelay Delay before count animation starts (for staggered effect)
 * @param animateOnChange Whether to animate when count changes
 */
@Composable
fun AnimatedStatItem(
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationDelay: Int = 0,
    animateOnChange: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "statScale"
    )
    
    // Track if this is the initial load
    var hasAnimated by remember { mutableStateOf(false) }
    var displayCount by remember { mutableIntStateOf(0) }
    
    // Counting animation
    LaunchedEffect(count) {
        if (!hasAnimated) {
            delay(animationDelay.toLong())
            hasAnimated = true
        }
        
        if (animateOnChange || !hasAnimated) {
            // Animate from current display to target count
            val startCount = displayCount
            val steps = 30
            val stepDuration = 20L
            
            for (i in 1..steps) {
                val progress = i.toFloat() / steps
                val easedProgress = easeOutCubic(progress)
                displayCount = (startCount + (count - startCount) * easedProgress).roundToInt()
                delay(stepDuration)
            }
            displayCount = count // Ensure final value is exact
        } else {
            displayCount = count
        }
    }
    
    // Entry animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "statAlpha"
    )
    
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "statOffset"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = formatCount(displayCount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Horizontal row of stat items with staggered animations.
 */
@Composable
fun AnimatedStatsRow(
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    onPostsClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedStatItem(
            label = "Posts",
            count = postsCount,
            onClick = onPostsClick,
            animationDelay = 0
        )
        
        Spacer(modifier = Modifier.width(32.dp))
        
        AnimatedStatItem(
            label = "Followers",
            count = followersCount,
            onClick = onFollowersClick,
            animationDelay = 100
        )
        
        Spacer(modifier = Modifier.width(32.dp))
        
        AnimatedStatItem(
            label = "Following",
            count = followingCount,
            onClick = onFollowingClick,
            animationDelay = 200
        )
    }
}

/**
 * Subtle vertical divider between stats.
 */
@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .graphicsLayer { alpha = 0.2f }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp)
        )
    }
}

/**
 * Formats a count into a human-readable string.
 * Examples: 1234 -> "1.2K", 1234567 -> "1.2M", 1234567890 -> "1.2B"
 */
fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000_000 -> {
            val formatted = count / 1_000_000_000.0
            if (formatted == formatted.toLong().toDouble()) {
                "${formatted.toLong()}B"
            } else {
                String.format("%.1fB", formatted).removeSuffix(".0B") + (if (formatted != formatted.toLong().toDouble()) "" else "")
            }
        }
        count >= 1_000_000 -> {
            val formatted = count / 1_000_000.0
            if (formatted == formatted.toLong().toDouble()) {
                "${formatted.toLong()}M"
            } else {
                String.format("%.1fM", formatted)
            }
        }
        count >= 10_000 -> {
            val formatted = count / 1_000.0
            if (formatted == formatted.toLong().toDouble()) {
                "${formatted.toLong()}K"
            } else {
                String.format("%.1fK", formatted)
            }
        }
        count >= 1_000 -> {
            val formatted = count / 1_000.0
            String.format("%.1fK", formatted)
        }
        else -> count.toString()
    }
}

/**
 * Easing function for smooth animations.
 */
private fun easeOutCubic(x: Float): Float {
    return 1 - (1 - x) * (1 - x) * (1 - x)
}

@Preview(showBackground = true)
@Composable
private fun AnimatedStatItemPreview() {
    MaterialTheme {
        AnimatedStatItem(
            label = "Followers",
            count = 12345,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnimatedStatsRowPreview() {
    MaterialTheme {
        AnimatedStatsRow(
            postsCount = 42,
            followersCount = 1234567,
            followingCount = 567,
            onPostsClick = {},
            onFollowersClick = {},
            onFollowingClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FormatCountPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            listOf(0, 999, 1000, 1234, 12345, 123456, 1234567, 12345678, 1234567890).forEach { count ->
                Text("$count -> ${formatCount(count)}")
            }
        }
    }
}
