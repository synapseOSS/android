package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.local.ChatDao
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Chat
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting user's chats
 */
class GetUserChatsUseCase(chatDao: ChatDao) {
    private val chatRepository = ChatRepository(chatDao, SupabaseClient.client)
    
    operator fun invoke(): Flow<Result<List<Chat>>> {
        return chatRepository.getUserChats()
    }
}
