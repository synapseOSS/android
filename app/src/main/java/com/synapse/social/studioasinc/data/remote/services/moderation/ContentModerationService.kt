package com.synapse.social.studioasinc.data.remote.services.moderation

import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Service for AI-powered content moderation using BaaS Edge Functions
 */
class ContentModerationService {
    
    private val supabase = SupabaseClient.client
    
    // Moderation state
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    /**
     * Analyze content for policy violations
     */
    suspend fun analyzeContent(
        content: String,
        contentId: String,
        contentType: ContentType,
        userId: String
    ): ModerationResult? {
        _isAnalyzing.value = true
        
        return try {
            val request = ModerationRequest(
                content = content,
                content_id = contentId,
                content_type = contentType.name.lowercase(),
                user_id = userId
            )
            
            val response = supabase.functions.invoke(
                function = "ai-content-moderator",
                body = request
            ).bodyAsText()
            
            val jsonResponse = Json.parseToJsonElement(response ?: "{}").jsonObject
            
            ModerationResult(
                flagged = jsonResponse["flagged"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                violations = jsonResponse["violations"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                actions = jsonResponse["actions"]?.let { element ->
                    Json.decodeFromJsonElement<List<String>>(element)
                } ?: emptyList(),
                scores = jsonResponse["scores"]?.let { element ->
                    Json.decodeFromJsonElement<ModerationScores>(element)
                } ?: ModerationScores()
            )
        } catch (e: Exception) {
            android.util.Log.e("ContentModerationService", "Failed to analyze content", e)
            null
        } finally {
            _isAnalyzing.value = false
        }
    }
    
    /**
     * Get real-time moderation feedback for typing
     */
    suspend fun getRealtimeFeedback(content: String): ModerationFeedback {
        if (content.length < 10) return ModerationFeedback.Safe
        
        // Quick local checks for immediate feedback
        val toxicWords = listOf("hate", "stupid", "idiot", "kill", "die")
        val spamWords = listOf("buy now", "click here", "free money")
        
        val lowerContent = content.lowercase()
        
        return when {
            toxicWords.any { lowerContent.contains(it) } -> ModerationFeedback.Warning("Consider rephrasing this message")
            spamWords.any { lowerContent.contains(it) } -> ModerationFeedback.Warning("This looks like spam content")
            content.length > 1000 -> ModerationFeedback.Info("Long message - consider breaking it up")
            else -> ModerationFeedback.Safe
        }
    }
    
    /**
     * Report content manually
     */
    suspend fun reportContent(
        contentId: String,
        contentType: ContentType,
        reason: String,
        description: String? = null,
        reporterId: String
    ): Boolean {
        return try {
            val request = mapOf(
                "content_id" to contentId,
                "content_type" to contentType.name.lowercase(),
                "reason" to reason,
                "description" to description,
                "reporter_id" to reporterId
            )
            
            supabase.functions.invoke(
                function = "moderation-actions",
                body = request
            )
            
            true
        } catch (e: Exception) {
            android.util.Log.e("ContentModerationService", "Failed to report content", e)
            false
        }
    }
    
    /**
     * Get moderation statistics
     */
    suspend fun getModerationStats(): ModerationStats? {
        return try {
            val response = supabase.functions.invoke(
                function = "moderation-dashboard/stats"
            ).bodyAsText()
            
            val jsonResponse = Json.parseToJsonElement(response ?: "{}").jsonObject
            
            ModerationStats(
                totalFlagged = jsonResponse["total_flagged"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                pending = jsonResponse["pending"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                reviewed = jsonResponse["reviewed"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                autoActioned = jsonResponse["auto_actioned"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            android.util.Log.e("ContentModerationService", "Failed to get moderation stats", e)
            null
        }
    }
}

// Data Models
@Serializable
data class ModerationRequest(
    val content: String,
    val content_id: String,
    val content_type: String,
    val user_id: String
)
