package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Comment reaction model aligned with comment_reactions table.
 * Represents a user's reaction to a comment.
 * 
 * Requirements: 6.2, 6.3, 6.4
 */
@Serializable
data class CommentReaction(
    val id: String? = null,
    @SerialName("comment_id")
    val commentId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("reaction_type")
    val reactionType: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    /**
     * Get the ReactionType enum from the string value
     */
    fun getReactionTypeEnum(): ReactionType {
        return ReactionType.fromString(reactionType)
    }

    companion object {
        /**
         * Create a new CommentReaction for insertion
         */
        fun create(
            commentId: String,
            userId: String,
            reactionType: ReactionType
        ): CommentReaction {
            return CommentReaction(
                commentId = commentId,
                userId = userId,
                reactionType = reactionType.name
            )
        }
    }
}
