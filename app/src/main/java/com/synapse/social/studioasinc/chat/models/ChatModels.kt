package com.synapse.social.studioasinc.chat.models

import com.synapse.social.studioasinc.chat.interfaces.*
import kotlinx.serialization.Serializable

/**
 * Chat Message Implementation
 */
@Serializable
data class ChatMessageImpl(
    override val id: String,
    override val chatId: String,
    override val senderId: String,
    override val receiverId: String? = null,
    override val messageText: String? = null,
    override val messageType: String,
    override val messageState: String,
    override val pushDate: Long,
    override val deliveredAt: Long? = null,
    override val readAt: Long? = null,
    override val repliedMessageId: String? = null,
    override val attachments: List<ChatAttachment>? = null,
    override val isEdited: Boolean = false,
    override val editedAt: Long? = null
) : ChatMessage

/**
 * Chat Attachment Implementation
 */
@Serializable
data class ChatAttachmentImpl(
    override val id: String,
    override val url: String,
    override val type: String,
    override val fileName: String? = null,
    override val fileSize: Long? = null,
    override val thumbnailUrl: String? = null,
    override val width: Int? = null,
    override val height: Int? = null,
    override val duration: Long? = null,
    override val mimeType: String? = null
) : ChatAttachment

/**
 * Chat Room Implementation
 */
@Serializable
data class ChatRoomImpl(
    override val id: String,
    override val participants: List<String>,
    override val isGroup: Boolean = false,
    override val groupName: String? = null,
    override val groupAvatar: String? = null,
    override val createdAt: Long,
    override val lastMessageId: String? = null,
    override val lastMessageText: String? = null,
    override val lastMessageTime: Long? = null,
    override val lastMessageSenderId: String? = null,
    override val unreadCount: Int = 0
) : ChatRoom

/**
 * Message States
 */
object MessageState {
    const val SENDING = "sending"
    const val SENT = "sent"
    const val DELIVERED = "delivered"
    const val READ = "read"
    const val FAILED = "failed"
}

/**
 * Message Types
 */
object MessageType {
    const val TEXT = "MESSAGE"
    const val ATTACHMENT = "ATTACHMENT_MESSAGE"
    const val VOICE = "VOICE_MESSAGE"
    const val SYSTEM = "SYSTEM_MESSAGE"
}

/**
 * Attachment Types
 */
object AttachmentType {
    const val IMAGE = "image"
    const val VIDEO = "video"
    const val AUDIO = "audio"
    const val DOCUMENT = "document"
}

/**
 * Typing Status
 */
@Serializable
data class TypingStatus(
    val userId: String,
    val chatId: String,
    val isTyping: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Read Receipt Event
 */
@Serializable
data class ReadReceiptEvent(
    val chatId: String,
    val userId: String,
    val messageIds: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Chat Preferences
 */
@Serializable
data class ChatPreferences(
    val sendReadReceipts: Boolean = true,
    val showTypingIndicators: Boolean = true
)

/**
 * User Status
 */
@Serializable
data class UserStatus(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: Long,
    val status: String? = null
)
