package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPersonaEditorScreen(
    onBackClick: () -> Unit,
    viewModel: AiPersonaEditorViewModel = viewModel()
) {
    val personaConfig by viewModel.personaConfig.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var traitsText by remember { mutableStateOf("") }
    var scheduleText by remember { mutableStateOf("") }

    // Initialize text fields when config loads
    LaunchedEffect(personaConfig) {
        personaConfig?.let {
            traitsText = it.personalityTraits?.toString() ?: ""
            scheduleText = it.postingSchedule?.toString() ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Persona Editor") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
             // Simple snackbar host or could rely on parent Scaffold if passed down
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
                if (successMessage != null) {
                    Text(text = successMessage!!, color = MaterialTheme.colorScheme.primary)
                }

                Text("Personality Traits (JSON or Text)", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = traitsText,
                    onValueChange = { traitsText = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("e.g. {\"tone\": \"friendly\", \"interests\": [\"tech\", \"art\"]}") }
                )

                Text("Posting Schedule (JSON or Text)", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = scheduleText,
                    onValueChange = { scheduleText = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("e.g. {\"frequency\": \"daily\", \"times\": [\"10:00\", \"18:00\"]}") }
                )

                Button(
                    onClick = { viewModel.savePersonaConfig(traitsText, scheduleText) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save Configuration")
                    }
                }
            }
        }
    }
}
