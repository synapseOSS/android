package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.local.ChatDao

import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing messages in real-time
 */
class ObserveMessagesUseCase(chatDao: ChatDao) {
    private val chatRepository = ChatRepository(chatDao, SupabaseClient.client)
    
    operator fun invoke(chatId: String): Flow<List<Message>> {
        // TODO: Verify cancellation handling - Ensure this flow is properly cancelled when the UI scope is destroyed
        return chatRepository.observeMessages(chatId)
    }
}
