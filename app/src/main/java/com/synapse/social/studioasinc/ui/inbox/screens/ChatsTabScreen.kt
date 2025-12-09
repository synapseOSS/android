package com.synapse.social.studioasinc.ui.inbox.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.inbox.components.ChatListShimmer
import com.synapse.social.studioasinc.ui.inbox.components.ChatSectionHeader
import com.synapse.social.studioasinc.ui.inbox.components.InboxEmptyState
import com.synapse.social.studioasinc.ui.inbox.components.SwipeableChatItem
import com.synapse.social.studioasinc.ui.inbox.components.ChatListItem
import com.synapse.social.studioasinc.ui.inbox.models.*
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsTabScreen(
    state: InboxUiState,
    searchQuery: String,
    onAction: (InboxAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is InboxUiState.Loading -> {
                ChatListShimmer()
            }
            is InboxUiState.Error -> {
                InboxEmptyState(
                    type = EmptyStateType.ERROR,
                    onActionClick = { onAction(InboxAction.RefreshChats) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is InboxUiState.Success -> {
                if (state.chats.isEmpty() && state.pinnedChats.isEmpty()) {
                    InboxEmptyState(
                        type = if (searchQuery.isNotEmpty()) EmptyStateType.SEARCH_NO_RESULTS else EmptyStateType.CHATS,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val groupedChats by remember(state.chats) {
                        derivedStateOf { groupChatsByDate(state.chats) }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                    ) {
                        // Pinned Chats Section
                        if (state.pinnedChats.isNotEmpty()) {
                            item {
                                ChatSectionHeader(title = "Pinned")
                            }
                            items(
                                items = state.pinnedChats,
                                key = { it.id }
                            ) { chat ->
                                SwipeableChatItem(
                                    isPinned = true, // It is in pinned section
                                    isMuted = chat.isMuted,
                                    onArchive = { onAction(InboxAction.ArchiveChat(chat.id)) },
                                    onDelete = { onAction(InboxAction.DeleteChat(chat.id)) },
                                    onMute = { onAction(InboxAction.MuteChat(chat.id, MuteDuration.EIGHT_HOURS)) },
                                    onPin = { onAction(InboxAction.UnpinChat(chat.id)) }
                                ) {
                                    ChatListItem(
                                        chat = chat,
                                        onClick = { onAction(InboxAction.OpenChat(chat.id, chat.otherUserId)) }
                                    )
                                }
                            }
                        }
                        
                        // Grouped Chats Sections
                        groupedChats.forEach { (section, chats) ->
                            if (chats.isNotEmpty()) {
                                stickyHeader {
                                    ChatSectionHeader(title = section.displayName)
                                }
                                
                                items(
                                    items = chats,
                                    key = { it.id }
                                ) { chat ->
                                    SwipeableChatItem(
                                        isPinned = false,
                                        isMuted = chat.isMuted,
                                        onArchive = { onAction(InboxAction.ArchiveChat(chat.id)) },
                                        onDelete = { onAction(InboxAction.DeleteChat(chat.id)) },
                                        onMute = { onAction(InboxAction.MuteChat(chat.id, MuteDuration.EIGHT_HOURS)) },
                                        onPin = { onAction(InboxAction.PinChat(chat.id)) }
                                    ) {
                                        ChatListItem(
                                            chat = chat,
                                            onClick = { onAction(InboxAction.OpenChat(chat.id, chat.otherUserId)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to group chats by date relations.
 */
private fun groupChatsByDate(chats: List<ChatItemUiModel>): Map<ChatSection, List<ChatItemUiModel>> {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val lastWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    
    val grouped = mutableMapOf<ChatSection, MutableList<ChatItemUiModel>>()
    
    // Initialize map to preserve order
    grouped[ChatSection.TODAY] = mutableListOf()
    grouped[ChatSection.YESTERDAY] = mutableListOf()
    grouped[ChatSection.LAST_WEEK] = mutableListOf()
    grouped[ChatSection.OLDER] = mutableListOf()
    
    chats.forEach { chat ->
        val chatDate = Calendar.getInstance().apply { timeInMillis = chat.lastMessageTime }
        
        when {
            isSameDay(chatDate, today) -> grouped[ChatSection.TODAY]?.add(chat)
            isSameDay(chatDate, yesterday) -> grouped[ChatSection.YESTERDAY]?.add(chat)
            chatDate.after(lastWeek) -> grouped[ChatSection.LAST_WEEK]?.add(chat)
            else -> grouped[ChatSection.OLDER]?.add(chat)
        }
    }
    
    // Remove empty sections
    return grouped.filterValues { it.isNotEmpty() }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
