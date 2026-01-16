package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val uid: String = "",
    val comment: String = "",  // Comment text
    @SerialName("push_time")
    val push_time: String = "",  // Timestamp
    val key: String = "",  // Comment ID
    val like: Long? = null,  // Like count (optional - may not exist in schema)
    @SerialName("post_key")
    val postKey: String = "",  // Post this comment belongs to
    @SerialName("reply_comment_key")
    val replyCommentKey: String? = null,  // If this is a reply, the parent comment key
    @SerialName("is_pinned")
    val isPinned: Boolean = false,
    @SerialName("pinned_at")
    val pinnedAt: String? = null,
    @SerialName("pinned_by")
    val pinnedBy: String? = null,
    @SerialName("edited_at")
    val editedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("report_count")
    val reportCount: Int = 0,
    @SerialName("photo_url")
    val photoUrl: String? = null,
    @SerialName("video_url")
    val videoUrl: String? = null,
    @SerialName("audio_url")
    val audioUrl: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null  // "photo", "video", "audio"
)
