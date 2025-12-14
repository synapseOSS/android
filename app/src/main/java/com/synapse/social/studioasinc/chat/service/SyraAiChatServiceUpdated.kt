package com.synapse.social.studioasinc.chat.service

import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AiChatRequest(
    val message: String,
    val conversation_id: String? = null,
    val user_api_key: String? = null,
    val provider: String? = null,
    val context: Map<String, Any>? = null
)

@Serializable
data class AiChatResponse(
    val response: String,
    val conversationId: String,
    val usedUserKey: Boolean,
    val provider: String,
    val tokensUsed: Int? = null
)

@Singleton
class SyraAiChatServiceUpdated @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    suspend fun sendMessage(
        message: String,
        conversationId: String? = null,
        context: Map<String, Any>? = null
    ): Result<AiChatResponse> {
        return try {
            _isLoading.value = true
            
            val token = supabaseClient.auth.currentAccessTokenOrNull()
                ?: return Result.failure(Exception("Not authenticated"))

            // Get user's preferred API key and provider
            val (userApiKey, provider) = getUserApiKey()

            val request = AiChatRequest(
                message = message,
                conversation_id = conversationId,
                user_api_key = userApiKey,
                provider = provider,
                context = context
            )

            val response = supabaseClient.functions.invoke(
                function = "ai-chat-assistant",
                body = request,
                headers = mapOf("Authorization" to "Bearer $token")
            )

            val result = Json.decodeFromString<Map<String, Any>>(response.data.toString())
            
            if (result["success"] == true) {
                val aiResponse = AiChatResponse(
                    response = result["response"].toString(),
                    conversationId = result["conversation_id"].toString(),
                    usedUserKey = result["used_user_key"] as? Boolean ?: false,
                    provider = result["provider"].toString(),
                    tokensUsed = result["tokens_used"] as? Int
                )
                
                // Update usage count if user key was used
                if (aiResponse.usedUserKey && aiResponse.tokensUsed != null) {
                    updateUsageCount(provider, aiResponse.tokensUsed)
                }
                
                Result.success(aiResponse)
            } else {
                Result.failure(Exception(result["error"].toString()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun getUserApiKey(): Pair<String?, String> {
        return try {
            val token = supabaseClient.auth.currentAccessTokenOrNull() ?: return null to "platform"
            
            // Get user's preferred provider
            val settingsResponse = supabaseClient.from("ai_provider_settings")
                .select("preferred_provider")
                .eq("user_id", supabaseClient.auth.currentUserOrNull()?.id ?: "")
                .singleOrNull()
            
            val preferredProvider = settingsResponse?.get("preferred_provider") as? String ?: "platform"
            
            if (preferredProvider == "platform") {
                return null to "platform"
            }

            // Get user's API key for the preferred provider
            val keyResponse = supabaseClient.functions.invoke(
                function = "api-key-manager",
                body = emptyMap<String, Any>(),
                headers = mapOf(
                    "Authorization" to "Bearer $token",
                    "X-Provider" to preferredProvider
                )
            )

            val keyResult = Json.decodeFromString<Map<String, Any>>(keyResponse.data.toString())
            
            if (keyResult["has_key"] == true && keyResult["use_platform_key"] == false) {
                keyResult["api_key"]?.toString() to preferredProvider
            } else {
                null to "platform"
            }
        } catch (e: Exception) {
            null to "platform"
        }
    }

    private suspend fun updateUsageCount(provider: String, tokensUsed: Int) {
        try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return
            
            // Increment usage count
            supabaseClient.from("user_api_keys")
                .update(mapOf("usage_count" to "usage_count + $tokensUsed"))
                .eq("user_id", userId)
                .eq("provider", provider)
                .eq("is_active", true)
        } catch (e: Exception) {
            // Log error but don't fail the main operation
        }
    }

    suspend fun getSmartReplies(
        message: String,
        conversationHistory: List<String> = emptyList()
    ): Result<List<String>> {
        return try {
            val token = supabaseClient.auth.currentAccessTokenOrNull()
                ?: return Result.failure(Exception("Not authenticated"))

            val (userApiKey, provider) = getUserApiKey()

            val request = mapOf(
                "message" to message,
                "conversation_history" to conversationHistory,
                "user_api_key" to userApiKey,
                "provider" to provider
            )

            val response = supabaseClient.functions.invoke(
                function = "smart-replies",
                body = request,
                headers = mapOf("Authorization" to "Bearer $token")
            )

            val result = Json.decodeFromString<Map<String, Any>>(response.data.toString())
            
            if (result["success"] == true) {
                val suggestions = result["suggestions"] as? List<*> ?: emptyList<String>()
                Result.success(suggestions.map { it.toString() })
            } else {
                Result.failure(Exception(result["error"].toString()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
