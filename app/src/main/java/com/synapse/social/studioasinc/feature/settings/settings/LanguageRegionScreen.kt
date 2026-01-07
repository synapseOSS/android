package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

/**
 * Language and Region Settings screen for managing language preferences.
 * 
 * Displays:
 * - Current language with checkmark indicator
 * - Available languages list with native script names (e.g., "日本語", "Español")
 * - Region preferences navigation (placeholder)
 * 
 * Uses Material 3 Expressive design with MediumTopAppBar and RadioButton selection.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageRegionScreen(
    viewModel: LanguageRegionViewModel,
    onBackClick: () -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Language & Region") },
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
            // Language Selection Section
            item {
                SettingsSection(title = "App Language") {
                    // Display available languages with RadioButton selection
                    availableLanguages.forEachIndexed { index, languageOption ->
                        LanguageSelectionItem(
                            languageOption = languageOption,
                            isSelected = viewModel.isLanguageSelected(languageOption),
                            onClick = {
                                if (!isLoading) {
                                    viewModel.setLanguage(languageOption)
                                }
                            },
                            enabled = !isLoading
                        )
                        
                        // Add divider between items (except after last item)
                        if (index < availableLanguages.size - 1) {
                            SettingsDivider()
                        }
                    }
                }
            }

            // Region Preferences Section (Placeholder)
            item {
                SettingsSection(title = "Region") {
                    SettingsNavigationItem(
                        title = "Region Preferences",
                        subtitle = "Date, time, and number formats",
                        icon = R.drawable.ic_public,
                        onClick = { viewModel.navigateToRegionPreferences() },
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
 * Composable for displaying a language selection item with RadioButton.
 * 
 * Shows the language name in its native script with a radio button indicator
 * for the currently selected language.
 * 
 * @param languageOption The language option to display
 * @param isSelected Whether this language is currently selected
 * @param onClick Callback when the item is clicked
 * @param enabled Whether the item is enabled for interaction
 * 
 * Requirements: 8.1, 8.4
 */
@Composable
private fun LanguageSelectionItem(
    languageOption: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SettingsSpacing.itemPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language name in native script
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Native name (primary display)
                Text(
                    text = languageOption.nativeName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                
                // English name (secondary, if different from native)
                if (languageOption.name != languageOption.nativeName) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = languageOption.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            }

            // Radio button indicator
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
