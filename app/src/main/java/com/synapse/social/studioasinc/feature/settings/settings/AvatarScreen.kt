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
 * Avatar settings screen for creating and editing profile avatars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarScreen(
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Avatar") },
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
                SettingsSection(title = "Profile Photo") {
                    SettingsClickableItem(
                        title = "Change Profile Photo",
                        subtitle = "Update your profile picture",
                        onClick = { }
                    )
                    SettingsClickableItem(
                        title = "Remove Profile Photo",
                        subtitle = "Use default avatar",
                        onClick = { }
                    )
                }
            }

            item {
                SettingsSection(title = "Avatar Creation") {
                    SettingsClickableItem(
                        title = "Create Avatar",
                        subtitle = "Design a custom avatar",
                        onClick = { }
                    )
                    SettingsClickableItem(
                        title = "Edit Avatar",
                        subtitle = "Modify existing avatar",
                        onClick = { }
                    )
                }
            }
        }
    }
}
