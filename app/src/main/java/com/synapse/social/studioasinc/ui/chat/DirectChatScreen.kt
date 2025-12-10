package com.synapse.social.studioasinc.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.chat.components.input.ChatInputBar
import com.synapse.social.studioasinc.ui.chat.components.message.MessageList
import com.synapse.social.studioasinc.ui.chat.components.EmptyChatState
import com.synapse.social.studioasinc.ui.chat.components.ScrollToBottomFab
import com.synapse.social.studioasinc.ui.chat.components.topbar.ChatTopBar
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors
import com.synapse.social.studioasinc.ui.chat.theme.ChatTheme

/**
 * Main Direct Chat Screen Composable
 * Assembles all chat components into a functional screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    uiState: ChatUiState,
    messages: List<MessageUiModel>,
    currentUserId: String,
    onIntent: (ChatIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToMediaViewer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // UI State for Dialogs/Sheets
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var showMessageActions by remember { mutableStateOf(false) }
    var selectedMessageForActions by remember { mutableStateOf<MessageUiModel?>(null) }
    
    // Show scroll to bottom FAB when scrolled up
    var showScrollToBottom by remember { mutableStateOf(false) }
    
    LaunchedEffect(listState.firstVisibleItemIndex) {
        showScrollToBottom = listState.firstVisibleItemIndex > 5
    }
    
    ChatTheme {
        Scaffold(
            topBar = {
                ChatTopBar(
                    userInfo = uiState.otherUser,
                    connectionState = uiState.connectionState,
                    onBackClick = onNavigateBack,
                    onProfileClick = { uiState.otherUser?.id?.let(onNavigateToProfile) },
                    onCallClick = { /* TODO */ },
                    onVideoCallClick = { /* TODO */ },
                    onMenuClick = { /* TODO */ }
                )
            },
            bottomBar = {
                Column {
                    // Attachment Previews
                    if (uiState.attachments.isNotEmpty()) {
                        com.synapse.social.studioasinc.ui.chat.components.input.AttachmentPreviewRow(
                            attachments = uiState.attachments,
                            onRemove = { id -> onIntent(ChatIntent.RemoveAttachment(id)) }
                        )
                    }
                    
                    ChatInputBar(
                        text = uiState.inputText,
                        onTextChange = { onIntent(ChatIntent.UpdateInputText(it)) },
                        onSendClick = { 
                            if (uiState.attachments.isNotEmpty()) {
                                onIntent(ChatIntent.SendMediaMessage(uiState.attachments, uiState.inputText))
                            } else {
                                onIntent(ChatIntent.SendMessage(uiState.inputText))
                            }
                        },
                        onAttachmentClick = { showAttachmentPicker = true },
                        onVoiceClick = { onIntent(ChatIntent.StartVoiceRecording) },
                        replyTo = uiState.replyTo,
                        onClearReply = { onIntent(ChatIntent.ClearReply) }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = ChatColors.ChatBackgroundLight // Or Dark based on theme
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(ChatTheme.colors.chatBackground)
            ) {
                if (messages.isEmpty() && !uiState.isLoading) {
                    EmptyChatState()
                } else {
                    MessageList(
                        messages = messages,
                        currentUserId = currentUserId,
                        listState = listState,
                        typingUsers = uiState.typingUsers,
                        onMessageClick = { /* Handle selection/actions */ },
                        onMessageLongClick = { message -> 
                            selectedMessageForActions = message
                            showMessageActions = true
                        },
                        onReplyClick = { onIntent(ChatIntent.SetReplyTo(it)) },
                        onMediaClick = { id, index -> onNavigateToMediaViewer(id) }
                    )
                }
                
                // Scroll to Bottom FAB
                Box(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    ScrollToBottomFab(
                        visible = showScrollToBottom,
                        onClick = { 
                            onIntent(ChatIntent.ScrollToBottom)
                            // Also scroll list immediately for responsiveness
                            // scope.launch { listState.animateScrollToItem(0) } 
                        }
                    )
                }
            }
        }
        
        // Bottom Sheets
        if (showAttachmentPicker) {
            com.synapse.social.studioasinc.ui.chat.components.dialogs.AttachmentPickerBottomSheet(
                onDismiss = { showAttachmentPicker = false },
                onPickImage = { 
                    showAttachmentPicker = false
                    // TODO: Launch Media Picker (Phase 7 implementation)
                    // For now handled via Intent/Effect or local launcher
                },
                onPickVideo = { showAttachmentPicker = false },
                onTakePhoto = { showAttachmentPicker = false },
                onPickDocument = { showAttachmentPicker = false },
                onPickAudio = { showAttachmentPicker = false },
                onPickLocation = { showAttachmentPicker = false }
            )
        }
        
        if (showMessageActions && selectedMessageForActions != null) {
            com.synapse.social.studioasinc.ui.chat.components.dialogs.MessageActionsBottomSheet(
                message = selectedMessageForActions!!,
                onDismiss = { showMessageActions = false },
                onReply = {
                    onIntent(ChatIntent.SetReplyTo(selectedMessageForActions!!))
                    showMessageActions = false
                },
                onCopy = {
                    onIntent(ChatIntent.CopyToClipboard(selectedMessageForActions!!.content)) // Needs Intent logic
                    showMessageActions = false
                },
                onEdit = {
                    // Start edit mode (update input text with message content)
                    onIntent(ChatIntent.UpdateInputText(selectedMessageForActions!!.content))
                    // Set editing flag or similar (omitted for MVP)
                    showMessageActions = false
                },
                onDelete = {
                    onIntent(ChatIntent.DeleteMessage(selectedMessageForActions!!.id, true))
                    showMessageActions = false
                },
                onForward = {
                    onIntent(ChatIntent.ForwardMessage(selectedMessageForActions!!.id, emptyList()))
                    showMessageActions = false
                },
                onInfo = {
                    showMessageActions = false
                }
            )
        }
    }
}
