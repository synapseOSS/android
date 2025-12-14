package com.synapse.social.studioasinc.ui.chat

// TODO: Implement Typing Indicators - Show when other user is typing (Realtime Presence)
// TODO: Implement File Attachments - Support images, videos, documents with upload progress
// TODO: Implement Block & Report - Add confirmation dialogs and backend RPC calls
// TODO: Implement Delete Chat - Handle soft delete and navigation after success

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.util.FileUtils
import com.synapse.social.studioasinc.ui.chat.components.ChatInputBar
import com.synapse.social.studioasinc.ui.chat.components.MessageItem
import com.synapse.social.studioasinc.ui.chat.components.ForwardMessageSheet
import com.synapse.social.studioasinc.ui.chat.components.topbar.ChatTopBar
import com.synapse.social.studioasinc.ui.chat.components.topbar.SelectionModeTopBar
import com.synapse.social.studioasinc.ui.chat.components.input.MediaPickerBottomSheet
import com.synapse.social.studioasinc.ui.chat.RealtimeConnectionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DirectChatScreen(
    chatId: String,
    otherUserId: String,
    onBackClick: () -> Unit,
    viewModel: DirectChatViewModel = viewModel()
) {
    // State
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val availableChats by viewModel.availableChats.collectAsState()
    
    // Derived State
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }
    
    // Dynamic Layout Measurements
    val localDensity = LocalDensity.current
    var inputBarHeightDp by remember { mutableStateOf(100.dp) }

    // Bottom Sheet State
    var showMessageOptions by remember { mutableStateOf(false) }
    var showForwardSheet by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<MessageUiModel?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Top Bar Menu State
    var showTopBarMenu by remember { mutableStateOf(false) }
    
    // Confirmation Dialog States
    var showBlockDialog by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }

    // Snackbar Host
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Media Pickers
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.handleIntent(ChatIntent.AddPendingAttachment(uri, AttachmentType.Image))
            viewModel.handleIntent(ChatIntent.HideMediaPicker)
        }
    }
    
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.handleIntent(ChatIntent.AddPendingAttachment(uri, AttachmentType.Video))
            viewModel.handleIntent(ChatIntent.HideMediaPicker)
        }
    }
    
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.handleIntent(ChatIntent.AddPendingAttachment(uri, AttachmentType.Audio))
            viewModel.handleIntent(ChatIntent.HideMediaPicker)
        }
    }

    // Camera Logic
    var tempCameraUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            viewModel.handleIntent(ChatIntent.AddPendingAttachment(tempCameraUri!!, AttachmentType.Image))
            viewModel.handleIntent(ChatIntent.HideMediaPicker)
        }
    }

    // Effect: Load Chat
    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
    }

    // Effect: Auto-scroll to bottom when new user message is added
    var previousMessageCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(messages.size) {
        if (messages.size > previousMessageCount) {
            // Scroll if we have new messages and the latest one is from current user
            if (messages.isNotEmpty() && messages.last().isFromCurrentUser) {
                // Delay to let bubble entrance animation start (per spec)
                kotlinx.coroutines.delay(50)
                listState.animateScrollToItem(0)
            }
        }
        previousMessageCount = messages.size
    }

    // Effect: Handle One-time Effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ChatEffect.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
                is ChatEffect.CopyToClipboard -> {
                   val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                   val clip = android.content.ClipData.newPlainText("Message", effect.text)
                   clipboard.setPrimaryClip(clip)
                }
                is ChatEffect.NavigateBack -> onBackClick()
                else -> {}
            }
        }
    }

    if (showMessageOptions && selectedMessage != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showMessageOptions = false
                selectedMessage = null
            },
            sheetState = sheetState
        ) {
            val msg = selectedMessage!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                 ListItem(
                    headlineContent = { Text("Reply") },
                    leadingContent = { Icon(Icons.Default.Reply, contentDescription = null) },
                    modifier = Modifier.clickable {
                        viewModel.handleIntent(ChatIntent.SetReplyTo(msg))
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showMessageOptions = false }
                    }
                )
                ListItem(
                    headlineContent = { Text("Forward") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null) },
                    modifier = Modifier.clickable {
                        viewModel.loadUserChats()
                        showForwardSheet = true
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showMessageOptions = false }
                    }
                )
                ListItem(
                    headlineContent = { Text("Copy") },
                    leadingContent = { Icon(androidx.compose.material.icons.Icons.Default.ContentCopy, contentDescription = null) },
                    modifier = Modifier.clickable {
                        viewModel.handleIntent(ChatIntent.CopyToClipboard(msg.content))
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showMessageOptions = false }
                    }
                )
                if (msg.isFromCurrentUser) {
                    ListItem(
                        headlineContent = { Text("Edit") },
                        leadingContent = { Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier.clickable {
                            editMessageText = msg.content
                            showEditDialog = true
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showMessageOptions = false }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Delete") },
                        leadingContent = { Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.clickable {
                            // Backend Context:
                            // 1. Soft Delete: Update 'messages' set is_deleted = true.
                            // 2. Realtime: Broadcast 'UPDATE' event.
                            viewModel.handleIntent(ChatIntent.DeleteMessage(msg.id, false))
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showMessageOptions = false }
                        }
                    )
                }
                ListItem(
                    headlineContent = { Text("Select") },
                    leadingContent = { Icon(androidx.compose.material.icons.Icons.Default.CheckCircle, contentDescription = null) },
                    modifier = Modifier.clickable {
                        viewModel.handleIntent(ChatIntent.ToggleMessageSelection(msg.id))
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showMessageOptions = false }
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showForwardSheet && selectedMessage != null) {
        ForwardMessageSheet(
            chats = availableChats,
            onDismiss = { showForwardSheet = false },
            onForward = { chatIds ->
                viewModel.handleIntent(ChatIntent.ForwardMessage(selectedMessage!!.id, chatIds))
                showForwardSheet = false
                selectedMessage = null
            }
        )
    }

    // Block User Confirmation Dialog
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Block User") },
            text = { 
                Text("Are you sure you want to block ${uiState.otherUser?.displayName ?: uiState.otherUser?.username ?: "this user"}? They won't be able to message you anymore.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockDialog = false
                        viewModel.blockUser(uiState.otherUser?.id ?: "")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Chat Confirmation Dialog
    if (showDeleteChatDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChatDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Chat") },
            text = { 
                Text("Are you sure you want to delete this conversation? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteChatDialog = false
                        viewModel.deleteChat()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Derived: Check if all selected messages are text (for copy action)
    val canCopySelected = remember(uiState.selectedMessageIds, messages) {
        val selectedMsgs = messages.filter { uiState.selectedMessageIds.contains(it.id) }
        selectedMsgs.isNotEmpty() && selectedMsgs.all { it.messageType == MessageType.Text }
    }

    // Multi-select delete confirmation dialog
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    // Edit Message Dialog State
    var showEditDialog by remember { mutableStateOf(false) }
    var editMessageText by remember { mutableStateOf("") }
    
    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete ${uiState.selectedMessageIds.size} message(s)?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteSelectedDialog = false
                        viewModel.handleIntent(ChatIntent.DeleteSelectedMessages)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Message Dialog
    if (showEditDialog && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                selectedMessage = null
            },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editMessageText,
                    onValueChange = { editMessageText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message") },
                    singleLine = false,
                    maxLines = 5
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editMessageText.isNotBlank() && editMessageText != selectedMessage?.content) {
                            viewModel.handleIntent(ChatIntent.EditMessage(selectedMessage!!.id, editMessageText))
                        }
                        showEditDialog = false
                        selectedMessage = null
                    },
                    enabled = editMessageText.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    selectedMessage = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Multi-select forward sheet
    var showForwardSelectedSheet by remember { mutableStateOf(false) }
    
    if (showForwardSelectedSheet && uiState.selectedMessageIds.isNotEmpty()) {
        ForwardMessageSheet(
            chats = availableChats,
            onDismiss = { showForwardSelectedSheet = false },
            onForward = { chatIds ->
                // Forward each selected message to selected chats
                uiState.selectedMessageIds.forEach { messageId ->
                    viewModel.handleIntent(ChatIntent.ForwardMessage(messageId, chatIds))
                }
                showForwardSelectedSheet = false
                viewModel.handleIntent(ChatIntent.ExitMultiSelectMode)
            }
        )
    }

    // Media Picker Bottom Sheet
    if (uiState.showMediaPicker) {
        MediaPickerBottomSheet(
            onDismiss = { viewModel.handleIntent(ChatIntent.HideMediaPicker) },
            onSelectCamera = {
                val uri = FileUtils.getTmpFileUri(context)
                tempCameraUri = uri
                takePicture.launch(uri)
            },
            onSelectGallery = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSelectVideo = {
                pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            onSelectAudio = {
                pickAudio.launch("audio/*")
            },
            onVoiceRecord = {
                // TODO: Implement voice recording UI
                viewModel.handleIntent(ChatIntent.HideMediaPicker)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isMultiSelectMode) {
                SelectionModeTopBar(
                    selectedCount = uiState.selectedMessageIds.size,
                    canCopy = canCopySelected,
                    onBackClick = { viewModel.handleIntent(ChatIntent.ExitMultiSelectMode) },
                    onDeleteClick = { showDeleteSelectedDialog = true },
                    onCopyClick = { viewModel.handleIntent(ChatIntent.CopySelectedMessages) },
                    onForwardClick = { 
                        viewModel.loadUserChats()
                        showForwardSelectedSheet = true 
                    }
                )
            } else {
                ChatTopBar(
                    userInfo = uiState.otherUser,
                    connectionState = RealtimeConnectionState.Connected, // TODO: observe real connection state
                    onBackClick = onBackClick,
                    onProfileClick = { /* TODO: Open Profile */ },
                    onCallClick = { /* TODO: Call */ },
                    onVideoCallClick = { /* TODO: Video Call */ },
                    onMenuClick = { showTopBarMenu = true },
                    isMenuExpanded = showTopBarMenu,
                    onDismissMenu = { showTopBarMenu = false },
                    menuContent = {
                        DropdownMenuItem(
                            text = { Text("Block User") },
                            onClick = { 
                                showTopBarMenu = false
                                showBlockDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Report User") },
                            onClick = {
                                showTopBarMenu = false
                                 // TODO: Report User
                                // Backend: Insert into 'reports' table.
                                viewModel.reportUser(uiState.otherUser?.id ?: "")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Chat") },
                            onClick = {
                                showTopBarMenu = false
                                showDeleteChatDialog = true
                            },
                             colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .imePadding() // Push content up when keyboard opens
        ) {
            // Background Image/Gradient could go here

            // Messages List
            LazyColumn(
                state = listState,
                reverseLayout = true, // Scroll from bottom
                contentPadding = PaddingValues(
                    bottom = inputBarHeightDp + 16.dp, // Dynamic padding + spacing
                    top = 16.dp,
                    start = 0.dp,
                    end = 0.dp
                ),
                // clipToPadding parameter removed - not available in current Compose version
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = messages.reversed(), // Reverse list because LazyColumn is reversed
                    key = { it.id }
                ) { message ->
                    // Animate item placement with smooth entrance for new messages
                    val isNewMessage = message.isAnimating
                    
                    // Entrance animation for new messages
                    var visible by remember { mutableStateOf(!isNewMessage) }
                    LaunchedEffect(message.id) {
                        if (isNewMessage) {
                            kotlinx.coroutines.delay(50) // Small delay for stagger effect
                            visible = true
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = visible || !isNewMessage,
                        enter = fadeIn(
                            animationSpec = tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        ) + scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        ),
                        modifier = Modifier.animateItem(
                            placementSpec = spring<IntOffset>(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                            )
                        )
                    ) {
                        MessageItem(
                            message = message.copy(isSelected = uiState.selectedMessageIds.contains(message.id)),
                            isSelectionMode = uiState.isMultiSelectMode,
                            onSelect = { msg -> viewModel.handleIntent(ChatIntent.ToggleMessageSelection(msg.id)) },
                            onReply = { msg -> viewModel.handleIntent(ChatIntent.SetReplyTo(msg)) },
                            onLongClick = { msg -> 
                                selectedMessage = msg
                                showMessageOptions = true
                            },
                            onAttachmentClick = { url, type -> /* Open Viewer */ }
                        )
                    }
                }
                
                // Loading Footer (at top because reversed)
                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // Scroll to Bottom FAB
            AnimatedVisibility(
                visible = showScrollToBottom,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = inputBarHeightDp + 16.dp, end = 16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll Down")
                }
            }

            // Floating Input Bar (hidden during selection mode)
            if (!uiState.isMultiSelectMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coordinates ->
                            inputBarHeightDp = with(localDensity) { coordinates.size.height.toDp() }
                        }
                ) {
                    ChatInputBar(
                        value = uiState.inputText,
                        onValueChange = { 
                            viewModel.handleIntent(ChatIntent.UpdateInputText(it)) 
                            // Backend: Update Realtime Presence 'is_typing: true'
                            if (it.text.isNotEmpty()) viewModel.setTypingStatus(true)
                        },
                        onSendClick = { 
                            viewModel.handleIntent(ChatIntent.SendMessage(uiState.inputText.text))
                            // Backend: Presence 'is_typing: false'
                            viewModel.setTypingStatus(false)
                        },
                        suggestions = uiState.mentionSuggestions,
                        onInsertMention = { user ->
                            viewModel.handleIntent(ChatIntent.InsertMention(user))
                        },
                        onAttachClick = { 
                            viewModel.handleIntent(ChatIntent.ShowMediaPicker)
                        },
                        replyingTo = uiState.replyTo,
                        onCancelReply = { viewModel.handleIntent(ChatIntent.ClearReply) },
                        typingUsers = uiState.typingUsers,
                        isSending = uiState.isSendingAnimation  // Animation trigger
                    )
                }
            }
        }
    }
}
