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
 * Notification Settings screen for managing notification preferences.
 * 
 * Displays comprehensive notification settings including:
 * - Conversation Tones: Custom notification sounds for chats
 * - Reminders: Notification reminders and scheduling
 * - Notification Tones: System notification sounds
 * - Vibrate: Vibration patterns and settings
 * - LED Colors: LED notification colors
 * - High Priority Notifications: Important notification handling
 * - Reaction Notifications: Message reaction alerts
 * - Call Ringtones: Incoming call sounds
 * 
 * Uses Material 3 Expressive design with MediumTopAppBar and grouped sections.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBackClick: () -> Unit
) {
    val notificationPreferences by viewModel.notificationPreferences.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Notifications") },
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
            // Conversation Tones Section
            item {
                SettingsSection(title = "Conversation Tones") {
                    SettingsNavigationItem(
                        title = "Conversation Tones",
                        subtitle = "Set custom notification sounds for individual chats",
                        icon = R.drawable.ic_music_note,
                        onClick = { },
                        enabled = !isLoading
                    )
                }
            }

            // Reminders Section
            item {
                SettingsSection(title = "Reminders") {
                    SettingsToggleItem(
                        title = "Reminders",
                        subtitle = "Get reminded about unread messages",
                        icon = R.drawable.ic_schedule,
                        checked = notificationPreferences.remindersEnabled,
                        onCheckedChange = { viewModel.setReminders(it) },
                        enabled = !isLoading
                    )
                }
            }

            // Notification Tones Section
            item {
                SettingsSection(title = "Notification Tones") {
                    SettingsNavigationItem(
                        title = "Notification Tone",
                        subtitle = "Choose your default notification sound",
                        icon = R.drawable.ic_volume_up,
                        onClick = { },
                        enabled = !isLoading
                    )
                }
            }

            // Vibrate Section
            item {
                SettingsSection(title = "Vibrate") {
                    SettingsSelectionItem(
                        title = "Vibrate",
                        subtitle = "Choose vibration pattern for notifications",
                        icon = R.drawable.ic_vibration,
                        options = listOf("Off", "Default", "Short", "Long"),
                        selectedOption = "Default",
                        onSelect = { },
                        enabled = !isLoading
                    )
                }
            }

            // LED Colors Section
            item {
                SettingsSection(title = "LED Colors") {
                    SettingsSelectionItem(
                        title = "LED Color",
                        subtitle = "Choose LED notification color",
                        icon = R.drawable.ic_lightbulb,
                        options = listOf("White", "Red", "Yellow", "Green", "Cyan", "Blue", "Purple"),
                        selectedOption = "Blue",
                        onSelect = { },
                        enabled = !isLoading
                    )
                }
            }

            // High Priority Notifications Section
            item {
                SettingsSection(title = "High Priority Notifications") {
                    SettingsToggleItem(
                        title = "High Priority Notifications",
                        subtitle = "Show important notifications even in Do Not Disturb",
                        icon = R.drawable.ic_priority_high,
                        checked = notificationPreferences.highPriorityEnabled,
                        onCheckedChange = { viewModel.setHighPriority(it) },
                        enabled = !isLoading
                    )
                }
            }

            // Reaction Notifications Section
            item {
                SettingsSection(title = "Reaction Notifications") {
                    SettingsToggleItem(
                        title = "Reaction Notifications",
                        subtitle = "Get notified when someone reacts to your messages",
                        icon = R.drawable.ic_emoji_emotions,
                        checked = notificationPreferences.reactionNotificationsEnabled,
                        onCheckedChange = { viewModel.setReactionNotifications(it) },
                        enabled = !isLoading
                    )
                }
            }

            // Call Ringtones Section
            item {
                SettingsSection(title = "Call Ringtones") {
                    SettingsNavigationItem(
                        title = "Call Ringtone",
                        subtitle = "Choose ringtone for incoming calls",
                        icon = R.drawable.ic_call,
                        onClick = { },
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
