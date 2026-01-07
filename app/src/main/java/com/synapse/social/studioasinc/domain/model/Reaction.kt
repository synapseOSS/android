package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Reaction model for Supabase database
 * Represents a user's reaction to a post (Like, Love, Haha, Wow, Sad, Angry)
 */
@Serializable
data class Reaction(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("target_id")
    val targetId: String,
    @SerialName("target_type")
    val targetType: String = "post",
    @SerialName("reaction_type")
    val reactionType: String = "LIKE", // Default to LIKE for backward compatibility
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

    /**
     * Check if this is a like reaction
     */
    fun isLike(): Boolean {
        return reactionType.equals("LIKE", ignoreCase = true)
    }
}

/**
 * Extension function to convert HashMap to Reaction object
 */
fun HashMap<String, Any>.toReaction(): Reaction {
    return Reaction(
        id = this["id"] as? String,
        userId = this["user_id"] as? String ?: "",
        targetId = this["target_id"] as? String ?: "",
        targetType = this["target_type"] as? String ?: "post",
        reactionType = this["reaction_type"] as? String ?: "LIKE",
        createdAt = this["created_at"] as? String,
        updatedAt = this["updated_at"] as? String
    )
}
