package com.synapse.social.studioasinc.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
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

    // Effect: Load Chat
    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
    }

    Scaffold(
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
                    IconButton(onClick = { /* TODO: Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
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
                            onLongClick = { /* Selection Mode */ },
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
