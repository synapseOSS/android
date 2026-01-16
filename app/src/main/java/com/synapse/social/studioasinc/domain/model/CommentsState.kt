package com.synapse.social.studioasinc.domain.model

/**
 * Sealed class representing the state of comments loading.
 * Used by PostDetailViewModel to manage comments UI state.
 * 
 * Requirements: 4.1
 */
sealed class CommentsState {
    /**
     * Initial loading state
     */
    object Loading : CommentsState()

    /**
     * Successfully loaded comments
     */
    data class Success(
        val comments: List<CommentWithUser>,
        val hasMore: Boolean = false
    ) : CommentsState()

    /**
     * Error occurred while loading comments
     */
    data class Error(val message: String, val throwable: Throwable? = null) : CommentsState()

    /**
     * No comments available
     */
    object Empty : CommentsState()
}
