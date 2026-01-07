package com.synapse.social.studioasinc.ui.inbox.models

/**
 * UI model for displaying a chat item in the inbox list.
 * This is a presentation-layer model optimized for Compose UI rendering.
 */
data class ChatItemUiModel(
    val id: String,
    val otherUserId: String,
    val displayName: String,
    val avatarUrl: String?,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val hasStory: Boolean = false,
    val isVerified: Boolean = false
) {
    /**
     * Returns formatted time string for display
     */
    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - lastMessageTime
        
        return when {
            diff < 60_000 -> "Now"
            diff < 3600_000 -> "${diff / 60_000}m"
            diff < 86400_000 -> "${diff / 3600_000}h"
            diff < 604800_000 -> "${diff / 86400_000}d"
            else -> {
                val date = java.util.Date(lastMessageTime)
                java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(date)
            }
        }
    }
    
    /**
     * Returns preview text for last message
     */
    fun getMessagePreview(): String {
        return when {
            isTyping -> "typing..."
            lastMessage.isEmpty() -> "No messages yet"
            lastMessage.length > 40 -> "${lastMessage.take(40)}..."
            else -> lastMessage
        }
    }
}

/**
 * Sealed interface representing the different states of the inbox screen.
 */
sealed interface InboxUiState {
    /**
     * Loading state - shown while fetching chats
     */
    data object Loading : InboxUiState
    
    /**
     * Success state with chat data
     */
    data class Success(
        val chats: List<ChatItemUiModel>,
        val pinnedChats: List<ChatItemUiModel> = emptyList(),
        val archivedChats: List<ChatItemUiModel> = emptyList(),
        val isRefreshing: Boolean = false,
        val selectionMode: Boolean = false,
        val selectedItems: Set<String> = emptySet(),
        val isArchivedView: Boolean = false,
        val filter: InboxFilter = InboxFilter.ALL,
        val error: String? = null // For deletion operation errors - Requirements: 2.4
    ) : InboxUiState
    
    /**
     * Error state with message
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : InboxUiState
}

/**
 * Enum for inbox tabs
 */
enum class InboxTab(val title: String, val icon: String) {
    CHATS("Chats", "chat"),
    CALLS("Calls", "call"),
    CONTACTS("Contacts", "contacts")
}

enum class InboxFilter(val displayName: String) {
    ALL("All"),
    UNREAD("Unread"),
    FAVORITES("Favorites"), // Pinned? Or Favorites? Assuming Pinned or a new flag. User said "favourites". Pinned is likely it.
    GROUPS("Groups"),
    CHANNELS("Channels") 
}

/**
 * Empty state types for different tabs
 */
enum class EmptyStateType {
    CHATS,
    CALLS,
    CONTACTS,
    SEARCH_NO_RESULTS,
    ARCHIVED,
    ERROR
}

/**
 * Chat section for grouping by date
 */
enum class ChatSection(val displayName: String) {
    PINNED("Pinned"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_WEEK("Last Week"),
    OLDER("Older")
}

/**
 * Mute duration options
 */
enum class MuteDuration(val displayName: String, val durationMs: Long) {
    EIGHT_HOURS("8 hours", 8 * 60 * 60 * 1000L),
    ONE_WEEK("1 week", 7 * 24 * 60 * 60 * 1000L),
    FOREVER("Forever", Long.MAX_VALUE)
}

/**
 * Deletion status for chat history operations
 * Requirements: 2.4
 */
sealed class DeletionStatus {
    data class InProgress(val completed: Int, val total: Int) : DeletionStatus()
    object Success : DeletionStatus()
    data class Error(val message: String) : DeletionStatus()
}

/**
 * User action events from inbox screen
 */
sealed interface InboxAction {
    data class OpenChat(val chatId: String, val userId: String) : InboxAction
    data class ArchiveChat(val chatId: String) : InboxAction
    data class UnarchiveChat(val chatId: String) : InboxAction
    data object ViewArchivedChats : InboxAction
    data object ViewInbox : InboxAction
    data class DeleteChat(val chatId: String) : InboxAction
    data class MuteChat(val chatId: String, val duration: MuteDuration) : InboxAction
    data class PinChat(val chatId: String) : InboxAction
    data class UnpinChat(val chatId: String) : InboxAction
    data class SearchQueryChanged(val query: String) : InboxAction
    data object RefreshChats : InboxAction
    data object NavigateToNewChat : InboxAction
    data object NavigateToSearch : InboxAction
    data class SetFilter(val filter: InboxFilter) : InboxAction

    // Selection Actions
    data class ToggleSelectionMode(val chatId: String? = null) : InboxAction
    data class ToggleSelection(val chatId: String) : InboxAction
    data object ClearSelection : InboxAction
    data object DeleteSelected : InboxAction
    data object ArchiveSelected : InboxAction
    data object UnarchiveSelected : InboxAction
    
    // Chat History Deletion Actions - Requirements: 2.4
    data class DeleteChatHistory(val chatId: String) : InboxAction
    data object DeleteSelectedChatHistory : InboxAction
    data object DeleteAllChatHistory : InboxAction
}
