package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    onEditProfileClick: () -> Unit,
    onNavigateToCategory: (SettingsDestination) -> Unit
) {
    val userProfile by viewModel.userProfileSummary.collectAsState()
    val categories by viewModel.settingsCategories.collectAsState()
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
                CircularProgressIndicator()
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
                            avatarUrl = profile.avatarUrl,
                            onEditProfileClick = onEditProfileClick
                        )
                    }
                }

                // Settings Categories
                items(categories) { category ->
                    SettingsCategoryCard(
                        category = category,
                        onClick = {
                            viewModel.onNavigateToCategory(category.destination)
                            onNavigateToCategory(category.destination)
                        }
                    )
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * A card displaying a settings category with icon, title, subtitle, and chevron.
 * 
 * Uses surfaceContainer background with 24dp corner radius. Icon is tinted with
 * onSurfaceVariant color, and chevron uses onSurfaceVariant at 0.5 alpha.
 * 
 * Requirements: 1.1, 1.4
 */
@Composable
private fun SettingsCategoryCard(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    SettingsCard {
        SettingsNavigationItem(
            title = category.title,
            subtitle = category.subtitle,
            icon = category.icon,
            onClick = onClick
        )
    }
}
