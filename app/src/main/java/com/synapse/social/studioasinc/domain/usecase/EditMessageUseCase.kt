package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.local.database.ChatDao
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.core.network.SupabaseClient

/**
 * Use case for editing messages
 */
class EditMessageUseCase(chatDao: ChatDao) {
    private val chatRepository = ChatRepository(chatDao, SupabaseClient.client)
    
    suspend operator fun invoke(messageId: String, newContent: String): Result<Unit> {
        return chatRepository.editMessage(messageId, newContent)
    }
}
