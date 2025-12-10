package com.synapse.social.studioasinc.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey
    val id: String,
    val postId: String,
    val authorUid: String,
    val text: String,
    val timestamp: Long,
    var username: String?,
    var avatarUrl: String?,
    val parentCommentId: String? = null
)
