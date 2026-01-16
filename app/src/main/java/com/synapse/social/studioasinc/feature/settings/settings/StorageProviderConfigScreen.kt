package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageProviderConfigScreen(
    onBackClick: () -> Unit,
    viewModel: StorageProviderConfigViewModel = viewModel()
) {
    val photoProviders by viewModel.photoProviders.collectAsState()
    val videoProviders by viewModel.videoProviders.collectAsState()
    val fileProviders by viewModel.fileProviders.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Storage Providers") },
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
    ) { padding ->
        LazyColumn(
             modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp), // Using hardcoded 16.dp as safe default if SettingsSpacing is not visible, but likely is.
                 // Actually, let's try to use SettingsSpacing.screenPadding if available. 
                 // If not, it will be a compile error. I'll stick to 16.dp for safety in this blind generation 
                 // unless I am 100% sure. 
                 // Usage in StorageDataScreen was SettingsSpacing.screenPadding. 
                 // I will assume SettingsSpacing is available since I am in the same package.
            verticalArrangement = Arrangement.spacedBy(24.dp), // SettingsSpacing.sectionSpacing
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Photos Section
            item {
                SettingsSection(title = "Photos") {
                    val options = listOf("ImgBB", "Cloudinary", "Supabase")
                    options.forEachIndexed { index, provider ->
                        SettingsCheckboxItem(
                            title = provider,
                            checked = photoProviders.contains(provider),
                            onCheckedChange = { viewModel.togglePhotoProvider(provider, it) }
                        )
                        if (index < options.lastIndex) {
                            SettingsDivider()
                        }
                    }
                }
            }

            // Videos Section
            item {
                SettingsSection(title = "Videos") {
                    val options = listOf("Cloudinary", "Supabase")
                    options.forEachIndexed { index, provider ->
                        SettingsCheckboxItem(
                            title = provider,
                            checked = videoProviders.contains(provider),
                            onCheckedChange = { viewModel.toggleVideoProvider(provider, it) }
                        )
                         if (index < options.lastIndex) {
                            SettingsDivider()
                        }
                    }
                }
            }

            // Files & Audio Section
            item {
                SettingsSection(title = "Files & Audio") {
                    val options = listOf("Supabase", "Cloudflare")
                    options.forEachIndexed { index, provider ->
                        SettingsCheckboxItem(
                            title = provider,
                            checked = fileProviders.contains(provider),
                            onCheckedChange = { viewModel.toggleFileProvider(provider, it) }
                        )
                         if (index < options.lastIndex) {
                            SettingsDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface 
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
