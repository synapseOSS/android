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
 * Favourites settings screen for managing favorite contacts and content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Favourites") },
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
                SettingsSection(title = "Manage Favourites") {
                    SettingsClickableItem(
                        title = "Add to Favourites",
                        subtitle = "Select contacts to add to favourites",
                        onClick = { }
                    )
                    SettingsClickableItem(
                        title = "Reorder Favourites",
                        subtitle = "Change the order of favourite contacts",
                        onClick = { }
                    )
                    SettingsClickableItem(
                        title = "Remove from Favourites",
                        subtitle = "Remove contacts from favourites list",
                        onClick = { }
                    )
                }
            }

            item {
                SettingsSection(title = "Display Options") {
                    SettingsToggleItem(
                        title = "Show Favourites in Chat List",
                        subtitle = "Display favourite contacts at the top",
                        checked = true,
                        onCheckedChange = { }
                    )
                }
            }
        }
    }
}
