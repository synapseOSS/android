package com.synapse.social.studioasinc.ui.components.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.ui.settings.PostViewStyle

// Helper state class for PostCard (can be expanded)
data class PostCardState(
    val post: Post,
    val user: User, // Add User here as it's needed for Header
    val isLiked: Boolean,
    val likeCount: Int,
    val commentCount: Int,
    val isBookmarked: Boolean,
    val hideLikeCount: Boolean = false,
    val mediaUrls: List<String> = emptyList(), // Extract from post
    val isVideo: Boolean = false,
    val pollQuestion: String? = null,
    val pollOptions: List<PollOption>? = null,
    val userPollVote: Int? = null, // Track user's vote for poll
    val topCommentAuthor: String? = null,
    val topCommentText: String? = null
)

@Composable
fun PostCard(
    state: PostCardState,
    postViewStyle: PostViewStyle = PostViewStyle.SWIPE,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onUserClick: () -> Unit,
    onPostClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    onOptionsClick: () -> Unit,
    onPollVote: (String) -> Unit,
    onReactionSelected: ((ReactionType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showReactionPicker by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(onClick = onPostClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            PostHeader(
                user = state.user,
                timestamp = com.synapse.social.studioasinc.core.util.TimeUtils.getTimeAgo(state.post.publishDate ?: ""),
                onUserClick = onUserClick,
                onOptionsClick = onOptionsClick
            )

            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                PostContent(
                    text = state.post.postText,
                    mediaUrls = state.mediaUrls,
                    postViewStyle = postViewStyle,
                    isVideo = state.isVideo,
                    pollQuestion = state.pollQuestion,
                    pollOptions = state.pollOptions,
                    onMediaClick = onMediaClick,
                    onPollVote = onPollVote
                )

                // Comment Preview Section
                if (state.commentCount > 0) {
                     CommentPreview(
                        commentCount = state.commentCount,
                        topCommentAuthor = state.topCommentAuthor,
                        topCommentText = state.topCommentText,
                        onViewAllClick = onCommentClick,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            PostInteractionBar(
                isLiked = state.isLiked,
                likeCount = state.likeCount,
                commentCount = state.commentCount,
                isBookmarked = state.isBookmarked,
                hideLikeCount = state.hideLikeCount,
                onLikeClick = onLikeClick,
                onCommentClick = onCommentClick,
                onShareClick = onShareClick,
                onBookmarkClick = onBookmarkClick,
                onReactionLongPress = if (onReactionSelected != null) {
                    { showReactionPicker = true }
                } else null
            )
        }
    }
    
    if (showReactionPicker && onReactionSelected != null) {
        ReactionPicker(
            onReactionSelected = { reaction ->
                onReactionSelected(reaction)
                showReactionPicker = false
            },
            onDismiss = { showReactionPicker = false }
        )
    }
}
