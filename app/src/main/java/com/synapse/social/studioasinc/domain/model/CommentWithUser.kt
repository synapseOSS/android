package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Comment model with embedded user information for display.
 * Supports nested replies via parent_comment_id reference.
 * 
 * Requirements: 4.2, 5.1
 */
@Serializable
data class CommentWithUser(
    val id: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,
    val content: String,
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("replies_count")
    val repliesCount: Int = 0,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("is_edited")
    val isEdited: Boolean = false,
    @SerialName("is_pinned")
    val isPinned: Boolean = false,
    // User information (populated from join)
    @Transient
    val user: UserProfile? = null,
    // Reaction data (populated separately)
    @Transient
    val userReaction: ReactionType? = null,
    @Transient
    val reactionSummary: Map<ReactionType, Int> = emptyMap()
) {
    /**
     * Check if this comment is a reply to another comment
     */
    fun isReply(): Boolean = parentCommentId != null

    /**
     * Check if this comment has replies
     */
    fun hasReplies(): Boolean = repliesCount > 0

    /**
     * Check if this comment has media attached
     */
    fun hasMedia(): Boolean = !mediaUrl.isNullOrEmpty()

    /**
     * Get total reaction count
     */
    fun getTotalReactions(): Int = reactionSummary.values.sum()

    /**
     * Get display name for the commenter
     */
    fun getDisplayName(): String = user?.displayName ?: "Unknown User"

    /**
     * Get username for the commenter
     */
    fun getUsername(): String = user?.username ?: "unknown"

    /**
     * Get avatar URL for the commenter
     */
    fun getAvatarUrl(): String? = user?.avatar
}
