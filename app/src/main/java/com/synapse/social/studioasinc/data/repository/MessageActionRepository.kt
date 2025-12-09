package com.synapse.social.studioasinc.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.model.MessageEdit
import com.synapse.social.studioasinc.model.MessageForward
import com.synapse.social.studioasinc.util.RetryHandler
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Repository for message action operations
 * Handles forward, edit, delete, and AI summary caching
 */
class MessageActionRepository(private val context: Context) {

    companion object {
        private const val TAG = "MessageActionRepository"
        private const val PREFS_NAME = "message_action_prefs"
        private const val SUMMARY_CACHE_PREFIX = "ai_summary_"
        private const val SUMMARY_EXPIRY_PREFIX = "ai_summary_expiry_"
        private const val SUMMARY_CACHE_MAX_SIZE = 100
        private const val SUMMARY_EXPIRY_DAYS = 7
        private const val FORTY_EIGHT_HOURS_MS = 48 * 60 * 60 * 1000L
    }

    private val chatService = SupabaseChatService()
    private val databaseService = SupabaseDatabaseService()
    private val client = SupabaseClient.client
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ==================== Forward Message Operations ====================

    /**
     * Forward a message to a single chat
     * @param messageData The original message data
     * @param targetChatId The chat ID to forward to
     * @return Result with the new message ID
     */
    suspend fun forwardMessageToChat(
        messageData: Map<String, Any?>,
        targetChatId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        // Extract message details first (before retry handler)
        val originalMessageId = messageData["id"]?.toString()
        if (originalMessageId == null) {
            return@withContext Result.failure(Exception("Message ID is required"))
        }
        val originalChatId = messageData["chat_id"]?.toString()
        if (originalChatId == null) {
            return@withContext Result.failure(Exception("Chat ID is required"))
        }
        val senderId = messageData["sender_id"]?.toString()
        if (senderId == null) {
            return@withContext Result.failure(Exception("Sender ID is required"))
        }
        
        // Use RetryHandler for network resilience
        val result = RetryHandler.executeWithRetryResult { attemptNumber ->
            Log.d(TAG, "Forwarding message to chat: $targetChatId - Attempt: $attemptNumber")
            val content = messageData["content"]?.toString() ?: ""
            val messageType = messageData["message_type"]?.toString() ?: "text"
            val mediaUrl = messageData["media_url"]?.toString()

            // Generate new message ID
            val newMessageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            // Create new message with forwarded metadata
            val forwardedMessageData = mutableMapOf<String, Any?>(
                "id" to newMessageId,
                "chat_id" to targetChatId,
                "sender_id" to senderId,
                "content" to content,
                "message_type" to messageType,
                "media_url" to mediaUrl,
                "created_at" to timestamp,
                "updated_at" to timestamp,
                "delivery_status" to "sent",
                "is_deleted" to false,
                "is_edited" to false,
                "forwarded_from_message_id" to originalMessageId,
                "forwarded_from_chat_id" to originalChatId
            )

            // Insert the forwarded message
            val insertResult = databaseService.insert("messages", forwardedMessageData)
            if (insertResult.isFailure) {
                Log.e(TAG, "Failed to insert forwarded message", insertResult.exceptionOrNull())
                throw insertResult.exceptionOrNull() ?: Exception("Failed to insert forwarded message")
            }

            // Store forward relationship in message_forwards table
            val forwardId = UUID.randomUUID().toString()
            val forwardData = mapOf(
                "id" to forwardId,
                "original_message_id" to originalMessageId,
                "original_chat_id" to originalChatId,
                "forwarded_message_id" to newMessageId,
                "forwarded_chat_id" to targetChatId,
                "forwarded_by" to senderId,
                "forwarded_at" to timestamp
            )

            val forwardResult = databaseService.insert("message_forwards", forwardData)
            if (forwardResult.isFailure) {
                Log.w(TAG, "Failed to store forward relationship", forwardResult.exceptionOrNull())
                // Don't fail the operation if forward tracking fails
            }

            // Update target chat's last message
            updateChatLastMessage(targetChatId, content, timestamp, senderId)

            Log.d(TAG, "Message forwarded successfully - NewMessageId: $newMessageId, TargetChatId: $targetChatId")
            newMessageId
        }
        return@withContext result
    }

    /**
     * Forward a message to multiple chats
     * @param messageData The original message data
     * @param targetChatIds List of chat IDs to forward to
     * @return Result with the count of successful forwards
     */
    suspend fun forwardMessageToMultipleChats(
        messageData: Map<String, Any?>,
        targetChatIds: List<String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Forwarding message to ${targetChatIds.size} chats")

            var successCount = 0
            val failures = mutableListOf<String>()

            for (chatId in targetChatIds) {
                val result = forwardMessageToChat(messageData, chatId)
                if (result.isSuccess) {
                    successCount++
                } else {
                    failures.add(chatId)
                    Log.w(TAG, "Failed to forward to chat $chatId: ${result.exceptionOrNull()?.message}")
                }
            }

            Log.d(TAG, "Forwarded to $successCount of ${targetChatIds.size} chats")

            if (successCount == 0) {
                Result.failure(Exception("Failed to forward message to any chat"))
            } else {
                Result.success(successCount)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding message to multiple chats", e)
            Result.failure(e)
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
    ) {
        try {
            val updateData = mapOf(
                "last_message" to lastMessage,
                "last_message_time" to timestamp,
                "last_message_sender" to senderId
            )
            databaseService.update("chats", updateData, "chat_id", chatId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update chat last message", e)
        }
    }

    // ==================== Edit Message Operations ====================

    /**
     * Edit a message with validation and history tracking
     * @param messageId The message ID to edit
     * @param newContent The new message content
     * @return Result indicating success or failure
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        // Validate non-empty content first
        if (newContent.isBlank()) {
            return@withContext Result.failure(Exception("Message content cannot be empty"))
        }
        
        // Use RetryHandler for network resilience
        val result = RetryHandler.executeWithRetryResult { attemptNumber ->
            Log.d(TAG, "Editing message: $messageId - Attempt: $attemptNumber, ContentLength: ${newContent.length}")

            // Get the current message to validate age and get current content
            val messageResult = client.from("messages")
                .select(columns = Columns.raw("*")) {
                    filter { eq("id", messageId) }
                    limit(1)
                }
                .decodeList<JsonObject>()

            if (messageResult.isEmpty()) {
                throw Exception("Message not found")
            }

            val message = messageResult.first()
            val createdAt = message["created_at"]?.toString()?.removeSurrounding("\"")?.toLongOrNull() ?: 0L
            val currentContent = message["content"]?.toString()?.removeSurrounding("\"") ?: ""
            val senderId = message["sender_id"]?.toString()?.removeSurrounding("\"") ?: ""

            // Validate message age (must be <48 hours old)
            val messageAge = System.currentTimeMillis() - createdAt
            if (messageAge > FORTY_EIGHT_HOURS_MS) {
                throw Exception("Message is too old to edit (>48 hours)")
            }

            // Get existing edit history
            val editHistoryJson = message["edit_history"]?.toString()?.removeSurrounding("\"")
            val editHistory = try {
                if (editHistoryJson != null && editHistoryJson != "null" && editHistoryJson.isNotEmpty()) {
                    JSONArray(editHistoryJson)
                } else {
                    JSONArray()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse edit history, creating new", e)
                JSONArray()
            }

            // Add current content to edit history
            val editEntry = JSONObject().apply {
                put("edited_at", System.currentTimeMillis())
                put("previous_content", currentContent)
                put("edited_by", senderId)
            }
            editHistory.put(editEntry)

            // Update message with new content and edit history
            val updateData = mapOf(
                "content" to newContent,
                "is_edited" to true,
                "edited_at" to System.currentTimeMillis(),
                "updated_at" to System.currentTimeMillis(),
                "edit_history" to editHistory.toString()
            )

            val updateResult = databaseService.update("messages", updateData, "id", messageId)
            if (updateResult.isFailure) {
                Log.e(TAG, "Failed to update message", updateResult.exceptionOrNull())
                throw updateResult.exceptionOrNull() ?: Exception("Failed to update message")
            }

            Log.d(TAG, "Message edited successfully - MessageId: $messageId, NewContentLength: ${newContent.length}")
            Unit
        }
        return@withContext result
    }

    /**
     * Get edit history for a message
     * @param messageId The message ID
     * @return Result with list of MessageEdit entries
     */
    suspend fun getEditHistory(messageId: String): Result<List<MessageEdit>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Getting edit history for message: $messageId")

            val messageResult = client.from("messages")
                .select(columns = Columns.raw("edit_history")) {
                    filter { eq("id", messageId) }
                    limit(1)
                }
                .decodeList<JsonObject>()

            if (messageResult.isEmpty()) {
                Result.failure(Exception("Message not found"))
            } else {
                val message = messageResult.first()
                val editHistoryJson = message["edit_history"]?.toString()?.removeSurrounding("\"")

                val editHistory = try {
                    if (editHistoryJson != null && editHistoryJson != "null" && editHistoryJson.isNotEmpty()) {
                        val jsonArray = JSONArray(editHistoryJson)
                        val edits = mutableListOf<MessageEdit>()
                        
                        for (i in 0 until jsonArray.length()) {
                            val editObj = jsonArray.getJSONObject(i)
                            edits.add(
                                MessageEdit(
                                    editedAt = editObj.getLong("edited_at"),
                                    previousContent = editObj.getString("previous_content"),
                                    editedBy = editObj.getString("edited_by")
                                )
                            )
                        }
                        
                        edits
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse edit history", e)
                    emptyList()
                }

                Log.d(TAG, "Retrieved ${editHistory.size} edit history entries")
                Result.success(editHistory)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting edit history", e)
            Result.failure(e)
        }
    }

    // ==================== Delete Message Operations ====================

    /**
     * Delete a message locally (for current user only)
     * This marks the message as deleted in local cache without updating the database
     * @param messageId The message ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteMessageLocally(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting message locally: $messageId")

            // Store local deletion flag in SharedPreferences
            prefs.edit().putBoolean("deleted_locally_$messageId", true).apply()

            Log.d(TAG, "Message marked as deleted locally: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message locally", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a message is deleted locally
     * @param messageId The message ID to check
     * @return true if deleted locally, false otherwise
     */
    fun isMessageDeletedLocally(messageId: String): Boolean {
        return prefs.getBoolean("deleted_locally_$messageId", false)
    }

    /**
     * Delete a message for everyone
     * This updates the database to mark the message as deleted for all users
     * @param messageId The message ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteMessageForEveryone(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        // Use RetryHandler for network resilience
        val result = RetryHandler.executeWithRetryResult { attemptNumber ->
            Log.d(TAG, "Deleting message for everyone: $messageId - Attempt: $attemptNumber")

            // Use the chat service to delete the message
            val deleteResult = chatService.deleteMessage(messageId, deleteForEveryone = true)
            if (deleteResult.isFailure) {
                Log.e(TAG, "Failed to delete message", deleteResult.exceptionOrNull())
                throw deleteResult.exceptionOrNull() ?: Exception("Failed to delete message")
            }

            Log.d(TAG, "Message deleted for everyone successfully - MessageId: $messageId")
            Unit
        }
        return@withContext result
    }

    // ==================== AI Summary Caching ====================

    /**
     * Get cached AI summary for a message
     * @param messageId The message ID
     * @return Cached summary text or null if not cached or expired
     */
    fun getCachedSummary(messageId: String): String? {
        try {
            val summaryKey = SUMMARY_CACHE_PREFIX + messageId
            val expiryKey = SUMMARY_EXPIRY_PREFIX + messageId

            // Check if summary exists
            val summary = prefs.getString(summaryKey, null)
            if (summary.isNullOrEmpty()) {
                return null
            }

            // Check if summary has expired
            val expiryTime = prefs.getLong(expiryKey, 0L)
            if (System.currentTimeMillis() > expiryTime) {
                Log.d(TAG, "Cached summary expired for message: $messageId")
                // Remove expired entry
                prefs.edit()
                    .remove(summaryKey)
                    .remove(expiryKey)
                    .apply()
                return null
            }

            Log.d(TAG, "Retrieved cached summary for message: $messageId")
            return summary
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached summary", e)
            return null
        }
    }

    /**
     * Cache an AI summary for a message with 7-day expiration
     * Implements LRU cache eviction when max size is reached
     * @param messageId The message ID
     * @param summary The summary text to cache
     */
    fun cacheSummary(messageId: String, summary: String) {
        try {
            Log.d(TAG, "Caching summary for message: $messageId")

            // Check cache size and evict oldest if necessary
            evictOldestCacheEntriesIfNeeded()

            val summaryKey = SUMMARY_CACHE_PREFIX + messageId
            val expiryKey = SUMMARY_EXPIRY_PREFIX + messageId
            val expiryTime = System.currentTimeMillis() + (SUMMARY_EXPIRY_DAYS * 24 * 60 * 60 * 1000L)

            prefs.edit()
                .putString(summaryKey, summary)
                .putLong(expiryKey, expiryTime)
                .apply()

            Log.d(TAG, "Summary cached successfully for message: $messageId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching summary", e)
        }
    }

    /**
     * Clear all cached summaries
     */
    fun clearSummaryCache() {
        try {
            Log.d(TAG, "Clearing all cached summaries")

            val editor = prefs.edit()
            val allKeys = prefs.all.keys

            // Remove all summary-related keys
            allKeys.forEach { key ->
                if (key.startsWith(SUMMARY_CACHE_PREFIX) || key.startsWith(SUMMARY_EXPIRY_PREFIX)) {
                    editor.remove(key)
                }
            }

            editor.apply()
            Log.d(TAG, "Summary cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing summary cache", e)
        }
    }

    /**
     * Get the count of cached summaries
     */
    private fun getCachedSummaryCount(): Int {
        return prefs.all.keys.count { it.startsWith(SUMMARY_CACHE_PREFIX) }
    }

    /**
     * Evict oldest cache entries if max size is reached (LRU eviction)
     */
    private fun evictOldestCacheEntriesIfNeeded() {
        try {
            val currentCount = getCachedSummaryCount()
            if (currentCount < SUMMARY_CACHE_MAX_SIZE) {
                return
            }

            Log.d(TAG, "Cache size limit reached ($currentCount/$SUMMARY_CACHE_MAX_SIZE), evicting oldest entries")

            // Get all summary entries with their expiry times
            val entries = mutableListOf<Pair<String, Long>>()
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(SUMMARY_CACHE_PREFIX)) {
                    val messageId = key.removePrefix(SUMMARY_CACHE_PREFIX)
                    val expiryKey = SUMMARY_EXPIRY_PREFIX + messageId
                    val expiryTime = prefs.getLong(expiryKey, 0L)
                    entries.add(Pair(messageId, expiryTime))
                }
            }

            // Sort by expiry time (oldest first)
            entries.sortBy { it.second }

            // Remove oldest 10% of entries
            val entriesToRemove = (currentCount * 0.1).toInt().coerceAtLeast(1)
            val editor = prefs.edit()

            entries.take(entriesToRemove).forEach { (messageId, _) ->
                editor.remove(SUMMARY_CACHE_PREFIX + messageId)
                editor.remove(SUMMARY_EXPIRY_PREFIX + messageId)
            }

            editor.apply()
            Log.d(TAG, "Evicted $entriesToRemove oldest cache entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error evicting cache entries", e)
        }
    }

    /**
     * Clean up expired cache entries
     * This should be called periodically to maintain cache health
     */
    fun cleanupExpiredCacheEntries() {
        try {
            Log.d(TAG, "Cleaning up expired cache entries")

            val currentTime = System.currentTimeMillis()
            val editor = prefs.edit()
            var removedCount = 0

            prefs.all.forEach { (key, _) ->
                if (key.startsWith(SUMMARY_EXPIRY_PREFIX)) {
                    val expiryTime = prefs.getLong(key, 0L)
                    if (currentTime > expiryTime) {
                        val messageId = key.removePrefix(SUMMARY_EXPIRY_PREFIX)
                        editor.remove(SUMMARY_CACHE_PREFIX + messageId)
                        editor.remove(key)
                        removedCount++
                    }
                }
            }

            editor.apply()
            Log.d(TAG, "Removed $removedCount expired cache entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up expired cache entries", e)
        }
    }
}
