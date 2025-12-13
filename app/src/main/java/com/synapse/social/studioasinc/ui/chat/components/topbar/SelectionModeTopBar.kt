package com.synapse.social.studioasinc.ui.chat.components.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Selection Mode Top Bar
 * Shown when messages are selected, replaces the normal ChatTopBar.
 * 
 * Features:
 * - Back arrow to exit selection mode
 * - Counter showing "N selected"
 * - Action icons: Delete, Copy (conditional), Forward
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionModeTopBar(
    selectedCount: Int,
    canCopy: Boolean,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onForwardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Exit selection mode"
                )
            }
        },
        actions = {
            // Copy Action - Only visible when all selected messages are text
            AnimatedVisibility(
                visible = canCopy,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(onClick = onCopyClick) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy messages"
                    )
                }
            }
            
            // Forward Action - Always available
            IconButton(onClick = onForwardClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Forward,
                    contentDescription = "Forward messages"
                )
            }
            
            // Delete Action - Always available
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete messages",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier
    )
}
