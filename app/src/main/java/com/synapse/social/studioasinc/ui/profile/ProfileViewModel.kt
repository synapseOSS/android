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
        val isLiked = postId in _state.value.likedPostIds
        
        // Optimistic update
        _state.update { state ->
            val likedPostIds = state.likedPostIds.toMutableSet()
            if (isLiked) {
                likedPostIds.remove(postId)
            } else {
                likedPostIds.add(postId)
            }
            state.copy(likedPostIds = likedPostIds)
        }
        
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
    fun mapPostToState(post: Post): PostCardState {
        // Use loaded profile data if available and matches author (fallback for missing post user data)
        val currentProfile = (_state.value.profileState as? ProfileUiState.Success)?.profile
        
        val username = if (!post.username.isNullOrBlank()) post.username 
                       else if (currentProfile?.id == post.authorUid) currentProfile?.username ?: "Unknown"
                       else "Unknown"
                       
        val avatarUrl = if (!post.avatarUrl.isNullOrBlank()) post.avatarUrl
                        else if (currentProfile?.id == post.authorUid) currentProfile?.profileImageUrl
                        else null

        val isVerified = if (post.isVerified) true 
                         else if (currentProfile?.id == post.authorUid) currentProfile?.isVerified == true
                         else false

        val user = User(
            uid = post.authorUid,
            username = username,
            avatar = avatarUrl,
            verify = if(isVerified) "1" else "0"
        )

        val mediaUrls = post.mediaItems?.mapNotNull { it.url } ?: listOfNotNull(post.postImage)

        return PostCardState(
            post = post,
            user = user,
            isLiked = post.hasUserReacted(),
            likeCount = post.likesCount,
            commentCount = post.commentsCount,
            isBookmarked = false, // Add logic if available in Post model or separate list
            mediaUrls = mediaUrls,
            isVideo = post.postType == "VIDEO",
            pollQuestion = post.pollQuestion,
            pollOptions = post.pollOptions?.mapIndexed { index, option ->
                PollOption(
                    id = index.toString(),
                    text = option.text,
                    voteCount = option.votes,
                    isSelected = false 
                )
            }
        )
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
