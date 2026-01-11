package com.synapse.social.studioasinc.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.local.database.AppDatabase
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.ui.components.post.PostCardState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.synapse.social.studioasinc.core.util.ScrollPositionState
import com.synapse.social.studioasinc.ui.components.post.PostEventBus
import com.synapse.social.studioasinc.ui.components.post.PostEvent
import com.synapse.social.studioasinc.ui.components.post.PostMapper
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.ui.settings.PostViewStyle

data class FeedUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val postViewStyle: PostViewStyle = PostViewStyle.SWIPE
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _modifiedPosts = MutableStateFlow<Map<String, Post>>(emptyMap())

    // Cache the raw PagingData FIRST to prevent "Attempt to collect twice from pageEventFlow"
    private val cachedPosts = postRepository.getPostsPaged()
        .cachedIn(viewModelScope)

    // Using PagingData for infinite scroll with modifications overlay
    val posts: Flow<PagingData<Post>> = cachedPosts
        .combine(_modifiedPosts) { pagingData, modifications ->
            pagingData.map { post ->
                modifications[post.id] ?: post
            }
        }

    private var savedScrollPosition: ScrollPositionState? = null

    init {
        // Observe Settings
        viewModelScope.launch {
            settingsRepository.appearanceSettings.collect { settings ->
                _uiState.update { it.copy(postViewStyle = settings.postViewStyle) }
            }
        }

        // Observe Global Post Events for Synchronization
        viewModelScope.launch {
            PostEventBus.events.collect { event ->
                when (event) {
                    is PostEvent.Liked -> {
                        val current = _modifiedPosts.value[event.postId]
                        // We only need to update if we have the post in PagingData or current view
                        // Since PagingData is a flow, we can't easily peek, but _modifiedPosts stores overrides.
                        // We can blindly update _modifiedPosts if we want to ensure sync, 
                        // but better to only update if we know about the post. 
                        // However, Paging requires us to know the previous state to copy it.
                        // A simpler approach: we cannot easily update PagingData from outside without a refresh 
                        // unless we keep a cache of 'latest state' for every post ID we've seen.
                        // For now, we'll try to update ONLY if it's already in _modifiedPosts, OR we assume we can't fully sync 
                        // PagingData without more complex caching. 
                        
                        // BUT, if the user liked it in Profile, they expect to see it liked in Feed.
                        // _modifiedPosts is used to override PagingData. 
                        // We can add a "Pending Modification" for this ID.
                        // But we need the original Post object to copy() it. We don't have it here if it's not in _modifiedPosts.
                        // So full bidirectional sync with Paging is hard.
                        // LUCKILY: PostEvent.Updated(post) carries the full post.
                    }
                    is PostEvent.Updated -> {
                        _modifiedPosts.update { it + (event.post.id to event.post) }
                    }
                    is PostEvent.Deleted -> {
                         // Removing from feed is hard with PagingData without refresh. 
                         // We can mark it as deleted in _modifiedPosts if we handle that in UI (e.g. filter or show hidden).
                    }
                    else -> {}
                }
            }
        }
    }

    private val reactionRepository: com.synapse.social.studioasinc.data.repository.ReactionRepository = com.synapse.social.studioasinc.data.repository.ReactionRepository()

    fun likePost(post: Post) {
        performReaction(post, ReactionType.LIKE)
    }
    
    fun reactToPost(post: Post, reactionType: ReactionType) {
        performReaction(post, reactionType)
    }

    private fun performReaction(post: Post, reactionType: ReactionType) {
         viewModelScope.launch {
             // 1. Calculate Optimistic Update
             val currentReaction = post.userReaction
             
             // Toggle logic: If selecting same reaction -> Remove. If different -> Update/Add.
             val isRemoving = currentReaction == reactionType
             val newReaction = if (isRemoving) null else reactionType
             
             // Calculate count change
             // If removing: -1
             // If adding (was null): +1
             // If changing (was A, now B): 0
             val countChange = when {
                 isRemoving -> -1
                 currentReaction == null -> 1
                 else -> 0
             }
             
             val newCount = post.likesCount + countChange
             
             // Update reactions summary map locally
             val updatedReactions = post.reactions?.toMutableMap() ?: mutableMapOf()
             if (isRemoving) {
                 val currentCount = updatedReactions[reactionType] ?: 1
                 updatedReactions[reactionType] = maxOf(0, currentCount - 1)
             } else {
                 // Decrement old if exists
                 if (currentReaction != null) {
                     val oldTypeCount = updatedReactions[currentReaction] ?: 1
                     updatedReactions[currentReaction] = maxOf(0, oldTypeCount - 1)
                 }
                 // Increment new
                 val newTypeCount = updatedReactions[reactionType] ?: 0
                 updatedReactions[reactionType] = newTypeCount + 1
             }

             // Create Updated Post
             val updatedPost = post.copy(
                 likesCount = maxOf(0, newCount),
                 userReaction = newReaction,
                 reactions = updatedReactions
             )

             // 2. Apply Local Update
             _modifiedPosts.update { it + (post.id to updatedPost) }
             PostEventBus.emit(PostEvent.Updated(updatedPost))

             // 3. Remote Update
             try {
                  reactionRepository.toggleReaction(post.id, "post", reactionType).onFailure {
                      // Revert on failure
                      _modifiedPosts.update { map -> map + (post.id to post) }
                      PostEventBus.emit(PostEvent.Updated(post))
                  }
             } catch (e: Exception) {
                  // Revert on exception
                  _modifiedPosts.update { map -> map + (post.id to post) }
                  PostEventBus.emit(PostEvent.Updated(post))
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
        PostEventBus.emit(PostEvent.Updated(updatedPost))

        viewModelScope.launch {
            try {
                val pollRepository = com.synapse.social.studioasinc.data.repository.PollRepository()
                pollRepository.submitVote(post.id, optionIndex)
            } catch (e: Exception) {
                e.printStackTrace()
                // Revert optimistic update
                _modifiedPosts.update { it - post.id }
                // We should also emit Reverted event
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
                val client = com.synapse.social.studioasinc.core.network.SupabaseClient.client
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
        // Let the UI handle isRefreshing state via PullToRefreshBox
        // The paging refresh will be handled by posts.refresh() in the UI
    }

    /**
     * Helper to convert Post model to PostCardState for UI
     */
    /**
     * Helper to convert Post model to PostCardState for UI
     */
    fun mapPostToState(post: Post): PostCardState {
        return PostMapper.mapToState(post)
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
