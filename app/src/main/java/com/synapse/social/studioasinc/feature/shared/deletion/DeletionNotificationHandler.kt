package com.synapse.social.studioasinc.ui.deletion

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.data.model.deletion.*
import com.synapse.social.studioasinc.data.repository.deletion.DeletionNotification
import com.synapse.social.studioasinc.data.repository.deletion.UserNotificationManager
import kotlinx.coroutines.flow.Flow

/**
 * Comprehensive notification handler for deletion operations.
 * 
 * Manages all types of deletion notifications including success summaries,
 * detailed error messages, retry notifications, and partial completion dialogs.
 * 
 * Requirements: 1.4, 1.5, 4.3, 4.4, 6.5
 */
@Composable
fun DeletionNotificationHandler(
    notificationFlow: Flow<DeletionNotification>,
    onRetryRequested: (List<DeletionOperation>) -> Unit,
    onPartialAccepted: (DeletionResult) -> Unit,
    onErrorRetry: () -> Unit
) {
    val notifications by notificationFlow.collectAsStateWithLifecycle(initialValue = null)
    
    var showSuccessDialog by remember { mutableStateOf<Pair<DeletionResult, DeletionType>?>(null) }
    var showErrorDialog by remember { mutableStateOf<Triple<DeletionError, List<com.synapse.social.studioasinc.data.repository.deletion.RecoverySuggestion>, Boolean>?>(null) }
    var showPartialDialog by remember { mutableStateOf<DeletionResult?>(null) }
    var showRetryDialog by remember { mutableStateOf<List<DeletionOperation>?>(null) }
    var showSnackbar by remember { mutableStateOf<DeletionNotification?>(null) }
    
    // Handle incoming notifications
    LaunchedEffect(notifications) {
        notifications?.let { notification ->
            when (notification) {
                is DeletionNotification.DetailedSuccess -> {
                    showSuccessDialog = notification.result to notification.request.type
                }
                
                is DeletionNotification.DetailedError -> {
                    showErrorDialog = Triple(
                        notification.error,
                        notification.suggestions,
                        notification.error is DeletionError.NetworkError && notification.error.retryable ||
                        notification.error is DeletionError.SystemError && notification.error.recoverable
                    )
                }
                
                is DeletionNotification.PartiallyCompleted -> {
                    showPartialDialog = notification.result
                }
                
                is DeletionNotification.MaxRetriesExceeded -> {
                    showRetryDialog = listOf(notification.operation)
                }
                
                is DeletionNotification.Failed -> {
                    showErrorDialog = Triple(
                        notification.error,
                        emptyList(),
                        notification.error is DeletionError.NetworkError && notification.error.retryable ||
                        notification.error is DeletionError.SystemError && notification.error.recoverable
                    )
                }
                
                // Show snackbar for quick status updates
                is DeletionNotification.Started,
                is DeletionNotification.Progress,
                is DeletionNotification.Completed,
                is DeletionNotification.Cancelled,
                is DeletionNotification.RetryStarted,
                is DeletionNotification.RetryCompleted,
                is DeletionNotification.RetryFailed,
                is DeletionNotification.RetryScheduled,
                is DeletionNotification.NetworkStatusChange,
                is DeletionNotification.RetryQueueStatus -> {
                    showSnackbar = notification
                }
                
                else -> {
                    // Handle other notification types as needed
                }
            }
        }
    }
    
    // Success dialog
    showSuccessDialog?.let { (result, deletionType) ->
        DeletionSuccessDialog(
            result = result,
            deletionType = deletionType,
            onDismiss = { showSuccessDialog = null }
        )
    }
    
    // Error dialog
    showErrorDialog?.let { (error, suggestions, canRetry) ->
        DeletionErrorDialog(
            error = error,
            suggestions = suggestions,
            onRetry = if (canRetry) onErrorRetry else null,
            onDismiss = { showErrorDialog = null }
        )
    }
    
    // Partial completion dialog
    showPartialDialog?.let { result ->
        PartialCompletionDialog(
            result = result,
            onRetryFailed = {
                onRetryRequested(result.failedOperations)
                showPartialDialog = null
            },
            onAcceptPartial = {
                onPartialAccepted(result)
                showPartialDialog = null
            },
            onDismiss = { showPartialDialog = null }
        )
    }
    
    // Retry failed operations dialog
    showRetryDialog?.let { failedOperations ->
        RetryFailedOperationsDialog(
            failedOperations = failedOperations,
            onRetrySelected = { selectedOps ->
                onRetryRequested(selectedOps)
                showRetryDialog = null
            },
            onRetryAll = {
                onRetryRequested(failedOperations)
                showRetryDialog = null
            },
            onDismiss = { showRetryDialog = null }
        )
    }
    
    // Snackbar notifications
    showSnackbar?.let { notification ->
        DeletionNotificationSnackbar(
            notification = notification,
            onAction = when (notification) {
                is DeletionNotification.PartiallyCompleted -> {
                    { showPartialDialog = notification.result }
                }
                is DeletionNotification.Failed -> {
                    { onErrorRetry() }
                }
                is DeletionNotification.RetryCompleted -> {
                    if (notification.failureCount > 0) {
                        { /* Show failed operations */ }
                    } else null
                }
                is DeletionNotification.RetryFailed -> {
                    { onErrorRetry() }
                }
                is DeletionNotification.MaxRetriesExceeded -> {
                    { showRetryDialog = listOf(notification.operation) }
                }
                is DeletionNotification.ErrorWithSuggestions -> {
                    { 
                        showErrorDialog = Triple(
                            notification.error,
                            notification.suggestions,
                            true
                        )
                    }
                }
                else -> null
            },
            onDismiss = { showSnackbar = null }
        )
    }
}

/**
 * Composable for handling deletion notifications in a screen.
 * 
 * @param viewModel The message deletion view model
 * @param userNotificationManager The user notification manager
 */
@Composable
fun HandleDeletionNotifications(
    viewModel: MessageDeletionViewModel,
    userNotificationManager: UserNotificationManager
) {
    DeletionNotificationHandler(
        notificationFlow = userNotificationManager.getNotifications(),
        onRetryRequested = { operations ->
            viewModel.retryFailedOperations(
                viewModel.getCurrentUserId(),
                operations
            )
        },
        onPartialAccepted = { result ->
            viewModel.handlePartialCompletion(
                result,
                PartialCompletionChoice.ACCEPT_PARTIAL
            )
        },
        onErrorRetry = {
            viewModel.retryFailedOperations(viewModel.getCurrentUserId())
        }
    )
}

/**
 * Enhanced notification summary for deletion operations.
 * 
 * Provides detailed information about what was deleted, from which storage
 * locations, and any issues that occurred during the process.
 * 
 * Requirements: 1.4, 6.5
 */
data class DeletionSummary(
    val totalMessagesDeleted: Int,
    val storageLocationsCleared: List<StorageType>,
    val operationsCompleted: Int,
    val operationsFailed: Int,
    val completionTime: Long,
    val errors: List<DeletionError> = emptyList()
) {
    val isFullySuccessful: Boolean
        get() = operationsFailed == 0 && errors.isEmpty()
    
    val isPartiallySuccessful: Boolean
        get() = operationsCompleted > 0 && (operationsFailed > 0 || errors.isNotEmpty())
    
    val hasFailures: Boolean
        get() = operationsFailed > 0 || errors.isNotEmpty()
}

/**
 * Create a deletion summary from a deletion result.
 */
fun DeletionResult.toSummary(): DeletionSummary {
    return DeletionSummary(
        totalMessagesDeleted = totalMessagesDeleted,
        storageLocationsCleared = completedOperations.map { it.storageType }.distinct(),
        operationsCompleted = completedOperations.size,
        operationsFailed = failedOperations.size,
        completionTime = System.currentTimeMillis(),
        errors = errors
    )
}