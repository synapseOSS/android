package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

/**
 * Accessibility and Help settings screen.
 * 
 * Displays comprehensive accessibility and help options including:
 * - Increase Contrast: Visual accessibility improvements
 * - Animation Toggles: Motion and animation controls
 * - Help Center: Support and documentation
 * - Feedback System: User feedback and bug reporting
 * - Terms: Terms of service and legal information
 * - App Info: Version and app information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen(
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Accessibility") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            // Increase Contrast Section
            item {
                SettingsSection(title = "Increase Contrast") {
                    SettingsToggleItem(
                        title = "Increase Contrast",
                        subtitle = "Darken key colors for better visibility",
                        icon = R.drawable.ic_contrast,
                        checked = false,
                        onCheckedChange = { }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "High Contrast Text",
                        subtitle = "Use high contrast colors for text",
                        icon = R.drawable.ic_text_format,
                        checked = false,
                        onCheckedChange = { }
                    )
                }
            }

            // Animation Toggles Section
            item {
                SettingsSection(title = "Animation Toggles") {
                    SettingsToggleItem(
                        title = "Reduce Animations",
                        subtitle = "Minimize motion and transitions",
                        icon = R.drawable.ic_animation,
                        checked = false,
                        onCheckedChange = { }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Auto-play Animations",
                        subtitle = "Toggle auto-play for stickers and GIFs",
                        icon = R.drawable.ic_play_circle,
                        checked = true,
                        onCheckedChange = { }
                    )
                }
            }

            // Help Center Section
            item {
                SettingsSection(title = "Help Center") {
                    SettingsNavigationItem(
                        title = "Help Center",
                        subtitle = "Get help and find answers",
                        icon = R.drawable.ic_help,
                        onClick = { }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Contact Support",
                        subtitle = "Get in touch with our support team",
                        icon = R.drawable.ic_support_agent,
                        onClick = { }
                    )
                }
            }

            // Feedback System Section
            item {
                SettingsSection(title = "Feedback System") {
                    SettingsNavigationItem(
                        title = "Send Feedback",
                        subtitle = "Share your thoughts and suggestions",
                        icon = R.drawable.ic_feedback,
                        onClick = { }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Report a Bug",
                        subtitle = "Report issues and bugs",
                        icon = R.drawable.ic_bug_report,
                        onClick = { }
                    )
                }
            }

            // Terms Section
            item {
                SettingsSection(title = "Terms") {
                    SettingsNavigationItem(
                        title = "Terms of Service",
                        subtitle = "Read our terms of service",
                        icon = R.drawable.ic_description,
                        onClick = { }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Privacy Policy",
                        subtitle = "Read our privacy policy",
                        icon = R.drawable.ic_privacy_tip,
                        onClick = { }
                    )
                }
            }

            // App Info Section
            item {
                SettingsSection(title = "App Info") {
                    SettingsNavigationItem(
                        title = "App Version",
                        subtitle = "Version 1.0.0 (Build 1)",
                        icon = R.drawable.ic_info,
                        onClick = { }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Open Source Licenses",
                        subtitle = "View third-party licenses",
                        icon = R.drawable.ic_code,
                        onClick = { }
                    )
                }
            }
        }
    }
}
