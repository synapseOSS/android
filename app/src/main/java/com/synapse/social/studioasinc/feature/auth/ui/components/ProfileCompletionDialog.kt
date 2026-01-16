package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ProfileCompletionDialog(
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Your Profile") },
        text = {
            Text("Add a profile picture, bio, and more to personalize your account and connect with others.")
        },
        confirmButton = {
            Button(onClick = onComplete) {
                Text("Complete Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}
