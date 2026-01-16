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
 * Displays comprehensive chat settings including:
 * - Theme: Chat themes and customization
 * - Font Size: Text size adjustment
 * - Enter is Send: Send behavior configuration
 * - Media Visibility: Media auto-download and visibility
 * - Voice Transcripts: Voice message transcription
 * - Archived Chats: Archive management
 * - Chat Backup: Backup and restore settings
 * - Chat History Management: History deletion and management
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
    onNavigateToChatPrivacy: () -> Unit,
    onNavigateToChatHistoryDeletion: () -> Unit = {},
    onNavigateToChatCustomization: () -> Unit = {},
    onNavigateToChatWallpapers: () -> Unit = {}
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
            // Theme Section
            item {
                SettingsSection(title = "Theme") {
                    SettingsNavigationItem(
                        title = "Chat Themes",
                        subtitle = "Customize chat appearance and colors",
                        icon = R.drawable.ic_palette,
                        onClick = onNavigateToChatCustomization,
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Chat Wallpapers",
                        subtitle = "Set custom backgrounds for conversations",
                        icon = R.drawable.ic_image,
                        onClick = onNavigateToChatWallpapers,
                        enabled = !isLoading
                    )
                }
            }

            // Font Size Section
            item {
                SettingsSection(title = "Font Size") {
                    SettingsSliderItem(
                        title = "Chat Font Size",
                        subtitle = "Adjust text size in chat messages",
                        value = viewModel.getChatFontSizeSliderValue(chatSettings.chatFontScale),
                        valueRange = 0f..3f,
                        steps = 2,
                        onValueChange = { value ->
                            val scale = viewModel.getChatFontScaleFromSliderValue(value)
                            viewModel.setChatFontScale(scale)
                        },
                        valueLabel = { value ->
                            val scale = viewModel.getChatFontScaleFromSliderValue(value)
                            viewModel.getChatFontScalePreviewText(scale)
                        },
                        enabled = !isLoading
                    )
                }
            }

            // Enter is Send Section
            item {
                SettingsSection(title = "Enter is Send") {
                    SettingsToggleItem(
                        title = "Enter is Send",
                        subtitle = "Press Enter to send messages instead of adding new line",
                        icon = R.drawable.ic_send,
                        checked = chatSettings.enterIsSendEnabled,
                        onCheckedChange = { viewModel.setEnterIsSend(it) },
                        enabled = !isLoading
                    )
                }
            }

            // Media Visibility Section
            item {
                SettingsSection(title = "Media Visibility") {
                    SettingsSelectionItem(
                        title = "Media Auto-Download",
                        subtitle = "Choose when to automatically download media",
                        icon = R.drawable.ic_download,
                        options = listOf("Wi-Fi Only", "Wi-Fi and Cellular", "Never"),
                        selectedOption = "Wi-Fi Only",
                        onSelect = { },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Media Visibility in Gallery",
                        subtitle = "Show chat media in device gallery",
                        icon = R.drawable.ic_photo_library,
                        checked = chatSettings.mediaVisibilityEnabled,
                        onCheckedChange = { viewModel.setMediaVisibility(it) },
                        enabled = !isLoading
                    )
                }
            }

            // Voice Transcripts Section
            item {
                SettingsSection(title = "Voice Transcripts") {
                    SettingsToggleItem(
                        title = "Voice Transcripts",
                        subtitle = "Automatically transcribe voice messages",
                        icon = R.drawable.ic_mic,
                        checked = chatSettings.voiceTranscriptsEnabled,
                        onCheckedChange = { viewModel.setVoiceTranscripts(it) },
                        enabled = !isLoading
                    )
                }
            }

            // Archived Chats Section
            item {
                SettingsSection(title = "Archived Chats") {
                    SettingsNavigationItem(
                        title = "Archived Chats",
                        subtitle = "View and manage archived conversations",
                        icon = R.drawable.ic_archive,
                        onClick = { },
                        enabled = !isLoading
                    )
                }
            }

            // Chat Backup Section
            item {
                SettingsSection(title = "Chat Backup") {
                    SettingsNavigationItem(
                        title = "Chat Backup",
                        subtitle = "Backup and restore your chat history",
                        icon = R.drawable.ic_backup,
                        onClick = { },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Auto Backup",
                        subtitle = "Automatically backup chats daily",
                        icon = R.drawable.ic_cloud_upload,
                        checked = chatSettings.autoBackupEnabled,
                        onCheckedChange = { viewModel.setAutoBackup(it) },
                        enabled = !isLoading
                    )
                }
            }

            // Chat History Management Section
            item {
                SettingsSection(title = "Chat History Management") {
                    SettingsNavigationItem(
                        title = "Chat History Deletion",
                        subtitle = "Delete chat history from all devices",
                        icon = R.drawable.ic_delete,
                        onClick = onNavigateToChatHistoryDeletion,
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsSelectionItem(
                        title = "Keep Messages",
                        subtitle = "Automatically delete old messages",
                        icon = R.drawable.ic_schedule,
                        options = listOf("Forever", "1 Year", "30 Days", "7 Days"),
                        selectedOption = "Forever",
                        onSelect = { },
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
