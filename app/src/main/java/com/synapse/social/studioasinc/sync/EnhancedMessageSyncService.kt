package com.synapse.social.studioasinc.sync

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced message sync service with offline queue and cross-platform sync
 */
class EnhancedMessageSyncService(private val scope: CoroutineScope) {
    
    private val supabase = SupabaseClient.client
    
    // Sync state
    private val _syncState = MutableStateFlow(SyncState.CONNECTED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _offlineQueue = MutableStateFlow<List<QueuedMessage>>(emptyList())
    val offlineQueue: StateFlow<List<QueuedMessage>> = _offlineQueue.asStateFlow()
    
    // Message delivery tracking
    private val pendingMessages = ConcurrentHashMap<String, QueuedMessage>()
    
    /**
     * Initialize real-time sync for a chat
     */
    fun initializeSync(chatId: String): Flow<MessageSyncEvent> = kotlinx.coroutines.flow.flow {
        try {
            // Simplified sync - emit sync state change
            emit(MessageSyncEvent.SyncStateChanged(SyncState.SYNCING))
        } catch (e: Exception) {
            emit(MessageSyncEvent.SyncError(e.message ?: "Unknown sync error"))
        }
    }
    
    /**
     * Send message with offline queue support
     */
    suspend fun sendMessage(message: MessageToSend): String {
        val messageId = java.util.UUID.randomUUID().toString()
        val queuedMessage = QueuedMessage(
            id = messageId,
            chatId = message.chatId,
            senderId = message.senderId,
            content = message.content,
            messageType = message.messageType,
            mediaUrl = message.mediaUrl,
            replyToId = message.replyToId,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING
        )
        
        // Add to pending messages
        pendingMessages[messageId] = queuedMessage
        
        return try {
            if (_syncState.value == SyncState.CONNECTED) {
                // Send immediately
                sendMessageToServer(queuedMessage)
                messageId
            } else {
                // Add to offline queue
                addToOfflineQueue(queuedMessage)
                messageId
            }
        } catch (e: Exception) {
            // Add to offline queue on failure
            addToOfflineQueue(queuedMessage.copy(status = MessageStatus.FAILED))
            messageId
        }
    }
    
    /**
     * Process offline queue when connection is restored
     */
    suspend fun processOfflineQueue() {
        val queue = _offlineQueue.value
        if (queue.isEmpty()) return
        
        _syncState.value = SyncState.SYNCING
        
        val successful = mutableListOf<String>()
        
        for (message in queue) {
            try {
                sendMessageToServer(message)
                successful.add(message.id)
            } catch (e: Exception) {
                // Keep failed messages in queue
                updateMessageStatus(message.id, MessageStatus.FAILED)
            }
        }
        
        // Remove successful messages from queue
        _offlineQueue.value = queue.filterNot { successful.contains(it.id) }
        _syncState.value = SyncState.CONNECTED
    }
    
    /**
     * Update message delivery status
     */
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        try {
            supabase.from("messages")
                .update(mapOf("message_state" to status.name.lowercase())) {
                    filter {
                        FilterOperator.eq("id", messageId)
                    }
                }
            
            // Update local tracking
            pendingMessages[messageId]?.let { message ->
                pendingMessages[messageId] = message.copy(status = status)
            }
        } catch (e: Exception) {
            // Handle error silently for status updates
        }
    }
    
    /**
     * Mark messages as read with cross-platform sync
     */
    suspend fun markMessagesAsRead(messageIds: List<String>, userId: String) {
        try {
            val readTimestamp = System.currentTimeMillis()
            
            // Update read status in database
            for (messageId in messageIds) {
                supabase.from("messages")
                    .update(mapOf(
                        "read_at" to readTimestamp,
                        "message_state" to "read"
                    )) {
                        filter {
                            FilterOperator.eq("id", messageId)
                        }
                    }
            }
            
            // Update user's last read message
            supabase.from("chat_participants")
                .update(mapOf(
                    "last_read_message_id" to messageIds.lastOrNull(),
                    "last_read_at" to readTimestamp
                )) {
                    filter {
                        FilterOperator.eq("user_id", userId)
                    }
                }
                
        } catch (e: Exception) {
            // Handle error - could add to retry queue
        }
    }
    
    /**
     * Get unread message count across all chats
     */
    suspend fun getUnreadCount(userId: String): Int {
        return try {
            val response = supabase.from("messages")
                .select(columns = Columns.list("id")) {
                    filter {
                        FilterOperator.neq("sender_id", userId)
                        FilterOperator.eq("message_state", "delivered")
                    }
                }
                .decodeList<Map<String, Any>>()
            
            response.size
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Sync conversation history for cross-platform consistency
     */
    suspend fun syncConversationHistory(chatId: String, lastSyncTimestamp: Long = 0): List<SyncedMessage> {
        return try {
            val response = supabase.from("messages")
                .select(columns = Columns.ALL) {
                    filter {
                        FilterOperator.eq("chat_id", chatId)
                        FilterOperator.gt("created_at", lastSyncTimestamp)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<MessageDto>()
            
            response.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun sendMessageToServer(message: QueuedMessage) {
        val messageDto = MessageDto(
            id = message.id,
            chat_id = message.chatId,
            sender_id = message.senderId,
            content = message.content,
            message_type = message.messageType,
            media_url = message.mediaUrl,
            reply_to_id = message.replyToId,
            message_state = "sent"
        )
        
        supabase.from("messages").insert(messageDto)
        updateMessageStatus(message.id, MessageStatus.SENT)
    }
    
    private fun addToOfflineQueue(message: QueuedMessage) {
        _offlineQueue.value = _offlineQueue.value + message
    }
    
    /**
     * Handle connection state changes
     */
    fun onConnectionStateChanged(isConnected: Boolean) {
        _syncState.value = if (isConnected) SyncState.CONNECTED else SyncState.OFFLINE
        
        if (isConnected && _offlineQueue.value.isNotEmpty()) {
            scope.launch {
                processOfflineQueue()
            }
        }
    }
}

// Data Models
enum class SyncState {
    CONNECTED, OFFLINE, SYNCING, ERROR
}

enum class MessageStatus {
    PENDING, SENT, DELIVERED, READ, FAILED
}

data class MessageToSend(
    val chatId: String,
    val senderId: String,
    val content: String,
    val messageType: String = "text",
    val mediaUrl: String? = null,
    val replyToId: String? = null
)

data class QueuedMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val messageType: String,
    val mediaUrl: String?,
    val replyToId: String?,
    val timestamp: Long,
    val status: MessageStatus
)

data class SyncedMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val messageType: String,
    val createdAt: Long,
    val messageState: String
)

sealed class MessageSyncEvent {
    data class MessageReceived(val message: SyncedMessage) : MessageSyncEvent()
    data class MessageUpdated(val message: SyncedMessage) : MessageSyncEvent()
    data class MessageDeleted(val messageId: String) : MessageSyncEvent()
    data class SyncStateChanged(val state: SyncState) : MessageSyncEvent()
    data class SyncError(val message: String) : MessageSyncEvent()
}

@Serializable
data class MessageDto(
    val id: String,
    val chat_id: String,
    val sender_id: String,
    val content: String,
    val message_type: String = "text",
    val media_url: String? = null,
    val reply_to_id: String? = null,
    val message_state: String = "sent",
    val created_at: String? = null
) {
    fun toDomain() = SyncedMessage(
        id = id,
        chatId = chat_id,
        senderId = sender_id,
        content = content,
        messageType = message_type,
        createdAt = created_at?.let { 
            try {
                Instant.parse(it).toEpochMilliseconds()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } ?: System.currentTimeMillis(),
        messageState = message_state
    )
}
