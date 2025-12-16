package com.synapse.social.studioasinc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.SupabaseFollowService
import com.synapse.social.studioasinc.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FollowButtonUiState(
    val isFollowing: Boolean = false,
    val isLoading: Boolean = false
)

class FollowButtonViewModel : ViewModel() {
    private val followService = SupabaseFollowService()
    private val authRepository = AuthRepository()
    
    private val _uiState = MutableStateFlow(FollowButtonUiState())
    val uiState: StateFlow<FollowButtonUiState> = _uiState.asStateFlow()
    
    private var currentUserId: String? = null
    private var targetUserId: String? = null

    fun initialize(targetUserId: String) {
        this.targetUserId = targetUserId
        
        viewModelScope.launch {
            currentUserId = authRepository.getCurrentUserUid()
            if (currentUserId != null && currentUserId != targetUserId) {
                checkFollowStatus()
            }
        }
    }

    private suspend fun checkFollowStatus() {
        val currentUid = currentUserId ?: return
        val targetUid = targetUserId ?: return
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        followService.isFollowing(currentUid, targetUid).fold(
            onSuccess = { isFollowing ->
                _uiState.value = _uiState.value.copy(
                    isFollowing = isFollowing,
                    isLoading = false
                )
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        )
    }

    fun toggleFollow() {
        val currentUid = currentUserId ?: return
        val targetUid = targetUserId ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = if (_uiState.value.isFollowing) {
                followService.unfollowUser(currentUid, targetUid)
            } else {
                followService.followUser(currentUid, targetUid)
            }
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isFollowing = !_uiState.value.isFollowing,
                        isLoading = false
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )
        }
    }
}
