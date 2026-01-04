package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

/**
 * Privacy and Security Settings screen composable.
 * 
 * Displays options for managing privacy and security settings including:
 * - Profile Privacy (visibility settings)
 * - Content Visibility (who can see posts)
 * - Security (2FA, biometric lock)
 * - Blocking (blocked users, muted users)
 * - Active Sessions
 * 
 * Uses MediumTopAppBar with back navigation and displays settings in grouped cards.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    viewModel: PrivacySecurityViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToMutedUsers: () -> Unit,
    onNavigateToActiveSessions: () -> Unit
) {
    val privacySettings by viewModel.privacySettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Privacy & Security") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            if (error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error ?: "")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            // Profile Privacy Section
            item {
                SettingsSection(title = "Profile Privacy") {
                    SettingsSelectionItem(
                        title = "Profile Visibility",
                        subtitle = "Control who can view your profile",
                        icon = R.drawable.ic_person,
                        options = ProfileVisibility.values().map { it.displayName() },
                        selectedOption = privacySettings.profileVisibility.displayName(),
                        onSelect = { selected ->
                            val visibility = ProfileVisibility.values()
                                .find { it.displayName() == selected }
                            visibility?.let { viewModel.setProfileVisibility(it) }
                        },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsSelectionItem(
                        title = "Content Visibility",
                        subtitle = "Control who can see your posts",
                        icon = R.drawable.ic_public,
                        options = ContentVisibility.values().map { it.displayName() },
                        selectedOption = privacySettings.contentVisibility.displayName(),
                        onSelect = { selected ->
                            val visibility = ContentVisibility.values()
                                .find { it.displayName() == selected }
                            visibility?.let { viewModel.setContentVisibility(it) }
                        },
                        enabled = !isLoading
                    )
                }
            }

            // Security Section
            item {
                SettingsSection(title = "Security") {
                    SettingsToggleItem(
                        title = "Two-Factor Authentication",
                        subtitle = "Add an extra layer of security to your account",
                        icon = R.drawable.ic_shield_lock,
                        checked = privacySettings.twoFactorEnabled,
                        onCheckedChange = { viewModel.setTwoFactorEnabled(it) },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Biometric Lock",
                        subtitle = "Require fingerprint or face recognition to open app",
                        icon = R.drawable.ic_lock,
                        checked = privacySettings.biometricLockEnabled,
                        onCheckedChange = { viewModel.setBiometricLockEnabled(it) },
                        enabled = !isLoading
                    )
                }
            }

            // Blocking Section
            item {
                SettingsSection(title = "Blocking") {
                    SettingsNavigationItem(
                        title = "Blocked Users",
                        subtitle = "Manage users you've blocked",
                        icon = R.drawable.ic_close,
                        onClick = {
                            viewModel.navigateToBlockedUsers()
                            onNavigateToBlockedUsers()
                        },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Muted Users",
                        subtitle = "Manage users you've muted",
                        icon = R.drawable.ic_music_off,
                        onClick = {
                            viewModel.navigateToMutedUsers()
                            onNavigateToMutedUsers()
                        },
                        enabled = !isLoading
                    )
                }
            }

            // Sessions Section
            item {
                SettingsSection(title = "Sessions") {
                    SettingsNavigationItem(
                        title = "Active Sessions",
                        subtitle = "Manage your active login sessions",
                        icon = R.drawable.ic_component_exchange,
                        onClick = {
                            viewModel.navigateToActiveSessions()
                            onNavigateToActiveSessions()
                        },
                        enabled = !isLoading
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
