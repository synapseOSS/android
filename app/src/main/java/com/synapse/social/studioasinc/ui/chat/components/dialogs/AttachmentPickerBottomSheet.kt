package com.synapse.social.studioasinc.ui.chat.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPickerBottomSheet(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickDocument: () -> Unit,
    onPickAudio: () -> Unit,
    onPickLocation: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Share Content",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            // Grid Layout or List? List is safer for bottom sheet
            
            AttachmentItem(
                icon = Icons.Rounded.Image,
                label = "Gallery (Photo)",
                color = Color(0xFF9C27B0), // Purple
                onClick = onPickImage
            )
            
            AttachmentItem(
                icon = Icons.Rounded.Videocam,
                label = "Gallery (Video)",
                color = Color(0xFFE91E63), // Pink
                onClick = onPickVideo
            )
            
            AttachmentItem(
                icon = Icons.Rounded.CameraAlt,
                label = "Camera",
                color = Color(0xFF2196F3), // Blue
                onClick = onTakePhoto
            )
            
            AttachmentItem(
                icon = Icons.Rounded.Description,
                label = "Document",
                color = Color(0xFFFF9800), // Orange
                onClick = onPickDocument
            )
            
            AttachmentItem(
                icon = Icons.Rounded.AudioFile,
                label = "Audio",
                color = Color(0xFFF44336), // Red
                onClick = onPickAudio
            )
            
            AttachmentItem(
                icon = Icons.Rounded.LocationOn,
                label = "Location",
                color = Color(0xFF4CAF50), // Green
                onClick = onPickLocation
            )
        }
    }
}

@Composable
private fun AttachmentItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored circle background
        // Colored circle background
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
