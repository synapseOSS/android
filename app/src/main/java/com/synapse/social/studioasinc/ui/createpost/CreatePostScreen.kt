package com.synapse.social.studioasinc.ui.createpost

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
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

    // Effects
    LaunchedEffect(true) {
        viewModel.loadDraft()
    }
    
    // Auto-save draft on pause handled by Activity or use DisposableEffect on screen
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

    var showPollDialog by remember { mutableStateOf(false) }
    var showYoutubeDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDraftDialog by remember { mutableStateOf(false) }

    // If needed to prompt user about recovering draft manually:
    // This logic is mostly handled by loadDraft() running automatically,
    // but if we wanted a dialog we could check !isEditMode && draftExists.
    // For now assuming automatic restore per design.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (uiState.isEditMode) "Edit Post" else "Create Post", 
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.submitPost() },
                        enabled = !uiState.isLoading && (uiState.postText.isNotBlank() || uiState.mediaItems.isNotEmpty() || uiState.pollData != null),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        if (uiState.isLoading) {
                            ExpressiveLoadingIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (uiState.isEditMode) "Update" else "Post")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            CreatePostBottomBar(
                onAddMedia = {
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
                },
                onAddPoll = {
                    if (uiState.mediaItems.isNotEmpty()) {
                        Toast.makeText(context, "Remove media to add poll", Toast.LENGTH_SHORT).show()
                    } else {
                        showPollDialog = true
                    }
                },
                onAddYoutube = { showYoutubeDialog = true },
                onAddLocation = { showLocationDialog = true },
                onSettings = { showSettingsSheet = true },
                currentPrivacy = uiState.privacy,
                onPrivacyClick = { showPrivacySheet = true }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    // Placeholder Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Posting as You", // Replace with actual user name
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        AssistChip(
                            onClick = { showPrivacySheet = true },
                            label = { Text(uiState.privacy.replaceFirstChar { it.uppercase() }) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when(uiState.privacy) {
                                        "followers" -> Icons.Default.Group
                                        "private" -> Icons.Default.Lock
                                        else -> Icons.Default.Public
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            border = null,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Input
            item {
                Box(modifier = Modifier.fillMaxWidth().requiredHeightIn(min = 120.dp)) {
                    if (uiState.postText.isEmpty()) {
                        Text(
                            text = "What's on your mind?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = uiState.postText,
                        onValueChange = { viewModel.updateText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Media Preview
            item {
                AnimatedVisibility(
                    visible = uiState.mediaItems.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(200.dp)
                    ) {
                        itemsIndexed(uiState.mediaItems) { index, item ->
                            MediaPreviewItem(item = item, onDelete = { viewModel.removeMedia(index) })
                        }
                    }
                }
            }
            
            // Poll Preview
            item {
                uiState.pollData?.let { poll ->
                    PollPreviewCard(poll = poll, onDelete = { viewModel.setPoll(null) })
                }
            }

            // Youtube Preview
            item {
                uiState.youtubeUrl?.let { url ->
                     YoutubePreviewCard(url = url, onDelete = { viewModel.setYoutubeUrl(null) })
                }
            }
            
            // Location Preview
            item {
                uiState.location?.let { loc ->
                    LocationPreviewCard(location = loc, onDelete = { viewModel.setLocation(null) })
                }
            }
        }
    }

    if (showPollDialog) {
        CreatePollDialog(
            onDismiss = { showPollDialog = false },
            onCreate = { poll -> 
                viewModel.setPoll(poll)
                showPollDialog = false
            }
        )
    }
    
    // Simple Privacy Dialog
    if (showPrivacySheet) {
        AlertDialog(
            onDismissRequest = { showPrivacySheet = false },
            title = { Text("Who can see this?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Public", "Followers", "Private").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    viewModel.setPrivacy(option.lowercase())
                                    showPrivacySheet = false 
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.privacy == option.lowercase(),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPrivacySheet = false }) { Text("Cancel") } }
        )
    }
    
    if (showLocationDialog) {
        // Simplified Location Input
        var locationText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Add Location") },
            text = {
                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    label = { Text("Location Name") },
                    singleLine = true
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
                     singleLine = true
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
    
    // Add Settings Sheet if needed (omitted in previous pass)
    // Assuming implementation similar to others or reusing existing bottom sheets logic if complex
}

@Composable
fun CreatePostBottomBar(
    onAddMedia: () -> Unit,
    onAddPoll: () -> Unit,
    onAddYoutube: () -> Unit,
    onAddLocation: () -> Unit,
    onSettings: () -> Unit,
    currentPrivacy: String,
    onPrivacyClick: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = onAddMedia) {
                    Icon(Icons.Default.Image, contentDescription = "Media", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onAddPoll) {
                    Icon(Icons.Default.Poll, contentDescription = "Poll", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onAddYoutube) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = "YouTube", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onAddLocation) {
                    Icon(Icons.Default.Place, contentDescription = "Location", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}

@Composable
fun MediaPreviewItem(item: MediaItem, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = item.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (item.type == MediaType.VIDEO) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun PollPreviewCard(poll: PollData, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Duration: ${poll.durationHours} hours",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun YoutubePreviewCard(url: String, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
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
        modifier = Modifier.fillMaxWidth()
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

@Composable
fun CreatePollDialog(onDismiss: () -> Unit, onCreate: (PollData) -> Unit) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Create Poll", style = MaterialTheme.typography.headlineSmall)
                
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                options.forEachIndexed { index, option ->
                    OutlinedTextField(
                        value = option,
                        onValueChange = { options[index] = it },
                        label = { Text("Option ${index + 1}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (options.size < 4) {
                    TextButton(onClick = { options.add("") }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Option")
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val validOptions = options.filter { it.isNotBlank() }
                            if (question.isNotBlank() && validOptions.size >= 2) {
                                onCreate(PollData(question, validOptions, 24))
                            }
                        }
                    ) { Text("Create") }
                }
            }
        }
    }
}
