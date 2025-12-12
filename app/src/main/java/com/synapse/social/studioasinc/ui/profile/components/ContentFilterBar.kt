package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.profile.ProfileContentFilter

/**
 * Enhanced content filter bar with icons and sliding indicator.
 * 
 * Features:
 * - Icon + optional text for each filter
 * - Smooth sliding indicator animation
 * - Active/inactive state colors
 * - Sticky behavior ready (controlled by parent)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentFilterBar(
    selectedFilter: ProfileContentFilter,
    onFilterSelected: (ProfileContentFilter) -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false
) {
    val density = LocalDensity.current
    var tabWidth by remember { mutableStateOf(0.dp) }
    
    // Calculate indicator offset based on selected filter
    val indicatorOffset by animateDpAsState(
        targetValue = when (selectedFilter) {
            ProfileContentFilter.POSTS -> 0.dp
            ProfileContentFilter.PHOTOS -> tabWidth
            ProfileContentFilter.REELS -> tabWidth * 2
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorOffset"
    )
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .onGloballyPositioned { coordinates ->
                        tabWidth = with(density) { (coordinates.size.width / 3).toDp() }
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileContentFilter.values().forEach { filter ->
                    FilterTabItem(
                        filter = filter,
                        isSelected = selectedFilter == filter,
                        showLabel = showLabels,
                        onClick = { onFilterSelected(filter) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Sliding indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Individual filter tab item with icon and optional label.
 */
@Composable
private fun FilterTabItem(
    filter: ProfileContentFilter,
    isSelected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "iconColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "iconScale"
    )
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = getFilterIcon(filter, isSelected),
                contentDescription = filter.name,
                tint = iconColor,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            )
            
            if (showLabel) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = getFilterLabel(filter),
                    style = MaterialTheme.typography.labelMedium,
                    color = iconColor
                )
            }
        }
    }
}

/**
 * Get the appropriate icon for a filter based on selection state.
 */
private fun getFilterIcon(filter: ProfileContentFilter, isSelected: Boolean): ImageVector {
    return when (filter) {
        ProfileContentFilter.POSTS -> if (isSelected) Icons.Filled.GridView else Icons.Outlined.GridView
        ProfileContentFilter.PHOTOS -> if (isSelected) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary
        ProfileContentFilter.REELS -> if (isSelected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
    }
}

/**
 * Get the label text for a filter.
 */
private fun getFilterLabel(filter: ProfileContentFilter): String {
    return when (filter) {
        ProfileContentFilter.POSTS -> "Posts"
        ProfileContentFilter.PHOTOS -> "Photos"
        ProfileContentFilter.REELS -> "Reels"
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentFilterBarPreview() {
    MaterialTheme {
        var selected by remember { mutableStateOf(ProfileContentFilter.POSTS) }
        ContentFilterBar(
            selectedFilter = selected,
            onFilterSelected = { selected = it }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentFilterBarWithLabelsPreview() {
    MaterialTheme {
        var selected by remember { mutableStateOf(ProfileContentFilter.PHOTOS) }
        ContentFilterBar(
            selectedFilter = selected,
            onFilterSelected = { selected = it },
            showLabels = true
        )
    }
}
