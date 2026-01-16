package com.synapse.social.studioasinc.feature.shared.reels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.synapse.social.studioasinc.data.local.database.AppDatabase
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ReelsUiState(
    val reels: Flow<PagingData<Post>> = emptyFlow(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReelsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    // Manual Dependency Injection to support AndroidViewModelFactory instantiation
    private val postDao = AppDatabase.getDatabase(application).postDao()
    private val client = SupabaseClient.client
    private val postRepository = PostRepository(postDao, client)

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    init {
        loadReels()
    }

    fun loadReels() {
        // Switch to PagingData
        val reelsFlow = postRepository.getReelsPaged()
            .cachedIn(viewModelScope)

        _uiState.update { it.copy(reels = reelsFlow) }
    }

    fun loadMoreReels() {
        // Handled by Paging 3 automatically
    }

    fun likeReel(reelId: String) {
        viewModelScope.launch {
             try {
                 val currentUserId = client.auth.currentUserOrNull()?.id as? String
                 if (currentUserId != null) {
                     postRepository.toggleReaction(reelId, currentUserId, com.synapse.social.studioasinc.domain.model.ReactionType.LIKE)
                 }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
