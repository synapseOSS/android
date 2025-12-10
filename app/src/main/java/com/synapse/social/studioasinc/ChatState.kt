package com.synapse.social.studioasinc

data class ChatState(
    val isRecording: Boolean = false,
    val isLoading: Boolean = false,
    val replyMessageId: String? = null
)
