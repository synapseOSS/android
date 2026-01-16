package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.delay

data class MediaItem(
    val id: String,
    val url: String,
    val isVideo: Boolean = false,
    val isMultiple: Boolean = false,
    val thumbnailUrl: String? = null
)

/**
 * Enhanced Photo Grid with staggered animations and improved styling.
 * 
 * Features:
 * - Staggered fade-in animation for items
 * - Rounded corners on grid items
 * - Video and multiple media indicators
 * - Shimmer loading placeholder
 * - Scale animation on press
 */
@Composable
fun PhotoGrid(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    spacing: Float = 2f
) {
    if (items.isEmpty() && !isLoading) {
        PhotoGridEmptyState(modifier = modifier)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier.heightIn(max = 2000.dp),
            contentPadding = PaddingValues(spacing.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id }
            ) { index, item ->
                AnimatedGridItem(
                    item = item,
                    onClick = { onItemClick(item) },
                    animationDelay = (index % 9) * 50 // Stagger within viewport
                )
            }


        }
    }
}

/**
 * Animated grid item with fade-in and scale animations.
 */
@Composable
private fun AnimatedGridItem(
    item: MediaItem,
    onClick: () -> Unit,
    animationDelay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var imageLoaded by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible && imageLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "itemAlpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            visible -> 1f
            else -> 0.8f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "itemScale"
    )
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        // Image
        AsyncImage(
            model = item.thumbnailUrl ?: item.url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onState = { state ->
                imageLoaded = state is AsyncImagePainter.State.Success
            }
        )
        

        
        // Video indicator
        if (item.isVideo) {
            VideoIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
        
        // Multiple media indicator
        if (item.isMultiple) {
            MultipleMediaIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
        
        // Subtle gradient overlay at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
        )
    }
}

/**
 * Video indicator badge.
 */
@Composable
private fun VideoIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Video",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Multiple media (carousel) indicator badge.
 */
@Composable
private fun MultipleMediaIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Collections,
            contentDescription = "Multiple photos",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
 * Shimmer placeholder for loading grid items.
 */
@Composable
private fun ShimmerGridItem(animationDelay: Int = 0) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "shimmerAlpha"
    )
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(4.dp))
            .background(shimmerBrush())
    )
}

/**
 * Empty state when no photos exist.
 */
@Composable
private fun PhotoGridEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Collections,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No photos yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Share your first photo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoGridPreview() {
    MaterialTheme {
        PhotoGrid(
            items = List(9) { index ->
                MediaItem(
                    id = index.toString(),
                    url = "",
                    isVideo = index == 2 || index == 5,
                    isMultiple = index == 3 || index == 7
                )
            },
            onItemClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoGridLoadingPreview() {
    MaterialTheme {
        PhotoGrid(
            items = emptyList(),
            onItemClick = {},
            isLoading = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoGridEmptyPreview() {
    MaterialTheme {
        PhotoGrid(
            items = emptyList(),
            onItemClick = {},
            isLoading = false
        )
    }
}
