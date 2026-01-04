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
 * Displays sections for:
 * - Push Notifications (Activity): Likes, Comments, Follows, Messages, Mentions
 * - Notification Preferences: Sound, Do Not Disturb
 * - In-App Notifications toggle
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
            // Activity Section - Push Notifications
            item {
                SettingsSection(title = "Activity") {
                    // Likes
                    SettingsToggleItem(
                        title = "Likes",
                        subtitle = "Get notified when someone likes your posts",
                        icon = R.drawable.ic_favorite_48px,
                        checked = notificationPreferences.likesEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleNotificationCategory(NotificationCategory.LIKES, enabled)
                        },
                        enabled = !isLoading
                    )

                    SettingsDivider()

                    // Comments
                    SettingsToggleItem(
                        title = "Comments",
                        subtitle = "Get notified when someone comments on your posts",
                        icon = R.drawable.ic_comment_48px,
                        checked = notificationPreferences.commentsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleNotificationCategory(NotificationCategory.COMMENTS, enabled)
                        },
                        enabled = !isLoading
                    )

                    SettingsDivider()

                    // Follows
                    SettingsToggleItem(
                        title = "Follows",
                        subtitle = "Get notified when someone follows you",
                        icon = R.drawable.ic_people,
                        checked = notificationPreferences.followsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleNotificationCategory(NotificationCategory.FOLLOWS, enabled)
                        },
                        enabled = !isLoading
                    )

                    SettingsDivider()

                    // Messages
                    SettingsToggleItem(
                        title = "Messages",
                        subtitle = "Get notified when you receive new messages",
                        icon = R.drawable.ic_message,
                        checked = notificationPreferences.messagesEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleNotificationCategory(NotificationCategory.MESSAGES, enabled)
                        },
                        enabled = !isLoading
                    )

                    SettingsDivider()

                    // Mentions
                    SettingsToggleItem(
                        title = "Mentions",
                        subtitle = "Get notified when someone mentions you",
                        icon = R.drawable.ic_text_fields_48px,
                        checked = notificationPreferences.mentionsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleNotificationCategory(NotificationCategory.MENTIONS, enabled)
                        },
                        enabled = !isLoading
                    )
                }
            }

            // Preferences Section
            item {
                SettingsSection(title = "Preferences") {
                    // Notification Sound (Placeholder)
                    SettingsNavigationItem(
                        title = "Notification Sound",
                        subtitle = "Choose your notification sound",
                        icon = R.drawable.ic_volume_up,
                        onClick = { viewModel.navigateToNotificationSound() },
                        enabled = !isLoading
                    )

                    SettingsDivider()

                    // Do Not Disturb (Placeholder)
                    SettingsNavigationItem(
                        title = "Do Not Disturb",
                        subtitle = "Set quiet hours for notifications",
                        icon = R.drawable.ic_music_off,
                        onClick = { viewModel.navigateToDoNotDisturb() },
                        enabled = !isLoading
                    )

                    SettingsDivider()

                    // In-App Notifications
                    SettingsToggleItem(
                        title = "In-App Notifications",
                        subtitle = "Show notification banners while using the app",
                        icon = R.drawable.ic_notifications,
                        checked = notificationPreferences.inAppNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleInAppNotifications(enabled)
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
