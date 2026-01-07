package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

/**
 * Storage and Data Settings screen for managing storage and data usage.
 * 
 * Displays comprehensive storage and data management including:
 * - Storage Management: Cache size, storage usage, cleanup
 * - Network Usage: Data consumption monitoring
 * - Data Saving: Reduce data usage settings
 * - Proxy: Network proxy configuration
 * - Media Upload Quality: Image and video quality settings
 * - Auto-Download Rules: Media download preferences
 * 
 * Uses Material 3 Expressive design with MediumTopAppBar and grouped sections.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDataScreen(
    viewModel: StorageDataViewModel,
    onBackClick: () -> Unit
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
            // Storage Management Section
            item {
                SettingsSection(title = "Storage Management") {
                    StorageUsageCard(
                        cacheSize = cacheSize,
                        isLoading = isLoading
                    )
                    SettingsDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = SettingsSpacing.itemHorizontalPadding,
                                vertical = SettingsSpacing.itemVerticalPadding
                            )
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.clearCache() },
                            enabled = !isClearingCache && cacheSize > 0,
                            modifier = Modifier.fillMaxWidth(),
                            shape = SettingsShapes.itemShape
                        ) {
                            if (isClearingCache) {
                                ExpressiveLoadingIndicator(
                                    modifier = Modifier.size(18.dp),
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

            // Network Usage Section
            item {
                SettingsSection(title = "Network Usage") {
                    SettingsNavigationItem(
                        title = "Network Usage",
                        subtitle = "View data consumption statistics",
                        icon = R.drawable.ic_network_check,
                        onClick = { },
                        enabled = !isLoading
                    )
                }
            }

            // Data Saving Section
            item {
                SettingsSection(title = "Data Saving") {
                    SettingsToggleItem(
                        title = "Data Saver",
                        subtitle = "Reduce image quality and disable auto-play videos",
                        icon = R.drawable.data_usage_24px,
                        checked = dataSaverEnabled,
                        onCheckedChange = { viewModel.toggleDataSaver(it) },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Low Data Mode",
                        subtitle = "Use less data when possible",
                        icon = R.drawable.ic_data_saver_on,
                        checked = false,
                        onCheckedChange = { },
                        enabled = !isLoading
                    )
                }
            }

            // Proxy Section
            item {
                SettingsSection(title = "Proxy") {
                    SettingsNavigationItem(
                        title = "Proxy Settings",
                        subtitle = "Configure network proxy",
                        icon = R.drawable.ic_vpn_key,
                        onClick = { },
                        enabled = !isLoading
                    )
                }
            }

            // Media Upload Quality Section
            item {
                SettingsSection(title = "Media Upload Quality") {
                    SettingsSelectionItem(
                        title = "Photo Upload Quality",
                        subtitle = "Choose quality for uploaded photos",
                        icon = R.drawable.ic_photo,
                        options = listOf("Auto", "Best Quality", "Data Saver"),
                        selectedOption = "Auto",
                        onSelect = { },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsSelectionItem(
                        title = "Video Upload Quality",
                        subtitle = "Choose quality for uploaded videos",
                        icon = R.drawable.ic_videocam,
                        options = listOf("Auto", "Best Quality", "Data Saver"),
                        selectedOption = "Auto",
                        onSelect = { },
                        enabled = !isLoading
                    )
                }
            }

            // Auto-Download Rules Section
            item {
                SettingsSection(title = "Auto-Download Rules") {
                    SettingsSelectionItem(
                        title = "When using mobile data",
                        subtitle = "Auto-download media on cellular",
                        icon = R.drawable.ic_signal_cellular_4_bar,
                        options = listOf("Photos", "Audio", "Videos", "Documents", "None"),
                        selectedOption = "Photos",
                        onSelect = { },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsSelectionItem(
                        title = "When connected on Wi-Fi",
                        subtitle = "Auto-download media on Wi-Fi",
                        icon = R.drawable.ic_wifi,
                        options = listOf("All Media", "Photos", "Audio", "Videos", "Documents"),
                        selectedOption = "All Media",
                        onSelect = { },
                        enabled = !isLoading
                    )
                    SettingsDivider()
                    SettingsSelectionItem(
                        title = "When roaming",
                        subtitle = "Auto-download media when roaming",
                        icon = R.drawable.ic_roaming,
                        options = listOf("None", "Photos", "Audio"),
                        selectedOption = "None",
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
