package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Comprehensive post detail model for the detailed post view.
 * Combines post data with author information, reactions, and engagement metrics.
 * 
 * Requirements: 1.1, 1.2, 1.4, 2.1, 4.2, 7.1
 */
@Serializable
data class PostDetail(
    val post: Post,
    val author: UserProfile,
    @Transient
    val reactionSummary: Map<ReactionType, Int> = emptyMap(),
    @Transient
    val userReaction: ReactionType? = null,
    val isBookmarked: Boolean = false,
    val hasReshared: Boolean = false,
    val pollResults: List<PollOptionResult>? = null,
    val userPollVote: Int? = null
) {
    /**
     * Check if the post has media items
     */
    fun hasMedia(): Boolean = !post.mediaItems.isNullOrEmpty()

    /**
     * Check if the post has a poll
     */
    fun hasPoll(): Boolean = post.hasPoll == true

    /**
     * Check if the post has location data
     */
    fun hasLocation(): Boolean = post.hasLocation == true

    /**
     * Check if the post has a YouTube embed
     */
    fun hasYouTubeEmbed(): Boolean = !post.youtubeUrl.isNullOrEmpty()

    /**
     * Check if the post is encrypted
     */
    fun isEncrypted(): Boolean = post.isEncrypted == true

    /**
     * Check if the post has been edited
     */
    fun isEdited(): Boolean = post.isEdited == true

    /**
     * Get total reaction count
     */
    fun getTotalReactions(): Int = reactionSummary.values.sum()

    /**
     * Check if the author is verified
     */
    fun isAuthorVerified(): Boolean = author.verify

    /**
     * Check if the author has premium account
     */
    fun isAuthorPremium(): Boolean = author.account_type == "premium"
}
