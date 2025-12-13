package com.synapse.social.studioasinc.ui.chat

import androidx.compose.animation.*
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
    var selectedMessage by remember { mutableStateOf<MessageUiModel?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Top Bar Menu State
    var showTopBarMenu by remember { mutableStateOf(false) }

    // Snackbar Host
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Effect: Load Chat
    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
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
                             // Ideally show a dialog input
                             // viewModel.handleIntent(ChatIntent.EditMessage(msg.id, "Edited Content"))
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showMessageOptions = false }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Delete") },
                        leadingContent = { Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.clickable {
                            viewModel.handleIntent(ChatIntent.DeleteMessage(msg.id, false))
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showMessageOptions = false }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        AsyncImage(
                            model = uiState.otherUser?.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = uiState.otherUser?.username ?: "Chat",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            uiState.otherUser?.let { user ->
                                Text(
                                    text = if (user.isOnline) "Online" else "Offline",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (user.isOnline) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTopBarMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showTopBarMenu,
                        onDismissRequest = { showTopBarMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Block User") },
                            onClick = { 
                                showTopBarMenu = false
                                // TODO: Implement Block
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Chat") },
                            onClick = {
                                showTopBarMenu = false
                                // TODO: Implement Delete Chat
                            },
                             colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    onValueChange = { viewModel.handleIntent(ChatIntent.UpdateInputText(it)) },
                    onSendClick = { viewModel.handleIntent(ChatIntent.SendMessage(uiState.inputText)) },
                    onAttachClick = { /* TODO */ },
                    replyingTo = uiState.replyTo,
                    onCancelReply = { viewModel.handleIntent(ChatIntent.ClearReply) },
                    typingUsers = uiState.typingUsers
                )
            }
        }
    }
}
