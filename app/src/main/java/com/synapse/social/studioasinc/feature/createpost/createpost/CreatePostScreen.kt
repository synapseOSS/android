package com.synapse.social.studioasinc.ui.createpost

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import com.synapse.social.studioasinc.feature.shared.components.ExpressiveButton
import com.synapse.social.studioasinc.feature.shared.components.ButtonVariant
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Sheet States
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showPollSheet by remember { mutableStateOf(false) }
    var showAddToPostSheet by remember { mutableStateOf(false) }

    // Dialog States (Legacy/Simple)
    var showYoutubeDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    // Effects
    LaunchedEffect(true) {
        viewModel.loadDraft()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveDraft()
        }
    }

    LaunchedEffect(uiState.isPostCreated) {
        if (uiState.isPostCreated) {
            val message = if (uiState.isEditMode) "Post updated!" else "Post created!"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onNavigateUp()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Launchers
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.addMedia(uris)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            mediaLauncher.launch("*/*")
        } else {
            Toast.makeText(context, "Permissions required to access media", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchMediaPicker() {
        if (uiState.pollData != null) {
            Toast.makeText(context, "Remove poll to add media", Toast.LENGTH_SHORT).show()
        } else {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(permissions)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (uiState.isEditMode) "Edit Post" else "Create Post", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val isEnabled = !uiState.isLoading && (uiState.postText.isNotBlank() || uiState.mediaItems.isNotEmpty() || uiState.pollData != null)
                    ExpressiveButton(
                        onClick = { viewModel.submitPost() },
                        enabled = isEnabled,
                        text = if (uiState.isLoading) "Posting..." else "Post",
                        variant = ButtonVariant.Filled,
                        modifier = Modifier.height(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            AddToPostBar(
                onMediaClick = { launchMediaPicker() },
                onMoreClick = { showAddToPostSheet = true }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // User Header
            item {
                UserHeader(
                    user = uiState.currentUserProfile,
                    privacy = uiState.privacy,
                    onPrivacyClick = { showPrivacySheet = true }
                )
            }

            // Input
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp)) {
                        if (uiState.postText.isEmpty()) {
                            Text(
                                text = "What's on your mind?",
                                style = if (uiState.mediaItems.isEmpty()) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Dynamic Text Sizing
                        val textSize = if (uiState.postText.length < 80 && uiState.mediaItems.isEmpty() && uiState.pollData == null) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.bodyLarge
                        }

                        BasicTextField(
                            value = uiState.postText,
                            onValueChange = { viewModel.updateText(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = textSize.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = textSize.lineHeight * 1.2
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }
            }

            // Attachments
            item {
                 // Media Grid
                 if (uiState.mediaItems.isNotEmpty()) {
                     MediaPreviewGrid(
                         mediaItems = uiState.mediaItems,
                         onRemove = { viewModel.removeMedia(it) }
                     )
                 }

                 // Poll
                 uiState.pollData?.let { poll ->
                     PollPreviewCard(poll = poll, onDelete = { viewModel.setPoll(null) })
                 }

                 // Youtube
                 uiState.youtubeUrl?.let { url ->
                      YoutubePreviewCard(url = url, onDelete = { viewModel.setYoutubeUrl(null) })
                 }

                 // Location
                 uiState.location?.let { loc ->
                     LocationPreviewCard(location = loc, onDelete = { viewModel.setLocation(null) })
                 }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp)) // Bottom padding for content
            }
        }
    }

    // Sheets
    if (showPrivacySheet) {
        PrivacySelectionSheet(
            currentPrivacy = uiState.privacy,
            onPrivacySelected = {
                viewModel.setPrivacy(it)
                showPrivacySheet = false
            },
            onDismiss = { showPrivacySheet = false }
        )
    }

    if (showPollSheet) {
        PollCreationSheet(
            onDismiss = { showPollSheet = false },
            onCreatePoll = {
                if (uiState.mediaItems.isNotEmpty()) {
                    Toast.makeText(context, "Remove media to add poll", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.setPoll(it)
                    showPollSheet = false
                }
            }
        )
    }

    if (showAddToPostSheet) {
        AddToPostSheet(
            onDismiss = { showAddToPostSheet = false },
            onMediaClick = { launchMediaPicker() },
            onPollClick = {
                if (uiState.mediaItems.isNotEmpty()) {
                    Toast.makeText(context, "Remove media to add poll", Toast.LENGTH_SHORT).show()
                } else {
                    showPollSheet = true
                }
            },
            onYoutubeClick = { showYoutubeDialog = true },
            onLocationClick = { showLocationDialog = true }
        )
    }

    // Legacy Dialogs (retained for now, could be modernized later)
    if (showYoutubeDialog) {
        var youtubeUrl by remember { mutableStateOf("") }
        AlertDialog(
             onDismissRequest = { showYoutubeDialog = false },
             title = { Text("Add YouTube Video") },
             text = {
                 OutlinedTextField(
                     value = youtubeUrl,
                     onValueChange = { youtubeUrl = it },
                     label = { Text("YouTube URL") },
                     singleLine = true,
                     shape = RoundedCornerShape(12.dp)
                 )
             },
             confirmButton = {
                 Button(onClick = {
                     if (youtubeUrl.contains("youtube") || youtubeUrl.contains("youtu.be")) {
                         viewModel.setYoutubeUrl(youtubeUrl)
                         showYoutubeDialog = false
                     } else {
                         Toast.makeText(context, "Invalid YouTube URL", Toast.LENGTH_SHORT).show()
                     }
                 }) { Text("Add") }
             },
             dismissButton = {
                 TextButton(onClick = { showYoutubeDialog = false }) { Text("Cancel") }
             }
        )
    }

    if (showLocationDialog) {
        var locationText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Add Location") },
            text = {
                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    label = { Text("Location Name") },
                    singleLine = true,
                     shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (locationText.isNotBlank()) {
                        viewModel.setLocation(LocationData(name = locationText))
                        showLocationDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// Retained Preview Cards
@Composable
fun PollPreviewCard(poll: PollData, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = poll.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            poll.options.forEach { option ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun YoutubePreviewCard(url: String, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "YouTube Video", style = MaterialTheme.typography.labelMedium)
                Text(text = url, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Remove")
            }
        }
    }
}

@Composable
fun LocationPreviewCard(location: LocationData, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = location.name, style = MaterialTheme.typography.titleSmall)
                location.address?.let { 
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Remove")
            }
        }
    }
}
