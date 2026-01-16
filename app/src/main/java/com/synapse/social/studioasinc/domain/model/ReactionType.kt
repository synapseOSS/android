package com.synapse.social.studioasinc.domain.model

import com.synapse.social.studioasinc.R
import kotlinx.serialization.Serializable

/**
 * Enum representing different types of reactions for posts
 * Similar to Facebook's reaction system
 */
@Serializable
enum class ReactionType(
    val displayName: String,
    val emoji: String,
    val iconRes: Int
) {
    LIKE("Like", "❤️", R.drawable.ic_reaction_like),
    LOVE("Love", "😍", R.drawable.ic_reaction_love),
    HAHA("Haha", "😂", R.drawable.ic_reaction_haha),
    WOW("Wow", "😮", R.drawable.ic_reaction_wow),
    SAD("Sad", "😢", R.drawable.ic_reaction_sad),
    ANGRY("Angry", "😡", R.drawable.ic_reaction_angry);

    companion object {
        /**
         * Get ReactionType from string value, default to LIKE if not found
         */
        fun fromString(value: String?): ReactionType {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: LIKE
        }

        /**
         * Get all reaction types as a list
         */
        fun getAllReactions(): List<ReactionType> {
            return values().toList()
        }
    }
}
