package com.synapse.social.studioasinc.feature.stories.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Story Viewer Screen
 * TODO: Re-implement chat feature - this screen is temporarily disabled
 */
@Composable
fun StoryViewerScreen(
    onClose: () -> Unit = {},
    viewModel: Any? = null // TODO: Re-implement with proper ViewModel
) {
    // TODO: Re-implement chat feature - placeholder screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Story Viewer",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Story viewer feature not implemented",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onClose) {
            Text("Close")
        }
    }
}
