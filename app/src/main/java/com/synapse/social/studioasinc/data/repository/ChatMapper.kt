package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.local.ChatEntity
import com.synapse.social.studioasinc.model.Chat

object ChatMapper {

    fun toEntity(chat: Chat): ChatEntity {
        return ChatEntity(
            id = chat.id,
            lastMessage = chat.lastMessage,
            timestamp = chat.lastMessageTime ?: 0,
            isGroup = chat.isGroup,
            lastMessageSender = chat.lastMessageSender,
            createdAt = chat.createdAt,
            isActive = chat.isActive
        )
    }

    fun toModel(entity: ChatEntity): Chat {
        return Chat(
            id = entity.id,
            isGroup = entity.isGroup,
            lastMessage = entity.lastMessage,
            lastMessageTime = entity.timestamp,
            lastMessageSender = entity.lastMessageSender,
            createdAt = entity.createdAt,
            isActive = entity.isActive
        )
    }
}
