package com.synapse.social.studioasinc.model

/**
 * Sealed class representing real-time comment events.
 * Used for Supabase Realtime subscription updates.
 * 
 * Requirements: 11.1, 11.3
 */
sealed class CommentEvent {
    /**
     * A new comment was added
     */
    data class Added(val comment: CommentWithUser) : CommentEvent()

    /**
     * An existing comment was updated
     */
    data class Updated(val comment: CommentWithUser) : CommentEvent()

    /**
     * A comment was deleted
     */
    data class Deleted(val commentId: String) : CommentEvent()
}
