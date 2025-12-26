package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.AiChatResponse
import com.synapse.social.studioasinc.model.AiChatSession
import com.synapse.social.studioasinc.model.AiPersonaConfig
import com.synapse.social.studioasinc.model.AiSummary
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository {

    private val client = SupabaseClient.client

    suspend fun getPersonaConfig(userId: String): AiPersonaConfig? = withContext(Dispatchers.IO) {
        try {
            client.from("ai_persona_config")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("persona_user_id", userId)
                    }
                }
                .decodeSingleOrNull<AiPersonaConfig>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun savePersonaConfig(config: AiPersonaConfig) = withContext(Dispatchers.IO) {
        try {
            client.from("ai_persona_config").upsert(config)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun updatePersonaConfig(userId: String, personalityTraits: JsonElement?, postingSchedule: JsonElement?) = withContext(Dispatchers.IO) {
        try {
             // Check if exists first
            val existing = getPersonaConfig(userId)
            if (existing != null) {
                client.from("ai_persona_config").update(
                    {
                        if (personalityTraits != null) set("personality_traits", personalityTraits)
                        if (postingSchedule != null) set("posting_schedule", postingSchedule)
                    }
                ) {
                    filter { eq("persona_user_id", userId) }
                }
            } else {
                // Create new, assuming id is handled by DB default gen, but data class has it mandatory.
                // We might need to generate a UUID here or adjust the data class to allow nullable ID for insertion if the DB generates it.
                // However, the schema says id is uuid and not null.
                // For now, I'll rely on update if exists. If not, I'll insert a new one with a random UUID.
                val newConfig = AiPersonaConfig(
                    id = java.util.UUID.randomUUID().toString(),
                    personaUserId = userId,
                    personalityTraits = personalityTraits,
                    postingSchedule = postingSchedule
                )
                savePersonaConfig(newConfig)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun createChatSession(userId: String, sessionType: String = "chat"): AiChatSession = withContext(Dispatchers.IO) {
        val session = AiChatSession(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            sessionType = sessionType,
            isActive = true
        )
        client.from("ai_chat_sessions").insert(session)
        session
    }

    suspend fun getChatSessions(userId: String): List<AiChatSession> = withContext(Dispatchers.IO) {
         try {
            client.from("ai_chat_sessions")
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", ascending = false)
                }
                .decodeList<AiChatSession>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveChatResponse(sessionId: String, userMessage: String, aiResponse: String): AiChatResponse = withContext(Dispatchers.IO) {
        val response = AiChatResponse(
            id = java.util.UUID.randomUUID().toString(),
            sessionId = sessionId,
            userMessage = userMessage,
            aiResponse = aiResponse
        )
        client.from("ai_chat_responses").insert(response)
        response
    }

    suspend fun getChatHistory(sessionId: String): List<AiChatResponse> = withContext(Dispatchers.IO) {
        try {
            client.from("ai_chat_responses")
                .select {
                    filter { eq("session_id", sessionId) }
                    order("created_at", ascending = true)
                }
                .decodeList<AiChatResponse>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getSummary(messageId: String): AiSummary? = withContext(Dispatchers.IO) {
        try {
            client.from("ai_summaries")
                .select {
                    filter { eq("message_id", messageId) }
                }
                .decodeSingleOrNull<AiSummary>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveSummary(summary: AiSummary) = withContext(Dispatchers.IO) {
        try {
            client.from("ai_summaries").insert(summary)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
