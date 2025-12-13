package com.synapse.social.studioasinc.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.domain.usecase.profile.*
import com.synapse.social.studioasinc.domain.usecase.post.*
import com.synapse.social.studioasinc.ui.profile.components.ViewAsMode
import com.synapse.social.studioasinc.ui.profile.utils.MemoryManager
import com.synapse.social.studioasinc.ui.profile.utils.NetworkOptimizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.home.User
import com.synapse.social.studioasinc.ui.components.post.PostCardState
import com.synapse.social.studioasinc.ui.components.post.PollOption
import com.synapse.social.studioasinc.ui.components.post.PostEventBus
import com.synapse.social.studioasinc.ui.components.post.PostEvent
import com.synapse.social.studioasinc.ui.components.post.PostMapper

data class ProfileScreenState(
    val profileState: ProfileUiState = ProfileUiState.Loading,
    val contentFilter: ProfileContentFilter = ProfileContentFilter.POSTS,
    val posts: List<Any> = emptyList(),
    val photos: List<Any> = emptyList(),
    val reels: List<Any> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val postsOffset: Int = 0,
    val photosOffset: Int = 0,
    val reelsOffset: Int = 0,
    val currentUserId: String = "",
    val isOwnProfile: Boolean = false,
    val showMoreMenu: Boolean = false,
    val likedPostIds: Set<String> = emptySet(),
    val savedPostIds: Set<String> = emptySet(),
    val showShareSheet: Boolean = false,
    val showViewAsSheet: Boolean = false,
    val showQrCode: Boolean = false,
    val showReportDialog: Boolean = false,
    val viewAsMode: ViewAsMode? = null,
    val viewAsUserName: String? = null
)

/**
 * ViewModel for Profile screen managing user profile state and actions.
 * 
 * Handles profile loading, content filtering, follow/unfollow operations,
 * and various profile actions like sharing, reporting, and blocking.
 * 
 * @property getProfileUseCase Use case for fetching user profiles
 * @property getProfileContentUseCase Use case for fetching profile content (posts, photos, reels)
 * @property followUserUseCase Use case for following users
 * @property unfollowUserUseCase Use case for unfollowing users
 */
class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfileContentUseCase: GetProfileContentUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val unlikePostUseCase: UnlikePostUseCase,
    private val savePostUseCase: SavePostUseCase,
    private val unsavePostUseCase: UnsavePostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val reportPostUseCase: ReportPostUseCase,
    private val lockProfileUseCase: LockProfileUseCase,
    private val archiveProfileUseCase: ArchiveProfileUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val reportUserUseCase: ReportUserUseCase,
    private val muteUserUseCase: MuteUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            PostEventBus.events.collect { event ->
                when (event) {
                    is PostEvent.Updated -> {
                        _state.update { currentState ->
                            val updatedPosts = currentState.posts.map { item ->
                                if (item is Post && item.id == event.post.id) event.post else item
                            }
                            currentState.copy(posts = updatedPosts)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadProfile(userId: String, currentUserId: String) {
        _state.update { it.copy(profileState = ProfileUiState.Loading, currentUserId = currentUserId) }
        viewModelScope.launch {
            getProfileUseCase(userId).collect { result ->
                result.onSuccess { profile ->
                    _state.update {
                        it.copy(
                            profileState = ProfileUiState.Success(profile),
                            isOwnProfile = userId == currentUserId
                        )
                    }
                    loadContent(userId, ProfileContentFilter.POSTS)
                }.onFailure { exception ->
                    _state.update {
                        it.copy(
                            profileState = ProfileUiState.Error(
                                exception.message ?: "Failed to load profile",
                                exception
                            )
                        )
                    }
                }
            }
        }
    }

    fun refreshProfile(userId: String) {
        loadProfile(userId, _state.value.currentUserId)
    }

    fun switchContentFilter(filter: ProfileContentFilter) {
        _state.update { it.copy(contentFilter = filter) }
        val profile = (_state.value.profileState as? ProfileUiState.Success)?.profile ?: return
        loadContent(profile.id, filter)
    }

    fun loadMoreContent(filter: ProfileContentFilter) {
        val profile = (_state.value.profileState as? ProfileUiState.Success)?.profile ?: return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            when (filter) {
                ProfileContentFilter.POSTS -> {
                    val offset = _state.value.postsOffset
                    getProfileContentUseCase.getPosts(profile.id, offset = offset).onSuccess { posts ->
                        _state.update {
                            val allPosts = MemoryManager.limitCacheSize(it.posts + posts)
                            it.copy(
                                posts = allPosts,
                                postsOffset = offset + posts.size,
                                isLoadingMore = false
                            )
                        }
                    }.onFailure {
                        _state.update { it.copy(isLoadingMore = false) }
                    }
                }
                ProfileContentFilter.PHOTOS -> {
                    val offset = _state.value.photosOffset
                    getProfileContentUseCase.getPhotos(profile.id, offset = offset).onSuccess { photos ->
                        _state.update {
                            it.copy(
                                photos = it.photos + photos,
                                photosOffset = offset + photos.size,
                                isLoadingMore = false
                            )
                        }
                    }.onFailure {
                        _state.update { it.copy(isLoadingMore = false) }
                    }
                }
                ProfileContentFilter.REELS -> {
                    val offset = _state.value.reelsOffset
                    getProfileContentUseCase.getReels(profile.id, offset = offset).onSuccess { reels ->
                        _state.update {
                            it.copy(
                                reels = it.reels + reels,
                                reelsOffset = offset + reels.size,
                                isLoadingMore = false
                            )
                        }
                    }.onFailure {
                        _state.update { it.copy(isLoadingMore = false) }
                    }
                }
            }
        }
    }

    fun followUser(targetUserId: String) {
        viewModelScope.launch {
            followUserUseCase(_state.value.currentUserId, targetUserId).onSuccess {
                _state.update { it.copy(isFollowing = true) }
            }
        }
    }

    fun unfollowUser(targetUserId: String) {
        viewModelScope.launch {
            unfollowUserUseCase(_state.value.currentUserId, targetUserId).onSuccess {
                _state.update { it.copy(isFollowing = false) }
            }
        }
    }

    fun toggleMoreMenu() {
        _state.update { it.copy(showMoreMenu = !it.showMoreMenu) }
    }

    fun toggleLike(postId: String) {
        // Find the post to toggle
        val post = _state.value.posts.filterIsInstance<Post>().find { it.id == postId } ?: return
        val isLiked = post.hasUserReacted() // Use Post state, not local Set which might be desynced
        
        // Optimistic update
        val newReaction = if (isLiked) null else com.synapse.social.studioasinc.model.ReactionType.LIKE
        val newCount = if (isLiked) post.likesCount - 1 else post.likesCount + 1
        
        val updatedPost = post.copy(likesCount = newCount).apply {
            userReaction = newReaction
        }
        
        // Update Local
        _state.update { state ->
            val updatedPosts = state.posts.map { if (it is Post && it.id == postId) updatedPost else it }
            state.copy(posts = updatedPosts)
        }
        
        // Emit Global
        PostEventBus.emit(PostEvent.Updated(updatedPost))
        
        // Backend call
        viewModelScope.launch {
            val result = if (isLiked) {
                unlikePostUseCase(postId, _state.value.currentUserId)
            } else {
                likePostUseCase(postId, _state.value.currentUserId)
            }
            
            result.collect { res ->
                res.onFailure {
                    // Revert on failure
                    _state.update { state ->
                        val likedPostIds = state.likedPostIds.toMutableSet()
                        if (isLiked) {
                            likedPostIds.add(postId)
                        } else {
                            likedPostIds.remove(postId)
                        }
                        state.copy(likedPostIds = likedPostIds)
                    }
                }
            }
        }
    }

    fun toggleSave(postId: String) {
        val isSaved = postId in _state.value.savedPostIds
        
        // Optimistic update
        _state.update { state ->
            val savedPostIds = state.savedPostIds.toMutableSet()
            if (isSaved) {
                savedPostIds.remove(postId)
            } else {
                savedPostIds.add(postId)
            }
            state.copy(savedPostIds = savedPostIds)
        }
        
        // Backend call
        viewModelScope.launch {
            val result = if (isSaved) {
                unsavePostUseCase(postId, _state.value.currentUserId)
            } else {
                savePostUseCase(postId, _state.value.currentUserId)
            }
            
            result.collect { res ->
                res.onFailure {
                    // Revert on failure
                    _state.update { state ->
                        val savedPostIds = state.savedPostIds.toMutableSet()
                        if (isSaved) {
                            savedPostIds.add(postId)
                        } else {
                            savedPostIds.remove(postId)
                        }
                        state.copy(savedPostIds = savedPostIds)
                    }
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            deletePostUseCase(postId, _state.value.currentUserId).collect { result ->
                result.onSuccess {
                    // Remove from local state
                    _state.update { state ->
                        state.copy(posts = state.posts.filterNot { (it as? Any)?.toString()?.contains(postId) == true })
                    }
                }
            }
        }
    }

    fun reportPost(postId: String, reason: String) {
        viewModelScope.launch {
            reportPostUseCase(postId, _state.value.currentUserId, reason).collect { result ->
                result.onSuccess {
                    // Optionally hide post from feed
                }
            }
        }
    }
    
    fun votePoll(postId: String, optionIndex: Int) {
        val post = _state.value.posts.filterIsInstance<Post>().find { it.id == postId } ?: return
        val currentOptions = post.pollOptions ?: return
        if (post.userPollVote != null) return

        val updatedOptions = currentOptions.mapIndexed { index, option ->
            if (index == optionIndex) option.copy(votes = option.votes + 1) else option
        }

        val updatedPost = post.copy(
            pollOptions = updatedOptions,
            userPollVote = optionIndex
        )
        
        _state.update { state ->
            val updatedPosts = state.posts.map { if (it is Post && it.id == postId) updatedPost else it }
            state.copy(posts = updatedPosts)
        }
        PostEventBus.emit(PostEvent.Updated(updatedPost))

        viewModelScope.launch {
            try {
                val pollRepository = com.synapse.social.studioasinc.data.repository.PollRepository()
                pollRepository.submitVote(postId, optionIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Phase 4: Advanced Features
    fun showShareSheet() {
        _state.update { it.copy(showShareSheet = true) }
    }

    fun hideShareSheet() {
        _state.update { it.copy(showShareSheet = false) }
    }

    fun showViewAsSheet() {
        _state.update { it.copy(showViewAsSheet = true) }
    }

    fun hideViewAsSheet() {
        _state.update { it.copy(showViewAsSheet = false) }
    }

    fun showQrCode() {
        _state.update { it.copy(showQrCode = true) }
    }

    fun hideQrCode() {
        _state.update { it.copy(showQrCode = false) }
    }

    fun showReportDialog() {
        _state.update { it.copy(showReportDialog = true) }
    }

    fun hideReportDialog() {
        _state.update { it.copy(showReportDialog = false) }
    }

    fun setViewAsMode(mode: ViewAsMode, userName: String? = null) {
        _state.update { it.copy(viewAsMode = mode, viewAsUserName = userName) }
    }

    fun exitViewAs() {
        _state.update { it.copy(viewAsMode = null, viewAsUserName = null) }
    }

    fun lockProfile(isLocked: Boolean) {
        viewModelScope.launch {
            lockProfileUseCase(_state.value.currentUserId, isLocked).collect { result ->
                result.onSuccess {
                    // Update profile state
                }
            }
        }
    }

    fun archiveProfile(isArchived: Boolean) {
        viewModelScope.launch {
            archiveProfileUseCase(_state.value.currentUserId, isArchived).collect { result ->
                result.onSuccess {
                    // Update profile state
                }
            }
        }
    }

    fun blockUser(blockedUserId: String) {
        viewModelScope.launch {
            blockUserUseCase(_state.value.currentUserId, blockedUserId).collect { result ->
                result.onSuccess {
                    // Navigate back or update UI
                }
            }
        }
    }

    fun reportUser(reportedUserId: String, reason: String) {
        viewModelScope.launch {
            reportUserUseCase(_state.value.currentUserId, reportedUserId, reason).collect { result ->
                result.onSuccess {
                    // Show success message
                }
            }
        }
    }

    fun muteUser(mutedUserId: String) {
        viewModelScope.launch {
            muteUserUseCase(_state.value.currentUserId, mutedUserId).collect { result ->
                result.onSuccess {
                    // Update UI
                }
            }
        }
    }

    /**
     * Helper to convert Post model to PostCardState for Shared UI
     */
    /**
     * Helper to convert Post model to PostCardState for Shared UI
     */
    fun mapPostToState(post: Post): PostCardState {
        // Use loaded profile data if available and matches author (fallback for missing post user data)
        val currentProfile = (_state.value.profileState as? ProfileUiState.Success)?.profile
        return PostMapper.mapToState(post, currentProfile)
    }

    private fun loadContent(userId: String, filter: ProfileContentFilter) {
        viewModelScope.launch {
            when (filter) {
                ProfileContentFilter.POSTS -> {
                    getProfileContentUseCase.getPosts(userId).onSuccess { posts ->
                        _state.update { it.copy(posts = posts, postsOffset = posts.size) }
                    }
                }
                ProfileContentFilter.PHOTOS -> {
                    getProfileContentUseCase.getPhotos(userId).onSuccess { photos ->
                        _state.update { it.copy(photos = photos, photosOffset = photos.size) }
                    }
                }
                ProfileContentFilter.REELS -> {
                    getProfileContentUseCase.getReels(userId).onSuccess { reels ->
                        _state.update { it.copy(reels = reels, reelsOffset = reels.size) }
                    }
                }
            }
        }
    }
}
