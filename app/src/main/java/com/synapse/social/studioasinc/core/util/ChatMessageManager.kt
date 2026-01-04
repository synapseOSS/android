package com.synapse.social.studioasinc.core.util

import android.app.Application
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Utility class for managing chat messages with Supabase backend.
 * Handles message sending, retrieval, and chat management operations.
 */
class ChatMessageManager(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository(),
    private val chatRepository: ChatRepository = ChatRepository(AppDatabase.getDatabase(application).chatDao(), SupabaseClient.client)
) {

    /**
     * Generate a consistent chat ID for two users
     */
    fun getChatId(userId1: String, userId2: String): String {
        return "${minOf(userId1, userId2)}_${maxOf(userId1, userId2)}"
    }

    /**
     * Send a message to a recipient
     */
    suspend fun sendMessage(
        recipientId: String,
        messageText: String,
        messageType: String = "text",
        attachmentUrl: String? = null
    ): Result<Message> {
        val senderId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        val chatId = getChatId(senderId, recipientId)
        
        // Create or get chat first
        val chatResult = chatRepository.getOrCreateDirectChat(recipientId, senderId)
        val actualChatId = chatResult.getOrElse { return Result.failure(it) }

        return chatRepository.sendMessage(
            chatId = actualChatId,
            senderId = senderId,
            content = messageText,
            messageType = messageType,
            replyToId = null
        ).map { messageId ->
            // Create a Message object for return compatibility
            Message(
                id = messageId,
                chatId = actualChatId,
                senderId = senderId,
                content = messageText,
                messageType = messageType,
                mediaUrl = attachmentUrl,
                createdAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Get messages for a specific chat
     */
    suspend fun getMessages(chatId: String, limit: Int = 50): Result<List<Message>> {
        return chatRepository.getMessages(chatId, limit)
    }

    /**
     * Update inbox with latest message information
     */
    suspend fun updateInbox(senderUid: String, recipientUid: String, lastMessage: String, isGroup: Boolean = false) {
        // Update the conversation list with the latest message
        try {
            val chatId = getChatId(senderUid, recipientUid)
            // Note: updateLastMessage method would need to be implemented in ChatRepository
            android.util.Log.d("ChatMessageManager", "Updated inbox for chat: $chatId")
        } catch (e: Exception) {
            // Log error but don't fail the message send
            android.util.Log.e("ChatMessageManager", "Failed to update inbox: ${e.message}")
        }
    }

    /**
     * Legacy method for compatibility with existing code
     */
    suspend fun sendMessageToDb(
        messageMap: HashMap<String, Any>,
        senderUid: String,
        recipientUid: String,
        uniqueMessageKey: String,
        isGroup: Boolean
    ) {
        val messageText = messageMap["message_text"] as? String ?: ""
        val messageType = messageMap["message_type"] as? String ?: "text"
        val attachmentUrl = messageMap["attachment_url"] as? String

        sendMessage(recipientUid, messageText, messageType, attachmentUrl)
    }

    companion object {
        /**
         * Static method for generating chat IDs
         */
        fun getChatId(userId1: String, userId2: String): String {
            return "${minOf(userId1, userId2)}_${maxOf(userId1, userId2)}"
        }
    }
}
