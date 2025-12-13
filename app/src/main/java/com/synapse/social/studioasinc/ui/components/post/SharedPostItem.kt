package com.synapse.social.studioasinc.ui.components.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.model.ReactionType

/**
 * A wrapper component that reduces code duplication in Features (Home, Profile).
 * It handles the creation of PostCardState using PostMapper.
 */
@Composable
fun SharedPostItem(
    post: Post,
    currentProfile: UserProfile? = null, // Optional, for Profile screen context
    actions: PostActions,
    modifier: Modifier = Modifier
) {
    // Use the shared mapper
    val state = remember(post, currentProfile) {
        PostMapper.mapToState(post, currentProfile)
    }

    PostCard(
        state = state,
        onLikeClick = { actions.onLike(post) },
        onCommentClick = { actions.onComment(post) },
        onShareClick = { actions.onShare(post) },
        onBookmarkClick = { actions.onBookmark(post) },
        onUserClick = { actions.onUserClick(post.authorUid) },
        onPostClick = { actions.onComment(post) }, // Default to opening details/comments
        onMediaClick = actions.onMediaClick,
        onOptionsClick = { actions.onOptionClick(post) },
        onPollVote = { optionId -> 
            val index = optionId.toIntOrNull()
            if (index != null) {
                actions.onPollVote(post, index)
            }
        },
        // Optional: Can extend PostActions if reaction picker is needed
        onReactionSelected = null,
        modifier = modifier
    )
}
