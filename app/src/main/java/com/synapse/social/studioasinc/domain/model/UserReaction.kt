package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Model for displaying users who reacted to a post
 * Combines user profile data with their reaction
 */
@Serializable
data class UserReaction(
    @SerialName("user_id")
    val userId: String,
    val username: String,
    @SerialName("profile_image")
    val profileImage: String? = null,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    @SerialName("reaction_type")
    val reactionType: String = "LIKE",
    @SerialName("reacted_at")
    val reactedAt: String? = null
) {
    /**
     * Get the ReactionType enum from the string value
     */
    fun getReactionTypeEnum(): ReactionType {
        return ReactionType.fromString(reactionType)
    }

    /**
     * Get formatted display name
     */
    fun getDisplayName(): String {
        return "@$username"
    }
}

/**
 * Extension function to convert HashMap to UserReaction object
 */
fun HashMap<String, Any>.toUserReaction(): UserReaction {
    return UserReaction(
        userId = this["user_id"] as? String ?: "",
        username = this["username"] as? String ?: "Unknown",
        profileImage = this["profile_image"] as? String,
        isVerified = this["is_verified"] as? Boolean ?: false,
        reactionType = this["reaction_type"] as? String ?: "LIKE",
        reactedAt = this["reacted_at"] as? String
    )
}
