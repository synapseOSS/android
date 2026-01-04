package com.synapse.social.studioasinc.ui.chat.components.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.ui.chat.ChatUserInfo
import com.synapse.social.studioasinc.ui.chat.RealtimeConnectionState
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors
import com.synapse.social.studioasinc.ui.chat.theme.ChatTheme

/**
 * Main Chat Top Bar
 * Shows back button, user info, and call actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    userInfo: ChatUserInfo?,
    connectionState: RealtimeConnectionState,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onMenuClick: () -> Unit,
    onRetryConnection: (() -> Unit)? = null,
    isMenuExpanded: Boolean = false,
    onDismissMenu: () -> Unit = {},
    menuContent: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TopAppBar(
            title = {
                if (userInfo != null) {
                    ChatUserHeader(
                        userInfo = userInfo,
                        onClick = onProfileClick
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = onCallClick) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Audio Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onVideoCallClick) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = "Video Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Box {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = onDismissMenu,
                        content = menuContent
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        // Connection status banner
        ConnectionStatusBanner(
            state = connectionState,
            onRetryClick = onRetryConnection
        )
    }
}

/**
 * User Header info (Avatar + Name + Status)
 */
@Composable
private fun ChatUserHeader(
    userInfo: ChatUserInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (userInfo.avatarUrl != null) {
                AsyncImage(
                    model = userInfo.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = userInfo.displayName?.take(1) ?: "?",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = userInfo.displayName ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Online status or Last seen
            val statusText = when {
                userInfo.isOnline -> "Online"
                userInfo.lastSeen != null -> "Last seen recently" // Simplified
                else -> "Offline"
            }
            
            val statusColor = if (userInfo.isOnline) {
                ChatColors.ConnectionConnected
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                maxLines = 1
            )
        }
    }
}

/**
 * Connection Status Banner
 * Shows when disconnected or connecting
 */
@Composable
fun ConnectionStatusBanner(
    state: RealtimeConnectionState,
    onRetryClick: (() -> Unit)? = null
) {
    val (visible, text, color) = when (state) {
        RealtimeConnectionState.Connected -> Triple(false, "", Color.Transparent)
        RealtimeConnectionState.Connecting -> Triple(true, "Connecting...", ChatColors.ConnectionConnecting)
        RealtimeConnectionState.Disconnected -> Triple(true, "Waiting for network...", ChatColors.ConnectionDisconnected)
        RealtimeConnectionState.Reconnecting -> Triple(true, "Reconnecting...", ChatColors.ConnectionConnecting)
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color)
                .padding(vertical = 4.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                
                // Show retry button only when disconnected
                if (state == RealtimeConnectionState.Disconnected && onRetryClick != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onRetryClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
