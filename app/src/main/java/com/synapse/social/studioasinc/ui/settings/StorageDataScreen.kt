package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

/**
 * Storage and Data Settings screen for managing storage and data usage.
 * 
 * Displays sections for:
 * - Storage Usage: Visual progress indicator and cache size
 * - Cache Management: Clear cache button
 * - Data Saver: Toggle for reducing data consumption
 * - Configuration: Storage Provider and AI Configuration
 * 
 * Uses Material 3 Expressive design with MediumTopAppBar and grouped sections.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDataScreen(
    viewModel: StorageDataViewModel,
    onBackClick: () -> Unit,
    onNavigateToStorageProvider: () -> Unit,
    onNavigateToAIConfig: () -> Unit
) {
    val cacheSize by viewModel.cacheSize.collectAsState()
    val dataSaverEnabled by viewModel.dataSaverEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isClearingCache by viewModel.isClearingCache.collectAsState()
    val error by viewModel.error.collectAsState()
    val cacheClearedMessage by viewModel.cacheClearedMessage.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when cache is cleared
    LaunchedEffect(cacheClearedMessage) {
        cacheClearedMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearCacheClearedMessage()
        }
    }

    // Show error snackbar
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Storage & Data") },
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
            SnackbarHost(hostState = snackbarHostState)
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
            // Storage Usage Section
            item {
                SettingsSection(title = "Storage Usage") {
                    StorageUsageCard(
                        cacheSize = cacheSize,
                        isLoading = isLoading
                    )
                }
            }

            // Cache Management Section
            item {
                SettingsSection(title = "Cache Management") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = SettingsSpacing.itemHorizontalPadding,
                                vertical = SettingsSpacing.itemVerticalPadding
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cache Size",
                                    style = SettingsTypography.itemTitle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatBytes(cacheSize),
                                    style = SettingsTypography.itemSubtitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Clear Cache Button
                        FilledTonalButton(
                            onClick = { viewModel.clearCache() },
                            enabled = !isClearingCache && cacheSize > 0,
                            modifier = Modifier.fillMaxWidth(),
                            shape = SettingsShapes.itemShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            if (isClearingCache) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clearing...")
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete_48px),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear Cache")
                            }
                        }
                    }
                }
            }

            // Data Saver Section
            item {
                SettingsSection(title = "Data Usage") {
                    SettingsToggleItem(
                        title = "Data Saver",
                        subtitle = "Reduce image quality and disable auto-play videos",
                        icon = R.drawable.data_usage_24px,
                        checked = dataSaverEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleDataSaver(enabled)
                        },
                        enabled = !isLoading
                    )
                }
            }

            // Configuration Section
            item {
                SettingsSection(title = "Configuration") {
                    // Storage Provider Configuration
                    SettingsNavigationItem(
                        title = "Storage Provider",
                        subtitle = "Configure image and file storage providers",
                        icon = R.drawable.file_save_24px,
                        onClick = {
                            viewModel.navigateToStorageProviderConfig()
                            onNavigateToStorageProvider()
                        },
                        enabled = !isLoading
                    )

                    SettingsDivider()

                    // AI Configuration
                    SettingsNavigationItem(
                        title = "AI Configuration",
                        subtitle = "Configure AI provider settings",
                        icon = R.drawable.ic_ai_summary,
                        onClick = {
                            viewModel.navigateToAIConfig()
                            onNavigateToAIConfig()
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

/**
 * Storage usage card with visual progress indicator.
 * 
 * Displays cache size with a visual representation.
 * 
 * Requirements: 7.1
 */
@Composable
private fun StorageUsageCard(
    cacheSize: Long,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Cache Storage",
                    style = SettingsTypography.itemTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Temporary files and cached data",
                    style = SettingsTypography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual progress indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            // Show a simple progress bar based on cache size
            // For visual purposes, we'll show progress relative to a 100MB threshold
            val maxSize = 100 * 1024 * 1024L // 100 MB
            val progress = (cacheSize.toFloat() / maxSize).coerceIn(0f, 1f)
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    progress < 0.5f -> MaterialTheme.colorScheme.primary
                    progress < 0.8f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Size display
        Text(
            text = formatBytes(cacheSize),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Formats bytes into a human-readable string (KB, MB, GB).
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
