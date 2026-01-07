package com.synapse.social.studioasinc.feature.stories.tray

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.domain.model.StoryWithUser
import com.synapse.social.studioasinc.domain.model.User

/**
 * Gradient colors for the unseen story ring
 */
private val storyGradientColors = listOf(
    Color(0xFFE040FB), // Purple
    Color(0xFFFF4081), // Pink
    Color(0xFFFF6E40), // Orange
    Color(0xFFFFAB00)  // Amber
)

/**
 * Color for seen story ring
 */
private val seenStoryRingColor = Color(0xFF424242)

/**
 * Horizontal Story Tray component displayed at the top of the feed.
 * Shows the current user's story first, followed by friend stories.
 */
@Composable
fun StoryTray(
    currentUser: User?,
    myStory: StoryWithUser?,
    friendStories: List<StoryWithUser>,
    onMyStoryClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    onStoryClick: (StoryWithUser) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            // My Story Slot (always first)
            item(key = "my_story") {
                MyStorySlot(
                    user = currentUser,
                    hasActiveStory = myStory != null && myStory.stories.isNotEmpty(),
                    onAddClick = onAddStoryClick,
                    onViewClick = onMyStoryClick
                )
            }
            
            // Friend Stories
            items(
                items = friendStories,
                key = { it.user.uid }
            ) { storyWithUser ->
                StoryTrayItem(
                    storyWithUser = storyWithUser,
                    onClick = { onStoryClick(storyWithUser) }
                )
            }
            
            // Loading placeholders
            if (isLoading) {
                items(5) { index ->
                    StoryTrayItemShimmer()
                }
            }
        }
        
        // Subtle divider
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

/**
 * "My Story" slot - shows add button when no story, or view button when active
 */
@Composable
private fun MyStorySlot(
    user: User?,
    hasActiveStory: Boolean,
    onAddClick: () -> Unit,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Avatar with conditional gradient ring
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .then(
                        if (hasActiveStory) {
                            Modifier.border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(storyGradientColors),
                                shape = CircleShape
                            )
                        } else {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                        }
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .clickable { if (hasActiveStory) onViewClick() else onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                if (user?.avatar != null) {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = "Your story",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.displayName?.firstOrNull()?.uppercase() 
                                ?: user?.username?.firstOrNull()?.uppercase() 
                                ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Add button overlay (only when no active story)
            if (!hasActiveStory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add story",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Your story",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Individual story item for a friend in the tray
 */
@Composable
fun StoryTrayItem(
    storyWithUser: StoryWithUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp
) {
    val user = storyWithUser.user
    val hasUnseen = storyWithUser.hasUnseenStories
    
    // Animate ring appearance
    val ringAlpha by animateFloatAsState(
        targetValue = if (hasUnseen) 1f else 0.5f,
        animationSpec = tween(300),
        label = "ringAlpha"
    )
    
    Column(
        modifier = modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .then(
                    if (hasUnseen) {
                        Modifier.border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                storyGradientColors.map { it.copy(alpha = ringAlpha) }
                            ),
                            shape = CircleShape
                        )
                    } else {
                        Modifier.border(
                            width = 2.dp,
                            color = seenStoryRingColor.copy(alpha = ringAlpha),
                            shape = CircleShape
                        )
                    }
                )
                .padding(4.dp)
                .clip(CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (user.avatar != null) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = "${user.displayName ?: user.username}'s story",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.displayName?.firstOrNull()?.uppercase()
                            ?: user.username?.firstOrNull()?.uppercase()
                            ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = user.displayName ?: user.username ?: "User",
            style = MaterialTheme.typography.labelSmall,
            color = if (hasUnseen) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (hasUnseen) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Shimmer placeholder for loading state
 */
@Composable
private fun StoryTrayItemShimmer(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(10.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
        )
    }
}
