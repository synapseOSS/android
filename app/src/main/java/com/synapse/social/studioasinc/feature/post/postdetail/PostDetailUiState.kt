package com.synapse.social.studioasinc.ui.postdetail

import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.PostDetail

data class PostDetailUiState(
    val isLoading: Boolean = false,
    val post: PostDetail? = null,
    val comments: List<CommentWithUser> = emptyList(),
    val isCommentsLoading: Boolean = false,
    val error: String? = null,
    val replyToComment: CommentWithUser? = null,
    val editingComment: CommentWithUser? = null,
    val hasMoreComments: Boolean = false,
    val currentUserId: String? = null,
    val replies: Map<String, List<CommentWithUser>> = emptyMap(),
    val replyLoading: Set<String> = emptySet(),
    val commentActionsLoading: Set<String> = emptySet(),
    val refreshTrigger: Int = 0
)
