package com.synapse.social.studioasinc.ui.reels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.local.database.AppDatabase
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.model.Post
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ReelsUiState(
    val reels: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReelsViewModel @Inject constructor(
    application: Application,
    private val postRepository: PostRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    init {
        loadReels()
    }

    fun loadReels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // We use getPosts() for now as it returns Flow<Result<List<Post>>>.
                // In a real scenario, we would filter for video type in the repository query.
                // Assuming client-side filter for now if repo doesn't support specific query yet.
                postRepository.getPosts().collect { result ->
                     result.onSuccess { posts ->
                         val videoPosts = posts.filter { it.postType == "VIDEO" || it.mediaItems?.any { m -> m.type == com.synapse.social.studioasinc.model.MediaType.VIDEO } == true }
                         _uiState.update { it.copy(reels = videoPosts, isLoading = false) }
                     }.onFailure { e ->
                         _uiState.update { it.copy(error = e.message, isLoading = false) }
                     }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadMoreReels() {
        // Since we are loading all reels at once in loadReels for now (due to basic filtering),
        // we can skip pagination logic or implement client-side pagination if needed.
        // For simplicity, we assume the list is complete or auto-updating via Flow.
    }

    fun likeReel(reelId: String) {
        viewModelScope.launch {
             try {
                 val client = com.synapse.social.studioasinc.core.network.SupabaseClient.client
                 val currentUserId = client.auth.currentUserOrNull()?.id as? String
                 if (currentUserId != null) {
                     postRepository.toggleReaction(reelId, currentUserId, com.synapse.social.studioasinc.model.ReactionType.LIKE)
                 }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
