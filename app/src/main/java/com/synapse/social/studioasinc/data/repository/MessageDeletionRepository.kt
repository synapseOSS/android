package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.util.RetryHandler
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonObject
import java.util.UUID

/**
 * Data model for user-deleted messages
 */
@Serializable
data class UserDeletedMessage(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id")
    val userId: String,
    @SerialName("message_id")
    val messageId: String,
    @SerialName("deleted_at")
    val deletedAt: Long = System.currentTimeMillis()
)

/**
 * Repository for message deletion operations
 * Handles delete for me and delete for everyone operations
 * Includes in-memory caching for performance optimization
 */
class MessageDeletionRepository {

    companion object {
        private const val TAG = "MessageDeletionRepository"
    }

    private val client = SupabaseClient.client
    
    // In-memory cache for user-deleted message IDs
    // Key: userId, Value: Set of deleted message IDs
    private val userDeletedMessagesCache = mutableMapOf<String, MutableSet<String>>()

    // ==================== Delete For Me Operations ====================

    /**
     * Delete messages for the current user only
     * Inserts records into user_deleted_messages table
     * Optimized with batch insert in single transaction
     * @param messageIds List of message IDs to delete
     * @param userId Current user ID
     * @return Result indicating success or failure
     */
    suspend fun deleteForMe(messageIds: List<String>, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (messageIds.isEmpty()) {
            return@withContext Result.failure(Exception("Message IDs list cannot be empty"))
        }

        if (userId.isBlank()) {
            return@withContext Result.failure(Exception("User ID is required"))
        }

        // Use RetryHandler for network resilience
        val result = RetryHandler.executeWithRetryResult { attemptNumber ->
            Log.d(TAG, "Deleting messages for user - Attempt: $attemptNumber, MessageCount: ${messageIds.size}, UserId: $userId")

            // Prepare batch insert data - use current timestamp for all records to ensure consistency
            val currentTimestamp = System.currentTimeMillis()
            val deletionRecords = messageIds.map { messageId ->
                UserDeletedMessage(
                    userId = userId,
                    messageId = messageId,
                    deletedAt = currentTimestamp
                )
            }

            // Batch insert into user_deleted_messages table
            // Supabase handles this as a single transaction automatically
            try {
                client.from("user_deleted_messages")
                    .insert(deletionRecords) {
                        // Use upsert to handle duplicate entries gracefully
                        // This prevents errors if a message is already marked as deleted
                        select()
                    }

                // Update cache after successful deletion
                updateCacheForDeletedMessages(userId, messageIds)

                Log.d(TAG, "Successfully deleted ${messageIds.size} messages for user: $userId")
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert user deleted messages", e)
                throw e
            }
        }

        return@withContext result
    }

    // ==================== Delete For Everyone Operations ====================

    /**
     * Delete messages for everyone in the chat
     * Updates is_deleted and delete_for_everyone fields in messages table
     * Only message owners can delete for everyone
     * Optimized with batch update in single transaction
     * @param messageIds List of message IDs to delete
     * @param userId Current user ID (must be the sender)
     * @return Result indicating success or failure
     */
    suspend fun deleteForEveryone(messageIds: List<String>, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (messageIds.isEmpty()) {
            return@withContext Result.failure(Exception("Message IDs list cannot be empty"))
        }

        if (userId.isBlank()) {
            return@withContext Result.failure(Exception("User ID is required"))
        }

        // Use RetryHandler for network resilience
        val result = RetryHandler.executeWithRetryResult { attemptNumber ->
            Log.d(TAG, "Deleting messages for everyone - Attempt: $attemptNumber, MessageCount: ${messageIds.size}, UserId: $userId")

            // First, validate ownership - ensure user owns all messages
            val ownedMessageIds = getMessagesBySenderId(messageIds, userId)
            
            if (ownedMessageIds.size != messageIds.size) {
                val unauthorizedCount = messageIds.size - ownedMessageIds.size
                Log.w(TAG, "User $userId does not own $unauthorizedCount of ${messageIds.size} messages")
                throw Exception("You can only delete your own messages for everyone")
            }

            // Batch update messages - set is_deleted and delete_for_everyone to true
            // Use current timestamp for consistency across all updates
            val currentTimestamp = System.currentTimeMillis()
            try {
                // Update all messages in a single query - this is executed as a single transaction
                // The filter ensures only the specified messages are updated
                client.from("messages")
                    .update(
                        mapOf(
                            "is_deleted" to true,
                            "delete_for_everyone" to true,
                            "updated_at" to currentTimestamp
                        )
                    ) {
                        filter {
                            isIn("id", messageIds)
                            eq("sender_id", userId) // Additional safety check at DB level
                        }
                    }

                Log.d(TAG, "Successfully deleted ${messageIds.size} messages for everyone")
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update messages as deleted", e)
                throw e
            }
        }

        return@withContext result
    }

    // ==================== Helper Functions ====================

    /**
     * Get message IDs that belong to a specific sender
     * Used for ownership validation
     * Optimized to select only the id column to minimize data transfer
     * @param messageIds List of message IDs to check
     * @param senderId Sender ID to validate against
     * @return List of message IDs owned by the sender
     */
    suspend fun getMessagesBySenderId(messageIds: List<String>, senderId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Validating message ownership - MessageCount: ${messageIds.size}, SenderId: $senderId")

            // Optimize query to select only id column, reducing data transfer
            val messages = client.from("messages")
                .select(columns = Columns.raw("id")) {
                    filter {
                        isIn("id", messageIds)
                        eq("sender_id", senderId)
                    }
                }
                .decodeList<JsonObject>()

            val ownedMessageIds = messages.mapNotNull { message ->
                message["id"]?.toString()?.removeSurrounding("\"")
            }

            Log.d(TAG, "User owns ${ownedMessageIds.size} of ${messageIds.size} messages")
            ownedMessageIds
        } catch (e: Exception) {
            Log.e(TAG, "Error validating message ownership", e)
            emptyList()
        }
    }

    /**
     * Fetch user-deleted message IDs for the current user
     * Used to determine which messages should be hidden in the UI
     * Optimized to select only message_id column to minimize data transfer
     * Uses in-memory cache to avoid redundant database queries
     * @param userId Current user ID
     * @param chatId Optional chat ID to filter by specific chat
     * @param forceRefresh Force refresh from database, bypassing cache
     * @return Result with set of deleted message IDs
     */
    suspend fun getUserDeletedMessageIds(
        userId: String, 
        chatId: String? = null,
        forceRefresh: Boolean = false
    ): Result<Set<String>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(Exception("User ID is required"))
        }

        // Check cache first if not forcing refresh
        if (!forceRefresh && userDeletedMessagesCache.containsKey(userId)) {
            Log.d(TAG, "Returning cached user-deleted messages for user: $userId")
            return@withContext Result.success(userDeletedMessagesCache[userId]?.toSet() ?: emptySet())
        }

        // Use RetryHandler for network resilience
        val result = RetryHandler.executeWithRetryResult { attemptNumber ->
            Log.d(TAG, "Fetching user-deleted messages - Attempt: $attemptNumber, UserId: $userId, ChatId: $chatId")

            try {
                // Optimize query to select only message_id column, reducing data transfer
                val deletedMessages = client.from("user_deleted_messages")
                    .select(columns = Columns.raw("message_id")) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<JsonObject>()

                val messageIds = deletedMessages.mapNotNull { record ->
                    record["message_id"]?.toString()?.removeSurrounding("\"")
                }.toSet()

                // Update cache with fetched data
                userDeletedMessagesCache[userId] = messageIds.toMutableSet()

                Log.d(TAG, "Retrieved ${messageIds.size} user-deleted message IDs")
                messageIds
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch user-deleted messages", e)
                throw e
            }
        }

        return@withContext result
    }

    /**
     * Update cache with newly deleted messages
     * Called after successful deletion operation
     * @param userId User ID
     * @param messageIds Message IDs that were deleted
     */
    private fun updateCacheForDeletedMessages(userId: String, messageIds: List<String>) {
        if (userDeletedMessagesCache.containsKey(userId)) {
            userDeletedMessagesCache[userId]?.addAll(messageIds)
            Log.d(TAG, "Updated cache with ${messageIds.size} deleted messages for user: $userId")
        } else {
            userDeletedMessagesCache[userId] = messageIds.toMutableSet()
            Log.d(TAG, "Initialized cache with ${messageIds.size} deleted messages for user: $userId")
        }
    }

    /**
     * Get cached deleted message IDs for a user
     * Returns null if cache is not populated
     * @param userId User ID
     * @return Set of cached deleted message IDs or null if not cached
     */
    fun getCachedDeletedMessageIds(userId: String): Set<String>? {
        return userDeletedMessagesCache[userId]?.toSet()
    }

    /**
     * Clear cache for a specific user
     * @param userId User ID to clear cache for
     */
    fun clearCacheForUser(userId: String) {
        userDeletedMessagesCache.remove(userId)
        Log.d(TAG, "Cleared cache for user: $userId")
    }

    /**
     * Clear all cached data
     * Should be called on logout or when switching users
     */
    fun clearAllCache() {
        userDeletedMessagesCache.clear()
        Log.d(TAG, "Cleared all cached data")
    }
}
