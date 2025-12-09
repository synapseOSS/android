package com.synapse.social.studioasinc.chat.service

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.chat.interfaces.*
import com.synapse.social.studioasinc.chat.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Supabase Chat Service
 * Handles all chat-related database operations using Supabase
 */
class SupabaseChatService(private val databaseService: SupabaseDatabaseService) {

    /**
     * Send a new message
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        receiverId: String?,
        messageText: String?,
        messageType: String,
        repliedMessageId: String? = null,
        attachments: List<ChatAttachment>? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val messageId = UUID.randomUUID().toString()
                val currentTime = System.currentTimeMillis()
                
                val messageData = mapOf(
                    "id" to messageId,
                    "chat_id" to chatId,
                    "sender_id" to senderId,
                    "receiver_id" to receiverId,
                    "message_text" to messageText,
                    "message_type" to messageType,
                    "message_state" to MessageState.SENT,
                    "delivered_at" to null,  // Initially null
                    "read_at" to null,       // Initially null
                    "push_date" to currentTime,
                    "replied_message_id" to repliedMessageId,
                    "attachments" to attachments?.map { attachment ->
                        mapOf(
                            "id" to attachment.id,
                            "url" to attachment.url,
                            "type" to attachment.type,
                            "file_name" to attachment.fileName,
                            "file_size" to attachment.fileSize,
                            "thumbnail_url" to attachment.thumbnailUrl
                        )
                    },
                    "is_edited" to false,
                    "created_at" to currentTime,
                    "updated_at" to currentTime
                )
                
                val result = databaseService.insert("messages", messageData)
                
                result.fold(
                    onSuccess = {
                        // Update chat room with last message info
                        updateChatLastMessage(chatId, messageId, messageText ?: "", senderId, currentTime)
                        
                        // Update user chats
                        updateUserChats(chatId, senderId, receiverId, messageText ?: "", currentTime)
                        
                        Result.success(messageId)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get messages for a chat
     */
    suspend fun getMessages(
        chatId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ChatMessage>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = databaseService.select(
                    "messages", 
                    "*"
                )
                
                result.fold(
                    onSuccess = { messages ->
                        val filteredMessages = messages
                            .filter { it["chat_id"] == chatId }
                            .sortedByDescending { it["push_date"]?.toString()?.toLongOrNull() ?: 0L }
                            .drop(offset)
                            .take(limit)
                        
                        val chatMessages = filteredMessages.map { messageData ->
                            ChatMessageImpl(
                                id = messageData["id"]?.toString() ?: "",
                                chatId = messageData["chat_id"]?.toString() ?: "",
                                senderId = messageData["sender_id"]?.toString() ?: "",
                                receiverId = messageData["receiver_id"]?.toString(),
                                messageText = messageData["message_text"]?.toString(),
                                messageType = messageData["message_type"]?.toString() ?: MessageType.TEXT,
                                messageState = messageData["message_state"]?.toString() ?: MessageState.SENT,
                                pushDate = messageData["push_date"]?.toString()?.toLongOrNull() ?: 0L,
                                repliedMessageId = messageData["replied_message_id"]?.toString(),
                                attachments = parseAttachments(messageData["attachments"]),
                                isEdited = messageData["is_edited"]?.toString()?.toBooleanStrictOrNull() ?: false,
                                editedAt = messageData["edited_at"]?.toString()?.toLongOrNull()
                            )
                        }
                        
                        Result.success(chatMessages)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get or create a chat room between two users
     */
    suspend fun getOrCreateChatRoom(
        user1Id: String,
        user2Id: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if chat already exists
                val existingChatResult = databaseService.select("chat_rooms", "*")
                
                existingChatResult.fold(
                    onSuccess = { chatRooms ->
                        val existingChat = chatRooms.find { chatRoom ->
                            val participants = parseParticipants(chatRoom["participants"])
                            participants.contains(user1Id) && participants.contains(user2Id) && participants.size == 2
                        }
                        
                        if (existingChat != null) {
                            Result.success(existingChat["id"]?.toString() ?: "")
                        } else {
                            // Create new chat room
                            createChatRoom(listOf(user1Id, user2Id), false)
                        }
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Create a new chat room
     */
    suspend fun createChatRoom(
        participants: List<String>,
        isGroup: Boolean,
        groupName: String? = null,
        groupAvatar: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val chatId = UUID.randomUUID().toString()
                val currentTime = System.currentTimeMillis()
                
                val chatRoomData = mapOf(
                    "id" to chatId,
                    "participants" to participants,
                    "is_group" to isGroup,
                    "group_name" to groupName,
                    "group_avatar" to groupAvatar,
                    "created_at" to currentTime,
                    "updated_at" to currentTime
                )
                
                val result = databaseService.insert("chat_rooms", chatRoomData)
                
                result.fold(
                    onSuccess = {
                        // Create user_chats entries for each participant
                        participants.forEach { participantId ->
                            val userChatData = kotlinx.serialization.json.buildJsonObject {
                                put("user_id", kotlinx.serialization.json.JsonPrimitive(participantId))
                                put("chat_id", kotlinx.serialization.json.JsonPrimitive(chatId))
                                put("joined_at", kotlinx.serialization.json.JsonPrimitive(currentTime))
                                put("last_read_message_id", kotlinx.serialization.json.JsonNull)
                                put("unread_count", kotlinx.serialization.json.JsonPrimitive(0))
                            }
                            databaseService.insert("user_chats", userChatData)
                        }
                        
                        Result.success(chatId)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get user's chat list
     */
    suspend fun getUserChats(userId: String): Result<List<ChatRoom>> {
        return withContext(Dispatchers.IO) {
            try {
                val userChatsResult = databaseService.selectWhere("user_chats", "*", "user_id", userId)
                
                userChatsResult.fold(
                    onSuccess = { userChats ->
                        val chatIds = userChats.mapNotNull { it["chat_id"]?.toString() }
                        
                        if (chatIds.isEmpty()) {
                            return@fold Result.success(emptyList<ChatRoom>())
                        }
                        
                        val chatRoomsResult = databaseService.select("chat_rooms", "*")
                        
                        chatRoomsResult.fold(
                            onSuccess = { allChatRooms ->
                                val userChatRooms = allChatRooms
                                    .filter { chatIds.contains(it["id"]?.toString()) }
                                    .map { chatRoomData ->
                                        val userChat = userChats.find { it["chat_id"] == chatRoomData["id"] }
                                        
                                        ChatRoomImpl(
                                            id = chatRoomData["id"]?.toString() ?: "",
                                            participants = parseParticipants(chatRoomData["participants"]),
                                            isGroup = chatRoomData["is_group"]?.toString()?.toBooleanStrictOrNull() ?: false,
                                            groupName = chatRoomData["group_name"]?.toString(),
                                            groupAvatar = chatRoomData["group_avatar"]?.toString(),
                                            createdAt = chatRoomData["created_at"]?.toString()?.toLongOrNull() ?: 0L,
                                            lastMessageId = chatRoomData["last_message_id"]?.toString(),
                                            lastMessageText = chatRoomData["last_message_text"]?.toString(),
                                            lastMessageTime = chatRoomData["last_message_time"]?.toString()?.toLongOrNull(),
                                            lastMessageSenderId = chatRoomData["last_message_sender_id"]?.toString(),
                                            unreadCount = userChat?.get("unread_count")?.toString()?.toIntOrNull() ?: 0
                                        )
                                    }
                                    .sortedByDescending { it.lastMessageTime ?: 0L }
                                
                                Result.success(userChatRooms)
                            },
                            onFailure = { error ->
                                Result.failure(error)
                            }
                        )
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Mark messages as read with batching support.
     * Updates message states to READ, sets read_at timestamp.
     * 
     * @param chatId The chat room identifier
     * @param userId The user marking messages as read
     * @param messageIds List of message IDs to mark as read
     * @param realtimeService Optional SupabaseRealtimeService for broadcasting read receipts
     */
    suspend fun markMessagesAsRead(
        chatId: String, 
        userId: String, 
        messageIds: List<String>,
        realtimeService: SupabaseRealtimeService? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                
                if (messageIds.isEmpty()) {
                    return@withContext Result.success(Unit)
                }
                
                // Batch update all messages in a single operation
                messageIds.forEach { messageId ->
                    val updateData = mapOf(
                        "message_state" to MessageState.READ,
                        "read_at" to timestamp,
                        "updated_at" to timestamp
                    )
                    databaseService.update("messages", updateData, "id", messageId)
                }
                
                // Reset unread count for user
                val userChatUpdate = mapOf(
                    "unread_count" to 0,
                    "last_read_at" to timestamp
                )
                
                // Find and update user_chat record
                val userChatsResult = databaseService.selectWhere("user_chats", "*", "user_id", userId)
                userChatsResult.fold(
                    onSuccess = { userChats ->
                        val userChat = userChats.find { it["chat_id"] == chatId }
                        if (userChat != null) {
                            databaseService.update("user_chats", userChatUpdate, "user_id", userId)
                        }
                    },
                    onFailure = { }
                )
                
                // Broadcast read receipt event via Realtime if service is provided
                realtimeService?.let { service ->
                    try {
                        service.broadcastReadReceipt(chatId, userId, messageIds)
                    } catch (e: Exception) {
                        // Don't fail the operation if broadcast fails
                    }
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Update typing status
     */
    suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val typingData = mapOf(
                    "chat_id" to chatId,
                    "user_id" to userId,
                    "is_typing" to isTyping,
                    "timestamp" to System.currentTimeMillis()
                )
                
                val result = databaseService.upsert("typing_status", typingData)
                result
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Update message delivery state to DELIVERED.
     * Sets delivered_at timestamp and broadcasts delivery event via Realtime.
     * 
     * @param messageId The message identifier
     * @param chatId The chat room identifier
     * @param userId The user who received the message
     * @param realtimeService Optional SupabaseRealtimeService for broadcasting delivery events
     */
    suspend fun updateMessageDeliveryState(
        messageId: String,
        chatId: String,
        userId: String,
        realtimeService: SupabaseRealtimeService? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                
                // Update message state to DELIVERED
                val updateData = mapOf(
                    "message_state" to MessageState.DELIVERED,
                    "delivered_at" to timestamp,
                    "updated_at" to timestamp
                )
                
                val result = databaseService.update("messages", updateData, "id", messageId)
                
                result.fold(
                    onSuccess = {
                        // Broadcast delivery event via Realtime if service is provided
                        realtimeService?.let { service ->
                            try {
                                service.broadcastReadReceipt(chatId, userId, listOf(messageId))
                            } catch (e: Exception) {
                                // Don't fail the operation if broadcast fails
                            }
                        }
                        Result.success(Unit)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Clear chat messages for a specific user
     * This removes all messages from the chat for the requesting user
     */
    suspend fun clearChatForUser(chatId: String, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Delete all messages in the chat
                val result = databaseService.deleteWhere("messages", "chat_id", chatId)
                
                result.fold(
                    onSuccess = {
                        // Update chat room to clear last message info
                        val updateData = mapOf(
                            "last_message_id" to null,
                            "last_message_text" to null,
                            "last_message_time" to null,
                            "last_message_sender_id" to null,
                            "updated_at" to System.currentTimeMillis()
                        )
                        databaseService.update("chat_rooms", updateData, "id", chatId)
                        
                        // Reset unread count for user
                        val userChatUpdate = mapOf(
                            "unread_count" to 0,
                            "last_read_at" to System.currentTimeMillis()
                        )
                        
                        // Find and update user_chat record
                        val userChatsResult = databaseService.selectWhere("user_chats", "*", "user_id", userId)
                        userChatsResult.fold(
                            onSuccess = { userChats ->
                                val userChat = userChats.find { it["chat_id"] == chatId }
                                if (userChat != null) {
                                    databaseService.update("user_chats", userChatUpdate, "user_id", userId)
                                }
                            },
                            onFailure = { }
                        )
                        
                        Result.success(Unit)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Helper methods
    private suspend fun updateChatLastMessage(
        chatId: String,
        messageId: String,
        messageText: String,
        senderId: String,
        timestamp: Long
    ) {
        val updateData = mapOf(
            "last_message_id" to messageId,
            "last_message_text" to messageText,
            "last_message_time" to timestamp,
            "last_message_sender_id" to senderId,
            "updated_at" to timestamp
        )
        
        databaseService.update("chat_rooms", updateData, "id", chatId)
    }

    private suspend fun updateUserChats(
        chatId: String,
        senderId: String,
        receiverId: String?,
        messageText: String,
        timestamp: Long
    ) {
        // Update unread count for receiver
        if (receiverId != null) {
            val userChatsResult = databaseService.selectWhere("user_chats", "*", "user_id", receiverId)
            userChatsResult.fold(
                onSuccess = { userChats ->
                    val userChat = userChats.find { it["chat_id"] == chatId }
                    if (userChat != null) {
                        val currentUnreadCount = userChat["unread_count"]?.toString()?.toIntOrNull() ?: 0
                        val updateData = mapOf(
                            "unread_count" to (currentUnreadCount + 1),
                            "last_message_text" to messageText,
                            "last_message_time" to timestamp,
                            "updated_at" to timestamp
                        )
                        databaseService.update("user_chats", updateData, "user_id", receiverId)
                    }
                },
                onFailure = { }
            )
        }
    }

    private fun parseAttachments(attachmentsData: Any?): List<ChatAttachment>? {
        // Parse attachments from database format
        // This would depend on how attachments are stored in the database
        return null // Placeholder implementation
    }

    private fun parseParticipants(participantsData: Any?): List<String> {
        return when (participantsData) {
            is List<*> -> participantsData.mapNotNull { it?.toString() }
            is String -> {
                try {
                    // If stored as JSON string, parse it
                    participantsData.split(",").map { it.trim() }
                } catch (e: Exception) {
                    listOf(participantsData)
                }
            }
            else -> emptyList()
        }
    }
}
