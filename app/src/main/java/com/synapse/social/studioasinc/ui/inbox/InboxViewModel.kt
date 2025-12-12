package com.synapse.social.studioasinc.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.ui.inbox.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Inbox screen.
 * Manages chat list state, search, and actions.
 */
class InboxViewModel(
    private val chatService: SupabaseChatService = SupabaseChatService(),
    private val authService: SupabaseAuthenticationService = SupabaseAuthenticationService(),
    private val databaseService: SupabaseDatabaseService = SupabaseDatabaseService()
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()
    
    // Selected tab index
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
    
    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()
    
    // Filtered chats based on search
    val filteredChats: StateFlow<List<ChatItemUiModel>> = combine(
        _uiState,
        _searchQuery
    ) { state, query ->
        when (state) {
            is InboxUiState.Success -> {
                if (query.isBlank()) {
                    state.chats
                } else {
                    state.chats.filter { chat ->
                        chat.displayName.contains(query, ignoreCase = true) ||
                            chat.lastMessage.contains(query, ignoreCase = true)
                    }
                }
            }
            else -> emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Current user ID
    private val currentUserId: String?
        get() = authService.getCurrentUser()?.id
    
    init {
        loadChats()
    }
    
    /**
     * Loads all chats for the current user
     */
    fun loadChats() {
        val userId = currentUserId
        if (userId == null) {
            _uiState.value = InboxUiState.Error(
                message = "Please log in to view your messages",
                canRetry = false
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = InboxUiState.Loading
            
            try {
                val result = chatService.getUserChats(userId)
                result.fold(
                    onSuccess = { chats ->
                        val chatItems = chats.mapNotNull { chatMap ->
                            mapToChatItemUiModel(chatMap, userId)
                        }
                        
                        // Load user data for each chat
                        val enrichedChats = chatItems.map { chat ->
                            enrichChatWithUserData(chat)
                        }
                        
                        val pinnedChats = enrichedChats.filter { it.isPinned }
                        val regularChats = enrichedChats.filter { !it.isPinned }
                        
                        _uiState.value = InboxUiState.Success(
                            chats = regularChats.sortedByDescending { it.lastMessageTime },
                            pinnedChats = pinnedChats.sortedByDescending { it.lastMessageTime }
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = InboxUiState.Error(
                            message = error.message ?: "Failed to load chats",
                            canRetry = true
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = InboxUiState.Error(
                    message = e.message ?: "An unexpected error occurred",
                    canRetry = true
                )
            }
        }
    }
    
    /**
     * Refreshes the chat list
     */
    fun refreshChats() {
        val currentState = _uiState.value
        if (currentState is InboxUiState.Success) {
            _uiState.value = currentState.copy(isRefreshing = true)
        }
        loadChats()
    }
    
    /**
     * Handles user actions
     */
    fun onAction(action: InboxAction) {
        when (action) {
            is InboxAction.SearchQueryChanged -> {
                _searchQuery.update { action.query }
            }
            is InboxAction.RefreshChats -> {
                refreshChats()
            }
            is InboxAction.ArchiveChat -> {
                archiveChat(action.chatId)
            }
            is InboxAction.DeleteChat -> {
                deleteChat(action.chatId)
            }
            is InboxAction.MuteChat -> {
                muteChat(action.chatId, action.duration)
            }
            is InboxAction.PinChat -> {
                pinChat(action.chatId)
            }
            is InboxAction.UnpinChat -> {
                unpinChat(action.chatId)
            }
            is InboxAction.ToggleSelectionMode -> {
                toggleSelectionMode(action.chatId)
            }
            is InboxAction.ToggleSelection -> {
                toggleSelection(action.chatId)
            }
            is InboxAction.ClearSelection -> {
                clearSelection()
            }
            is InboxAction.DeleteSelected -> {
                deleteSelectedChats()
            }
            is InboxAction.ArchiveSelected -> {
                archiveSelectedChats()
            }
            else -> {
                // Navigation actions handled by the UI
            }
        }
    }
    
    /**
     * Sets the selected tab
     */
    fun selectTab(index: Int) {
        _selectedTab.update { index }
    }
    
    /**
     * Toggles search mode
     */
    fun toggleSearch(active: Boolean) {
        _isSearchActive.update { active }
        if (!active) {
            _searchQuery.update { "" }
        }
    }
    
    /**
     * Toggles selection mode
     */
    private fun toggleSelectionMode(initialChatId: String? = null) {
        _uiState.update { currentState ->
            if (currentState is InboxUiState.Success) {
                val newSelectionMode = !currentState.selectionMode
                val newSelectedItems = if (newSelectionMode && initialChatId != null) {
                    setOf(initialChatId)
                } else {
                    emptySet()
                }

                currentState.copy(
                    selectionMode = newSelectionMode,
                    selectedItems = newSelectedItems
                )
            } else {
                currentState
            }
        }
    }

    /**
     * Toggles selection of a specific chat
     */
    private fun toggleSelection(chatId: String) {
        _uiState.update { currentState ->
            if (currentState is InboxUiState.Success && currentState.selectionMode) {
                val currentSelected = currentState.selectedItems
                val newSelected = if (currentSelected.contains(chatId)) {
                    currentSelected - chatId
                } else {
                    currentSelected + chatId
                }

                // If nothing left selected, exit selection mode
                if (newSelected.isEmpty()) {
                    currentState.copy(
                        selectionMode = false,
                        selectedItems = emptySet()
                    )
                } else {
                    currentState.copy(selectedItems = newSelected)
                }
            } else {
                currentState
            }
        }
    }

    /**
     * Clears all selection
     */
    private fun clearSelection() {
        _uiState.update { currentState ->
            if (currentState is InboxUiState.Success) {
                currentState.copy(
                    selectionMode = false,
                    selectedItems = emptySet()
                )
            } else {
                currentState
            }
        }
    }

    /**
     * Archives a chat
     */
    private fun archiveChat(chatId: String) {
        viewModelScope.launch {
            // TODO: Implement archive in backend
            _uiState.update { currentState ->
                if (currentState is InboxUiState.Success) {
                    val updatedChats = currentState.chats.filter { it.id != chatId }
                    val updatedPinned = currentState.pinnedChats.filter { it.id != chatId }

                    // Find chat to archive (could be in regular or pinned)
                    val archivedChat = currentState.chats.find { it.id == chatId }
                        ?: currentState.pinnedChats.find { it.id == chatId }

                    currentState.copy(
                        chats = updatedChats,
                        pinnedChats = updatedPinned,
                        archivedChats = currentState.archivedChats + listOfNotNull(archivedChat)
                    )
                } else {
                    currentState
                }
            }
        }
    }

    private fun archiveSelectedChats() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                if (currentState is InboxUiState.Success) {
                    val selected = currentState.selectedItems
                    val updatedChats = currentState.chats.filter { !selected.contains(it.id) }
                    val updatedPinned = currentState.pinnedChats.filter { !selected.contains(it.id) }

                    val archivedChats = currentState.chats.filter { selected.contains(it.id) } +
                                        currentState.pinnedChats.filter { selected.contains(it.id) }

                    currentState.copy(
                        chats = updatedChats,
                        pinnedChats = updatedPinned,
                        archivedChats = currentState.archivedChats + archivedChats,
                        selectionMode = false,
                        selectedItems = emptySet()
                    )
                } else {
                    currentState
                }
            }
        }
    }
    
    /**
     * Deletes a chat
     */
    private fun deleteChat(chatId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            // Optimistic update
            _uiState.update { currentState ->
                if (currentState is InboxUiState.Success) {
                    val updatedChats = currentState.chats.filter { it.id != chatId }
                    val updatedPinned = currentState.pinnedChats.filter { it.id != chatId }
                    currentState.copy(
                        chats = updatedChats,
                        pinnedChats = updatedPinned
                    )
                } else {
                    currentState
                }
            }

            // Implement delete in backend
            val result = chatService.deleteChat(chatId, userId)

            // Reload chats if failed to ensure consistency
            if (result.isFailure) {
                loadChats()
            }
        }
    }

    private fun deleteSelectedChats() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is InboxUiState.Success) return@launch

            val selected = currentState.selectedItems
            if (selected.isEmpty()) return@launch

            // Optimistic update
            _uiState.update { state ->
                if (state is InboxUiState.Success) {
                    val updatedChats = state.chats.filter { !selected.contains(it.id) }
                    val updatedPinned = state.pinnedChats.filter { !selected.contains(it.id) }

                    state.copy(
                        chats = updatedChats,
                        pinnedChats = updatedPinned,
                        selectionMode = false,
                        selectedItems = emptySet()
                    )
                } else {
                    state
                }
            }

            // Backend calls (parallel execution)
            val results = kotlinx.coroutines.coroutineScope {
                kotlinx.coroutines.awaitAll(
                    *selected.map { chatId ->
                        kotlinx.coroutines.async { chatService.deleteChat(chatId, userId) }
                    }.toTypedArray()
                )
            }

            // If any failed, reload chats to ensure consistency
            if (results.any { it.isFailure }) {
                loadChats()
            }
        }
    }
    
    /**
     * Mutes a chat for specified duration
     */
    private fun muteChat(chatId: String, duration: MuteDuration) {
        viewModelScope.launch {
            // TODO: Implement mute in backend
            _uiState.update { currentState ->
                if (currentState is InboxUiState.Success) {
                    val updatedChats = currentState.chats.map { chat ->
                        if (chat.id == chatId) chat.copy(isMuted = true) else chat
                    }
                    val updatedPinned = currentState.pinnedChats.map { chat ->
                        if (chat.id == chatId) chat.copy(isMuted = true) else chat
                    }
                    currentState.copy(
                        chats = updatedChats,
                        pinnedChats = updatedPinned
                    )
                } else {
                    currentState
                }
            }
        }
    }
    
    /**
     * Pins a chat
     */
    private fun pinChat(chatId: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                if (currentState is InboxUiState.Success) {
                    val chatToPin = currentState.chats.find { it.id == chatId }
                    if (chatToPin != null) {
                        val updatedChats = currentState.chats.filter { it.id != chatId }
                        val updatedPinned = currentState.pinnedChats + chatToPin.copy(isPinned = true)
                        currentState.copy(
                            chats = updatedChats,
                            pinnedChats = updatedPinned
                        )
                    } else {
                        currentState
                    }
                } else {
                    currentState
                }
            }
        }
    }
    
    /**
     * Unpins a chat
     */
    private fun unpinChat(chatId: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                if (currentState is InboxUiState.Success) {
                    val chatToUnpin = currentState.pinnedChats.find { it.id == chatId }
                    if (chatToUnpin != null) {
                        val updatedPinned = currentState.pinnedChats.filter { it.id != chatId }
                        val updatedChats = currentState.chats + chatToUnpin.copy(isPinned = false)
                        currentState.copy(
                            chats = updatedChats.sortedByDescending { it.lastMessageTime },
                            pinnedChats = updatedPinned
                        )
                    } else {
                        currentState
                    }
                } else {
                    currentState
                }
            }
        }
    }
    
    /**
     * Maps raw chat data to UI model
     */
    private fun mapToChatItemUiModel(
        chatMap: Map<String, Any?>,
        currentUserId: String
    ): ChatItemUiModel? {
        val chatId = chatMap["chat_id"]?.toString() ?: return null
        
        // Extract other user ID from chat_id (format: dm_userId1_userId2)
        val parts = chatId.split("_")
        val otherUserId = if (parts.size == 3 && parts[0] == "dm") {
            if (parts[1] == currentUserId) parts[2] else parts[1]
        } else {
            return null
        }
        
        return ChatItemUiModel(
            id = chatId,
            otherUserId = otherUserId,
            displayName = chatMap["other_user_name"]?.toString() ?: "User",
            avatarUrl = chatMap["other_user_avatar"]?.toString(),
            lastMessage = chatMap["last_message"]?.toString() ?: "",
            lastMessageTime = chatMap["last_message_time"]?.toString()?.toLongOrNull() ?: 0L,
            unreadCount = chatMap["unread_count"]?.toString()?.toIntOrNull() ?: 0,
            isOnline = chatMap["is_online"]?.toString()?.toBooleanStrictOrNull() ?: false,
            isMuted = chatMap["is_muted"]?.toString()?.toBooleanStrictOrNull() ?: false,
            isPinned = chatMap["is_pinned"]?.toString()?.toBooleanStrictOrNull() ?: false
        )
    }
    
    /**
     * Enriches chat with user data from database
     */
    private suspend fun enrichChatWithUserData(chat: ChatItemUiModel): ChatItemUiModel {
        return try {
            val userResult = databaseService.selectWhere("users", "*", "uid", chat.otherUserId)
            userResult.fold(
                onSuccess = { users ->
                    val userData = users.firstOrNull()
                    if (userData != null) {
                        chat.copy(
                            displayName = userData["username"]?.toString() ?: chat.displayName,
                            avatarUrl = userData["avatar"]?.toString() ?: chat.avatarUrl,
                            isVerified = userData["is_verified"]?.toString()?.toBooleanStrictOrNull() ?: false
                        )
                    } else {
                        chat
                    }
                },
                onFailure = { chat }
            )
        } catch (e: Exception) {
            chat
        }
    }
}
