package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

/**
 * Account Settings screen composable.
 * 
 * Displays comprehensive account management options including:
 * - Security: Security notifications, passkeys, two-step verification
 * - Account Information: Email, phone number, account details
 * - Business: Business platform features
 * - Linked Accounts: Google, Facebook, Apple integration
 * - Session Management: Logout functionality
 * - Account Deletion: Delete account option
 * 
 * Uses MediumTopAppBar with back navigation and displays settings in grouped sections.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    onNavigateBack: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val linkedAccounts by viewModel.linkedAccounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showChangeEmailDialog by viewModel.showChangeEmailDialog.collectAsState()
    val showChangePasswordDialog by viewModel.showChangePasswordDialog.collectAsState()
    val showDeleteAccountDialog by viewModel.showDeleteAccountDialog.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.account_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_description)
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
            // Security Section
            item {
                SettingsSection(title = "Security") {
                    SettingsToggleItem(
                        title = "Security Notifications",
                        subtitle = "Get notified about security events",
                        checked = true,
                        onCheckedChange = { }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Passkeys",
                        subtitle = "Manage your passkeys",
                        icon = R.drawable.ic_key,
                        onClick = { }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Two-Step Verification",
                        subtitle = "Add extra security to your account",
                        icon = R.drawable.ic_security,
                        onClick = { }
                    )
                }
            }

            // Account Information Section
            item {
                SettingsSection(title = "Account Information") {
                    SettingsNavigationItem(
                        title = "Email Address",
                        subtitle = "Update your email address",
                        icon = R.drawable.ic_email_enhanced,
                        onClick = { viewModel.showChangeEmailDialog() }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Change Number",
                        subtitle = "Update your phone number",
                        icon = R.drawable.ic_phone,
                        onClick = { }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Account Info",
                        subtitle = "View your account details",
                        icon = R.drawable.ic_info,
                        onClick = { }
                    )
                }
            }

            // Business Section
            item {
                SettingsSection(title = "Business") {
                    SettingsNavigationItem(
                        title = "Business Platform",
                        subtitle = "Manage business features",
                        icon = R.drawable.ic_business,
                        onClick = { }
                    )
                }
            }

            // Linked Accounts Section
            item {
                SettingsSection(title = "Linked Accounts") {
                    // Google Account
                    LinkedAccountItem(
                        provider = SocialProvider.GOOGLE,
                        isLinked = linkedAccounts.googleLinked,
                        onConnect = { viewModel.connectSocialAccount(SocialProvider.GOOGLE) },
                        onDisconnect = { viewModel.disconnectSocialAccount(SocialProvider.GOOGLE) },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    // Facebook Account
                    LinkedAccountItem(
                        provider = SocialProvider.FACEBOOK,
                        isLinked = linkedAccounts.facebookLinked,
                        onConnect = { viewModel.connectSocialAccount(SocialProvider.FACEBOOK) },
                        onDisconnect = { viewModel.disconnectSocialAccount(SocialProvider.FACEBOOK) },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    // Apple Account
                    LinkedAccountItem(
                        provider = SocialProvider.APPLE,
                        isLinked = linkedAccounts.appleLinked,
                        onConnect = { viewModel.connectSocialAccount(SocialProvider.APPLE) },
                        onDisconnect = { viewModel.disconnectSocialAccount(SocialProvider.APPLE) },
                        enabled = !isLoading
                    )
                }
            }

            // Session Section
            item {
                SettingsSection(title = "Session") {
                    SettingsButtonItem(
                        title = "Logout",
                        onClick = onLogout,
                        isDestructive = true,
                        enabled = !isLoading
                    )
                }
            }

            // Danger Zone Section
            item {
                SettingsSection(title = "Danger Zone") {
                    SettingsButtonItem(
                        title = "Delete Account",
                        onClick = { viewModel.showDeleteAccountDialog() },
                        isDestructive = true,
                        enabled = !isLoading
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Dialogs
        if (showChangeEmailDialog) {
            ChangeEmailDialog(
                onDismiss = { viewModel.dismissChangeEmailDialog() },
                onConfirm = { newEmail, password ->
                    viewModel.changeEmail(newEmail, password)
                },
                isLoading = isLoading,
                error = error
            )
        }

        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                onDismiss = { viewModel.dismissChangePasswordDialog() },
                onConfirm = { currentPassword, newPassword, confirmPassword ->
                    viewModel.changePassword(currentPassword, newPassword, confirmPassword)
                },
                isLoading = isLoading,
                error = error,
                calculatePasswordStrength = { viewModel.calculatePasswordStrength(it) }
            )
        }

        if (showDeleteAccountDialog) {
            DeleteAccountDialog(
                onDismiss = { viewModel.dismissDeleteAccountDialog() },
                onConfirm = { confirmationText ->
                    viewModel.deleteAccount(confirmationText)
                },
                isLoading = isLoading,
                error = error
            )
        }
    }
}

/**
 * Composable for displaying a linked social account item.
 * 
 * Shows the provider icon, name, connection status, and connect/disconnect button.
 * 
 * @param provider The social provider
 * @param isLinked Whether the account is currently linked
 * @param onConnect Callback when connect button is clicked
 * @param onDisconnect Callback when disconnect button is clicked
 * @param enabled Whether the item is interactive
 */
@Composable
private fun LinkedAccountItem(
    provider: SocialProvider,
    isLinked: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Provider icon
            Icon(
                painter = painterResource(getProviderIcon(provider)),
                contentDescription = null,
                modifier = Modifier.size(SettingsSpacing.iconSize),
                tint = SettingsColors.itemIcon
            )
            
            // Provider name and status
            Column {
                Text(
                    text = provider.displayName,
                    style = SettingsTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLinked) "Connected" else "Not connected",
                    style = SettingsTypography.itemSubtitle,
                    color = if (isLinked) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Connect/Disconnect button
        FilledTonalButton(
            onClick = if (isLinked) onDisconnect else onConnect,
            enabled = enabled,
            shape = SettingsShapes.itemShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isLinked) MaterialTheme.colorScheme.errorContainer
                               else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isLinked) MaterialTheme.colorScheme.onErrorContainer
                              else MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(
                text = if (isLinked) "Disconnect" else "Connect",
                style = SettingsTypography.buttonText
            )
        }
    }
}

/**
 * Returns the appropriate icon resource for a social provider.
 */
private fun getProviderIcon(provider: SocialProvider): Int = when (provider) {
    SocialProvider.GOOGLE -> R.drawable.ic_google_logo
    SocialProvider.FACEBOOK -> R.drawable.ic_facebook_logo
    SocialProvider.APPLE -> R.drawable.ic_apple_logo
}
