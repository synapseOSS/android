package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reply(
    val uid: String = "",
    val comment: String = "",
    val push_time: String = "",
    val key: String = "",
    val like: Long? = null,
    val replyCommentkey: String = "",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("photo_url")
    val photoUrl: String? = null,
    @SerialName("video_url")
    val videoUrl: String? = null,
    @SerialName("audio_url")
    val audioUrl: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null
)
