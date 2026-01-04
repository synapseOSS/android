package com.synapse.social.studioasinc.ui.inbox.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.remote.services.SupabaseChatService
import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class UserSelectionUiModel(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val isSelected: Boolean = false
)

sealed interface CreateGroupUiState {
    data object Loading : CreateGroupUiState
    data class Content(
        val users: List<UserSelectionUiModel> = emptyList(),
        val selectedUsers: List<UserSelectionUiModel> = emptyList(),
        val groupName: String = "",
        val isCreating: Boolean = false,
        val error: String? = null
    ) : CreateGroupUiState
}

class CreateGroupViewModel(
    private val databaseService: SupabaseDatabaseService = SupabaseDatabaseService(),
    private val chatService: SupabaseChatService = SupabaseChatService(),
    private val authService: SupabaseAuthenticationService = SupabaseAuthenticationService()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateGroupUiState>(CreateGroupUiState.Content())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            updateContentState { it.copy(users = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce

            val currentUserId = authService.getCurrentUserId() ?: return@launch

            databaseService.searchUsers(query).fold(
                onSuccess = { users ->
                    val userModels = users.mapNotNull { userMap ->
                        val id = userMap["uid"]?.toString() ?: userMap["id"]?.toString()
                        if (id == null || id == currentUserId) return@mapNotNull null

                        val username = userMap["username"]?.toString() ?: "Unknown"
                        val displayName = userMap["display_name"]?.toString() ?: username
                        val avatarUrl = userMap["avatar"]?.toString()

                        UserSelectionUiModel(
                            id = id,
                            username = username,
                            displayName = displayName,
                            avatarUrl = avatarUrl
                        )
                    }

                    updateContentState { state ->
                        // Preserve selection state
                        val updatedUsers = userModels.map { user ->
                            user.copy(isSelected = state.selectedUsers.any { it.id == user.id })
                        }
                        state.copy(users = updatedUsers)
                    }
                },
                onFailure = {
                    // Handle error silently or show toast
                }
            )
        }
    }

    fun toggleUserSelection(user: UserSelectionUiModel) {
        updateContentState { state ->
            val isSelected = state.selectedUsers.any { it.id == user.id }
            val newSelectedUsers = if (isSelected) {
                state.selectedUsers.filter { it.id != user.id }
            } else {
                state.selectedUsers + user.copy(isSelected = true)
            }

            val updatedUsers = state.users.map {
                if (it.id == user.id) it.copy(isSelected = !isSelected) else it
            }

            state.copy(selectedUsers = newSelectedUsers, users = updatedUsers)
        }
    }

    fun removeSelectedUser(user: UserSelectionUiModel) {
        updateContentState { state ->
            val newSelectedUsers = state.selectedUsers.filter { it.id != user.id }
            val updatedUsers = state.users.map {
                if (it.id == user.id) it.copy(isSelected = false) else it
            }
            state.copy(selectedUsers = newSelectedUsers, users = updatedUsers)
        }
    }

    fun onGroupNameChanged(name: String) {
        updateContentState { it.copy(groupName = name) }
    }

    fun createGroup(onSuccess: (String) -> Unit) {
        val currentState = _uiState.value as? CreateGroupUiState.Content ?: return
        if (currentState.groupName.isBlank() || currentState.selectedUsers.isEmpty()) return

        updateContentState { it.copy(isCreating = true, error = null) }

        viewModelScope.launch {
            val currentUserId = authService.getCurrentUserId()
            if (currentUserId == null) {
                 updateContentState { it.copy(isCreating = false, error = "Not authenticated") }
                 return@launch
            }

            val userIds = currentState.selectedUsers.map { it.id }

            chatService.createGroupChat(currentState.groupName, userIds, currentUserId).fold(
                onSuccess = { chatId ->
                    updateContentState { it.copy(isCreating = false) }
                    onSuccess(chatId)
                },
                onFailure = { error ->
                    updateContentState { it.copy(isCreating = false, error = error.message) }
                }
            )
        }
    }

    private fun updateContentState(update: (CreateGroupUiState.Content) -> CreateGroupUiState.Content) {
        val currentState = _uiState.value
        if (currentState is CreateGroupUiState.Content) {
            _uiState.value = update(currentState)
        } else {
            // Should not happen normally if we initialize with Content
             _uiState.value = update(CreateGroupUiState.Content())
        }
    }
}
