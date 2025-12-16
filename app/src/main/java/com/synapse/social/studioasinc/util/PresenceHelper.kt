package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Database helper functions for efficient presence management
 */
object PresenceHelper {
    
    private val dbService = SupabaseDatabaseService()
    
    /**
     * Batch update multiple user presences
     */
    suspend fun batchUpdatePresences(updates: List<PresenceUpdate>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                updates.forEach { update ->
                    val updateData = mapOf(
                        "user_id" to update.userId,
                        "is_online" to update.isOnline,
                        "last_seen" to update.lastSeen,
                        "activity_status" to update.activityStatus,
                        "current_chat_id" to update.currentChatId,
                        "updated_at" to System.currentTimeMillis()
                    )
                    dbService.upsert("user_presence", updateData)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get presence for multiple users efficiently
     */
    suspend fun getMultiplePresences(userIds: List<String>): Result<Map<String, UserPresenceData>> {
        return withContext(Dispatchers.IO) {
            try {
                val presences = mutableMapOf<String, UserPresenceData>()
                
                // For now, fetch individually - could be optimized with a single query
                userIds.forEach { userId ->
                    val result = dbService.getSingle("user_presence", "user_id", userId).getOrNull()
                    result?.let {
                        presences[userId] = UserPresenceData(
                            userId = userId,
                            isOnline = it["is_online"] as? Boolean ?: false,
                            lastSeen = (it["last_seen"] as? Number)?.toLong() ?: 0L,
                            activityStatus = it["activity_status"] as? String ?: "offline",
                            currentChatId = it["current_chat_id"] as? String
                        )
                    }
                }
                
                Result.success(presences)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Clean up old presence records (offline for more than 24 hours)
     */
    suspend fun cleanupOldPresences(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                
                // This would need to be implemented in SupabaseDatabaseService
                // For now, return success with 0 cleaned
                Result.success(0)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get users currently online in a specific chat
     */
    suspend fun getOnlineUsersInChat(chatId: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                // This would need a more complex query
                // For now, return empty list
                Result.success(emptyList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update typing status for multiple users (batch operation)
     */
    suspend fun batchUpdateTypingStatus(updates: List<TypingUpdate>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                updates.forEach { update ->
                    val updateData = mapOf(
                        "chat_id" to update.chatId,
                        "user_id" to update.userId,
                        "is_typing" to update.isTyping,
                        "timestamp" to System.currentTimeMillis(),
                        "updated_at" to System.currentTimeMillis()
                    )
                    dbService.upsert("typing_status", updateData)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Clean up old typing status records (older than 10 seconds)
     */
    suspend fun cleanupOldTypingStatus(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val tenSecondsAgo = System.currentTimeMillis() - 10000
                
                // This would need to be implemented in SupabaseDatabaseService
                // For now, return success with 0 cleaned
                Result.success(0)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get typing users for multiple chats efficiently
     */
    suspend fun getTypingUsersForChats(chatIds: List<String>, excludeUserId: String): Result<Map<String, List<String>>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = mutableMapOf<String, List<String>>()
                
                // For now, fetch individually - could be optimized
                chatIds.forEach { chatId ->
                    // This would use the existing getTypingUsers method
                    result[chatId] = emptyList()
                }
                
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

/**
 * Data class for presence updates
 */
data class PresenceUpdate(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: Long,
    val activityStatus: String,
    val currentChatId: String? = null
)

/**
 * Data class for typing updates
 */
data class TypingUpdate(
    val chatId: String,
    val userId: String,
    val isTyping: Boolean
)

/**
 * Data class for user presence data
 */
data class UserPresenceData(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: Long,
    val activityStatus: String,
    val currentChatId: String? = null
)
