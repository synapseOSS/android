package com.synapse.social.studioasinc.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.local.AIConfig
import com.synapse.social.studioasinc.data.local.StorageConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onAccountClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val aiConfig by viewModel.aiConfig.collectAsState()
    val storageConfig by viewModel.storageConfig.collectAsState()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // AI Configuration Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "AI Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    AIConfigurationCard(
                        aiConfig = aiConfig,
                        onConfigChange = { provider, key, endpoint ->
                            viewModel.updateAIConfig(provider, key, endpoint)
                        }
                    )
                }
            }

            // Storage & Data Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Storage & Data",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    StorageConfigurationCard(
                        storageConfig = storageConfig,
                        onProviderChange = { viewModel.updateStorageProvider(it) },
                        onImgBBChange = { viewModel.updateImgBBConfig(it) },
                        onCloudinaryChange = { cloud, key, secret -> viewModel.updateCloudinaryConfig(cloud, key, secret) },
                        onR2Change = { acc, key, secret, bucket -> viewModel.updateR2Config(acc, key, secret, bucket) }
                    )
                }
            }

            // General Preferences Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "General",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        SettingRow(
                            icon = R.drawable.ic_person,
                            title = stringResource(R.string.settings_account),
                            subtitle = stringResource(R.string.settings_account_subtitle),
                            onClick = onAccountClick
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingRow(
                            icon = R.drawable.ic_shield_lock,
                            title = stringResource(R.string.settings_privacy),
                            subtitle = stringResource(R.string.settings_privacy_subtitle),
                            onClick = onPrivacyClick
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingRow(
                            icon = R.drawable.ic_notifications,
                            title = stringResource(R.string.settings_notifications),
                            subtitle = stringResource(R.string.settings_notifications_subtitle),
                            onClick = onNotificationsClick
                        )
                    }
                }
            }

            // Logout Section
            item {
                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_logout),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out")
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfigurationCard(
    aiConfig: AIConfig,
    onConfigChange: (String, String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val providers = listOf("Gemini", "OpenAI", "Anthropic")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Provider Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = aiConfig.provider,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("AI Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    providers.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider) },
                            onClick = {
                                onConfigChange(provider, aiConfig.apiKey, aiConfig.endpoint)
                                expanded = false
                            }
                        )
                    }
                }
            }

            SecureTextField(
                value = aiConfig.apiKey,
                onValueChange = { onConfigChange(aiConfig.provider, it, aiConfig.endpoint) },
                label = "API Key"
            )

            OutlinedTextField(
                value = aiConfig.endpoint,
                onValueChange = { onConfigChange(aiConfig.provider, aiConfig.apiKey, it) },
                label = { Text("Endpoint URL (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageConfigurationCard(
    storageConfig: StorageConfig,
    onProviderChange: (String) -> Unit,
    onImgBBChange: (String) -> Unit,
    onCloudinaryChange: (String, String, String) -> Unit,
    onR2Change: (String, String, String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val providers = listOf("ImgBB", "Cloudinary", "Cloudflare R2")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Provider Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = storageConfig.provider,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Storage Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    providers.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider) },
                            onClick = {
                                onProviderChange(provider)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Dynamic Fields based on Provider
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (storageConfig.provider) {
                        "ImgBB" -> {
                            SecureTextField(
                                value = storageConfig.imgBBConfig.apiKey,
                                onValueChange = onImgBBChange,
                                label = "ImgBB API Key"
                            )
                        }
                        "Cloudinary" -> {
                            OutlinedTextField(
                                value = storageConfig.cloudinaryConfig.cloudName,
                                onValueChange = { onCloudinaryChange(it, storageConfig.cloudinaryConfig.apiKey, storageConfig.cloudinaryConfig.apiSecret) },
                                label = { Text("Cloud Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            SecureTextField(
                                value = storageConfig.cloudinaryConfig.apiKey,
                                onValueChange = { onCloudinaryChange(storageConfig.cloudinaryConfig.cloudName, it, storageConfig.cloudinaryConfig.apiSecret) },
                                label = "API Key"
                            )
                            SecureTextField(
                                value = storageConfig.cloudinaryConfig.apiSecret,
                                onValueChange = { onCloudinaryChange(storageConfig.cloudinaryConfig.cloudName, storageConfig.cloudinaryConfig.apiKey, it) },
                                label = "API Secret"
                            )
                        }
                        "Cloudflare R2" -> {
                            OutlinedTextField(
                                value = storageConfig.r2Config.accountId,
                                onValueChange = { onR2Change(it, storageConfig.r2Config.accessKeyId, storageConfig.r2Config.secretAccessKey, storageConfig.r2Config.bucketName) },
                                label = { Text("Account ID") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            SecureTextField(
                                value = storageConfig.r2Config.accessKeyId,
                                onValueChange = { onR2Change(storageConfig.r2Config.accountId, it, storageConfig.r2Config.secretAccessKey, storageConfig.r2Config.bucketName) },
                                label = "Access Key ID"
                            )
                            SecureTextField(
                                value = storageConfig.r2Config.secretAccessKey,
                                onValueChange = { onR2Change(storageConfig.r2Config.accountId, storageConfig.r2Config.accessKeyId, it, storageConfig.r2Config.bucketName) },
                                label = "Secret Access Key"
                            )
                            OutlinedTextField(
                                value = storageConfig.r2Config.bucketName,
                                onValueChange = { onR2Change(storageConfig.r2Config.accountId, storageConfig.r2Config.accessKeyId, storageConfig.r2Config.secretAccessKey, it) },
                                label = { Text("Bucket Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val image = if (passwordVisible)
                painterResource(R.drawable.ic_visibility_off)
            else
                painterResource(R.drawable.ic_visibility_off) // Using same icon for now as 'on' might be missing, or use text fallback if preferred.
            
            // Actually, let's use the text fallback to be safe as I only saw ic_visibility_off in the file list
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                 Icon(
                     painter = if (passwordVisible) painterResource(R.drawable.ic_visibility_off) else painterResource(R.drawable.ic_visibility_off), // Placeholder logic
                     contentDescription = if (passwordVisible) "Hide password" else "Show password",
                     tint = if (passwordVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                 )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
private fun SettingRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String?,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
