package com.synapse.social.studioasinc.chat.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Message Reaction Model
 * Represents a reaction (emoji) to a message
 */
@Serializable
data class MessageReaction(
    val id: String,
    @SerialName("message_id")
    val messageId: String,
    @SerialName("user_id")
    val userId: String,
    val emoji: String,
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Grouped reactions for a message
 */
data class GroupedReaction(
    val emoji: String,
    val count: Int,
    val userIds: List<String>,
    val hasCurrentUserReacted: Boolean = false
)

/**
 * Common emoji reactions
 */
object EmojiReactions {
    const val THUMBS_UP = "👍"
    const val HEART = "❤️"
    const val LAUGH = "😂"
    const val WOW = "😮"
    const val SAD = "😢"
    const val ANGRY = "😠"
    const val FIRE = "🔥"
    const val CLAP = "👏"
    
    val COMMON_REACTIONS = listOf(
        THUMBS_UP, HEART, LAUGH, WOW, SAD, ANGRY, FIRE, CLAP
    )
}
