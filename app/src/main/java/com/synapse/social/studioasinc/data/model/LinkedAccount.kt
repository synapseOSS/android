package com.synapse.social.studioasinc.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkedAccount(
    val id: String,
    val platform: String,
    val username: String,
    val url: String,
    @SerialName("is_verified")
    val isVerified: Boolean = false
)
