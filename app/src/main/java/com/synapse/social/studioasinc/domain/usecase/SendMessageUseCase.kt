package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.local.ChatDao
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
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
        android.util.Log.d("SendMessageUseCase", "=== sendMessage START ===")
        android.util.Log.d("SendMessageUseCase", "Sending message to chatId: $chatId")
        android.util.Log.d("SendMessageUseCase", "Content length: ${content.length}, type: $messageType")

        if (content.isBlank()) {
            android.util.Log.w("SendMessageUseCase", "Message content is blank, aborting send")
            return Result.failure(IllegalArgumentException("Message content is blank"))
        }

        val senderId = authService.getCurrentUserId()
        android.util.Log.d("SendMessageUseCase", "Current user ID: $senderId")

        if (senderId == null) {
            android.util.Log.e("SendMessageUseCase", "User not authenticated")
            return Result.failure(IllegalStateException("User not authenticated"))
        }

        // Stop typing indicator when sending message
        android.util.Log.d("SendMessageUseCase", "Stopping typing indicator")
        typingIndicatorManager?.onUserStoppedTyping(chatId, senderId)

        return try {
            val result = chatRepository.sendMessage(chatId, senderId, content, messageType, replyToId)
            result.onSuccess { messageId ->
                android.util.Log.d("SendMessageUseCase", "Message sent successfully, messageId: $messageId")
            }.onFailure { exception ->
                android.util.Log.e("SendMessageUseCase", "Failed to send message: ${exception.message}", exception)
            }
            result
        } catch (e: Exception) {
            android.util.Log.e("SendMessageUseCase", "Exception in sendMessage: ${e.message}", e)
            Result.failure(e)
        } finally {
            android.util.Log.d("SendMessageUseCase", "=== sendMessage END ===")
        }
    }
}
