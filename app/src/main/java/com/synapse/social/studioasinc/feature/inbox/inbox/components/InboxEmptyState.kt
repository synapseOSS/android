package com.synapse.social.studioasinc.ui.inbox.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.inbox.models.EmptyStateType

/**
 * Beautiful empty state component for inbox tabs.
 * Shows icon with animation, title, description, and optional CTA.
 */
@Composable
fun InboxEmptyState(
    type: EmptyStateType,
    message: String? = null,
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (icon, title, description, actionText) = remember(type, message) {
        when (type) {
            EmptyStateType.CHATS -> EmptyStateContent(
                icon = Icons.Outlined.Email,
                title = "No messages yet",
                description = "Your conversations will appear here",
                actionText = "New Message"
            )
            EmptyStateType.CALLS -> EmptyStateContent(
                icon = Icons.Outlined.Call,
                title = "No calls yet",
                description = "Your call history will appear here. Start a call with someone!",
                actionText = null
            )
            EmptyStateType.CONTACTS -> EmptyStateContent(
                icon = Icons.Outlined.People,
                title = "Connect with friends",
                description = "Find your friends on Synapse and start messaging",
                actionText = "Find Friends"
            )
            EmptyStateType.SEARCH_NO_RESULTS -> EmptyStateContent(
                icon = Icons.Outlined.SearchOff,
                title = "No results found",
                description = "Try searching with different keywords",
                actionText = null
            )
            EmptyStateType.ARCHIVED -> EmptyStateContent(
                icon = Icons.Outlined.Archive,
                title = "No archived chats",
                description = "Chats you archive will appear here",
                actionText = null
            )
            EmptyStateType.ERROR -> EmptyStateContent(
                icon = Icons.Default.Warning,
                title = "Something went wrong",
                description = message ?: "We couldn't load your messages. Please try again.",
                actionText = "Retry"
            )
        }
    }
    
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 200
        ),
        label = "contentAlpha"
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(iconScale),
            contentAlignment = Alignment.Center
        ) {
            // Background circle
            Surface(
                modifier = Modifier.size(100.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {}
            
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(contentAlpha)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(contentAlpha)
                .padding(horizontal = 24.dp)
        )
        
        // Action button
        if (actionText != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 400)) + 
                    scaleIn(initialScale = 0.9f, animationSpec = tween(400, delayMillis = 400))
            ) {
                Button(
                    onClick = onActionClick,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actionText)
                }
            }
        }
    }
}

/**
 * Data class for empty state content.
 */
private data class EmptyStateContent(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionText: String?
)

/**
 * Compact empty state for inline usage.
 */
@Composable
fun CompactEmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
