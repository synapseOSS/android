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
 * Appearance Settings screen composable.
 * 
 * Displays options for customizing the app's appearance including:
 * - Theme Mode (Light, Dark, System Default)
 * - Dynamic Color (Android 12+ wallpaper-based theming)
 * - Font Size (text size customization with live preview)
 * - Chat Customization (placeholder navigation)
 * 
 * Theme changes are applied immediately using SynapseTheme.
 * Dynamic Color option is conditionally visible based on SDK >= 31.
 * 
 * Uses MediumTopAppBar with back navigation and displays settings in grouped cards.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    viewModel: AppearanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChatCustomization: () -> Unit = {}
) {
    val appearanceSettings by viewModel.appearanceSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Appearance") },
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
            // Theme Section
            item {
                SettingsSection(title = "Theme") {
                    // Theme Mode Selection
                    SettingsSelectionItem(
                        title = "Theme Mode",
                        subtitle = "Choose your preferred theme",
                        icon = R.drawable.ic_rounded_corner,
                        options = viewModel.getThemeModeOptions(),
                        selectedOption = viewModel.getThemeModeDisplayName(appearanceSettings.themeMode),
                        onSelect = { selected ->
                            val mode = viewModel.getThemeModeFromDisplayName(selected)
                            viewModel.setThemeMode(mode)
                        },
                        enabled = !isLoading
                    )
                    
                    // Dynamic Color Toggle (conditionally visible for Android 12+)
                    if (viewModel.isDynamicColorSupported) {
                        SettingsDivider()
                        SettingsToggleItem(
                            title = "Dynamic Color",
                            subtitle = "Use colors from your wallpaper",
                            icon = R.drawable.ic_rounded_corner,
                            checked = appearanceSettings.dynamicColorEnabled,
                            onCheckedChange = { viewModel.setDynamicColorEnabled(it) },
                            enabled = !isLoading
                        )
                    }
                }
            }

            // Display Section
            item {
                SettingsSection(title = "Display") {
                    // Font Size Slider with live preview
                    SettingsSliderItem(
                        title = "Font Size",
                        subtitle = "Adjust text size for better readability",
                        value = viewModel.getSliderValueFromFontScale(appearanceSettings.fontScale),
                        valueRange = 0f..3f,
                        steps = 2, // 4 discrete values: 0, 1, 2, 3
                        onValueChange = { value ->
                            val scale = viewModel.getFontScaleFromSliderValue(value)
                            viewModel.setFontScale(scale)
                        },
                        valueLabel = { value ->
                            val scale = viewModel.getFontScaleFromSliderValue(value)
                            viewModel.getFontScalePreviewText(scale)
                        },
                        enabled = !isLoading
                    )
                }
            }


            // Media Layout Section
            item {
                SettingsSection(title = "Media Layout (Beta)") {
                    val options = viewModel.getPostViewStyleOptions()
                    val selected = viewModel.getPostViewStyleFromDisplayName(appearanceSettings.postViewStyle.displayName()).displayName()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SettingsSpacing.itemHorizontalPadding, vertical = SettingsSpacing.itemVerticalPadding)
                    ) {
                        Text(
                            text = "Post Media Grid",
                            style = SettingsTypography.itemTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose how multiple images/videos are displayed",
                            style = SettingsTypography.itemSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    onClick = { 
                                        val style = viewModel.getPostViewStyleFromDisplayName(label)
                                        viewModel.setPostViewStyle(style)
                                    },
                                    selected = label == selected,
                                    label = { 
                                        Row(
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(label)
                                            if (label == "Grid") {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ) {
                                                    Text(
                                                        text = "Beta",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
