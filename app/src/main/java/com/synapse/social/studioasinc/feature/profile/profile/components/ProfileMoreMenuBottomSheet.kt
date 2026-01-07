package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMoreMenuBottomSheet(
    isOwnProfile: Boolean,
    onDismiss: () -> Unit,
    onShareProfile: () -> Unit,
    onViewAs: () -> Unit,
    onLockProfile: () -> Unit,
    onArchiveProfile: () -> Unit,
    onQrCode: () -> Unit,
    onCopyLink: () -> Unit,
    onSettings: () -> Unit,
    onActivityLog: () -> Unit,
    onBlockUser: () -> Unit,
    onReportUser: () -> Unit,
    onMuteUser: () -> Unit
) {
    val context = LocalContext.current
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            MenuOption(
                icon = Icons.Default.Share,
                text = "Share Profile",
                onClick = { onShareProfile(); onDismiss() }
            )
            
            if (isOwnProfile) {
                MenuOption(
                    icon = Icons.Default.Visibility,
                    text = "View As...",
                    onClick = { onViewAs(); onDismiss() }
                )

                MenuOption(
                    icon = Icons.Default.Lock,
                    text = "Lock Profile",
                    onClick = { onLockProfile(); onDismiss() }
                )
                MenuOption(
                    icon = Icons.Default.Archive,
                    text = "Archive Profile",
                    onClick = { onArchiveProfile(); onDismiss() }
                )
            }
            
            MenuOption(
                icon = Icons.Default.QrCode,
                text = "QR Code",
                onClick = { onQrCode(); onDismiss() }
            )
            MenuOption(
                icon = Icons.Default.ContentCopy,
                text = "Copy Profile Link",
                onClick = { onCopyLink(); onDismiss() }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            if (isOwnProfile) {
                MenuOption(
                    icon = Icons.Default.Settings,
                    text = "Settings",
                    onClick = { 
                        android.widget.Toast.makeText(context, "Settings clicked - attempting to navigate", android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                        onSettings()
                    }
                )
                MenuOption(
                    icon = Icons.Default.History,
                    text = "Activity Log",
                    onClick = { 
                        onDismiss()
                        onActivityLog()
                    }
                )
            } else {
                MenuOption(
                    icon = Icons.Default.Block,
                    text = "Block User",
                    onClick = { onBlockUser(); onDismiss() }
                )
                MenuOption(
                    icon = Icons.Default.Report,
                    text = "Report User",
                    onClick = { onReportUser(); onDismiss() }
                )
                MenuOption(
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                    text = "Mute User",
                    onClick = { onMuteUser(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun MenuOption(
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
