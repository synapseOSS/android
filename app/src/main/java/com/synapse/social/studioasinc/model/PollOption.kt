package com.synapse.social.studioasinc.model

import kotlinx.serialization.Serializable

@Serializable
data class PollOption(
    val text: String,
    val votes: Int = 0
)
