package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.local.database.ChatDao
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.remote.services.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.domain.interfaces.ITypingIndicatorManager

/**
 * Use case for sending messages
 */
class SendMessageUseCase(
    chatDao: ChatDao,
    private val authService: IAuthenticationService
) {
    private val chatRepository = ChatRepository(chatDao, SupabaseClient.client)
    
    suspend operator fun invoke(
        chatId: String,
        content: String,
        messageType: String = "text",
        replyToId: String? = null,
        typingIndicatorManager: ITypingIndicatorManager? = null
    ): Result<String> {
        // Domain layer should not use Android logging directly
        // Consider using a domain-specific logger interface
        
        if (content.isBlank()) {
            return Result.failure(IllegalArgumentException("Message content is blank"))
        }

        val senderId = authService.getCurrentUserId()

        if (senderId == null) {
            return Result.failure(IllegalStateException("User not authenticated"))
        }

        // Stop typing indicator when sending message
        typingIndicatorManager?.onUserStoppedTyping(chatId, senderId)

        return try {
            val result = chatRepository.sendMessage(chatId, senderId, content, messageType, replyToId)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
