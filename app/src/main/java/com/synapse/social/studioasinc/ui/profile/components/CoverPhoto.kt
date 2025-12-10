package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

/**
 * Cover photo component with parallax scrolling effect.
 * 
 * Features:
 * - Parallax movement based on scroll offset
 * - Gradient overlay for better text readability
 * - Edit button overlay for own profile
 * - Smooth fade-in animation on load
 * - Placeholder with gradient background
 * 
 * @param coverImageUrl URL of the cover image, null shows placeholder
 * @param scrollOffset Current scroll offset for parallax calculation (0f to 1f)
 * @param isOwnProfile Whether this is the current user's profile
 * @param onEditClick Callback when edit button is clicked
 * @param height Height of the cover photo section
 * @param parallaxFactor How much the image moves relative to scroll (0.5f = half speed)
 */
@Composable
fun CoverPhoto(
    coverImageUrl: String?,
    scrollOffset: Float = 0f,
    isOwnProfile: Boolean = false,
    onEditClick: () -> Unit = {},
    height: Dp = 200.dp,
    parallaxFactor: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    var imageLoaded by remember { mutableStateOf(false) }
    
    // Fade-in animation when image loads
    val alpha by animateFloatAsState(
        targetValue = if (imageLoaded || coverImageUrl == null) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "coverFadeIn"
    )
    
    // Calculate parallax offset
    val parallaxOffset = scrollOffset * parallaxFactor * 100f
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
    ) {
        // Cover Image or Placeholder
        if (coverImageUrl != null) {
            AsyncImage(
                model = coverImageUrl,
                contentDescription = "Cover photo",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = parallaxOffset
                        // Slight zoom for smoother parallax
                        scaleX = 1.1f
                        scaleY = 1.1f
                    }
                    .graphicsLayer { this.alpha = alpha },
                contentScale = ContentScale.Crop,
                onState = { state ->
                    imageLoaded = state is AsyncImagePainter.State.Success
                }
            )
        } else {
            // Gradient placeholder when no cover image
            CoverPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = parallaxOffset * 0.3f
                    }
            )
        }
        
        // Gradient overlay for better readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.4f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        
        // Edit button for own profile
        if (isOwnProfile) {
            FloatingActionButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(36.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Edit cover photo",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Animated gradient placeholder for cover photo.
 */
@Composable
private fun CoverPlaceholder(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "coverPlaceholder")
    
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )
    
    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = androidx.compose.ui.geometry.Offset(
                        animatedOffset * 500f,
                        animatedOffset * 200f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        500f + animatedOffset * 500f,
                        200f + animatedOffset * 200f
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle icon indicating no cover set
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { alpha = 0.2f },
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Cover photo with overlay profile image.
 * Combines cover photo with profile picture that overlaps the cover.
 */
@Composable
fun CoverPhotoWithProfile(
    coverImageUrl: String?,
    profileImageUrl: String?,
    scrollOffset: Float = 0f,
    isOwnProfile: Boolean = false,
    hasStory: Boolean = false,
    onCoverEditClick: () -> Unit = {},
    onProfileImageClick: () -> Unit = {},
    coverHeight: Dp = 180.dp,
    profileImageSize: Dp = 110.dp,
    modifier: Modifier = Modifier
) {
    val profileImageOffset = profileImageSize / 2
    
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Cover Photo
        CoverPhoto(
            coverImageUrl = coverImageUrl,
            scrollOffset = scrollOffset,
            isOwnProfile = isOwnProfile,
            onEditClick = onCoverEditClick,
            height = coverHeight
        )
        
        // Profile Image overlapping cover
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp)
                .offset(y = profileImageOffset)
        ) {
            ProfileImageWithRing(
                profileImageUrl = profileImageUrl,
                size = profileImageSize,
                hasStory = hasStory,
                isOwnProfile = isOwnProfile,
                onClick = onProfileImageClick
            )
        }
    }
}

/**
 * Profile image with animated story ring.
 */
@Composable
fun ProfileImageWithRing(
    profileImageUrl: String?,
    size: Dp,
    hasStory: Boolean = false,
    isOwnProfile: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "storyRing")
    
    // Pulsing animation for story ring
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasStory) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "storyPulse"
    )
    
    // Rotating gradient for story ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "storyRotation"
    )
    
    val ringWidth = 4.dp
    val ringPadding = 3.dp
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Story ring background (if has story)
        if (hasStory) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation }
                    .clip(CircleShape)
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
            )
        }
        
        // White/surface background ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (hasStory) ringWidth else 0.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
        
        // Profile image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (hasStory) ringWidth + ringPadding else 2.dp)
                .clip(CircleShape)
        ) {
            if (profileImageUrl != null) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(size * 0.5f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Add story button for own profile without story
        if (isOwnProfile && !hasStory) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CoverPhotoPreview() {
    MaterialTheme {
        CoverPhoto(
            coverImageUrl = null,
            isOwnProfile = true,
            onEditClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileImageWithRingPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileImageWithRing(
                profileImageUrl = null,
                size = 80.dp,
                hasStory = true,
                isOwnProfile = false
            )
            ProfileImageWithRing(
                profileImageUrl = null,
                size = 80.dp,
                hasStory = false,
                isOwnProfile = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CoverPhotoWithProfilePreview() {
    MaterialTheme {
        Column {
            CoverPhotoWithProfile(
                coverImageUrl = null,
                profileImageUrl = null,
                isOwnProfile = true,
                hasStory = true
            )
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
