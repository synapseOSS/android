package com.synapse.social.studioasinc.ui.inbox.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.synapse.social.studioasinc.ui.inbox.components.ChatSectionHeader
import com.synapse.social.studioasinc.ui.inbox.components.InboxEmptyState
import com.synapse.social.studioasinc.ui.inbox.components.SwipeableChatItem
// import com.synapse.social.studioasinc.ui.inbox.components.ChatListItem
import com.synapse.social.studioasinc.ui.inbox.models.*
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsTabScreen(
    state: InboxUiState,
    searchQuery: String,
    onAction: (InboxAction) -> Unit,
    onLongPressChat: (String) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is InboxUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveLoadingIndicator()
                }
            }
            is InboxUiState.Error -> {
                InboxEmptyState(
                    type = EmptyStateType.ERROR,
                    message = state.message,
                    onActionClick = { onAction(InboxAction.RefreshChats) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is InboxUiState.Success -> {
                // Determine which chats to show based on view mode
                val chatsToDisplay = if (state.isArchivedView) state.archivedChats else state.chats
                
                if (chatsToDisplay.isEmpty() && (!state.isArchivedView && state.pinnedChats.isEmpty())) {
                    InboxEmptyState(
                        type = if (searchQuery.isNotEmpty()) EmptyStateType.SEARCH_NO_RESULTS else EmptyStateType.CHATS,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val groupedChats by remember(chatsToDisplay) {
                        derivedStateOf { groupChatsByDate(chatsToDisplay) }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Filter Chips - Only in Main Inbox
                        if (!state.isArchivedView) {
                            item { 
                                InboxFilterRow(
                                    currentFilter = state.filter,
                                    onFilterSelected = { onAction(InboxAction.SetFilter(it)) }
                                )
                            }
                        }

                        // Archived Chats Entry Point - Only in Main Inbox
                        if (!state.isArchivedView && state.archivedChats.isNotEmpty()) {
                            item {
                                androidx.compose.material3.Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clickable { onAction(InboxAction.ViewArchivedChats) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Default.Email, // Archive Icon
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Archived",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${state.archivedChats.size} chats",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (state.isArchivedView) {
                             // Back button in Archived View
                             item {
                                androidx.compose.material3.TextButton(
                                    onClick = { onAction(InboxAction.ViewInbox) },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                    Text("Back to Inbox")
                                }
                             }
                             
                             // Optional Header for Archived
                             item {
                                Text(
                                    text = "Archived Chats",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                             }
                        }

                        // Pinned Chats Section - Only in Main Inbox
                        if (!state.isArchivedView && state.pinnedChats.isNotEmpty()) {
                            item {
                                ChatSectionHeader(title = "Pinned", modifier = Modifier.padding(bottom = 4.dp))
                            }
                            itemsIndexed(
                                items = state.pinnedChats,
                                key = { _, chat -> chat.id }
                            ) { index, chat ->
                                val shape = getShapeForItem(index, state.pinnedChats.size)
                                val isSelected = state.selectedItems.contains(chat.id)

                                SwipeableChatItem(
                                    isPinned = true,
                                    isMuted = chat.isMuted,
                                    isArchived = chat.isArchived,
                                    onArchive = { 
                                        if (chat.isArchived) onAction(InboxAction.UnarchiveChat(chat.id))
                                        else onAction(InboxAction.ArchiveChat(chat.id)) 
                                    },
                                    onDelete = { onAction(InboxAction.DeleteChat(chat.id)) },
                                    onMute = { onAction(InboxAction.MuteChat(chat.id, MuteDuration.EIGHT_HOURS)) },
                                    onPin = { onAction(InboxAction.UnpinChat(chat.id)) }
                                ) {
                                    // ChatListItem has been removed. Placeholder for now.
                                    // ChatListItem(
                                    //     chat = chat,
                                    //     shape = shape,
                                    //     isSelected = isSelected,
                                    //     selectionMode = state.selectionMode,
                                    //     onClick = {
                                    //         if (state.selectionMode) {
                                    //             onAction(InboxAction.ToggleSelection(chat.id))
                                    //         } else {
                                    //             onAction(InboxAction.OpenChat(chat.id, chat.otherUserId))
                                    //         }
                                    //     },
                                    //     onLongPress = {
                                    //         if (state.selectionMode) {
                                    //              onAction(InboxAction.ToggleSelection(chat.id))
                                    //         } else {
                                    //             onLongPressChat(chat.id)
                                    //         }
                                    //     }
                                    // )
                                    androidx.compose.material3.Text("Chat Item Placeholder")
                                }
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                        
                        // Grouped Chats Sections
                        groupedChats.forEach { (section, chats) ->
                            if (chats.isNotEmpty()) {
                                stickyHeader {
                                    ChatSectionHeader(title = section.displayName, modifier = Modifier.padding(vertical = 4.dp))
                                }
                                
                                itemsIndexed(
                                    items = chats,
                                    key = { _, chat -> chat.id }
                                ) { index, chat ->
                                    val shape = getShapeForItem(index, chats.size)
                                    val isSelected = state.selectedItems.contains(chat.id)

                                    SwipeableChatItem(
                                        isPinned = false,
                                        isMuted = chat.isMuted,
                                        isArchived = chat.isArchived,
                                        onArchive = {
                                            if (chat.isArchived) onAction(InboxAction.UnarchiveChat(chat.id))
                                            else onAction(InboxAction.ArchiveChat(chat.id))
                                        },
                                        onDelete = { onAction(InboxAction.DeleteChat(chat.id)) },
                                        onMute = { onAction(InboxAction.MuteChat(chat.id, MuteDuration.EIGHT_HOURS)) },
                                        onPin = { onAction(InboxAction.PinChat(chat.id)) }
                                    ) {
                                    // ChatListItem has been removed. Placeholder for now.
                                    // ChatListItem(
                                    //     chat = chat,
                                    //     shape = shape,
                                    //     isSelected = isSelected,
                                    //     selectionMode = state.selectionMode,
                                    //     onClick = {
                                    //         if (state.selectionMode) {
                                    //             onAction(InboxAction.ToggleSelection(chat.id))
                                    //         } else {
                                    //             onAction(InboxAction.OpenChat(chat.id, chat.otherUserId))
                                    //         }
                                    //     },
                                    //     onLongPress = {
                                    //         if (state.selectionMode) {
                                    //              onAction(InboxAction.ToggleSelection(chat.id))
                                    //         } else {
                                    //             onLongPressChat(chat.id)
                                    //         }
                                    //     }
                                    // )
                                    androidx.compose.material3.Text("Chat Item Placeholder")
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                            }
                        }

                        // Bottom spacer for navigation bar
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculates the shape based on the item's position in the group.
 * Single: Rounded all (24dp)
 * Top: Rounded top (24dp), Sharp bottom (4dp)
 * Middle: Sharp (4dp)
 * Bottom: Sharp top (4dp), Rounded bottom (24dp)
 */
private fun getShapeForItem(index: Int, count: Int): androidx.compose.ui.graphics.Shape {
    val outerRadius = 24.dp
    val innerRadius = 4.dp

    return when {
        count == 1 -> RoundedCornerShape(outerRadius)
        index == 0 -> RoundedCornerShape(topStart = outerRadius, topEnd = outerRadius, bottomStart = innerRadius, bottomEnd = innerRadius)
        index == count - 1 -> RoundedCornerShape(topStart = innerRadius, topEnd = innerRadius, bottomStart = outerRadius, bottomEnd = outerRadius)
        else -> RoundedCornerShape(innerRadius)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxFilterRow(
    currentFilter: InboxFilter,
    onFilterSelected: (InboxFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 0.dp), // Padding handled by LazyColumn contentPadding? No, nested lazy.
        // LazyColumn has contentPadding(16.dp).
        // If I put this item inside LazyColumn, it will be padded by 16.dp.
        // So I should reduce padding here or use negative padding?
        // Actually, if I want edge-to-edge scrolling for chips, I should handle it carefully.
        // But for simplicity, I'll let it be padded.
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        items(InboxFilter.values()) { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(20.dp) // Pill shaped
            )
        }
    }
}
