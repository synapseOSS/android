package com.synapse.social.studioasinc.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.window.DialogProperties
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.model.AppUpdateInfo

/**
 * About and Support Settings screen.
 * 
 * Displays app information, version details, and provides access to support
 * resources including Terms of Service, Privacy Policy, Help Center, feedback
 * submission, update checking, and open source licenses.
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSupportScreen(
    onBackClick: () -> Unit,
    onNavigateToLicenses: () -> Unit = {},
    viewModel: AboutSupportViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    val appVersion by viewModel.appVersion.collectAsState()
    val buildNumber by viewModel.buildNumber.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    
    var showFeedbackDialog by remember { mutableStateOf(false) }
    
    // Show error/message snackbar if present
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("About & Support") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // App Logo and Version Header Card
            item {
                AppInfoHeaderCard(
                    appVersion = appVersion,
                    buildNumber = buildNumber
                )
            }

            // Legal Section
            item {
                SettingsSection(title = "Legal") {
                    SettingsNavigationItem(
                        title = "Terms of Service",
                        subtitle = "View our terms and conditions",
                        icon = R.drawable.ic_docs_48px,
                        onClick = {
                            val url = viewModel.getTermsOfServiceUrl()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Privacy Policy",
                        subtitle = "Learn how we protect your data",
                        icon = R.drawable.ic_shield_lock,
                        onClick = {
                            val url = viewModel.getPrivacyPolicyUrl()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Support Section
            item {
                SettingsSection(title = "Support") {
                    SettingsNavigationItem(
                        title = "Help Center",
                        subtitle = "FAQs and support resources",
                        icon = R.drawable.ic_info_48px,
                        onClick = {
                            val url = viewModel.getHelpCenterUrl()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Report a Problem",
                        subtitle = "Send us feedback or report issues",
                        icon = R.drawable.ic_bug_report_48px,
                        onClick = {
                            showFeedbackDialog = true
                        }
                    )
                }
            }

            // App Updates Section
            item {
                SettingsSection(title = "Updates") {
                    SettingsNavigationItem(
                        title = "Check for Updates",
                        subtitle = "See if a new version is available",
                        icon = R.drawable.ic_download,
                        onClick = {
                            viewModel.checkForUpdates()
                        },
                        enabled = !isLoading
                    )
                }
            }

            // Licenses Section
            item {
                SettingsSection(title = "Licenses") {
                    SettingsNavigationItem(
                        title = "Open Source Licenses",
                        subtitle = "View third-party library attributions",
                        icon = R.drawable.ic_bug_report_48px,
                        onClick = {
                            viewModel.navigateToLicenses()
                            onNavigateToLicenses()
                        }
                    )
                }
            }

            // Copyright Footer
            item {
                Text(
                    text = "© 2024 Synapse Social. All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }
    }

    // Feedback Dialog
    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = {
                showFeedbackDialog = false
                viewModel.resetFeedbackState()
            },
            onSubmit = { category, description ->
                viewModel.submitFeedback(category, description)
            },
            isLoading = isLoading,
            viewModel = viewModel
        )
    }

    // Update Available Dialog
    if (updateInfo != null) {
        UpdateAvailableDialog(
            updateInfo = updateInfo!!,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onDownload = {
                updateInfo?.downloadUrl?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
                viewModel.dismissUpdateDialog()
            }
        )
    }
}

/**
 * App information header card displaying logo and version info.
 * 
 * Displays the app logo, version name, and build number in a centered
 * card with elevated background.
 * 
 * @param appVersion The app version string
 * @param buildNumber The build number string
 * 
 * Requirements: 9.1
 */
@Composable
private fun AppInfoHeaderCard(
    appVersion: String,
    buildNumber: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SettingsSpacing.profileHeaderPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo
            Image(
                painter = painterResource(R.drawable.synapse_logo_small),
                contentDescription = "Synapse Logo",
                modifier = Modifier.size(80.dp)
            )

            // App Name
            Text(
                text = "Synapse",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Version Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Version $appVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Build $buildNumber",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Dialog to show when an app update is available.
 */
@Composable
fun UpdateAvailableDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isMandatory) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isMandatory,
            dismissOnClickOutside = !updateInfo.isMandatory
        ),
        title = { Text(stringResource(R.string.update_available_title)) },
        text = {
            Column {
                Text(stringResource(R.string.update_available_message_version, updateInfo.versionName))
                if (!updateInfo.releaseNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(updateInfo.releaseNotes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text(stringResource(R.string.update_action))
            }
        },
        dismissButton = {
            if (!updateInfo.isMandatory) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later))
                }
            }
        }
    )
}

/**
 * Feedback dialog for submitting bug reports and feature requests.
 * 
 * Displays a dialog with category selection dropdown, multiline description
 * text field, and submit button with loading state. Uses AlertDialog with
 * 28dp corner radius.
 * 
 * @param onDismiss Callback when dialog is dismissed
 * @param onSubmit Callback when feedback is submitted with category and description
 * @param isLoading Whether the submission is in progress
 * @param viewModel The AboutSupportViewModel for managing feedback state
 * 
 * Requirements: 9.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (category: String, description: String) -> Unit,
    isLoading: Boolean,
    viewModel: AboutSupportViewModel
) {
    var selectedCategory by remember { mutableStateOf("Bug") }
    var description by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    
    val categories = listOf("Bug", "Feature Request", "Other")
    val feedbackSubmitted by viewModel.feedbackSubmitted.collectAsState()
    
    // Close dialog when feedback is successfully submitted
    LaunchedEffect(feedbackSubmitted) {
        if (feedbackSubmitted) {
            kotlinx.coroutines.delay(500) // Brief delay to show success
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isLoading, dismissOnClickOutside = !isLoading)
    ) {
        Surface(
            shape = SettingsShapes.cardShape, // 28dp corner radius
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dialog Title
            Text(
                text = "Report a Problem",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { if (!isLoading) categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isLoading,
                    label = { Text("Category") },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) 
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = SettingsShapes.inputShape, // 12dp corner radius
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Description TextField
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                enabled = !isLoading,
                label = { Text("Description") },
                placeholder = { Text("Please describe the issue or suggestion...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = SettingsShapes.inputShape, // 12dp corner radius
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                maxLines = 6,
                minLines = 4
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (description.isNotBlank()) {
                            onSubmit(selectedCategory, description)
                        }
                    },
                    enabled = !isLoading && description.isNotBlank(),
                    shape = SettingsShapes.itemShape // 16dp corner radius
                ) {
                    if (isLoading) {
                        ExpressiveLoadingIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoading) "Submitting..." else "Submit")
                }
            }

            // Success Message
            if (feedbackSubmitted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Feedback submitted successfully!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        }
    }
}
