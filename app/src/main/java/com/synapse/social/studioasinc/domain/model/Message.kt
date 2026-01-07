package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Custom serializer that handles both ISO 8601 strings and Long timestamps
 * Uses JsonElement to properly handle the JSON value regardless of its type
 */
object FlexibleTimestampSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleTimestamp", PrimitiveKind.LONG)
    
    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeLong() // Fallback for non-JSON decoders
        
        val element = jsonDecoder.decodeJsonElement().jsonPrimitive
        
        // Try to get as Long first (for numeric values)
        element.longOrNull?.let { return it }
        
        // Otherwise parse as string (ISO 8601 format)
        val stringValue = element.content
        return try {
            Instant.parse(stringValue).toEpochMilli()
        } catch (e: DateTimeParseException) {
            // Try parsing with offset format like "2025-12-13T16:18:31.301132+00:00"
            try {
                java.time.OffsetDateTime.parse(stringValue).toInstant().toEpochMilli()
            } catch (e2: Exception) {
                // Try without the timezone suffix
                try {
                    stringValue.toLong()
                } catch (e3: NumberFormatException) {
                    0L // Last resort fallback
                }
            }
        }
    }
    
    override fun serialize(encoder: Encoder, value: Long) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            // Serialize as ISO 8601 string for database compatibility
            jsonEncoder.encodeJsonElement(JsonPrimitive(Instant.ofEpochMilli(value).toString()))
        } else {
            encoder.encodeLong(value)
        }
    }
}

/**
 * Message model for Supabase
 */
@Serializable
data class Message(
    val id: String = "",
    @SerialName("chat_id")
    val chatId: String = "",
    @SerialName("sender_id")
    val senderId: String = "",
    val content: String = "",
    @SerialName("message_type")
    val messageType: String = "text", // text, image, video, audio, file
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @Serializable(with = FlexibleTimestampSerializer::class)
    @SerialName("created_at")
    val createdAt: Long = 0L,
    @Serializable(with = FlexibleTimestampSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Long = 0L,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("is_edited")
    val isEdited: Boolean = false,
    @SerialName("reply_to_id")
    val replyToId: String? = null,
    
    // Message actions fields
    @SerialName("edit_history")
    val editHistory: List<MessageEdit>? = null,
    @SerialName("forwarded_from_message_id")
    val forwardedFromMessageId: String? = null,
    @SerialName("forwarded_from_chat_id")
    val forwardedFromChatId: String? = null,
    @SerialName("delete_for_everyone")
    val deleteForEveryone: Boolean = false,
    
    // Attachments field (JSONB array in database)
    val attachments: List<ChatAttachmentImpl>? = null,
    
    // UI-related properties (not serialized from database)
    var senderName: String? = null,
    var senderAvatarUrl: String? = null,
    var isFromCurrentUser: Boolean = false,
    
    // Delivery status - serialized from database
    @SerialName("delivery_status")
    val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    
    var timestamp: String? = null
) {
    /**
     * Checks if this is a media message
     */
    fun isMediaMessage(): Boolean {
        return messageType in listOf("image", "video", "audio", "file")
    }
    
    /**
     * Gets the display content for the message
     */
    fun getDisplayContent(): String {
        return when {
            isDeleted -> "This message was deleted"
            messageType == "image" -> "📷 Image"
            messageType == "video" -> "🎥 Video"
            messageType == "audio" -> "🎵 Audio"
            messageType == "file" -> "📎 File"
            else -> content
        }
    }
    
    /**
     * Checks if message is deleted for a specific user
     * Returns true if deleted for everyone OR if user has deleted it for themselves
     * 
     * Requirements: 1.2, 2.2
     */
    fun isDeletedForUser(userId: String, userDeletedMessageIds: Set<String>): Boolean {
        return when {
            deleteForEveryone && isDeleted -> true
            userDeletedMessageIds.contains(id) -> true
            else -> false
        }
    }
    
    /**
     * Gets the appropriate deleted message placeholder text
     * Returns "This message was deleted" for messages deleted for everyone
     * Returns "You deleted this message" for messages deleted by current user only
     * 
     * Requirements: 1.2, 2.2
     */
    fun getDeletedMessageText(userId: String, userDeletedMessageIds: Set<String>): String {
        return when {
            deleteForEveryone && isDeleted -> "This message was deleted"
            userDeletedMessageIds.contains(id) -> "You deleted this message"
            else -> content
        }
    }
}

/**
 * Message delivery status
 * Values match the lowercase strings stored in the database delivery_status column
 */
@Serializable
enum class MessageDeliveryStatus {
    @SerialName("sending")
    SENDING,
    @SerialName("sent")
    SENT,
    @SerialName("delivered")
    DELIVERED,
    @SerialName("read")
    READ,
    @SerialName("failed")
    FAILED
}

/**
 * Message edit history entry
 */
@Serializable
data class MessageEdit(
    @SerialName("edited_at")
    val editedAt: Long,
    @SerialName("previous_content")
    val previousContent: String,
    @SerialName("edited_by")
    val editedBy: String
)

/**
 * Extension function to convert HashMap to Message object
 */
fun HashMap<String, Any>.toMessage(): Message {
    return Message(
        id = this["id"] as? String ?: "",
        chatId = this["chat_id"] as? String ?: "",
        senderId = this["sender_id"] as? String ?: "",
        content = this["content"] as? String ?: "",
        messageType = this["message_type"] as? String ?: "text",
        mediaUrl = this["media_url"] as? String,
        createdAt = (this["created_at"] as? String)?.toLongOrNull() ?: 0L,
        updatedAt = (this["updated_at"] as? String)?.toLongOrNull() ?: 0L,
        isDeleted = (this["is_deleted"] as? String) == "true",
        isEdited = (this["is_edited"] as? String) == "true",
        replyToId = this["reply_to_id"] as? String,
        forwardedFromMessageId = this["forwarded_from_message_id"] as? String,
        forwardedFromChatId = this["forwarded_from_chat_id"] as? String,
        deleteForEveryone = (this["delete_for_everyone"] as? String) == "true"
    )
}

/**
 * Extension function to convert Message to HashMap for database operations
 */
fun Message.toHashMap(): HashMap<String, Any?> {
    return hashMapOf(
        "id" to id,
        "chat_id" to chatId,
        "sender_id" to senderId,
        "content" to content,
        "message_type" to messageType,
        "media_url" to mediaUrl,
        "created_at" to createdAt.toString(),
        "updated_at" to updatedAt.toString(),
        "is_deleted" to isDeleted.toString(),
        "is_edited" to isEdited.toString(),
        "reply_to_id" to replyToId,
        "forwarded_from_message_id" to forwardedFromMessageId,
        "forwarded_from_chat_id" to forwardedFromChatId,
        "delete_for_everyone" to deleteForEveryone.toString()
    )
}
