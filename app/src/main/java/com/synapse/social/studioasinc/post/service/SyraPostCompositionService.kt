package com.synapse.social.studioasinc.post.service

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Service for Syra AI post composition assistance using BaaS Edge Functions
 */
class SyraPostCompositionService {
    
    private val supabase = SupabaseClient.client
    
    // Post enhancement state
    private val _isEnhancing = MutableStateFlow(false)
    val isEnhancing: StateFlow<Boolean> = _isEnhancing.asStateFlow()
    
    private val _hashtagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val hashtagSuggestions: StateFlow<List<String>> = _hashtagSuggestions.asStateFlow()
    
    private val _syraAdvice = MutableStateFlow<String?>(null)
    val syraAdvice: StateFlow<String?> = _syraAdvice.asStateFlow()
    
    /**
     * Get Syra's advice for post content
     */
    suspend fun getSyraAdvice(
        content: String,
        userId: String,
        requestType: String = "general"
    ): SyraPostAdvice? {
        _isEnhancing.value = true
        
        return try {
            val request = SyraAdviceRequest(
                content = content,
                requestType = requestType,
                userId = userId
            )
            
            val response = supabase.functions.invoke(
                function = "syra-content-generator",
                body = request
            ).bodyAsText()
            
            val jsonResponse = Json.parseToJsonElement(response ?: "{}").jsonObject
            
            val advice = SyraPostAdvice(
                advice = jsonResponse["advice"]?.jsonPrimitive?.content ?: "",
                reasoning = jsonResponse["reasoning"]?.jsonPrimitive?.content ?: "",
                suggestions = jsonResponse["suggestions"]?.let { element ->
                    Json.decodeFromJsonElement<List<String>>(element)
                } ?: emptyList(),
                hashtags = jsonResponse["hashtags"]?.let { element ->
                    Json.decodeFromJsonElement<List<String>>(element)
                } ?: emptyList()
            )
            
            _syraAdvice.value = advice.advice
            _hashtagSuggestions.value = advice.hashtags
            
            advice
        } catch (e: Exception) {
            null
        } finally {
            _isEnhancing.value = false
        }
    }
    
    /**
     * Enhance post content with AI suggestions
     */
    suspend fun enhanceContent(
        content: String,
        enhancementType: EnhancementType,
        userId: String
    ): ContentEnhancement? {
        _isEnhancing.value = true
        
        return try {
            val request = ContentEnhancementRequest(
                content = content,
                enhancement_type = enhancementType.name.lowercase(),
                user_id = userId
            )
            
            val response = supabase.functions.invoke(
                function = "syra-content-generator",
                body = request
            ).bodyAsText()
            
            val jsonResponse = Json.parseToJsonElement(response ?: "{}").jsonObject
            
            ContentEnhancement(
                enhancedContent = jsonResponse["enhanced_content"]?.jsonPrimitive?.content ?: content,
                suggestions = jsonResponse["suggestions"]?.let { element ->
                    Json.decodeFromJsonElement<List<String>>(element)
                } ?: emptyList(),
                syraComment = jsonResponse["syra_tip"]?.jsonPrimitive?.content,
                confidence = jsonResponse["confidence"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            )
        } catch (e: Exception) {
            null
        } finally {
            _isEnhancing.value = false
        }
    }
    
    /**
     * Generate hashtag suggestions
     */
    suspend fun generateHashtags(content: String, userId: String) {
        try {
            val request = mapOf(
                "content" to content,
                "user_id" to userId,
                "type" to "hashtags"
            )
            
            val response = supabase.functions.invoke(
                function = "syra-content-generator",
                body = request
            ).bodyAsText()
            
            val jsonResponse = Json.parseToJsonElement(response ?: "{}").jsonObject
            val hashtags = jsonResponse["hashtags"]?.let { element ->
                Json.decodeFromJsonElement<List<String>>(element)
            } ?: emptyList()
            
            _hashtagSuggestions.value = hashtags
        } catch (e: Exception) {
            _hashtagSuggestions.value = emptyList()
        }
    }
    
    /**
     * Get content ideas from Syra
     */
    suspend fun getContentIdeas(
        topic: String? = null,
        userId: String
    ): List<ContentIdea> {
        return try {
            val request = mapOf(
                "type" to "content_ideas",
                "topic" to topic,
                "user_id" to userId
            )
            
            val response = supabase.functions.invoke(
                function = "syra-content-generator",
                body = request
            ).bodyAsText()
            
            val jsonResponse = Json.parseToJsonElement(response ?: "{}").jsonObject
            jsonResponse["ideas"]?.let { element ->
                Json.decodeFromJsonElement<List<ContentIdea>>(element)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clear current suggestions
     */
    fun clearSuggestions() {
        _hashtagSuggestions.value = emptyList()
        _syraAdvice.value = null
    }
    
    /**
     * Get optimal posting time suggestion
     */
    suspend fun getOptimalPostingTime(userId: String): PostingTimeAdvice? {
        return try {
            val request = mapOf(
                "type" to "optimal_time",
                "user_id" to userId
            )
            
            val response = supabase.functions.invoke(
                function = "syra-content-generator",
                body = request
            ).bodyAsText()
            
            val jsonResponse = Json.parseToJsonElement(response ?: "{}").jsonObject
            
            PostingTimeAdvice(
                recommendedTime = jsonResponse["recommended_time"]?.jsonPrimitive?.content ?: "",
                reason = jsonResponse["reason"]?.jsonPrimitive?.content ?: "",
                engagementScore = jsonResponse["engagement_score"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            )
        } catch (e: Exception) {
            null
        }
    }
}

// Data Models
enum class EnhancementType {
    GRAMMAR, TONE, ENGAGEMENT, CLARITY, HASHTAGS
}

@Serializable
data class SyraAdviceRequest(
    val content: String,
    val requestType: String,
    val userId: String
)

@Serializable
data class ContentEnhancementRequest(
    val content: String,
    val enhancement_type: String,
    val user_id: String
)

data class SyraPostAdvice(
    val advice: String,
    val reasoning: String,
    val suggestions: List<String>,
    val hashtags: List<String>
)

data class ContentEnhancement(
    val enhancedContent: String,
    val suggestions: List<String>,
    val syraComment: String?,
    val confidence: Double
)

@Serializable
data class ContentIdea(
    val title: String,
    val description: String,
    val hashtags: List<String>,
    val category: String
)

data class PostingTimeAdvice(
    val recommendedTime: String,
    val reason: String,
    val engagementScore: Double
)
