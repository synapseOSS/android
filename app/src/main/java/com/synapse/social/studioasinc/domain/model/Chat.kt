package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Chat model for Supabase
 */
@Serializable
data class Chat(
    val id: String = "",
    val name: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_at")
    val createdAt: Long = 0L,
    @SerialName("updated_at")
    val updatedAt: Long = 0L,
    @SerialName("is_group")
    val isGroup: Boolean = false,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("participant_count")
    val participantCount: Int = 0,
    @SerialName("last_message")
    val lastMessage: String? = null,
    @SerialName("last_message_time")
    val lastMessageTime: Long? = null,
    @SerialName("last_message_sender")
    val lastMessageSender: String? = null,
    
    // UI-related properties
    var chatDisplayName: String? = null,
    var avatarUrl: String? = null,
    var unreadCount: Int = 0,
    var isOnline: Boolean = false
) {
    /**
     * Gets the display name for the chat
     */
    fun getDisplayName(): String {
        return chatDisplayName ?: name ?: "Chat"
    }
    
    /**
     * Gets formatted last message time
     */
    fun getFormattedLastMessageTime(): String {
        if (lastMessageTime == null) return ""
        
        val now = System.currentTimeMillis()
        val diff = now - lastMessageTime
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> {
                val date = java.util.Date(lastMessageTime)
                java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(date)
            }
        }
    }
}

/**
 * Extension function to convert HashMap to Chat object
 */
fun HashMap<String, Any>.toChat(): Chat {
    return Chat(
        id = this["id"] as? String ?: "",
        name = this["name"] as? String,
        createdBy = this["created_by"] as? String,
        createdAt = (this["created_at"] as? String)?.toLongOrNull() ?: 0L,
        updatedAt = (this["updated_at"] as? String)?.toLongOrNull() ?: 0L,
        isGroup = (this["is_group"] as? String) == "true",
        isActive = (this["is_active"] as? String) == "true",
        participantCount = (this["participant_count"] as? String)?.toIntOrNull() ?: 0,
        lastMessage = this["last_message"] as? String,
        lastMessageTime = (this["last_message_time"] as? String)?.toLongOrNull(),
        lastMessageSender = this["last_message_sender"] as? String
    )
}

/**
 * Extension function to convert Chat to HashMap for database operations
 */
fun Chat.toHashMap(): HashMap<String, Any?> {
    return hashMapOf(
        "id" to id,
        "name" to name,
        "created_by" to createdBy,
        "created_at" to createdAt.toString(),
        "updated_at" to updatedAt.toString(),
        "is_group" to isGroup.toString(),
        "is_active" to isActive.toString(),
        "participant_count" to participantCount.toString(),
        "last_message" to lastMessage,
        "last_message_time" to lastMessageTime?.toString(),
        "last_message_sender" to lastMessageSender
    )
}
