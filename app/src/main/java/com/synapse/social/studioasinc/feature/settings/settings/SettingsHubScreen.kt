package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R


/**
 * Settings Hub screen - the main entry point for all settings.
 * 
 * Displays a profile header card with user information and categorized
 * settings groups that navigate to dedicated sub-screens. Uses Material 3
 * Expressive design with LargeTopAppBar and smooth scrolling behavior.
 * 
 * Requirements: 1.1, 1.4, 1.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    viewModel: SettingsHubViewModel,
    onBackClick: () -> Unit,
    onNavigateToCategory: (SettingsDestination) -> Unit
) {
    val userProfile by viewModel.userProfileSummary.collectAsState()
    val settingsGroups by viewModel.settingsGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_hub_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.settings_back_description),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToCategory(SettingsDestination.Search) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = "Search Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (isLoading && userProfile == null) {
            // Show loading indicator while profile loads
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                ExpressiveLoadingIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = SettingsSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Profile Header Card
                item {
                    userProfile?.let { profile ->
                        ProfileHeaderCard(
                            displayName = profile.displayName,
                            email = profile.email,
                            avatarUrl = profile.avatarUrl
                        )
                    }
                }

                // Settings Groups
                items(settingsGroups) { group ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (group.title != null) {
                             SettingsHeaderItem(title = group.title)
                        } else {
                            // If no title, we might want to add a spacer, but the LazyColumn
                            // already has spacedBy(SettingsSpacing.sectionSpacing)
                        }

                        // Render Group Items
                        SettingsCard {
                             group.categories.forEachIndexed { index, category ->
                                val position = when {
                                    group.categories.size == 1 -> SettingsItemPosition.Single
                                    index == 0 -> SettingsItemPosition.Top
                                    index == group.categories.lastIndex -> SettingsItemPosition.Bottom
                                    else -> SettingsItemPosition.Middle
                                }

                                SettingsNavigationItem(
                                    title = category.title,
                                    subtitle = category.subtitle,
                                    icon = category.icon,
                                    onClick = {
                                        viewModel.onNavigateToCategory(category.destination)
                                        onNavigateToCategory(category.destination)
                                    },
                                    position = position
                                )

                                if (index < group.categories.size - 1) {
                                    SettingsDivider()
                                }
                            }
                        }
                    }
                }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
    }
}
