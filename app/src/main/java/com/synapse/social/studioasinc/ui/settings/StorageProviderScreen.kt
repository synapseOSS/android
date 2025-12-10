package com.synapse.social.studioasinc.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageProviderScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel
) {
    val storageConfig by viewModel.storageConfig.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Handle back press
    BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Storage Providers") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Configure how your media files are stored. You can use the default providers or your own self-hosted keys.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Global Provider Selection
            item {
                ProviderSelectionCard(
                    currentProvider = storageConfig.provider,
                    onProviderSelected = { viewModel.updateStorageProvider(it) }
                )
            }

            // Dynamic Configuration Section based on selection
            item {
                AnimatedContent(
                    targetState = storageConfig.provider,
                    label = "provider_config"
                ) { provider ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when (provider) {
                            "ImgBB" -> {
                                ImgBBConfigCard(
                                    apiKey = storageConfig.imgBBConfig.apiKey,
                                    onApiKeyChange = { viewModel.updateImgBBConfig(it) }
                                )
                            }
                            "Cloudinary" -> {
                                CloudinaryConfigCard(
                                    cloudName = storageConfig.cloudinaryConfig.cloudName,
                                    apiKey = storageConfig.cloudinaryConfig.apiKey,
                                    apiSecret = storageConfig.cloudinaryConfig.apiSecret,
                                    onConfigChange = { name, key, secret ->
                                        viewModel.updateCloudinaryConfig(name, key, secret)
                                    }
                                )
                            }
                            "Cloudflare R2" -> {
                                R2ConfigCard(
                                    accountId = storageConfig.r2Config.accountId,
                                    accessKeyId = storageConfig.r2Config.accessKeyId,
                                    secretAccessKey = storageConfig.r2Config.secretAccessKey,
                                    bucketName = storageConfig.r2Config.bucketName,
                                    onConfigChange = { acc, key, secret, bucket ->
                                        viewModel.updateR2Config(acc, key, secret, bucket)
                                    }
                                )
                            }
                            "Supabase" -> {
                                SupabaseConfigCard(
                                    url = storageConfig.supabaseConfig.url,
                                    apiKey = storageConfig.supabaseConfig.apiKey,
                                    bucketName = storageConfig.supabaseConfig.bucketName,
                                    onConfigChange = { url, key, bucket ->
                                        viewModel.updateSupabaseConfig(url, key, bucket)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSelectionCard(
    currentProvider: String,
    onProviderSelected: (String) -> Unit
) {
    val providers = listOf("ImgBB", "Cloudinary", "Cloudflare R2", "Supabase")
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Active Provider",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = currentProvider,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Provider") },
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
                                onProviderSelected(provider)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImgBBConfigCard(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    ConfigSectionCard(title = "ImgBB Configuration", iconVector = Icons.Default.Image) {
        StorageSecureTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = "API Key"
        )
        Text(
            text = "Used for image hosting only. Get your key from api.imgbb.com.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun CloudinaryConfigCard(
    cloudName: String,
    apiKey: String,
    apiSecret: String,
    onConfigChange: (String, String, String) -> Unit
) {
    ConfigSectionCard(title = "Cloudinary Configuration", iconVector = Icons.Default.CloudUpload) {
        OutlinedTextField(
            value = cloudName,
            onValueChange = { newName -> onConfigChange(newName, apiKey, apiSecret) },
            label = { Text("Cloud Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        StorageSecureTextField(
            value = apiKey,
            onValueChange = { newKey -> onConfigChange(cloudName, newKey, apiSecret) },
            label = "API Key"
        )
        StorageSecureTextField(
            value = apiSecret,
            onValueChange = { newSecret -> onConfigChange(cloudName, apiKey, newSecret) },
            label = "API Secret"
        )
        Text(
            text = "Supports images and videos. Optimized for media delivery.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun R2ConfigCard(
    accountId: String,
    accessKeyId: String,
    secretAccessKey: String,
    bucketName: String,
    onConfigChange: (String, String, String, String) -> Unit
) {
    ConfigSectionCard(title = "Cloudflare R2 Configuration", iconVector = Icons.Default.Cloud) {
        OutlinedTextField(
            value = accountId,
            onValueChange = { newVal -> onConfigChange(newVal, accessKeyId, secretAccessKey, bucketName) },
            label = { Text("Account ID") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        StorageSecureTextField(
            value = accessKeyId,
            onValueChange = { newVal -> onConfigChange(accountId, newVal, secretAccessKey, bucketName) },
            label = "Access Key ID"
        )
        StorageSecureTextField(
            value = secretAccessKey,
            onValueChange = { newVal -> onConfigChange(accountId, accessKeyId, newVal, bucketName) },
            label = "Secret Access Key"
        )
        OutlinedTextField(
            value = bucketName,
            onValueChange = { newVal -> onConfigChange(accountId, accessKeyId, secretAccessKey, newVal) },
            label = { Text("Bucket Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Text(
            text = "S3-compatible object storage. Good for all file types.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun SupabaseConfigCard(
    url: String,
    apiKey: String,
    bucketName: String,
    onConfigChange: (String, String, String) -> Unit
) {
    ConfigSectionCard(title = "Supabase Storage", iconVector = Icons.Default.Storage) {
        OutlinedTextField(
            value = url,
            onValueChange = { newVal -> onConfigChange(newVal, apiKey, bucketName) },
            label = { Text("Project URL") },
            placeholder = { Text("https://your-project.supabase.co") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        StorageSecureTextField(
            value = apiKey,
            onValueChange = { newVal -> onConfigChange(url, newVal, bucketName) },
            label = "Service Role / API Key"
        )
        OutlinedTextField(
            value = bucketName,
            onValueChange = { newVal -> onConfigChange(url, apiKey, newVal) },
            label = { Text("Bucket Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Text(
            text = "Open source Firebase alternative. Ensure policies allow read/write.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun ConfigSectionCard(
    title: String,
    iconVector: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun StorageSecureTextField(
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
            val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                 Icon(
                     imageVector = icon,
                     contentDescription = if (passwordVisible) "Hide password" else "Show password"
                 )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}
