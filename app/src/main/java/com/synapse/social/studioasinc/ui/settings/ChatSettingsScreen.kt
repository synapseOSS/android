package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

/**
 * Chat Settings screen for managing chat-related preferences.
 * 
 * Displays sections for:
 * - Chat Behavior: Read Receipts, Typing Indicators
 * - Media: Auto-Download preferences
 * - Privacy: Message Requests, Chat Privacy
 * 
 * Uses Material 3 Expressive design with MediumTopAppBar and grouped sections.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    viewModel: ChatSettingsViewModel,
    onBackClick: () -> Unit,
    onNavigateToChatPrivacy: () -> Unit
) {
    val chatSettings by viewModel.chatSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Chat Settings") },
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
            error?.let { errorMessage ->
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
            // Chat Behavior Section
            item {
                SettingsSection(title = "Chat Behavior") {
                    // Read Receipts
                    SettingsToggleItem(
                        title = "Read Receipts",
                        subtitle = "Let others see when you've read their messages",
                        icon = R.drawable.ic_check_circle,
                        checked = chatSettings.readReceiptsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleReadReceipts(enabled)
                        },
                        enabled = !isLoading
                    )

                    SettingsDivider()

                    // Typing Indicators
                    SettingsToggleItem(
                        title = "Typing Indicators",
                        subtitle = "Show when you're typing a message",
                        icon = R.drawable.ic_edit,
                        checked = chatSettings.typingIndicatorsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleTypingIndicators(enabled)
                        },
                        enabled = !isLoading
                    )
                }
            }

            // Media Section
            item {
                SettingsSection(title = "Media") {
                    // Media Auto-Download
                    SettingsSelectionItem(
                        title = "Media Auto-Download",
                        subtitle = "Choose when to automatically download media in chats",
                        icon = R.drawable.ic_download,
                        options = viewModel.getMediaAutoDownloadOptions().map { it.displayName() },
                        selectedOption = chatSettings.mediaAutoDownload.displayName(),
                        onSelect = { selectedName ->
                            // Find the enum value that matches the selected display name
                            val selectedSetting = viewModel.getMediaAutoDownloadOptions()
                                .find { it.displayName() == selectedName }
                            selectedSetting?.let { viewModel.setMediaAutoDownload(it) }
                        },
                        enabled = !isLoading
                    )
                }
            }

            // Privacy Section
            item {
                SettingsSection(title = "Privacy") {
                    // Message Requests (Placeholder)
                    SettingsNavigationItem(
                        title = "Message Requests",
                        subtitle = "Manage message requests from non-followers",
                        icon = R.drawable.ic_message,
                        onClick = { viewModel.navigateToMessageRequests() },
                        enabled = !isLoading
                    )

                    SettingsDivider()

                    // Chat Privacy (Links to existing ChatPrivacySettingsActivity)
                    SettingsNavigationItem(
                        title = "Chat Privacy",
                        subtitle = "Control who can message you",
                        icon = R.drawable.ic_lock,
                        onClick = {
                            viewModel.navigateToChatPrivacy()
                            onNavigateToChatPrivacy()
                        },
                        enabled = !isLoading
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
