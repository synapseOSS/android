package com.synapse.social.studioasinc.chat.service

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Service for Syra AI chat integration using BaaS Edge Functions
 */
class SyraAiChatService {
    
    private val supabase = SupabaseClient.client
    
    // Smart reply suggestions state
    private val _smartReplies = MutableStateFlow<List<String>>(emptyList())
    val smartReplies: StateFlow<List<String>> = _smartReplies.asStateFlow()
    
    private val _isLoadingReplies = MutableStateFlow(false)
    val isLoadingReplies: StateFlow<Boolean> = _isLoadingReplies.asStateFlow()
    
    /**
     * Check if message mentions @syra
     */
    fun containsSyraMention(message: String): Boolean {
        return message.contains("@syra", ignoreCase = true)
    }
    
    /**
     * Send message to Syra AI assistant
     */
    suspend fun sendToSyra(
        userId: String,
        message: String,
        chatId: String? = null
    ): SyraResponse? {
        return try {
            val request = SyraRequest(
                user_id = userId,
                message = message,
                chat_id = chatId,
                session_type = "assistant"
            )
            
            val response = supabase.functions.invoke(
                function = "ai-chat-assistant",
                body = request
            ).bodyAsText()
            
            val jsonResponse = Json.parseToJsonElement(response ?: "{}").jsonObject
            
            SyraResponse(
                response = jsonResponse["response"]?.jsonPrimitive?.content ?: "",
                sessionId = jsonResponse["session_id"]?.jsonPrimitive?.content ?: "",
                responseTimeMs = jsonResponse["response_time_ms"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                type = jsonResponse["type"]?.jsonPrimitive?.content ?: "assistant"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate smart reply suggestions for a message
     */
    suspend fun generateSmartReplies(
        userId: String,
        message: String,
        chatContext: List<String> = emptyList()
    ) {
        _isLoadingReplies.value = true
        
        try {
            val request = SmartReplyRequest(
                user_id = userId,
                message = message,
                chat_context = chatContext
            )
            
            val response = supabase.functions.invoke(
                function = "smart-replies",
                body = request
            ).bodyAsText()
            
            val jsonResponse = Json.parseToJsonElement(response ?: "{}").jsonObject
            val suggestions = jsonResponse["suggestions"]?.let { element ->
                Json.decodeFromJsonElement<List<String>>(element)
            } ?: emptyList()
            
            _smartReplies.value = suggestions
        } catch (e: Exception) {
            _smartReplies.value = emptyList()
        } finally {
            _isLoadingReplies.value = false
        }
    }
    
    /**
     * Clear smart reply suggestions
     */
    fun clearSmartReplies() {
        _smartReplies.value = emptyList()
    }
    
    /**
     * Submit feedback for AI response
     */
    suspend fun submitFeedback(
        responseId: String,
        rating: Int,
        comment: String? = null
    ) {
        try {
            val request = mapOf(
                "response_id" to responseId,
                "rating" to rating,
                "comment" to comment
            )
            
            supabase.functions.invoke(
                function = "ai-chat-analytics",
                body = request
            )
        } catch (e: Exception) {
            android.util.Log.e("SyraAiChatService", "Failed to submit feedback", e)
        }
    }
    
    /**
     * Get AI chat analytics
     */
    suspend fun getAnalytics(): AiChatAnalytics? {
        return try {
            val response = supabase.functions.invoke(
                function = "ai-chat-analytics/stats"
            ).bodyAsText()
            
            val jsonResponse = Json.parseToJsonElement(response ?: "{}").jsonObject
            
            AiChatAnalytics(
                totalSessions = jsonResponse["total_sessions"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                totalResponses = jsonResponse["total_responses"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                avgResponseTimeMs = jsonResponse["avg_response_time_ms"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                avgUserRating = jsonResponse["avg_user_rating"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            )
        } catch (e: Exception) {
            null
        }
    }
}

// Data Models
@Serializable
data class SyraRequest(
    val user_id: String,
    val message: String,
    val chat_id: String? = null,
    val session_type: String = "assistant"
)

@Serializable
data class SmartReplyRequest(
    val user_id: String,
    val message: String,
    val chat_context: List<String> = emptyList()
)

data class SyraResponse(
    val response: String,
    val sessionId: String,
    val responseTimeMs: Int,
    val type: String
)

data class AiChatAnalytics(
    val totalSessions: Int,
    val totalResponses: Int,
    val avgResponseTimeMs: Int,
    val avgUserRating: Double
)
