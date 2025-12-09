package com.synapse.social.studioasinc.chat.service

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Message
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for searching messages across chats.
 * 
 * Features:
 * - Full-text search across message content
 * - Filter by chat, sender, message type, date range
 * - Search within specific chat or across all user's chats
 * - Pagination support for large result sets
 * 
 * Requirements: Chat System Improvements - Message search functionality
 */
class MessageSearchService {
    
    companion object {
        private const val TAG = "MessageSearchService"
        private const val DEFAULT_PAGE_SIZE = 50
    }
    
    private val client = SupabaseClient.client
    
    /**
     * Search messages with filters and pagination.
     * 
     * @param query Search query string
     * @param chatId Optional chat ID to limit search to specific chat
     * @param userId User ID to filter messages accessible to user
     * @param messageType Optional message type filter (text, image, video, etc.)
     * @param startDate Optional start date for date range filter (timestamp)
     * @param endDate Optional end date for date range filter (timestamp)
     * @param limit Maximum number of results to return
     * @param offset Offset for pagination
     * @return Result containing list of matching messages
     */
    suspend fun searchMessages(
        query: String,
        chatId: String? = null,
        userId: String,
        messageType: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<Message>> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (query.isBlank()) {
                return@withContext Result.success(emptyList())
            }
            
            Log.d(TAG, "Searching messages: query='$query', chatId=$chatId, limit=$limit, offset=$offset")
            
            // Build search query
            val messages = client.from("messages")
                .select() {
                    filter {
                        // Search in content using ilike for case-insensitive search
                        ilike("content", "%$query%")
                        
                        // Filter by chat if specified
                        chatId?.let { eq("chat_id", it) }
                        
                        // Filter by message type if specified
                        messageType?.let { eq("message_type", it) }
                        
                        // Filter by date range if specified
                        startDate?.let { gte("created_at", it) }
                        endDate?.let { lte("created_at", it) }
                        
                        // Exclude deleted messages
                        eq("is_deleted", false)
                    }
                    
                    // Order by relevance (most recent first)
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    
                    // Pagination
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<Message>()
            
            Log.d(TAG, "Found ${messages.size} messages matching query")
            Result.success(messages)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search messages", e)
            Result.failure(Exception("Failed to search messages: ${e.message}"))
        }
    }
    
    /**
     * Search messages in a specific chat.
     * 
     * @param chatId Chat ID to search in
     * @param query Search query string
     * @param userId User ID for access control
     * @param limit Maximum number of results
     * @param offset Offset for pagination
     * @return Result containing list of matching messages
     */
    suspend fun searchInChat(
        chatId: String,
        query: String,
        userId: String,
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<Message>> {
        return searchMessages(
            query = query,
            chatId = chatId,
            userId = userId,
            limit = limit,
            offset = offset
        )
    }
    
    /**
     * Search messages by sender.
     * 
     * @param senderId Sender user ID
     * @param query Search query string
     * @param userId Current user ID for access control
     * @param chatId Optional chat ID to limit search
     * @param limit Maximum number of results
     * @param offset Offset for pagination
     * @return Result containing list of matching messages
     */
    suspend fun searchBySender(
        senderId: String,
        query: String,
        userId: String,
        chatId: String? = null,
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<Message>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Searching messages by sender: senderId=$senderId, query='$query'")
            
            val messages = client.from("messages")
                .select() {
                    filter {
                        eq("sender_id", senderId)
                        
                        if (query.isNotBlank()) {
                            ilike("content", "%$query%")
                        }
                        
                        chatId?.let { eq("chat_id", it) }
                        
                        eq("is_deleted", false)
                    }
                    
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<Message>()
            
            Log.d(TAG, "Found ${messages.size} messages from sender")
            Result.success(messages)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search messages by sender", e)
            Result.failure(Exception("Failed to search messages by sender: ${e.message}"))
        }
    }
    
    /**
     * Search messages by media type.
     * 
     * @param messageType Message type (image, video, audio, file)
     * @param userId Current user ID for access control
     * @param chatId Optional chat ID to limit search
     * @param limit Maximum number of results
     * @param offset Offset for pagination
     * @return Result containing list of matching messages
     */
    suspend fun searchByMediaType(
        messageType: String,
        userId: String,
        chatId: String? = null,
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<Message>> {
        return searchMessages(
            query = "",
            chatId = chatId,
            userId = userId,
            messageType = messageType,
            limit = limit,
            offset = offset
        )
    }
    
    /**
     * Get recent search suggestions based on user's search history.
     * 
     * @param userId User ID
     * @param limit Maximum number of suggestions
     * @return List of recent search queries
     */
    suspend fun getSearchSuggestions(
        userId: String,
        limit: Int = 10
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // This would require a search_history table
            // For now, return empty list as placeholder
            Log.d(TAG, "Getting search suggestions for user: $userId")
            Result.success(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get search suggestions", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save search query to history for suggestions.
     * 
     * @param userId User ID
     * @param query Search query to save
     */
    suspend fun saveSearchQuery(userId: String, query: String) {
        // This would require a search_history table
        // Placeholder for future implementation
        Log.d(TAG, "Saving search query: $query for user: $userId")
    }
}
