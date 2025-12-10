package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * MessageForward model for tracking message forwarding relationships
 */
@Serializable
data class MessageForward(
    val id: String = "",
    @SerialName("original_message_id")
    val originalMessageId: String = "",
    @SerialName("original_chat_id")
    val originalChatId: String = "",
    @SerialName("forwarded_message_id")
    val forwardedMessageId: String = "",
    @SerialName("forwarded_chat_id")
    val forwardedChatId: String = "",
    @SerialName("forwarded_by")
    val forwardedBy: String = "",
    @SerialName("forwarded_at")
    val forwardedAt: Long = 0L
)

/**
 * Extension function to convert HashMap to MessageForward object
 */
fun HashMap<String, Any>.toMessageForward(): MessageForward {
    return MessageForward(
        id = this["id"] as? String ?: "",
        originalMessageId = this["original_message_id"] as? String ?: "",
        originalChatId = this["original_chat_id"] as? String ?: "",
        forwardedMessageId = this["forwarded_message_id"] as? String ?: "",
        forwardedChatId = this["forwarded_chat_id"] as? String ?: "",
        forwardedBy = this["forwarded_by"] as? String ?: "",
        forwardedAt = (this["forwarded_at"] as? String)?.toLongOrNull() ?: 0L
    )
}

/**
 * Extension function to convert MessageForward to HashMap for database operations
 */
fun MessageForward.toHashMap(): HashMap<String, Any?> {
    return hashMapOf(
        "id" to id,
        "original_message_id" to originalMessageId,
        "original_chat_id" to originalChatId,
        "forwarded_message_id" to forwardedMessageId,
        "forwarded_chat_id" to forwardedChatId,
        "forwarded_by" to forwardedBy,
        "forwarded_at" to forwardedAt.toString()
    )
}
