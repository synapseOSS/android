package com.synapse.social.studioasinc.ui.deletion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.model.deletion.*
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import com.synapse.social.studioasinc.ui.settings.SettingsColors

/**
 * Deletion confirmation dialog components for chat history deletion feature.
 * 
 * Provides clear warnings and specific deletion details to prevent accidental data loss.
 * Follows Material 3 design guidelines with consistent styling and accessibility support.
 * 
 * Requirements: 3.1, 3.2, 3.5
 */

/**
 * Data class representing chat information for deletion confirmation.
 */
data class ChatDeletionInfo(
    val chatId: String,
    val chatName: String,
    val messageCount: Int,
    val lastMessageDate: String?
)

/**
 * Confirmation dialog for chat history deletion operations.
 * 
 * Displays clear warnings about permanent data loss and specific details about what will be deleted.
 * Supports both complete history deletion and selective chat deletion.
 * 
 * @param deletionRequest The deletion request containing operation details
 * @param chatInfoList List of chat information for selective deletions
 * @param totalMessageCount Total number of messages that will be deleted
 * @param onConfirm Callback when user confirms the deletion
 * @param onCancel Callback when user cancels the deletion
 * 
 * Requirements: 3.1, 3.2, 3.5
 */
@Composable
fun DeletionConfirmationDialog(
    deletionRequest: DeletionRequest,
    chatInfoList: List<ChatDeletionInfo> = emptyList(),
    totalMessageCount: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = SettingsShapes.cardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning icon and title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = when (deletionRequest.type) {
                            DeletionType.COMPLETE_HISTORY -> "Delete All Chat History"
                            DeletionType.SELECTIVE_CHATS -> "Delete Selected Chats"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Warning message
                Text(
                    text = "⚠️ This action cannot be undone. Your chat data will be permanently deleted from all devices and cannot be recovered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Start
                )

                // Deletion details
                DeletionDetailsSection(
                    deletionType = deletionRequest.type,
                    chatInfoList = chatInfoList,
                    totalMessageCount = totalMessageCount
                )

                // Storage locations warning
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Data will be deleted from:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        
                        val storageLocations = listOf(
                            "• Local device storage",
                            "• Cloud backup (Supabase)",
                            "• Cached data and temporary files"
                        )
                        
                        storageLocations.forEach { location ->
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_48px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (deletionRequest.type) {
                                DeletionType.COMPLETE_HISTORY -> "Delete All"
                                DeletionType.SELECTIVE_CHATS -> "Delete Selected"
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section displaying specific details about what will be deleted.
 * 
 * Shows different information based on deletion type:
 * - Complete deletion: Total message count and warning
 * - Selective deletion: List of chats with message counts
 * 
 * @param deletionType Type of deletion operation
 * @param chatInfoList List of chat information for selective deletions
 * @param totalMessageCount Total number of messages to be deleted
 */
@Composable
private fun DeletionDetailsSection(
    deletionType: DeletionType,
    chatInfoList: List<ChatDeletionInfo>,
    totalMessageCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (deletionType) {
                DeletionType.COMPLETE_HISTORY -> {
                    Text(
                        text = "Complete History Deletion",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "All your chat conversations and messages will be permanently deleted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (totalMessageCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_message),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$totalMessageCount messages will be deleted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                DeletionType.SELECTIVE_CHATS -> {
                    Text(
                        text = "Selected Chats (${chatInfoList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (chatInfoList.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chatInfoList.forEach { chatInfo ->
                                ChatDeletionItem(chatInfo = chatInfo)
                            }
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_message),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Total: $totalMessageCount messages",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual chat item showing chat name and message count for deletion confirmation.
 * 
 * @param chatInfo Information about the chat to be deleted
 */
@Composable
private fun ChatDeletionItem(
    chatInfo: ChatDeletionInfo
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = chatInfo.chatName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            
            if (chatInfo.lastMessageDate != null) {
                Text(
                    text = "Last message: ${chatInfo.lastMessageDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Text(
            text = "${chatInfo.messageCount} messages",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Progress dialog for chat history deletion operations.
 * 
 * Displays real-time progress with percentage, estimated time remaining, and current operation status.
 * Supports cancellation for long-running operations with confirmation.
 * 
 * @param progress Current deletion progress information
 * @param onCancel Callback when user requests cancellation
 * @param onDismiss Callback when dialog should be dismissed (operation complete)
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4
 */
@Composable
fun DeletionProgressDialog(
    progress: DeletionProgress,
    onCancel: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showCancelConfirmation by remember { mutableStateOf(false) }
    
    // Auto-dismiss when operation is complete
    LaunchedEffect(progress.completedOperations, progress.totalOperations) {
        if (progress.completedOperations >= progress.totalOperations && progress.totalOperations > 0) {
            kotlinx.coroutines.delay(1000) // Brief delay to show completion
            onDismiss()
        }
    }
    
    AlertDialog(
        onDismissRequest = { 
            // Only allow dismissal if operation is complete or cancellable
            if (progress.completedOperations >= progress.totalOperations || !progress.canCancel) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = SettingsShapes.cardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Title and status
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Deleting Chat History",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (progress.currentOperation != null) {
                        Text(
                            text = progress.currentOperation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Progress indicator and percentage
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val progressPercentage = if (progress.totalOperations > 0) {
                        (progress.completedOperations.toFloat() / progress.totalOperations.toFloat())
                    } else {
                        0f
                    }
                    
                    LinearProgressIndicator(
                        progress = { progressPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(progressPercentage * 100).toInt()}% complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (progress.estimatedTimeRemaining != null && progress.estimatedTimeRemaining > 0) {
                            Text(
                                text = formatTimeRemaining(progress.estimatedTimeRemaining),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Operation details
                if (progress.totalOperations > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Operations:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${progress.completedOperations} / ${progress.totalOperations}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            if (progress.completedOperations >= progress.totalOperations) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check_circle),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Deletion completed successfully",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (progress.completedOperations >= progress.totalOperations) {
                        Arrangement.End
                    } else {
                        Arrangement.SpaceBetween
                    }
                ) {
                    // Cancel button (only show if operation can be cancelled and not complete)
                    if (progress.canCancel && progress.completedOperations < progress.totalOperations && onCancel != null) {
                        TextButton(
                            onClick = { showCancelConfirmation = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel")
                        }
                    }
                    
                    // Done button (only show when operation is complete)
                    if (progress.completedOperations >= progress.totalOperations) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
    
    // Cancel confirmation dialog
    if (showCancelConfirmation) {
        CancelDeletionConfirmationDialog(
            onConfirm = {
                showCancelConfirmation = false
                onCancel?.invoke()
            },
            onDismiss = {
                showCancelConfirmation = false
            }
        )
    }
}

/**
 * Confirmation dialog for cancelling an ongoing deletion operation.
 * 
 * @param onConfirm Callback when user confirms cancellation
 * @param onDismiss Callback when user dismisses the dialog
 */
@Composable
private fun CancelDeletionConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = SettingsShapes.cardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Cancel Deletion?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Cancelling will stop the deletion process. Any data that has already been deleted cannot be recovered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Continue Deletion")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Cancel Deletion")
                    }
                }
            }
        }
    }
}

/**
 * Formats time remaining in milliseconds to a human-readable string.
 * 
 * @param timeMs Time remaining in milliseconds
 * @return Formatted time string (e.g., "2m 30s", "45s")
 */
private fun formatTimeRemaining(timeMs: Long): String {
    val seconds = (timeMs / 1000).toInt()
    return when {
        seconds < 60 -> "${seconds}s remaining"
        seconds < 3600 -> {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            if (remainingSeconds > 0) {
                "${minutes}m ${remainingSeconds}s remaining"
            } else {
                "${minutes}m remaining"
            }
        }
        else -> {
            val hours = seconds / 3600
            val remainingMinutes = (seconds % 3600) / 60
            if (remainingMinutes > 0) {
                "${hours}h ${remainingMinutes}m remaining"
            } else {
                "${hours}h remaining"
            }
        }
    }
}



/**
 * Dialog for selecting specific chats to delete.
 * 
 * Displays a list of available chats with checkboxes for selection.
 * Shows chat names, message counts, and last message dates.
 * 
 * @param availableChats List of chats available for deletion
 * @param onConfirm Callback when user confirms deletion with selected chats
 * @param onCancel Callback when user cancels the dialog
 */
@Composable
fun SelectiveDeletionDialog(
    availableChats: List<ChatDeletionInfo>,
    onConfirm: (List<ChatDeletionInfo>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedChats by remember { mutableStateOf(setOf<String>()) }
    
    val selectedChatInfos = availableChats.filter { it.chatId in selectedChats }
    val totalSelectedMessages = selectedChatInfos.sumOf { it.messageCount }
    
    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = SettingsShapes.cardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Select Chats to Delete",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Selection summary
                if (selectedChats.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${selectedChats.size} chats selected • $totalSelectedMessages messages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Chat list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableChats) { chat ->
                        SelectableChatItem(
                            chatInfo = chat,
                            isSelected = chat.chatId in selectedChats,
                            onSelectionChange = { isSelected ->
                                selectedChats = if (isSelected) {
                                    selectedChats + chat.chatId
                                } else {
                                    selectedChats - chat.chatId
                                }
                            }
                        )
                    }
                }
                
                // Warning for selected chats
                if (selectedChats.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_warning),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Selected chats will be permanently deleted from all devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { onConfirm(selectedChatInfos) },
                        enabled = selectedChats.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_48px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Selected")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for handling failed deletion operations with retry options.
 * 
 * Displays failed operations with detailed error information and provides
 * manual retry options for users to recover from failures.
 * 
 * @param failedOperations List of failed deletion operations
 * @param onRetrySelected Callback when user selects operations to retry
 * @param onRetryAll Callback when user chooses to retry all failed operations
 * @param onDismiss Callback when dialog is dismissed
 * 
 * Requirements: 4.1, 4.3, 4.5
 */
@Composable
fun RetryFailedOperationsDialog(
    failedOperations: List<DeletionOperation>,
    onRetrySelected: (List<DeletionOperation>) -> Unit,
    onRetryAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOperations by remember { mutableStateOf(setOf<String>()) }
    
    val selectedOps = failedOperations.filter { it.id in selectedOperations }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = SettingsShapes.cardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and summary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_error),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Failed Operations",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = "${failedOperations.size} deletion operations failed. You can retry them individually or all at once.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Selection summary
                if (selectedOperations.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${selectedOperations.size} operations selected for retry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Failed operations list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(failedOperations) { operation ->
                        FailedOperationItem(
                            operation = operation,
                            isSelected = operation.id in selectedOperations,
                            onSelectionChange = { isSelected ->
                                selectedOperations = if (isSelected) {
                                    selectedOperations + operation.id
                                } else {
                                    selectedOperations - operation.id
                                }
                            }
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (selectedOperations.isNotEmpty()) {
                        Button(
                            onClick = { onRetrySelected(selectedOps) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_refresh_black),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Selected")
                        }
                    }
                    
                    Button(
                        onClick = onRetryAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh_black),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry All")
                    }
                }
            }
        }
    }
}

/**
 * Individual failed operation item for the retry dialog.
 * 
 * @param operation The failed deletion operation
 * @param isSelected Whether the operation is selected for retry
 * @param onSelectionChange Callback when selection state changes
 */
@Composable
private fun FailedOperationItem(
    operation: DeletionOperation,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChange(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            }
        ),
        border = if (isSelected) {
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
            
            // Operation info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = operation.storageType.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (operation.retryCount > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Retry ${operation.retryCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                if (operation.messagesAffected > 0) {
                    Text(
                        text = "${operation.messagesAffected} messages affected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Show chat IDs if available
                if (!operation.chatIds.isNullOrEmpty()) {
                    Text(
                        text = "Chats: ${operation.chatIds.take(3).joinToString(", ")}${if (operation.chatIds.size > 3) "..." else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Error indicator
            Icon(
                painter = painterResource(R.drawable.ic_error),
                contentDescription = "Failed operation",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Dialog for displaying partial completion results with retry options.
 * 
 * Shows successful and failed operations separately, allowing users to
 * retry only the failed operations or accept the partial completion.
 * 
 * @param result The deletion result with partial completion
 * @param onRetryFailed Callback when user chooses to retry failed operations
 * @param onAcceptPartial Callback when user accepts the partial completion
 * @param onDismiss Callback when dialog is dismissed
 * 
 * Requirements: 4.3, 4.4
 */
@Composable
fun PartialCompletionDialog(
    result: DeletionResult,
    onRetryFailed: () -> Unit,
    onAcceptPartial: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = SettingsShapes.cardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Partial Completion",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Summary
                Text(
                    text = "The deletion operation completed partially. Some operations succeeded while others failed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Results summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Successful operations
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${result.completedOperations.size} operations completed successfully",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Failed operations
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_error),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${result.failedOperations.size} operations failed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Messages deleted
                        if (result.totalMessagesDeleted > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_message),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${result.totalMessagesDeleted} messages deleted",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                // Error details (if available)
                if (result.errors.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Error Details:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                            
                            result.errors.take(3).forEach { error ->
                                Text(
                                    text = "• ${when (error) {
                                        is DeletionError.NetworkError -> "Network: ${error.message}"
                                        is DeletionError.DatabaseError -> "Database: ${error.message}"
                                        is DeletionError.ValidationError -> "Validation: ${error.message}"
                                        is DeletionError.SystemError -> "System: ${error.message}"
                                    }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            
                            if (result.errors.size > 3) {
                                Text(
                                    text = "... and ${result.errors.size - 3} more errors",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Close")
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = onAcceptPartial,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Accept Partial")
                    }
                    
                    Button(
                        onClick = onRetryFailed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh_black),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry Failed")
                    }
                }
            }
        }
    }
}

/**
 * Individual selectable chat item for the selective deletion dialog.
 * 
 * @param chatInfo Information about the chat
 * @param isSelected Whether the chat is currently selected
 * @param onSelectionChange Callback when selection state changes
 */
@Composable
private fun SelectableChatItem(
    chatInfo: ChatDeletionInfo,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChange(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) {
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
            
            // Chat info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = chatInfo.chatName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${chatInfo.messageCount} messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (chatInfo.lastMessageDate != null) {
                        Text(
                            text = "Last: ${chatInfo.lastMessageDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}