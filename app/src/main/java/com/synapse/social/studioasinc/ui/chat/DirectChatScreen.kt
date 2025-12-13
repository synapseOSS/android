package com.synapse.social.studioasinc.ui.chat

// TODO: Implement Typing Indicators - Show when other user is typing (Realtime Presence)
// TODO: Implement File Attachments - Support images, videos, documents with upload progress
// TODO: Implement Block & Report - Add confirmation dialogs and backend RPC calls
// TODO: Implement Delete Chat - Handle soft delete and navigation after success

import androidx.compose.animation.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.ui.chat.components.ChatInputBar
import com.synapse.social.studioasinc.ui.chat.components.MessageItem
import com.synapse.social.studioasinc.ui.chat.components.ForwardMessageSheet
import com.synapse.social.studioasinc.ui.chat.components.topbar.ChatTopBar
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

    // Media Picker
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
             viewModel.sendAttachment(uri, "image")
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
                             // TODO: Implement Edit Dialog (simplification for now: trigger intent with current text)
                             // Ideally show a dialog input with 'msg.content' pre-filled.
                             // Backend Context:
                             // 1. Update 'messages' table: content = newText, is_edited = true, updated_at = now
                             // 2. Realtime: Broadcast 'UPDATE' event to listeners.
                             // viewModel.handleIntent(ChatIntent.EditMessage(msg.id, "Edited Content"))
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
                    // Animate item placement
                    Box(modifier = Modifier) {
                        MessageItem(
                            message = message,
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

            // Floating Input Bar
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
                        // TODO: Trigger Typing Indicator
                        // Backend: Update Realtime Presence 'is_typing: true'
                        if (it.isNotEmpty()) viewModel.setTypingStatus(true)
                    },
                    onSendClick = { 
                        viewModel.handleIntent(ChatIntent.SendMessage(uiState.inputText))
                        // Backend: Presence 'is_typing: false'
                        viewModel.setTypingStatus(false)
                    },
                    onAttachClick = { 
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    replyingTo = uiState.replyTo,
                    onCancelReply = { viewModel.handleIntent(ChatIntent.ClearReply) },
                    typingUsers = uiState.typingUsers
                )
            }
        }
    }
}
