package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ProfileInfoCustomizationDialog(
    onDismiss: () -> Unit,
    onNavigateToEditProfile: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Profile Details") },
        text = {
            Column {
                Text("Control which details are visible on your profile.")
                Spacer(modifier = Modifier.height(16.dp))
                // Placeholder for future implementation of toggles per item
                Text("For now, you can edit your details in the Edit Profile screen.",
                     style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onNavigateToEditProfile()
            }) {
                Text("Go to Edit Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
