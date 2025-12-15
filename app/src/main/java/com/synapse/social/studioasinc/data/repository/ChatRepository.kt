package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.data.local.ChatDao
import com.synapse.social.studioasinc.data.local.ChatEntity
import com.synapse.social.studioasinc.model.Chat
import com.synapse.social.studioasinc.model.Message
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.statement.bodyAsText
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class ChatRepository(private val chatDao: ChatDao) {

    private val chatService = SupabaseChatService()
    private val databaseService = SupabaseDatabaseService()
    private val client = SupabaseClient.client
    
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(expirationMs: Long = CACHE_EXPIRATION_MS): Boolean {
            return System.currentTimeMillis() - timestamp > expirationMs
        }
    }
    
    private val messagesCache = mutableMapOf<String, CacheEntry<List<Message>>>()
    
    companion object {
        private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    fun invalidateCache() {
        messagesCache.clear()
        android.util.Log.d("ChatRepository", "Cache invalidated")
    }
    
    private fun getCacheKey(chatId: String, beforeTimestamp: Long?, limit: Int): String {
        return "messages_chat_${chatId}_before_${beforeTimestamp}_limit_${limit}"
    }

    suspend fun createChat(participantUids: List<String>, chatName: String? = null): Result<String> {
        val result = if (participantUids.size == 2) {
            chatService.getOrCreateDirectChat(participantUids[0], participantUids[1])
        } else {
            Result.failure(Exception("Group chats not yet implemented"))
        }

        result.onSuccess { chatId ->
            refreshUserChats(participantUids[0])
        }

        return result
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        content: String,
        messageType: String = "text",
        replyToId: String? = null
    ): Result<String> {
        android.util.Log.d("ChatRepository", "=== sendMessage START ===")
        android.util.Log.d("ChatRepository", "Sending message:")
        android.util.Log.d("ChatRepository", "  - chatId: $chatId")
        android.util.Log.d("ChatRepository", "  - senderId: $senderId")
        android.util.Log.d("ChatRepository", "  - content length: ${content.length}")
        android.util.Log.d("ChatRepository", "  - messageType: $messageType")
        android.util.Log.d("ChatRepository", "  - replyToId: $replyToId")
        
        val result = chatService.sendMessage(
            chatId = chatId,
            senderId = senderId,
            content = content,
            messageType = messageType,
            replyToId = replyToId
        )
        
        result.onSuccess { messageId ->
            android.util.Log.d("ChatRepository", "✓ Message sent successfully, messageId: $messageId")
            refreshUserChats(senderId)
            
            // Check for Syra mentions and trigger AI response
            processMentions(chatId, content, senderId, messageType)
        }.onFailure { error ->
            android.util.Log.e("ChatRepository", "✗ Failed to send message: ${error.message}", error)
        }
        
        android.util.Log.d("ChatRepository", "=== sendMessage END ===")
        return result
    }
    
    private suspend fun processMentions(
        chatId: String,
        messageText: String,
        senderId: String,
        mentionType: String
    ) {
        try {
            // Extract mentions from the message
            val mentionedUsers = com.synapse.social.studioasinc.util.MentionParser.extractMentions(messageText)
            
            if (mentionedUsers.contains("syra")) {
                android.util.Log.d("ChatRepository", "Syra mentioned - calling mention handler")
                
                // Call the syra-mention-handler function
                val mentionRequest = mapOf(
                    "chatId" to chatId,
                    "messageText" to messageText,
                    "mentionedUsers" to mentionedUsers,
                    "senderId" to senderId,
                    "mentionType" to "chat"
                )
                
                val response = client.functions.invoke(
                    function = "syra-mention-handler",
                    body = mentionRequest
                )
                
                android.util.Log.d("ChatRepository", "Syra mention handler response: ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Failed to process mentions: ${e.message}", e)
        }
    }

    suspend fun getMessages(
        chatId: String, 
        limit: Int = 50, 
        beforeTimestamp: Long? = null
    ): Result<List<Message>> {
        return try {
            val result = chatService.getMessages(chatId, limit, beforeTimestamp)
            result.map { messagesList ->
                messagesList.map { messageData ->
                    mapToMessage(messageData)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getParticipantsForChats(chatIds: List<String>): Result<Map<String, List<String>>> {
        return chatService.getParticipantsForChats(chatIds)
    }
    
    suspend fun getMessagesPage(
        chatId: String,
        beforeTimestamp: Long? = null,
        limit: Int = 50
    ): Result<List<Message>> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("ChatRepository", "=== getMessagesPage START ===")
            android.util.Log.d("ChatRepository", "Parameters: chatId=$chatId, beforeTimestamp=$beforeTimestamp, limit=$limit")
            
            val cacheKey = getCacheKey(chatId, beforeTimestamp, limit)
            val cachedEntry = messagesCache[cacheKey]
            
            if (cachedEntry != null && !cachedEntry.isExpired()) {
                android.util.Log.d("ChatRepository", "✓ Cache HIT - Returning ${cachedEntry.data.size} cached messages")
                android.util.Log.d("ChatRepository", "=== getMessagesPage END (cached) ===")
                return@withContext Result.success(cachedEntry.data)
            }
            
            android.util.Log.d("ChatRepository", "✗ Cache MISS - Fetching from database")
            android.util.Log.d("ChatRepository", "Query details:")
            android.util.Log.d("ChatRepository", "  - Table: messages")
            android.util.Log.d("ChatRepository", "  - Filter: chat_id = $chatId")
            if (beforeTimestamp != null) {
                android.util.Log.d("ChatRepository", "  - Filter: created_at < $beforeTimestamp")
            }
            android.util.Log.d("ChatRepository", "  - Limit: ${if (limit == Int.MAX_VALUE) "ALL (Int.MAX_VALUE)" else limit}")
            android.util.Log.d("ChatRepository", "  - Order: created_at DESC")
            
            val messages = client.from("messages")
                .select() {
                    filter {
                        eq("chat_id", chatId)
                        beforeTimestamp?.let { 
                            lt("created_at", it) 
                        }
                    }
                    if (limit < Int.MAX_VALUE) {
                        limit(limit.toLong())
                        android.util.Log.d("ChatRepository", "Applied limit: $limit")
                    } else {
                        android.util.Log.d("ChatRepository", "No limit applied - fetching ALL messages")
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<Message>()
            
            android.util.Log.d("ChatRepository", "✓ Query successful - Received ${messages.size} messages")
            
            if (messages.isNotEmpty()) {
                android.util.Log.d("ChatRepository", "First message: id=${messages.first().id}, content=${messages.first().content.take(30)}, createdAt=${messages.first().createdAt}")
                android.util.Log.d("ChatRepository", "Last message: id=${messages.last().id}, content=${messages.last().content.take(30)}, createdAt=${messages.last().createdAt}")
            }
            
            messagesCache[cacheKey] = CacheEntry(messages)
            android.util.Log.d("ChatRepository", "Messages cached with key: $cacheKey")
            
            android.util.Log.d("ChatRepository", "=== getMessagesPage END (success) ===")
            Result.success(messages)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "=== getMessagesPage FAILED ===")
            android.util.Log.e("ChatRepository", "Failed to fetch messages page: ${e.message}", e)
            
            val errorMessage = when {
                e.message?.contains("relation \"messages\" does not exist", ignoreCase = true) == true -> 
                    "Database table 'messages' does not exist. Please create the messages table in your Supabase database."
                e.message?.contains("connection", ignoreCase = true) == true -> 
                    "Cannot connect to Supabase. Check your internet connection and Supabase configuration."
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    "Request timed out. Please check your internet connection and try again."
                e.message?.contains("unauthorized", ignoreCase = true) == true -> 
                    "Unauthorized access to messages. Check your API key and RLS policies."
                e.message?.contains("serialization", ignoreCase = true) == true -> 
                    "Data format error. The database schema might not match the expected format."
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Network error occurred. Please check your internet connection."
                else -> "Failed to load messages: ${e.message ?: "Unknown error"}"
            }
            
            Result.failure(Exception(errorMessage))
        }
    }

    fun getUserChats(): Flow<Result<List<Chat>>> {
        return chatDao.getAllChats().map<List<ChatEntity>, Result<List<Chat>>> { entities ->
            Result.success(entities.map { ChatMapper.toModel(it) })
        }.catch { e ->
            emit(Result.failure(Exception("Error getting chats from database: ${e.message}")))
        }
    }

    suspend fun refreshUserChats(userId: String): Result<Unit> {
        return try {
            val result = chatService.getUserChats(userId)
            result.getOrNull()?.let { chats ->
                chatDao.insertAll(chats.map { ChatMapper.toEntity(mapToChat(it)) })
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Failed to refresh user chats: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return chatService.deleteMessage(messageId)
    }

    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        return try {
            val updateData = mapOf(
                "content" to newContent,
                "is_edited" to true,
                "edited_at" to System.currentTimeMillis()
            )
            databaseService.update("messages", updateData, "id", messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatParticipants(chatId: String): Result<List<String>> {
        return try {
            val participants = client.from("chat_participants")
                .select(columns = Columns.raw("user_id")) {
                    filter { eq("chat_id", chatId) }
                }
                .decodeList<JsonObject>()
            
            val userIds = participants.map { 
                it["user_id"].toString().removeSurrounding("\"") 
            }
            Result.success(userIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addParticipant(chatId: String, userId: String): Result<Unit> {
        return try {
            val participantData = mapOf(
                "chat_id" to chatId,
                "user_id" to userId,
                "role" to "member",
                "is_admin" to false,
                "can_send_messages" to true,
                "joined_at" to System.currentTimeMillis()
            )
            databaseService.insert("chat_participants", participantData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeParticipant(chatId: String, userId: String): Result<Unit> {
        return try {
            client.from("chat_participants").delete {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMessages(chatId: String): Flow<List<Message>> {
        return try {
            val channel = client.channel("messages:$chatId")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
                filter("chat_id", FilterOperator.EQ, chatId)
            }.map { action ->
                when (action) {
                    is PostgresAction.Insert, is PostgresAction.Update, is PostgresAction.Delete -> {
                        val result = chatService.getMessages(chatId)
                        result.getOrNull()?.map { mapToMessage(it) } ?: emptyList()
                    }
                    else -> emptyList()
                }
            }.catch { e ->
                android.util.Log.e("ChatRepository", "Error observing messages", e)
                emit(emptyList())
            }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    fun observeUserChats(userId: String): Flow<List<Chat>> {
        return try {
            val channel = client.channel("user_chats:$userId")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chats"
            }.map { action ->
                when (action) {
                    is PostgresAction.Insert, is PostgresAction.Update, is PostgresAction.Delete -> {
                        val result = chatService.getUserChats(userId)
                        result.getOrNull()?.map { mapToChat(it) } ?: emptyList()
                    }
                    else -> emptyList()
                }
            }.catch { e ->
                android.util.Log.e("ChatRepository", "Error observing chats", e)
                emit(emptyList())
            }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    suspend fun createDirectChat(currentUserId: String, otherUserId: String): Result<String> {
        return chatService.getOrCreateDirectChat(currentUserId, otherUserId)
    }

    suspend fun findDirectChat(userId1: String, userId2: String): Result<String?> {
        return try {
            val chatId = if (userId1 < userId2) {
                "dm_${userId1}_${userId2}"
            } else {
                "dm_${userId2}_${userId1}"
            }
            
            val existingChat = client.from("chats")
                .select(columns = Columns.raw("chat_id")) {
                    filter { eq("chat_id", chatId) }
                    limit(1)
                }
                .decodeList<JsonObject>()
            
            if (existingChat.isNotEmpty()) {
                Result.success(chatId)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrCreateDirectChat(currentUserId: String, otherUserId: String): Result<String> {
        return chatService.getOrCreateDirectChat(currentUserId, otherUserId)
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return chatService.markMessagesAsRead(chatId, userId)
    }

    private fun mapToMessage(data: Map<String, Any?>): Message {
        return Message(
            id = data["id"]?.toString() ?: "",
            chatId = data["chat_id"]?.toString() ?: "",
            senderId = data["sender_id"]?.toString() ?: "",
            content = data["content"]?.toString() ?: "",
            messageType = data["message_type"]?.toString() ?: "text",
            createdAt = data["created_at"]?.toString()?.toLongOrNull() ?: 0L,
            isDeleted = data["is_deleted"]?.toString()?.toBooleanStrictOrNull() ?: false,
            isEdited = data["is_edited"]?.toString()?.toBooleanStrictOrNull() ?: false,
            replyToId = data["reply_to_id"]?.toString()
        )
    }

    private fun mapToChat(data: Map<String, Any?>): Chat {
        return Chat(
            id = data["chat_id"]?.toString() ?: "",
            isGroup = data["is_group"]?.toString()?.toBooleanStrictOrNull() ?: false,
            lastMessage = data["last_message"]?.toString(),
            lastMessageTime = data["last_message_time"]?.toString()?.toLongOrNull(),
            lastMessageSender = data["last_message_sender"]?.toString(),
            createdAt = data["created_at"]?.toString()?.toLongOrNull() ?: 0L,
            isActive = data["is_active"]?.toString()?.toBooleanStrictOrNull() ?: true
        )
    }
    suspend fun blockUser(blockerId: String, blockedId: String): Result<Unit> {
        return try {
            val data = mapOf(
                "blocker_id" to blockerId,
                "blocked_id" to blockedId,
                "created_at" to System.currentTimeMillis()
            )
            databaseService.insert("user_blocks", data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportUser(reporterId: String, reportedId: String, reason: String = "spam"): Result<Unit> {
        return try {
            val data = mapOf(
                "reporter_id" to reporterId,
                "reported_id" to reportedId,
                "reason" to reason,
                "created_at" to System.currentTimeMillis()
            )
            databaseService.insert("user_reports", data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChat(chatId: String, userId: String): Result<Unit> {
        return try {
            client.from("chat_participants").delete {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
