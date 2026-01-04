package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Like model for Supabase database
 */
@Serializable
data class Like(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("target_id")
    val targetId: String,
    @SerialName("target_type")
    val targetType: String = "post",
    @SerialName("created_at")
    val createdAt: String? = null
)
