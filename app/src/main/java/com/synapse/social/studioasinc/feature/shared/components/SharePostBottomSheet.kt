package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostBottomSheet(
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    onShareToStory: () -> Unit,
    onShareViaMessage: () -> Unit,
    onShareExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "Share Post",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            ShareOption(
                icon = Icons.Default.Link,
                text = "Copy Link",
                onClick = {
                    onCopyLink()
                    onDismiss()
                }
            )
            
            ShareOption(
                icon = Icons.Default.AddCircle,
                text = "Share to Story",
                onClick = {
                    onShareToStory()
                    onDismiss()
                }
            )
            
            ShareOption(
                icon = Icons.AutoMirrored.Filled.Send,
                text = "Share via Message",
                onClick = {
                    onShareViaMessage()
                    onDismiss()
                }
            )
            
            ShareOption(
                icon = Icons.Default.Share,
                text = "Share to Other Apps",
                onClick = {
                    onShareExternal()
                    onDismiss()
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShareOption(
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
