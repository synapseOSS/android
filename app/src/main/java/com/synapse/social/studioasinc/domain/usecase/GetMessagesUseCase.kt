package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.local.database.ChatDao

import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.model.Message
import com.synapse.social.studioasinc.core.network.SupabaseClient

/**
 * Use case for getting messages
 */
class GetMessagesUseCase(chatDao: ChatDao) {
    private val chatRepository = ChatRepository(chatDao, SupabaseClient.client)
    
    suspend operator fun invoke(chatId: String, limit: Int = 50, beforeTimestamp: Long? = null): Result<List<Message>> {
        return chatRepository.getMessages(chatId, limit, beforeTimestamp)
    }
}
