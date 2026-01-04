package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AiPersonaConfig(
    val id: String,
    @SerialName("persona_user_id") val personaUserId: String,
    @SerialName("personality_traits") val personalityTraits: JsonElement? = null,
    @SerialName("posting_schedule") val postingSchedule: JsonElement? = null,
    @SerialName("interaction_rules") val interactionRules: JsonElement? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class AiChatSession(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("chat_id") val chatId: String? = null,
    @SerialName("session_type") val sessionType: String? = null,
    @SerialName("context_data") val contextData: JsonElement? = null,
    @SerialName("is_active") val isActive: Boolean? = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class AiChatResponse(
    val id: String,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("user_message") val userMessage: String,
    @SerialName("ai_response") val aiResponse: String,
    @SerialName("response_type") val responseType: String? = null,
    @SerialName("confidence_score") val confidenceScore: Double? = null,
    @SerialName("tokens_used") val tokensUsed: Int? = null,
    @SerialName("response_time_ms") val responseTimeMs: Int? = null,
    @SerialName("user_feedback") val userFeedback: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AiSummary(
    val id: String,
    @SerialName("message_id") val messageId: String,
    @SerialName("summary_text") val summaryText: String,
    @SerialName("generated_at") val generatedAt: Long,
    @SerialName("generated_by") val generatedBy: String,
    @SerialName("model_version") val modelVersion: String? = null,
    @SerialName("character_count") val characterCount: Int,
    @SerialName("created_at") val createdAt: String? = null
)
