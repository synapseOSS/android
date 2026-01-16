package com.synapse.social.studioasinc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.remote.services.SupabaseFollowService
import com.synapse.social.studioasinc.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean

data class FollowButtonUiState(
    val isFollowing: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class FollowButtonViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val followService = SupabaseFollowService()
    
    private val _uiState = MutableStateFlow(FollowButtonUiState())
    val uiState: StateFlow<FollowButtonUiState> = _uiState.asStateFlow()
    
    private var currentUserId: String? = null
    private var targetUserId: String? = null
    private val isOperationInProgress = AtomicBoolean(false)

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
        
        if (!isOperationInProgress.compareAndSet(false, true)) return // Prevent concurrent operations
        
        viewModelScope.launch {
            try {
                val currentFollowState = _uiState.value.isFollowing
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = if (currentFollowState) {
                    followService.unfollowUser(currentUid, targetUid)
                } else {
                    followService.followUser(currentUid, targetUid)
                }
                
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isFollowing = !currentFollowState,
                            isLoading = false
                        )
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                )
            } finally {
                isOperationInProgress.set(false)
            }
        }
    }
}
