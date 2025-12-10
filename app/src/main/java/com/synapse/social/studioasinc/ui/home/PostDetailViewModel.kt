package com.synapse.social.studioasinc.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.home.User
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.ui.components.post.PostCardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostDetailUiState(
    val post: PostCardState? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PostDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val postRepository: PostRepository = PostRepository(AppDatabase.getDatabase(application).postDao())

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    fun loadPost(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = postRepository.getPost(postId)
                result.onSuccess { post ->
                    if (post != null) {
                        val cardState = mapPostToState(post)
                        _uiState.update { it.copy(post = cardState, isLoading = false) }
                    } else {
                        _uiState.update { it.copy(error = "Post not found", isLoading = false) }
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    // Duplicated mapping logic from FeedViewModel - ideally should be in a mapper class
    private fun mapPostToState(post: Post): PostCardState {
        val user = User(
            uid = post.authorUid,
            username = post.username ?: "Unknown",
            avatar = post.avatarUrl,
            verify = if(post.isVerified) "1" else "0"
        )

        val mediaUrls = post.mediaItems?.mapNotNull { it.url } ?: listOfNotNull(post.postImage)

        return PostCardState(
            post = post,
            user = user,
            isLiked = post.hasUserReacted(),
            likeCount = post.likesCount,
            commentCount = post.commentsCount,
            isBookmarked = false,
            mediaUrls = mediaUrls,
            isVideo = post.postType == "VIDEO",
            pollQuestion = post.pollQuestion,
            pollOptions = post.pollOptions?.mapIndexed { index, option ->
                com.synapse.social.studioasinc.ui.components.post.PollOption(
                    id = index.toString(),
                    text = option.text,
                    voteCount = option.votes,
                    isSelected = false
                )
            }
        )
    }
}
