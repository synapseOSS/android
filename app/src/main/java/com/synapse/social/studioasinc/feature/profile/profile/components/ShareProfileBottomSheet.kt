package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProfileBottomSheet(
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    onShareToStory: () -> Unit,
    onShareViaMessage: () -> Unit,
    onShareExternal: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                "Share Profile",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            ShareOption(
                icon = Icons.Default.ContentCopy,
                text = "Copy Link",
                onClick = { onCopyLink(); onDismiss() }
            )
            ShareOption(
                icon = Icons.Default.AutoStories,
                text = "Share to Story",
                onClick = { onShareToStory(); onDismiss() }
            )
            ShareOption(
                icon = Icons.Default.Message,
                text = "Share via Message",
                onClick = { onShareViaMessage(); onDismiss() }
            )
            ShareOption(
                icon = Icons.Default.Share,
                text = "Share to External Apps",
                onClick = { onShareExternal(); onDismiss() }
            )
        }
    }
}

@Composable
private fun ShareOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
