package com.synapse.social.studioasinc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.remote.services.SupabaseFollowService
import com.synapse.social.studioasinc.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class FollowListUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val followService: SupabaseFollowService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    fun loadUsers(userId: String, listType: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = when (listType) {
                    "followers" -> followService.getFollowers(userId)
                    "following" -> followService.getFollowing(userId)
                    else -> Result.success(emptyList())
                }
                
                result.fold(
                    onSuccess = { userMaps ->
                        val users = userMaps.map { userMap ->
                            User(
                                uid = userMap["uid"]?.toString() ?: "",
                                username = userMap["username"]?.toString() ?: "",
                                displayName = userMap["display_name"]?.toString(),
                                avatar = userMap["avatar"]?.toString(),
                                verify = userMap["verify"]?.toString()?.toBoolean() ?: false
                            )
                        }
                        _uiState.value = _uiState.value.copy(
                            users = users,
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load users: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
}
