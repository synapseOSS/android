package com.synapse.social.studioasinc.ui.deletion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.model.deletion.DeletionRequest
import com.synapse.social.studioasinc.data.model.deletion.DeletionType
import com.synapse.social.studioasinc.ui.settings.*

/**
 * Chat History Deletion settings screen.
 * 
 * Provides options for managing chat history deletion including:
 * - Complete history deletion
 * - Selective chat deletion interface
 * - Deletion history and status viewing
 * 
 * Uses Material 3 Expressive design with MediumTopAppBar and grouped sections.
 * 
 * Requirements: 3.1, 6.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryDeletionScreen(
    onBackClick: () -> Unit,
    viewModel: ChatHistoryDeletionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deletionHistory by viewModel.deletionHistory.collectAsState()
    val availableChats by viewModel.availableChats.collectAsState()
    
    var showDeleteAllConfirmation by remember { mutableStateOf(false) }
    var showSelectiveDeleteDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Handle UI state changes
    LaunchedEffect(uiState.isDeleting) {
        showProgressDialog = uiState.isDeleting
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Chat History Deletion") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            // Show error snackbar if there's an error
            uiState.error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(errorMessage)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Warning Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                text = "Permanent Deletion Warning",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Deleted chat history cannot be recovered. Data will be removed from all devices and cloud storage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Deletion Options Section
            item {
                SettingsSection(title = "Deletion Options") {
                    // Delete All History
                    SettingsButtonItem(
                        title = "Delete All Chat History",
                        onClick = { showDeleteAllConfirmation = true },
                        isDestructive = true,
                        enabled = !uiState.isDeleting && availableChats.isNotEmpty()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Selective Deletion
                    SettingsNavigationItem(
                        title = "Delete Specific Chats",
                        subtitle = if (availableChats.isEmpty()) "No chats available" else "${availableChats.size} chats available",
                        icon = R.drawable.ic_delete_48px,
                        onClick = { showSelectiveDeleteDialog = true },
                        enabled = !uiState.isDeleting && availableChats.isNotEmpty(),
                        position = SettingsItemPosition.Single
                    )
                }
            }

            // Current Status Section
            if (uiState.isDeleting || uiState.lastDeletionResult != null) {
                item {
                    SettingsSection(title = "Current Status") {
                        if (uiState.isDeleting) {
                            DeletionStatusCard(
                                title = "Deletion in Progress",
                                subtitle = "Please wait while your chat history is being deleted...",
                                icon = R.drawable.auto_delete_24px,
                                isLoading = true
                            )
                        } else if (uiState.lastDeletionResult != null) {
                            val result = uiState.lastDeletionResult!!
                            DeletionStatusCard(
                                title = if (result.success) "Deletion Completed" else "Deletion Failed",
                                subtitle = if (result.success) {
                                    "Successfully deleted ${result.totalMessagesDeleted} messages"
                                } else {
                                    "Some operations failed. Check deletion history for details."
                                },
                                icon = if (result.success) R.drawable.ic_check_circle else R.drawable.ic_error,
                                isLoading = false,
                                isError = !result.success
                            )
                        }
                    }
                }
            }

            // Deletion History Section
            if (deletionHistory.isNotEmpty()) {
                item {
                    SettingsHeaderItem(title = "Deletion History")
                }
                
                items(deletionHistory.take(5)) { historyItem ->
                    DeletionHistoryItem(
                        historyItem = historyItem,
                        onRetryClick = if (historyItem.canRetry) {
                            { viewModel.retryFailedDeletion(historyItem.id) }
                        } else null
                    )
                }
                
                if (deletionHistory.size > 5) {
                    item {
                        SettingsNavigationItem(
                            title = "View All History",
                            subtitle = "See all ${deletionHistory.size} deletion operations",
                            icon = R.drawable.ic_newsmode_48px,
                            onClick = { viewModel.navigateToFullHistory() },
                            position = SettingsItemPosition.Single
                        )
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Confirmation Dialogs
    if (showDeleteAllConfirmation) {
        DeletionConfirmationDialog(
            deletionRequest = DeletionRequest(
                id = "",
                userId = viewModel.getCurrentUserId(),
                type = DeletionType.COMPLETE_HISTORY,
                timestamp = System.currentTimeMillis()
            ),
            totalMessageCount = viewModel.getTotalMessageCount(),
            onConfirm = {
                showDeleteAllConfirmation = false
                viewModel.deleteAllHistory()
            },
            onCancel = {
                showDeleteAllConfirmation = false
            }
        )
    }

    if (showSelectiveDeleteDialog) {
        SelectiveDeletionDialog(
            availableChats = availableChats,
            onConfirm = { selectedChats ->
                showSelectiveDeleteDialog = false
                viewModel.deleteSelectedChats(selectedChats)
            },
            onCancel = {
                showSelectiveDeleteDialog = false
            }
        )
    }

    // Progress Dialog
    uiState.deletionProgress?.let { progress ->
        if (showProgressDialog) {
            DeletionProgressDialog(
                progress = progress,
                onCancel = if (progress.canCancel) {
                    { viewModel.cancelDeletion() }
                } else null,
                onDismiss = {
                    showProgressDialog = false
                    viewModel.clearProgress()
                }
            )
        }
    }
}

/**
 * Card displaying current deletion status.
 */
@Composable
private fun DeletionStatusCard(
    title: String,
    subtitle: String,
    icon: Int,
    isLoading: Boolean = false,
    isError: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                isLoading -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = when {
                        isError -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Individual deletion history item.
 */
@Composable
private fun DeletionHistoryItem(
    historyItem: DeletionHistoryItem,
    onRetryClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SettingsColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = historyItem.operationType,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = historyItem.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = historyItem.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            historyItem.isSuccess -> MaterialTheme.colorScheme.primary
                            historyItem.isError -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (historyItem.messagesAffected > 0) {
                        Text(
                            text = "${historyItem.messagesAffected} messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (onRetryClick != null) {
                    TextButton(
                        onClick = onRetryClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

/**
 * Data class representing a deletion history item.
 */
data class DeletionHistoryItem(
    val id: String,
    val operationType: String,
    val timestamp: String,
    val status: String,
    val messagesAffected: Int,
    val isSuccess: Boolean,
    val isError: Boolean,
    val canRetry: Boolean
)