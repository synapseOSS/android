package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.local.ChatDao

import com.synapse.social.studioasinc.data.repository.ChatRepository

/**
 * Use case for sending messages
 */
class SendMessageUseCase(chatDao: ChatDao) {
    private val chatRepository = ChatRepository(chatDao)
    
    suspend operator fun invoke(
        chatId: String,
        senderId: String,
        content: String,
        messageType: String = "text",
        replyToId: String? = null
    ): Result<String> {
        return chatRepository.sendMessage(chatId, senderId, content, messageType, replyToId)
    }
}
