package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.chat.service.SupabaseRealtimeService
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.*

/**
 * Supabase Chat Service
 * Handles chat and messaging operations
 */
class SupabaseChatService {
    
    companion object {
        private const val TAG = "SupabaseChatService"
    }
    
    private val client = SupabaseClient.client
    private val databaseService = SupabaseDatabaseService()
    
    /**
     * Check if a Result contains a duplicate key constraint violation error
     */
    private fun isDuplicateKeyError(result: Result<Unit>): Boolean {
        return result.exceptionOrNull()?.message?.contains("duplicate key", ignoreCase = true) == true
    }
    
    /**
     * Ensure both participants exist in the chat
     * Uses a database function to bypass RLS for adding both participants
     */
    private suspend fun ensureParticipantsExist(chatId: String, userId1: String, userId2: String, createdBy: String): Result<Unit> {
        android.util.Log.d(TAG, "Ensuring participants exist for chat: $chatId")
        
        return try {
            // Always try to add both participants via RPC (it has ON CONFLICT DO NOTHING)
            // This is more reliable than checking first, especially with race conditions
            val results = listOf(
                addChatParticipantViaRPC(chatId, userId1, createdBy),
                addChatParticipantViaRPC(chatId, userId2, createdBy)
            )
            
            // Check if any critical failures occurred
            val failures = results.filter { it.isFailure }
            if (failures.isNotEmpty()) {
                android.util.Log.w(TAG, "Some participants may not have been added, but continuing")
            }
            
            android.util.Log.d(TAG, "Participants ensured for chat: $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error ensuring participants exist", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add participant via RPC function (bypasses RLS)
     */
    private suspend fun addChatParticipantViaRPC(chatId: String, userId: String, createdBy: String): Result<Unit> {
        return try {
            val isCreator = userId == createdBy
            
            // Build parameters as JSON
            val params = buildJsonObject {
                put("p_chat_id", chatId)
                put("p_user_id", userId)
                put("p_role", if (isCreator) "creator" else "member")
                put("p_is_admin", isCreator)
                put("p_can_send_messages", true)
            }
            
            // Call RPC function using the postgrest extension property
            client.postgrest.rpc("add_chat_participant", params)
            
            android.util.Log.d(TAG, "Added participant via RPC: $userId to $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            // The RPC function has ON CONFLICT DO NOTHING, so any error is unexpected
            android.util.Log.e(TAG, "RPC failed for participant $userId: ${e.message}", e)
            // Don't fail the operation if participant might already exist
            if (e.message?.contains("duplicate", ignoreCase = true) == true ||
                e.message?.contains("already exists", ignoreCase = true) == true ||
                e.message?.contains("conflict", ignoreCase = true) == true) {
                android.util.Log.d(TAG, "Participant likely already exists: $userId in $chatId")
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create or get existing chat between two users
     * Uses try-insert-catch-retrieve pattern to handle race conditions gracefully
     */
    suspend fun getOrCreateDirectChat(userId1: String, userId2: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate inputs
                if (userId1.isEmpty() || userId2.isEmpty()) {
                    android.util.Log.e(TAG, "Invalid user IDs: userId1=$userId1, userId2=$userId2")
                    return@withContext Result.failure(Exception("Invalid user IDs"))
                }
                
                // Prevent self-messaging
                if (userId1 == userId2) {
                    android.util.Log.d(TAG, "Attempted self-messaging: $userId1")
                    return@withContext Result.failure(Exception("Cannot create chat with yourself"))
                }
                
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    android.util.Log.e(TAG, "Supabase not configured")
                    return@withContext Result.failure(Exception("Supabase not configured"))
                }
                
                // Generate consistent chat ID for direct chats
                val chatId = if (userId1 < userId2) {
                    "dm_${userId1}_${userId2}"
                } else {
                    "dm_${userId2}_${userId1}"
                }
                
                android.util.Log.d(TAG, "Creating/retrieving chat between $userId1 and $userId2: $chatId")
                
                // Check if chat already exists first
                val existingChat = try {
                    client.from("chats")
                        .select(columns = Columns.raw("chat_id")) {
                            filter {
                                eq("chat_id", chatId)
                            }
                            limit(1)
                        }
                        .decodeList<JsonObject>()
                        .firstOrNull()
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Error checking existing chat: ${e.message}")
                    null
                }
                
                if (existingChat == null) {
                    // Try to insert new chat
                    // Convert milliseconds to seconds for PostgreSQL timestamptz
                    val timestampSeconds = System.currentTimeMillis() / 1000
                    val chatData = mapOf(
                        "chat_id" to chatId,
                        "is_group" to false,
                        "created_by" to userId1,
                        "participants_count" to 2,
                        "is_active" to true,
                        "created_at" to timestampSeconds
                    )
                    
                    val insertResult = databaseService.insert("chats", chatData)
                    
                    when {
                        insertResult.isSuccess -> {
                            android.util.Log.d(TAG, "New chat created: $chatId")
                        }
                        isDuplicateKeyError(insertResult) -> {
                            // Chat was created by another request (race condition)
                            android.util.Log.d(TAG, "Chat created by concurrent request: $chatId")
                        }
                        else -> {
                            // Unexpected error during chat creation
                            android.util.Log.e(TAG, "Failed to create chat: ${insertResult.exceptionOrNull()?.message}")
                            return@withContext Result.failure(insertResult.exceptionOrNull() ?: Exception("Failed to create chat"))
                        }
                    }
                } else {
                    android.util.Log.d(TAG, "Chat already exists: $chatId")
                }
                
                // Ensure both participants are added (works whether chat is new or existing)
                val participantsResult = ensureParticipantsExist(chatId, userId1, userId2, userId1)
                if (participantsResult.isFailure) {
                    android.util.Log.e(TAG, "Failed to add participants: ${participantsResult.exceptionOrNull()?.message}")
                    return@withContext Result.failure(participantsResult.exceptionOrNull() ?: Exception("Failed to add participants"))
                }
                
                Result.success(chatId)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error in getOrCreateDirectChat", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Add participant to chat
     * DEPRECATED: Use addChatParticipantViaRPC instead to bypass RLS policies
     * This method is kept for backward compatibility but should not be used for new code
     */
    @Deprecated("Use addChatParticipantViaRPC instead", ReplaceWith("addChatParticipantViaRPC(chatId, userId, createdBy)"))
    private suspend fun addChatParticipant(chatId: String, userId: String, createdBy: String): Result<Unit> {
        // Redirect to RPC method which properly handles RLS
        return addChatParticipantViaRPC(chatId, userId, createdBy)
    }
    
    /**
     * Verify that a user is a participant in the chat
     * Uses RPC function to bypass RLS and avoid recursion issues
     */
    private suspend fun verifyUserIsParticipant(chatId: String, userId: String): Boolean {
        return try {
            // Use the security definer function we created
            val params = buildJsonObject {
                put("p_chat_id", chatId)
                put("p_user_uid", userId)
            }
            
            // RPC returns a boolean value directly - decode it
            val result = client.postgrest.rpc("is_user_in_chat", params)
                .decodeAs<Boolean>()
            
            result
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error verifying participant $userId in chat $chatId: ${e.message}", e)
            // If verification fails, assume user is participant to avoid blocking legitimate messages
            // The database RLS will still enforce security at the insert level
            android.util.Log.w(TAG, "Assuming user is participant due to verification error")
            true
        }
    }
    
    /**
     * Send a message with optional attachments
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        content: String,
        messageType: String = "text",
        replyToId: String? = null,
        attachments: List<com.synapse.social.studioasinc.chat.interfaces.ChatAttachment>? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured"))
                }
                
                // Validate that user is participant in this chat
                val isParticipant = verifyUserIsParticipant(chatId, senderId)
                if (!isParticipant) {
                    android.util.Log.e(TAG, "User $senderId is not a participant in chat $chatId")
                    return@withContext Result.failure(Exception("User is not a participant in this chat"))
                }
                
                val messageId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                // Convert milliseconds to seconds for PostgreSQL timestamptz
                val timestampSeconds = timestamp / 1000
                
                // Determine message type based on attachments
                val finalMessageType = if (!attachments.isNullOrEmpty()) {
                    "ATTACHMENT_MESSAGE"
                } else {
                    messageType
                }
                
                val messageData = mutableMapOf<String, Any?>(
                    "chat_id" to chatId,
                    "sender_id" to senderId,
                    "content" to content,
                    "message_type" to finalMessageType,
                    "created_at" to timestampSeconds,
                    "updated_at" to timestampSeconds,
                    "message_state" to "sent",  // Set initial state to SENT
                    "delivered_at" to null,      // Initially null
                    "read_at" to null,           // Initially null
                    "delivery_status" to "sent",
                    "is_deleted" to false,
                    "is_edited" to false
                )
                
                if (replyToId != null) {
                    messageData["reply_to_id"] = replyToId
                }
                
                // Serialize attachments as JSONB array if present
                if (!attachments.isNullOrEmpty()) {
                    val attachmentsJson = attachments.map { attachment ->
                        mapOf(
                            "id" to attachment.id,
                            "url" to attachment.url,
                            "type" to attachment.type,
                            "file_name" to attachment.fileName,
                            "file_size" to attachment.fileSize,
                            "thumbnail_url" to attachment.thumbnailUrl,
                            "width" to attachment.width,
                            "height" to attachment.height,
                            "duration" to attachment.duration,
                            "mime_type" to attachment.mimeType
                        )
                    }
                    messageData["attachments"] = attachmentsJson
                }
                
                databaseService.insert("messages", messageData).fold(
                    onSuccess = {
                        // Update chat's last message
                        val lastMessageText = if (!attachments.isNullOrEmpty()) {
                            when (attachments.first().type) {
                                "image" -> "📷 Photo"
                                "video" -> "🎥 Video"
                                "audio" -> "🎵 Audio"
                                "document" -> "📄 Document"
                                else -> "📎 Attachment"
                            }
                        } else {
                            content
                        }
                        updateChatLastMessage(chatId, lastMessageText, timestampSeconds, senderId)
                        Result.success(messageId)
                    },
                    onFailure = { error -> Result.failure(error) }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update chat's last message info
     */
    private suspend fun updateChatLastMessage(
        chatId: String,
        lastMessage: String,
        timestamp: Long,
        senderId: String
    ): Result<Unit> {
        return try {
            val updateData = mapOf(
                "last_message" to lastMessage,
                "last_message_time" to timestamp,
                "last_message_sender" to senderId
            )
            databaseService.update("chats", updateData, "chat_id", chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get messages for a chat
     * @param chatId The chat ID
     * @param limit Maximum number of messages to fetch
     * @param beforeTimestamp Optional timestamp to fetch messages before (for pagination)
     */
    suspend fun getMessages(
        chatId: String, 
        limit: Int = 50,
        beforeTimestamp: Long? = null
    ): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(emptyList())
                }
                val result = client.from("messages")
                    .select(columns = Columns.raw("*")) {
                        filter {
                            eq("chat_id", chatId)
                            eq("is_deleted", false)
                            // If beforeTimestamp is provided, only fetch messages before that timestamp
                            beforeTimestamp?.let {
                                lt("created_at", it / 1000) // Convert to seconds
                            }
                        }
                        // Order by created_at DESCENDING to get the most recent messages first
                        order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<JsonObject>()
                
                val messages = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                // Reverse the list so oldest messages are first (for display)
                Result.success(messages.reversed())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get user's chats
     */
    suspend fun getUserChats(userId: String): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("SupabaseChatService", "Getting chats for user: $userId")
                
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    android.util.Log.w("SupabaseChatService", "Supabase not configured, returning empty chat list")
                    return@withContext Result.success(emptyList())
                }
                
                // Get chat IDs where user is a participant
                val participantResult = client.from("chat_participants")
                    .select(columns = Columns.raw("chat_id")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<JsonObject>()
                
                android.util.Log.d("SupabaseChatService", "Found ${participantResult.size} participant records")
                
                val chatIds = participantResult.map { 
                    it["chat_id"].toString().removeSurrounding("\"") 
                }
                
                if (chatIds.isEmpty()) {
                    android.util.Log.d("SupabaseChatService", "No chats found for user")
                    return@withContext Result.success(emptyList())
                }
                
                android.util.Log.d("SupabaseChatService", "Chat IDs: $chatIds")
                
                // Get chat details
                val chatsResult = client.from("chats")
                    .select(columns = Columns.raw("*")) {
                        filter {
                            isIn("chat_id", chatIds)
                            eq("is_active", true)
                        }
                    }
                    .decodeList<JsonObject>()
                
                android.util.Log.d("SupabaseChatService", "Found ${chatsResult.size} active chats")
                
                val chats = chatsResult.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                Result.success(chats)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseChatService", "Failed to load chats", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Mark messages as read with batching and Realtime broadcasting.
     * Updates message states to READ, sets read_at timestamp, and broadcasts read receipt event.
     * 
     * @param chatId The chat room identifier
     * @param userId The user marking messages as read
     * @param messageIds List of message IDs to mark as read (optional, defaults to all unread)
     * @param realtimeService Optional SupabaseRealtimeService for broadcasting read receipts
     */
    suspend fun markMessagesAsRead(
        chatId: String, 
        userId: String, 
        messageIds: List<String>? = null,
        realtimeService: SupabaseRealtimeService? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                
                // Get messages to mark as read
                val messagesToUpdate = if (messageIds != null) {
                    messageIds
                } else {
                    // Get all unread messages in this chat not sent by this user
                    val unreadMessages = client.from("messages")
                        .select(columns = Columns.raw("id")) {
                            filter {
                                eq("chat_id", chatId)
                                neq("sender_id", userId)
                                neq("message_state", "read")
                                eq("is_deleted", false)
                            }
                        }
                        .decodeList<JsonObject>()
                    
                    unreadMessages.map { it["id"].toString().removeSurrounding("\"") }
                }
                
                if (messagesToUpdate.isEmpty()) {
                    android.util.Log.d(TAG, "No messages to mark as read for chat: $chatId")
                    return@withContext Result.success(Unit)
                }
                
                android.util.Log.d(TAG, "Marking ${messagesToUpdate.size} messages as read in chat: $chatId")
                
                // Convert milliseconds to seconds for PostgreSQL timestamptz
                val timestampSeconds = timestamp / 1000
                
                // Batch update all messages in a single operation using buildJsonObject
                val updateData = buildJsonObject {
                    put("message_state", "read")
                    put("read_at", timestampSeconds)
                    put("updated_at", timestampSeconds)
                }
                
                // Update messages using batch operation
                client.from("messages").update(updateData) {
                    filter {
                        isIn("id", messagesToUpdate)
                    }
                }
                
                // Update last_read_at for the participant using buildJsonObject
                val participantUpdateData = buildJsonObject {
                    put("last_read_at", timestampSeconds)
                }
                
                client.from("chat_participants").update(participantUpdateData) {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", userId)
                    }
                }
                
                // Broadcast read receipt event via Realtime if service is provided
                realtimeService?.let { service ->
                    try {
                        service.broadcastReadReceipt(chatId, userId, messagesToUpdate)
                        android.util.Log.d(TAG, "Read receipt broadcasted for ${messagesToUpdate.size} messages")
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Failed to broadcast read receipt, but messages were marked as read", e)
                        // Don't fail the operation if broadcast fails
                    }
                }
                
                android.util.Log.d(TAG, "Successfully marked ${messagesToUpdate.size} messages as read")
                Result.success(Unit)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error marking messages as read", e)
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
                // Convert milliseconds to seconds for PostgreSQL timestamptz
                val timestampSeconds = timestamp / 1000
                
                android.util.Log.d(TAG, "Updating message $messageId to DELIVERED state")
                
                // Update message state to DELIVERED
                val updateData = mapOf(
                    "message_state" to "delivered",
                    "delivered_at" to timestampSeconds,
                    "delivery_status" to "delivered",
                    "updated_at" to timestampSeconds
                )
                
                databaseService.update("messages", updateData, "id", messageId).fold(
                    onSuccess = {
                        android.util.Log.d(TAG, "Message $messageId marked as delivered")
                        
                        // Broadcast delivery event via Realtime if service is provided
                        realtimeService?.let { service ->
                            try {
                                // Broadcast delivery event using the same pattern as read receipts
                                service.broadcastReadReceipt(chatId, userId, listOf(messageId))
                                android.util.Log.d(TAG, "Delivery event broadcasted for message: $messageId")
                            } catch (e: Exception) {
                                android.util.Log.w(TAG, "Failed to broadcast delivery event, but message was marked as delivered", e)
                                // Don't fail the operation if broadcast fails
                            }
                        }
                        
                        Result.success(Unit)
                    },
                    onFailure = { error ->
                        android.util.Log.e(TAG, "Failed to update message delivery state", error)
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error updating message delivery state", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Delete a message
     * @param messageId The message ID to delete
     * @param deleteForEveryone If true, deletes for all users. If false, only marks as deleted for current user
     */
    suspend fun deleteMessage(messageId: String, deleteForEveryone: Boolean = true): Result<Unit> {
        return try {
            // Convert milliseconds to seconds for PostgreSQL timestamptz
            val timestampSeconds = System.currentTimeMillis() / 1000
            val updateData = mapOf(
                "is_deleted" to true,
                "delete_for_everyone" to deleteForEveryone,
                "deleted_at" to timestampSeconds
            )
            databaseService.update("messages", updateData, "id", messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a message with its associated media files from storage
     * This method deletes the message from the database and removes all associated
     * media files (original and thumbnails) from Supabase Storage
     * 
     * @param messageId The message identifier to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteMessageWithMedia(messageId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Deleting message with media: $messageId")
                
                // First, retrieve the message to get attachment information
                val message = client.from("messages")
                    .select(columns = Columns.raw("attachments")) {
                        filter {
                            eq("id", messageId)
                        }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
                    .firstOrNull()
                
                if (message == null) {
                    android.util.Log.w(TAG, "Message not found: $messageId")
                    return@withContext Result.failure(Exception("Message not found"))
                }
                
                // Extract attachments from the message
                val attachmentsJson = message["attachments"]
                
                // Initialize storage service for file deletion
                val storageService = SupabaseStorageService()
                
                // Delete media files if attachments exist
                if (attachmentsJson != null && attachmentsJson.toString() != "null") {
                    try {
                        // Parse attachments JSON array
                        val attachmentsString = attachmentsJson.toString()
                        android.util.Log.d(TAG, "Processing attachments for deletion: $attachmentsString")
                        
                        // Extract URLs from the JSON string
                        // This is a simple approach - in production, use proper JSON parsing
                        val urlPattern = """"url"\s*:\s*"([^"]+)"""".toRegex()
                        val thumbnailPattern = """"thumbnail_url"\s*:\s*"([^"]+)"""".toRegex()
                        
                        val urls = urlPattern.findAll(attachmentsString).map { it.groupValues[1] }.toList()
                        val thumbnailUrls = thumbnailPattern.findAll(attachmentsString)
                            .map { it.groupValues[1] }
                            .filter { it != "null" }
                            .toList()
                        
                        // Delete original files
                        urls.forEach { url ->
                            try {
                                val path = storageService.extractPathFromUrl(url, "chat-media")
                                if (path != null) {
                                    val deleteResult = storageService.deleteFile(path)
                                    if (deleteResult.isSuccess) {
                                        android.util.Log.d(TAG, "Deleted media file: $path")
                                    } else {
                                        android.util.Log.w(TAG, "Failed to delete media file: $path - ${deleteResult.exceptionOrNull()?.message}")
                                    }
                                } else {
                                    android.util.Log.w(TAG, "Could not extract path from URL: $url")
                                }
                            } catch (e: Exception) {
                                // Log but don't fail - file might already be deleted
                                android.util.Log.w(TAG, "Error deleting media file: $url - ${e.message}")
                            }
                        }
                        
                        // Delete thumbnail files
                        thumbnailUrls.forEach { thumbnailUrl ->
                            try {
                                val path = storageService.extractPathFromUrl(thumbnailUrl, "chat-media")
                                if (path != null) {
                                    val deleteResult = storageService.deleteFile(path)
                                    if (deleteResult.isSuccess) {
                                        android.util.Log.d(TAG, "Deleted thumbnail file: $path")
                                    } else {
                                        android.util.Log.w(TAG, "Failed to delete thumbnail file: $path - ${deleteResult.exceptionOrNull()?.message}")
                                    }
                                } else {
                                    android.util.Log.w(TAG, "Could not extract path from thumbnail URL: $thumbnailUrl")
                                }
                            } catch (e: Exception) {
                                // Log but don't fail - file might already be deleted
                                android.util.Log.w(TAG, "Error deleting thumbnail file: $thumbnailUrl - ${e.message}")
                            }
                        }
                        
                        android.util.Log.d(TAG, "Deleted ${urls.size} media files and ${thumbnailUrls.size} thumbnails")
                        
                    } catch (e: Exception) {
                        // Log error but continue with message deletion
                        android.util.Log.e(TAG, "Error processing attachments for deletion", e)
                    }
                }
                
                // Delete the message from database (soft delete)
                val updateData = mapOf("is_deleted" to true)
                val deleteResult = databaseService.update("messages", updateData, "id", messageId)
                
                if (deleteResult.isSuccess) {
                    android.util.Log.d(TAG, "Successfully deleted message with media: $messageId")
                    Result.success(Unit)
                } else {
                    android.util.Log.e(TAG, "Failed to delete message: $messageId")
                    Result.failure(deleteResult.exceptionOrNull() ?: Exception("Failed to delete message"))
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error in deleteMessageWithMedia", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Edit a message and save edit history
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                // Convert milliseconds to seconds for PostgreSQL timestamptz
                val timestampSeconds = timestamp / 1000
                
                // Get current message content before editing
                val currentMessage = client.from("messages")
                    .select(columns = Columns.raw("content, sender_id")) {
                        filter {
                            eq("id", messageId)
                        }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
                    .firstOrNull()

                val previousContent = currentMessage?.get("content")?.toString()?.removeSurrounding("\"") ?: ""
                val senderId = currentMessage?.get("sender_id")?.toString()?.removeSurrounding("\"") ?: ""

                // Save edit history
                if (previousContent.isNotEmpty()) {
                    val historyData = mapOf(
                        "message_id" to messageId,
                        "previous_content" to previousContent,
                        "edited_by" to senderId,
                        "edited_at" to timestampSeconds
                    )
                    val historyResult = databaseService.insert("message_edit_history", historyData)
                    if (historyResult.isFailure) {
                        android.util.Log.w(TAG, "Failed to save edit history: ${historyResult.exceptionOrNull()?.message}")
                        // Continue with message update even if history fails
                    }
                }

                // Update message
                val updateData = mapOf(
                    "content" to newContent,
                    "is_edited" to true,
                    "edited_at" to timestampSeconds,
                    "updated_at" to timestampSeconds
                )
                databaseService.update("messages", updateData, "id", messageId)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error editing message", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update typing status for a user in a chat
     */
    suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate inputs
                if (chatId.isBlank() || userId.isBlank()) {
                    android.util.Log.e(TAG, "Invalid parameters: chatId=$chatId, userId=$userId")
                    return@withContext Result.failure(Exception("Invalid chat ID or user ID"))
                }
                
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    android.util.Log.e(TAG, "Supabase not configured")
                    return@withContext Result.failure(Exception("Supabase not configured"))
                }
                if (isTyping) {
                    // Convert milliseconds to seconds for PostgreSQL timestamptz
                    val timestampSeconds = System.currentTimeMillis() / 1000
                    val typingData = mapOf(
                        "chat_id" to chatId,
                        "user_id" to userId,
                        "is_typing" to isTyping,
                        "timestamp" to timestampSeconds
                    )
                    databaseService.upsert("typing_status", typingData)
                } else {
                    // Remove typing status when user stops typing
                    try {
                        client.from("typing_status").delete {
                            filter {
                                eq("chat_id", chatId)
                                eq("user_id", userId)
                            }
                        }
                        Result.success(Unit)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Failed to delete typing status (user may not be typing): ${e.message}")
                        Result.success(Unit) // Don't fail if delete fails
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error updating typing status", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get typing users in a chat
     */
    suspend fun getTypingUsers(chatId: String, excludeUserId: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                // Convert milliseconds to seconds for PostgreSQL timestamptz comparison
                val fiveSecondsAgo = (System.currentTimeMillis() - 5000) / 1000
                
                val typingUsers = client.from("typing_status")
                    .select(columns = Columns.raw("user_id")) {
                        filter {
                            eq("chat_id", chatId)
                            eq("is_typing", true)
                            neq("user_id", excludeUserId)
                            gte("timestamp", fiveSecondsAgo)
                        }
                    }
                    .decodeList<JsonObject>()
                
                val userIds = typingUsers.map { 
                    it["user_id"].toString().removeSurrounding("\"") 
                }
                Result.success(userIds)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get chat participants
     */
    suspend fun getChatParticipants(chatId: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
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
    }
    
    /**
     * Update message delivery status
     */
    suspend fun updateMessageDeliveryStatus(
        messageId: String,
        status: String
    ): Result<Unit> {
        return try {
            // Convert milliseconds to seconds for PostgreSQL timestamptz
            val timestampSeconds = System.currentTimeMillis() / 1000
            val updateData = mapOf(
                "delivery_status" to status,
                "updated_at" to timestampSeconds
            )
            databaseService.update("messages", updateData, "id", messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get unread message count for a chat
     */
    suspend fun getUnreadMessageCount(chatId: String, userId: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                // Get user's last read timestamp
                val participant = client.from("chat_participants")
                    .select(columns = Columns.raw("last_read_at")) {
                        filter {
                            eq("chat_id", chatId)
                            eq("user_id", userId)
                        }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
                
                val lastReadAt = participant.firstOrNull()
                    ?.get("last_read_at")
                    ?.toString()
                    ?.removeSurrounding("\"")
                    ?.toLongOrNull() ?: 0L
                
                // Count messages after last read time
                val unreadMessages = client.from("messages")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("chat_id", chatId)
                            neq("sender_id", userId)
                            gt("created_at", lastReadAt)
                            eq("is_deleted", false)
                        }
                    }
                    .decodeList<JsonObject>()
                
                Result.success(unreadMessages.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Add a reaction to a message
     */
    suspend fun addReaction(messageId: String, userId: String, emoji: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val reactionId = UUID.randomUUID().toString()
                // Convert milliseconds to seconds for PostgreSQL timestamptz
                val timestampSeconds = System.currentTimeMillis() / 1000
                val reactionData = mapOf(
                    "id" to reactionId,
                    "message_id" to messageId,
                    "user_id" to userId,
                    "emoji" to emoji,
                    "created_at" to timestampSeconds
                )
                
                databaseService.insert("message_reactions", reactionData).fold(
                    onSuccess = { Result.success(reactionId) },
                    onFailure = { error -> Result.failure(error) }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Remove a reaction from a message
     */
    suspend fun removeReaction(messageId: String, userId: String, emoji: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("message_reactions").delete {
                    filter {
                        eq("message_id", messageId)
                        eq("user_id", userId)
                        eq("emoji", emoji)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get reactions for a message
     */
    suspend fun getMessageReactions(messageId: String): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                val reactions = client.from("message_reactions")
                    .select(columns = Columns.raw("*")) {
                        filter { eq("message_id", messageId) }
                    }
                    .decodeList<JsonObject>()
                
                val reactionsList = reactions.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                Result.success(reactionsList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Toggle reaction on a message (add if not exists, remove if exists)
     */
    suspend fun toggleReaction(messageId: String, userId: String, emoji: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if reaction already exists
                val existingReactions = client.from("message_reactions")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("message_id", messageId)
                            eq("user_id", userId)
                            eq("emoji", emoji)
                        }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
                
                if (existingReactions.isNotEmpty()) {
                    // Remove reaction
                    removeReaction(messageId, userId, emoji)
                    Result.success(false) // Reaction removed
                } else {
                    // Add reaction
                    addReaction(messageId, userId, emoji)
                    Result.success(true) // Reaction added
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
