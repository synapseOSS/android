package com.synapse.social.studioasinc.feature.profile.editprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelectionScreen(
    onBackClick: () -> Unit,
    onRegionSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    val countries = remember {
        Locale.getISOCountries()
            .map { code -> Locale("", code) }
            .map { locale -> locale.displayCountry }
            .filter { it.isNotEmpty() }
            .sorted()
            .distinct()
    }

    val filteredCountries = if (searchQuery.isEmpty()) {
        countries
    } else {
        countries.filter {
            it.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            if (!isActive) {
                TopAppBar(
                    title = { Text("Select Region") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isActive) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { isActive = false },
                    active = isActive,
                    onActiveChange = { isActive = it },
                    placeholder = { Text("Search region") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        } else {
                             IconButton(onClick = { isActive = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        }
                    }
                ) {
                    LazyColumn {
                        items(filteredCountries) { country ->
                            ListItem(
                                headlineContent = { Text(country) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onRegionSelected(country)
                                    }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCountries) { country ->
                        ListItem(
                            headlineContent = { Text(country) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRegionSelected(country)
                                }
                        )
                    }
                }
            }
        }
    }
}
