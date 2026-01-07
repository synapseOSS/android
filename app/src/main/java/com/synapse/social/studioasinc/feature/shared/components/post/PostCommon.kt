package com.synapse.social.studioasinc.ui.components.post

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.data.model.UserProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Shared Mapper logic for converting Post domain model to UI State.
 * This ensures Home and Profile screens render posts identically.
 */
object PostMapper {
    fun mapToState(post: Post, currentProfile: UserProfile? = null): PostCardState {
        // Resolve User Info: Default to Post fields, fallback to provided Profile if ID matches
        val resolvedUsername = when {
            !post.username.isNullOrBlank() -> post.username!!
            currentProfile?.id == post.authorUid -> currentProfile?.username ?: "Unknown"
            else -> "Unknown"
        }

        val resolvedAvatar = when {
            !post.avatarUrl.isNullOrBlank() -> post.avatarUrl
            currentProfile?.id == post.authorUid -> currentProfile?.avatar
            else -> null
        }

        val resolvedVerified = when {
            post.isVerified -> true
            currentProfile?.id == post.authorUid -> currentProfile?.isVerified == true
            else -> false
        }

        val user = User(
            uid = post.authorUid,
            username = resolvedUsername,
            avatar = resolvedAvatar,
            verify = resolvedVerified
        )

        val mediaUrls = post.mediaItems?.mapNotNull { it.url } ?: listOfNotNull(post.postImage)

        // Poll Mapping
        val mappedPollOptions = post.pollOptions?.mapIndexed { index, option ->
            PollOption(
                id = index.toString(),
                text = option.text,
                voteCount = option.votes,
                isSelected = post.userPollVote == index
            )
        }

        return PostCardState(
            post = post,
            user = user,
            isLiked = post.userReaction == ReactionType.LIKE,
            likeCount = post.likesCount,
            commentCount = post.commentsCount,
            isBookmarked = false, // To be populated if Model supports it
            hideLikeCount = post.postHideLikeCount == "true",
            mediaUrls = mediaUrls,
            isVideo = post.postType == "VIDEO",
            pollQuestion = post.pollQuestion,
            pollOptions = mappedPollOptions,
            userPollVote = post.userPollVote,
            topCommentAuthor = post.latestCommentAuthor,
            topCommentText = post.latestCommentText
        )
    }
}

/**
 * Event for Post interactions to sync state across screens.
 */
sealed class PostEvent {
    data class Liked(val postId: String, val isLiked: Boolean, val newLikeCount: Int) : PostEvent()
    data class PollVoted(val postId: String, val optionIndex: Int, val pollOptions: List<com.synapse.social.studioasinc.domain.model.PollOption>, val userVote: Int?) : PostEvent()
    data class Deleted(val postId: String) : PostEvent()
    data class Updated(val post: Post) : PostEvent()
    data class Error(val message: String) : PostEvent()
}

/**
 * Global Bus for Post Events.
 * ViewModels should emit to this when they change post state, and listen to it to update their local lists.
 */
object PostEventBus {
    private val _events = MutableSharedFlow<PostEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    fun emit(event: PostEvent) {
        _events.tryEmit(event)
    }
}

/**
 * Interface to standardize actions passed from UI to ViewModel.
 */
data class PostActions(
    val onLike: (Post) -> Unit,
    val onComment: (Post) -> Unit,
    val onShare: (Post) -> Unit,
    val onBookmark: (Post) -> Unit,
    val onOptionClick: (Post) -> Unit,
    val onPollVote: (Post, Int) -> Unit,
    val onUserClick: (String) -> Unit,
    val onMediaClick: (Int) -> Unit
)
