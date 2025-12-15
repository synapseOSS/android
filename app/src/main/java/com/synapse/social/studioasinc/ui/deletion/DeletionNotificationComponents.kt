package com.synapse.social.studioasinc.ui.deletion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.model.deletion.*
import com.synapse.social.studioasinc.data.repository.deletion.DeletionNotification
import com.synapse.social.studioasinc.data.repository.deletion.RecoverySuggestion
import com.synapse.social.studioasinc.data.repository.deletion.RecoveryPriority
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import java.text.SimpleDateFormat
import java.util.*

/**
 * Comprehensive notification components for deletion operations.
 * 
 * Provides detailed error messages, success notifications with summaries,
 * and partial completion notifications with retry options.
 * 
 * Requirements: 4.3, 4.4, 6.5
 */

/**
 * Success notification dialog with detailed deletion summary.
 * 
 * @param result The successful deletion result
 * @param deletionType Type of deletion that was performed
 * @param onDismiss Callback when dialog is dismissed
 * 
 * Requirements: 1.4, 6.5
 */
@Composable
fun DeletionSuccessDialog(
    result: DeletionResult,
    deletionType: DeletionType,
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
                // Success header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Deletion Completed",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (deletionType) {
                                DeletionType.COMPLETE_HISTORY -> "All chat history deleted"
                                DeletionType.SELECTIVE_CHATS -> "Selected chats deleted"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Deletion summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Deletion Summary",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Messages deleted
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
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Operations completed
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
                                text = "${result.completedOperations.size} storage operations completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Storage locations
                        val storageTypes = result.completedOperations.map { it.storageType }.distinct()
                        if (storageTypes.isNotEmpty()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            
                            Text(
                                text = "Data deleted from:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            storageTypes.forEach { storageType ->
                                Text(
                                    text = "• ${storageType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Completion time
                        Text(
                            text = "Completed at ${SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Action button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
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

/**
 * Error notification dialog with detailed error information and recovery suggestions.
 * 
 * @param error The deletion error that occurred
 * @param suggestions List of recovery suggestions
 * @param onRetry Callback when user chooses to retry
 * @param onDismiss Callback when dialog is dismissed
 * 
 * Requirements: 1.5, 4.4
 */
@Composable
fun DeletionErrorDialog(
    error: DeletionError,
    suggestions: List<RecoverySuggestion> = emptyList(),
    onRetry: (() -> Unit)? = null,
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
                    .heightIn(max = 600.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_error),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Column {
                        Text(
                            text = "Deletion Failed",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = getErrorTypeDescription(error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // Error details
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
                            text = "Error Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = when (error) {
                                is DeletionError.NetworkError -> error.message
                                is DeletionError.DatabaseError -> error.message
                                is DeletionError.ValidationError -> error.message
                                is DeletionError.SystemError -> error.message
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        // Additional error context
                        when (error) {
                            is DeletionError.NetworkError -> {
                                if (error.retryable) {
                                    Text(
                                        text = "This error can be retried automatically when connection is restored.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            is DeletionError.DatabaseError -> {
                                Text(
                                    text = "Storage system: ${error.storageType.name.replace("_", " ").lowercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            is DeletionError.ValidationError -> {
                                Text(
                                    text = "Field: ${error.field}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            is DeletionError.SystemError -> {
                                if (error.recoverable) {
                                    Text(
                                        text = "This error may be recoverable with a retry.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Recovery suggestions
                if (suggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Suggested Actions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        
                        suggestions.forEach { suggestion ->
                            RecoverySuggestionCard(suggestion = suggestion)
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
                    
                    if (onRetry != null && (error is DeletionError.NetworkError && error.retryable || 
                                          error is DeletionError.SystemError && error.recoverable)) {
                        Button(
                            onClick = onRetry,
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
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recovery suggestion card component.
 * 
 * @param suggestion The recovery suggestion to display
 */
@Composable
private fun RecoverySuggestionCard(
    suggestion: RecoverySuggestion
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (suggestion.priority) {
                RecoveryPriority.CRITICAL -> 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                RecoveryPriority.HIGH -> 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Priority indicator
                val (icon, color) = when (suggestion.priority) {
                    RecoveryPriority.CRITICAL -> 
                        R.drawable.ic_error to MaterialTheme.colorScheme.error
                    RecoveryPriority.HIGH -> 
                        R.drawable.ic_warning to MaterialTheme.colorScheme.primary
                    RecoveryPriority.MEDIUM -> 
                        R.drawable.ic_info_48px to MaterialTheme.colorScheme.onSurfaceVariant
                    RecoveryPriority.LOW -> 
                        R.drawable.ic_info_48px to MaterialTheme.colorScheme.onSurfaceVariant
                }
                
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
                
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                if (suggestion.isAutomaticRetry) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Auto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (suggestion.actionText != null) {
                Text(
                    text = "Action: ${suggestion.actionText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Notification snackbar for quick status updates.
 * 
 * @param notification The deletion notification to display
 * @param onAction Callback for notification action
 * @param onDismiss Callback when notification is dismissed
 * 
 * Requirements: 4.4, 6.5
 */
@Composable
fun DeletionNotificationSnackbar(
    notification: DeletionNotification,
    onAction: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val (message, actionLabel, isError) = when (notification) {
        is DeletionNotification.Started -> {
            Triple("Deletion started...", null, false)
        }
        is DeletionNotification.Progress -> {
            val percentage = if (notification.progress.totalOperations > 0) {
                (notification.progress.completedOperations * 100) / notification.progress.totalOperations
            } else 0
            Triple("Deleting... $percentage%", null, false)
        }
        is DeletionNotification.Completed -> {
            Triple("Deletion completed successfully", null, false)
        }
        is DeletionNotification.PartiallyCompleted -> {
            Triple("Deletion partially completed", "View Details", false)
        }
        is DeletionNotification.Failed -> {
            Triple("Deletion failed", "Retry", true)
        }
        is DeletionNotification.Cancelled -> {
            Triple("Deletion cancelled", null, false)
        }
        is DeletionNotification.RetryStarted -> {
            Triple("Retrying ${notification.operationCount} operations...", null, false)
        }
        is DeletionNotification.RetryCompleted -> {
            if (notification.failureCount > 0) {
                Triple("Retry completed: ${notification.successCount} succeeded, ${notification.failureCount} failed", "View Failed", false)
            } else {
                Triple("Retry completed successfully", null, false)
            }
        }
        is DeletionNotification.RetryFailed -> {
            Triple("Retry failed: ${notification.error}", "Try Again", true)
        }
        is DeletionNotification.RetryScheduled -> {
            val delaySeconds = notification.delayMs / 1000
            Triple("Retry scheduled in ${delaySeconds}s", null, false)
        }
        is DeletionNotification.MaxRetriesExceeded -> {
            Triple("Max retries exceeded for operation", "Manual Retry", true)
        }
        is DeletionNotification.ErrorWithSuggestions -> {
            val errorMessage = when (notification.error) {
                is DeletionError.NetworkError -> notification.error.message
                is DeletionError.DatabaseError -> notification.error.message
                is DeletionError.ValidationError -> notification.error.message
                is DeletionError.SystemError -> notification.error.message
            }
            Triple("Error: $errorMessage", "View Solutions", true)
        }
        is DeletionNotification.DetailedSuccess -> {
            Triple("Deletion completed successfully", null, false)
        }
        is DeletionNotification.DetailedError -> {
            val errorMessage = when (notification.error) {
                is DeletionError.NetworkError -> notification.error.message
                is DeletionError.DatabaseError -> notification.error.message
                is DeletionError.ValidationError -> notification.error.message
                is DeletionError.SystemError -> notification.error.message
            }
            Triple("Error: $errorMessage", "View Details", true)
        }
        is DeletionNotification.NetworkStatusChange -> {
            if (notification.isConnected) {
                Triple("Network restored", null, false)
            } else {
                Triple("Network disconnected", null, true)
            }
        }
        is DeletionNotification.RetryQueueStatus -> {
            Triple("Processing retry queue...", null, false)
        }
    }
    
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = if (actionLabel != null && onAction != null) {
            {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        } else null,
        dismissAction = {
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Dismiss"
                )
            }
        },
        containerColor = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.inverseSurface
        },
        contentColor = if (isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.inverseOnSurface
        }
    ) {
        Text(message)
    }
}

/**
 * Get user-friendly description for error types.
 */
private fun getErrorTypeDescription(error: DeletionError): String {
    return when (error) {
        is DeletionError.NetworkError -> "Network connection issue"
        is DeletionError.DatabaseError -> "Database access problem"
        is DeletionError.ValidationError -> "Invalid request data"
        is DeletionError.SystemError -> "System error occurred"
    }
}