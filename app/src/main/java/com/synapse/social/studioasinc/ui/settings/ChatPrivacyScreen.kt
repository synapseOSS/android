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

/**
 * Chat Privacy Settings screen composable.
 *
 * Re-imagined with Material 3 Expressive components.
 * Displays options for managing chat privacy settings:
 * - Read Receipts
 * - Typing Indicators
 * - Privacy Information Card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPrivacyScreen(
    viewModel: ChatPrivacyViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Chat Privacy") },
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

            // Privacy Toggles
            item {
                SettingsSection(title = "Chat Privacy") {
                    SettingsToggleItem(
                        title = "Send Read Receipts",
                        subtitle = "Let others know when you've read their messages. You'll still receive read receipts from others.",
                        checked = uiState.sendReadReceipts,
                        onCheckedChange = { viewModel.toggleReadReceipts(it) },
                        enabled = !uiState.isLoading
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Send Typing Indicators",
                        subtitle = "Let others see when you're typing a message. You'll still see typing indicators from others.",
                        checked = uiState.showTypingIndicators,
                        onCheckedChange = { viewModel.toggleTypingIndicators(it) },
                        enabled = !uiState.isLoading
                    )
                }
            }

            // Privacy Information Card
            item {
                SettingsSection(title = "Privacy Information") {
                    // Custom content for the information card using standard spacing
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val bulletPoints = listOf(
                            "Disabling read receipts means others won't know when you've read their messages",
                            "Disabling typing indicators means others won't see when you're typing",
                            "These settings only affect what you send to others",
                            "You'll still receive read receipts and typing indicators from other users"
                        )

                        bulletPoints.forEach { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = point,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
