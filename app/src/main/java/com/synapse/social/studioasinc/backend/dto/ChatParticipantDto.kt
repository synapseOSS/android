package com.synapse.social.studioasinc.backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatParticipantDto(
    val chat_id: String,
    val user_id: String,
    val joined_at: String? = null
)
