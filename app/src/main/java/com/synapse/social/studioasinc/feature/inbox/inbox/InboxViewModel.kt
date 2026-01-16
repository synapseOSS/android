package com.synapse.social.studioasinc.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import com.synapse.social.studioasinc.ui.inbox.models.*
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.UserProfileManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Inbox screen.
 * Note: Chat functionality removed - shows empty state UI
 */
class InboxViewModel(
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
    
    // Filtered chats based on search (always empty since no chat data)
    val filteredChats: StateFlow<List<ChatItemUiModel>> = flowOf<List<ChatItemUiModel>>(emptyList()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Current user ID
    private val currentUserId: String?
        get() = authService.getCurrentUser()?.id
    
    // Current user profile
    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()

    init {
        loadChats()
        loadCurrentUserProfile()
    }

    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            try {
                val profile = UserProfileManager.getCurrentUserProfile()
                _currentUserProfile.value = profile
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    /**
     * Loads all chats for the current user
     * Note: Chat functionality removed - shows empty state
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
            
            // Simulate loading delay for better UX
            kotlinx.coroutines.delay(500)
            
            // Show empty state since chat functionality is removed
            _uiState.value = InboxUiState.Success(
                chats = emptyList(),
                pinnedChats = emptyList(),
                archivedChats = emptyList()
            )
        }
    }

    /**
     * Refreshes the chat list
     */
    fun refreshChats() {
        loadChats()
    }

    /**
     * Handles UI actions
     */
    fun onAction(action: InboxAction) {
        when (action) {
            is InboxAction.RefreshChats -> refreshChats()
            is InboxAction.SearchQueryChanged -> updateSearchQuery(action.query)
            is InboxAction.ToggleSelectionMode -> toggleSelectionMode(action.chatId)
            is InboxAction.ToggleSelection -> selectChat(action.chatId)
            is InboxAction.ClearSelection -> clearSelection()
            is InboxAction.ArchiveChat -> archiveChat(action.chatId)
            is InboxAction.ArchiveSelected -> archiveSelectedChats()
            is InboxAction.UnarchiveChat -> unarchiveChat(action.chatId)
            is InboxAction.UnarchiveSelected -> unarchiveSelectedChats()
            is InboxAction.DeleteChat -> deleteChat(action.chatId)
            is InboxAction.DeleteSelected -> deleteSelectedChats()
            is InboxAction.DeleteChatHistory -> deleteChatHistory(action.chatId)
            is InboxAction.DeleteSelectedChatHistory -> deleteSelectedChatHistory()
            is InboxAction.DeleteAllChatHistory -> deleteAllChatHistory()
            is InboxAction.MuteChat -> muteChat(action.chatId, action.duration)
            is InboxAction.PinChat -> pinChat(action.chatId)
            is InboxAction.UnpinChat -> unpinChat(action.chatId)
            is InboxAction.ViewArchivedChats -> toggleArchivedView(true)
            is InboxAction.ViewInbox -> toggleArchivedView(false)
            is InboxAction.SetFilter -> setFilter(action.filter)
            is InboxAction.NavigateToNewChat -> { /* No-op */ }
            is InboxAction.NavigateToSearch -> { /* No-op */ }
            is InboxAction.OpenChat -> { /* Handled by UI */ }
        }
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    private fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun toggleSelectionMode(chatId: String?) {
        // No-op: Chat functionality removed
    }

    private fun selectChat(chatId: String) {
        // No-op: Chat functionality removed
    }

    private fun clearSelection() {
        // No-op: Chat functionality removed
    }

    private fun deleteChatHistory(chatId: String) {
        // No-op: Chat functionality removed
    }
    
    private fun deleteSelectedChatHistory() {
        // No-op: Chat functionality removed
    }

    fun deleteAllChatHistory() {
        // No-op: Chat functionality removed
    }

    private fun archiveChat(chatId: String) {
        // No-op: Chat functionality removed
    }

    private fun archiveSelectedChats() {
        // No-op: Chat functionality removed
    }
    
    private fun unarchiveChat(chatId: String) {
        // No-op: Chat functionality removed
    }

    private fun unarchiveSelectedChats() {
        // No-op: Chat functionality removed
    }

    private fun toggleArchivedView(showArchived: Boolean) {
        // No-op: Chat functionality removed
    }

    private fun deleteChat(chatId: String) {
        // No-op: Chat functionality removed
    }

    private fun deleteSelectedChats() {
        // No-op: Chat functionality removed
    }
    
    private fun muteChat(chatId: String, duration: MuteDuration) {
        // No-op: Chat functionality removed
    }
    
    private fun pinChat(chatId: String) {
        // No-op: Chat functionality removed
    }
    
    private fun unpinChat(chatId: String) {
        // No-op: Chat functionality removed
    }

    private fun setFilter(filter: InboxFilter) {
        // No-op: Chat functionality removed
    }
}
