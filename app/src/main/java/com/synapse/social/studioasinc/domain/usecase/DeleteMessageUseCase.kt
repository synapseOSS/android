package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.local.ChatDao
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.core.network.SupabaseClient

/**
 * Use case for deleting messages
 */
class DeleteMessageUseCase(chatDao: ChatDao) {
    private val chatRepository = ChatRepository(chatDao, SupabaseClient.client)
    
    suspend operator fun invoke(messageId: String): Result<Unit> {
        return chatRepository.deleteMessage(messageId)
    }
}
