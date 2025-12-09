package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.runBlocking

/**
 * Chat utility functions for Supabase
 */

object ChatUtils {
    private val authService = SupabaseAuthenticationService()
    private val dbService = SupabaseDatabaseService()
    
    /**
     * Generates a chat ID from two user IDs
     */
    fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }
    
    /**
     * Sends a message to the database
     */
    suspend fun sendMessageToDb(
        chatId: String,
        senderId: String,
        messageText: String,
        messageType: String = "text",
        attachmentUrl: String? = null,
        replyToMessageId: String? = null
    ): String {
        val messageKey = generateMessageKey()
        val messageData = mapOf(
            "message_key" to messageKey,
            "chat_id" to chatId,
            "sender_id" to senderId,
            "message_text" to messageText,
            "message_type" to messageType,
            "attachment_url" to attachmentUrl,
            "reply_to_message_id" to replyToMessageId,
            "push_date" to System.currentTimeMillis().toString()
        )
        
        dbService.insert("messages", messageData)
        return messageKey
    }
    
    /**
     * Updates inbox for both participants
     */
    suspend fun updateInbox(
        chatId: String,
        participant1: String,
        participant2: String,
        lastMessageId: String
    ) {
        // Update inbox for participant 1
        dbService.upsert("inbox", mapOf(
            "user_id" to participant1,
            "chat_partner_id" to participant2,
            "last_message_id" to lastMessageId,
            "updated_at" to System.currentTimeMillis().toString()
        ))
        
        // Update inbox for participant 2
        dbService.upsert("inbox", mapOf(
            "user_id" to participant2,
            "chat_partner_id" to participant1,
            "last_message_id" to lastMessageId,
            "updated_at" to System.currentTimeMillis().toString()
        ))
    }
    
    private fun generateMessageKey(): String {
        return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

/**
 * Post utility functions
 */
object PostUtils {
    private val dbService = SupabaseDatabaseService()
    
    /**
     * Determines post type based on content
     */
    fun determinePostType(content: String?, imageUrl: String?, videoUrl: String?): String {
        return when {
            !videoUrl.isNullOrEmpty() -> "video"
            !imageUrl.isNullOrEmpty() -> "image"
            !content.isNullOrEmpty() -> "text"
            else -> "text"
        }
    }
}

/**
 * Extension function to get chat ID from user IDs
 */
fun getChatId(uid1: String, uid2: String): String {
    return ChatUtils.getChatId(uid1, uid2)
}

/**
 * Extension function to send message to database
 */
suspend fun sendMessageToDb(
    chatId: String,
    senderId: String,
    messageText: String,
    messageType: String = "text",
    attachmentUrl: String? = null,
    replyToMessageId: String? = null
): String {
    return ChatUtils.sendMessageToDb(chatId, senderId, messageText, messageType, attachmentUrl, replyToMessageId)
}

/**
 * Extension function to update inbox
 */
suspend fun updateInbox(
    chatId: String,
    participant1: String,
    participant2: String,
    lastMessageId: String
) {
    ChatUtils.updateInbox(chatId, participant1, participant2, lastMessageId)
}
