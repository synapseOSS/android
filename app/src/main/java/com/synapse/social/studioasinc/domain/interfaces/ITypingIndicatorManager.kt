package com.synapse.social.studioasinc.domain.interfaces

/**
 * Interface for managing typing indicators
 */
interface ITypingIndicatorManager {
    fun onUserStoppedTyping(chatId: String, userId: String)
}
