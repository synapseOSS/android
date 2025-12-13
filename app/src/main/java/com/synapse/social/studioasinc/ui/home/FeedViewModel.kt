package com.synapse.social.studioasinc.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.home.User
import com.synapse.social.studioasinc.ui.components.post.PostCardState
import com.synapse.social.studioasinc.util.ScrollPositionState
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

data class FeedUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository: AuthRepository = AuthRepository()
    private val postRepository: PostRepository = PostRepository(AppDatabase.getDatabase(application).postDao())

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _modifiedPosts = MutableStateFlow<Map<String, Post>>(emptyMap())

    // Using PagingData for infinite scroll
    val posts: Flow<PagingData<Post>> = postRepository.getPostsPaged()
        .combine(_modifiedPosts) { pagingData, modifications ->
            pagingData.map { post ->
                modifications[post.id] ?: post
            }
        }
        .cachedIn(viewModelScope)

    private var savedScrollPosition: ScrollPositionState? = null

    init {
        // Initial load logic if needed (PagingData handles most)
    }

    fun likePost(post: Post) {
        viewModelScope.launch {
             try {
                 val currentUserId = authRepository.getCurrentUserId()
                 if (currentUserId != null) {
                    postRepository.toggleReaction(post.id, currentUserId, ReactionType.LIKE)
                 }
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }
    
    fun reactToPost(post: Post, reactionType: ReactionType) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId()
                if (currentUserId != null) {
                    postRepository.toggleReaction(post.id, currentUserId, reactionType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun votePoll(post: Post, optionIndex: Int) {
        // Optimistic update
        val currentOptions = post.pollOptions ?: return
        if (post.userPollVote != null) return // Already voted

        val updatedOptions = currentOptions.mapIndexed { index, option ->
            if (index == optionIndex) option.copy(votes = option.votes + 1) else option
        }

        val updatedPost = post.copy(
            pollOptions = updatedOptions,
            userPollVote = optionIndex
        )

        _modifiedPosts.update { it + (post.id to updatedPost) }

        viewModelScope.launch {
            try {
                val pollRepository = com.synapse.social.studioasinc.data.repository.PollRepository()
                pollRepository.submitVote(post.id, optionIndex)
            } catch (e: Exception) {
                e.printStackTrace()
                // Revert optimistic update
                _modifiedPosts.update { it - post.id }
            }
        }
    }

    fun revokeVote(post: Post) {
        val currentVoteIndex = post.userPollVote ?: return
        val currentOptions = post.pollOptions ?: return

        // Optimistic update
        val updatedOptions = currentOptions.mapIndexed { index, option ->
            if (index == currentVoteIndex) option.copy(votes = maxOf(0, option.votes - 1)) else option
        }

        val updatedPost = post.copy(
            pollOptions = updatedOptions,
            userPollVote = null
        )

        _modifiedPosts.update { it + (post.id to updatedPost) }

        viewModelScope.launch {
            try {
                val pollRepository = com.synapse.social.studioasinc.data.repository.PollRepository()
                pollRepository.revokeVote(post.id)
            } catch (e: Exception) {
                e.printStackTrace()
                // Revert
                _modifiedPosts.update { it - post.id }
            }
        }
    }

    fun bookmarkPost(post: Post) {
        viewModelScope.launch {
            try {
                // Using direct Supabase client call for bookmarks as fallback
                val client = com.synapse.social.studioasinc.SupabaseClient.client
                val currentUserId = authRepository.getCurrentUserId()
                if (currentUserId != null) {
                    client.from("bookmarks").insert(mapOf(
                        "user_id" to currentUserId,
                        "post_id" to post.id
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        // PagingAdapter refresh() triggers the refresh in UI usually
        // But if we have manual refresh logic here:
        // postRepository.refresh()
        // For now, we simulate refresh completion or let the UI pull trigger the paging refresh
         _uiState.value = _uiState.value.copy(isRefreshing = false)
    }

    /**
     * Helper to convert Post model to PostCardState for UI
     */
    fun mapPostToState(post: Post): PostCardState {
        // Fetch user data if not present in Post.
        // For now assuming Post has embedded user info or we can't map fully without extra calls.
        // In real app, Post usually has `author` field or we fetch users separately.
        // The `Post` model in `model/Post.kt` has `username` and `avatarUrl` as transient.
        // We might need to ensure these are populated by the Repository/PagingSource.

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
            isBookmarked = false, // Add logic if available
            mediaUrls = mediaUrls,
            isVideo = post.postType == "VIDEO",
            pollQuestion = post.pollQuestion,
            pollOptions = post.pollOptions?.mapIndexed { index, option ->
                com.synapse.social.studioasinc.ui.components.post.PollOption(
                    id = index.toString(),
                    text = option.text,
                    voteCount = option.votes,
                    isSelected = post.userPollVote == index
                )
            },
            userPollVote = post.userPollVote
        )
    }

    fun saveScrollPosition(position: Int, offset: Int) {
        savedScrollPosition = ScrollPositionState(position, offset)
    }

    fun restoreScrollPosition(): ScrollPositionState? {
        val position = savedScrollPosition
        return if (position != null && !position.isExpired()) {
            position
        } else {
            savedScrollPosition = null
            null
        }
    }

    fun isPostOwner(post: Post): Boolean {
        return authRepository.getCurrentUserId() == post.authorUid
    }

    fun areCommentsDisabled(post: Post): Boolean {
        return post.postDisableComments?.toBoolean() ?: false
    }

    fun editPost(post: Post) {
        // Navigation handled by Activity/Fragment
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(post.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sharePost(post: Post) {
        // Handled by Activity/Fragment with Intent
    }

    fun copyPostLink(post: Post) {
        // Handled by Activity/Fragment with ClipboardManager
    }

    fun toggleComments(post: Post) {
        viewModelScope.launch {
            try {
                val newState = !(post.postDisableComments?.toBoolean() ?: false)
                postRepository.updatePost(post.id, mapOf("post_disable_comments" to newState))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reportPost(post: Post) {
        // Report logic
    }

    fun blockUser(userId: String) {
        // Block logic
    }
}
