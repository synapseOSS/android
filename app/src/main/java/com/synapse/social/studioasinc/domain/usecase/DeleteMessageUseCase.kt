package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.local.ChatDao
import com.synapse.social.studioasinc.data.repository.ChatRepository

/**
 * Use case for deleting messages
 */
class DeleteMessageUseCase(chatDao: ChatDao) {
    private val chatRepository = ChatRepository(chatDao)
    
    suspend operator fun invoke(messageId: String): Result<Unit> {
        return chatRepository.deleteMessage(messageId)
    }
}
