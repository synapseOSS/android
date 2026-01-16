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
 * Synapse Plus settings screen for premium features and verification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynapsePlusScreen(
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Synapse Plus") },
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
            item {
                SettingsSection(title = "Verification") {
                    SettingsToggleItem(
                        title = "Show Verified Badge",
                        subtitle = "Display verification badge on your profile",
                        checked = false,
                        onCheckedChange = { }
                    )
                }
            }

            item {
                SettingsSection(title = "Premium Features") {
                    SettingsClickableItem(
                        title = "Upgrade to Plus",
                        subtitle = "Unlock premium features and verification",
                        onClick = { }
                    )
                }
            }
        }
    }
}
