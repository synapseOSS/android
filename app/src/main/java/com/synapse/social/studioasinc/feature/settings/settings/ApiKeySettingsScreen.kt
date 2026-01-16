package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.settings.ApiKeyInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySettingsScreen(
    viewModel: ApiKeySettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val apiKeys by viewModel.apiKeys
    val providerSettings by viewModel.providerSettings
    val isLoading by viewModel.isLoading
    
    var showAddKeyDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Key Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Provider Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Provider Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Preferred Provider Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = viewModel.getProviderDisplayName(providerSettings.preferredProvider),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Preferred Provider") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            viewModel.getAvailableProviders().forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(viewModel.getProviderDisplayName(provider)) },
                                    onClick = {
                                        viewModel.updatePreferredProvider(provider)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Fallback Setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = providerSettings.fallbackToPlatform,
                            onCheckedChange = { viewModel.updateFallbackSetting(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fallback to platform AI if user key fails")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // API Keys Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your API Keys",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        IconButton(
                            onClick = { showAddKeyDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add API Key")
                        }
                    }
                    
                    if (apiKeys.isEmpty()) {
                        Text(
                            text = "No API keys configured. Add your own keys for unlimited usage.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            items(apiKeys) { apiKey ->
                                ApiKeyItem(
                                    apiKey = apiKey,
                                    onDelete = { viewModel.deleteApiKey(apiKey.id) }
                                )
                            }
                        }
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    
    if (showAddKeyDialog) {
        AddApiKeyDialog(
            availableProviders = viewModel.getAvailableProviders(),
            onDismiss = { showAddKeyDialog = false },
            onAdd = { provider, key, name, limit ->
                viewModel.addApiKey(provider, name ?: "", key)
                showAddKeyDialog = false
            }
        )
    }
}

@Composable
fun ApiKeyItem(
    apiKey: ApiKeyInfo,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = apiKey.keyName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${apiKey.provider.uppercase()} • ${apiKey.usageCount}/${apiKey.usageLimit ?: "∞"} used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = { onDelete(apiKey.id) }
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddApiKeyDialog(
    availableProviders: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String?, Int?) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(availableProviders.firstOrNull() ?: "openai") }
    var apiKey by remember { mutableStateOf("") }
    var keyName by remember { mutableStateOf("") }
    var usageLimit by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add API Key") },
        text = {
            Column {
                // Provider Selection
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedProvider.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableProviders.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.uppercase()) },
                                onClick = {
                                    selectedProvider = provider
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // API Key Input
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) "Hide" else "Show"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Key Name
                OutlinedTextField(
                    value = keyName,
                    onValueChange = { keyName = it },
                    label = { Text("Key Name (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Usage Limit
                OutlinedTextField(
                    value = usageLimit,
                    onValueChange = { usageLimit = it },
                    label = { Text("Usage Limit (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        selectedProvider,
                        apiKey,
                        keyName.takeIf { it.isNotBlank() },
                        usageLimit.toIntOrNull()
                    )
                },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
