package com.synapse.social.studioasinc.feature.settings.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

data class SettingsSearchItem(
    val title: String,
    val subtitle: String,
    val category: String,
    val route: String,
    val keywords: List<String>
)

/**
 * Settings search screen for finding specific settings quickly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSearchScreen(
    onBackClick: () -> Unit,
    onNavigateToSetting: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchItems = remember { getSearchableSettings() }
    
    val filteredItems = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            searchItems.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.keywords.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search settings...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true
            )
            
            // Search results
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(filteredItems) { item ->
                    SettingsSearchResultItem(
                        item = item,
                        onClick = { onNavigateToSetting(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSearchResultItem(
    item: SettingsSearchItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun getSearchableSettings(): List<SettingsSearchItem> = listOf(
    SettingsSearchItem(
        title = "Security Notifications",
        subtitle = "Get notified about security events",
        category = "Account",
        route = "account",
        keywords = listOf("security", "alerts", "notifications")
    ),
    SettingsSearchItem(
        title = "Read Receipts",
        subtitle = "Show when you've read messages",
        category = "Privacy",
        route = "privacy",
        keywords = listOf("read", "receipts", "seen", "messages")
    ),
    SettingsSearchItem(
        title = "Chat Themes",
        subtitle = "Customize chat appearance",
        category = "Chat",
        route = "chat",
        keywords = listOf("theme", "colors", "appearance", "customize")
    ),
    SettingsSearchItem(
        title = "Notification Tones",
        subtitle = "Choose notification sounds",
        category = "Notifications",
        route = "notifications",
        keywords = listOf("sound", "tone", "ringtone", "alert")
    ),
    SettingsSearchItem(
        title = "Data Saver",
        subtitle = "Reduce data usage",
        category = "Storage & Data",
        route = "storage",
        keywords = listOf("data", "saver", "usage", "bandwidth")
    )
)
