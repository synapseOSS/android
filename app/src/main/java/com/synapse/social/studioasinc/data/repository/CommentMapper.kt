package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.local.database.CommentEntity
import com.synapse.social.studioasinc.domain.model.Comment

object CommentMapper {

    fun toEntity(comment: Comment, username: String? = null, avatarUrl: String? = null): CommentEntity {
        return CommentEntity(
            id = comment.key,
            postId = comment.postKey,
            authorUid = comment.uid,
            text = comment.comment,
            timestamp = parsePushTime(comment.push_time),
            username = username,
            avatarUrl = avatarUrl,
            parentCommentId = comment.replyCommentKey
        )
    }

    fun toModel(entity: CommentEntity): Comment {
        return Comment(
            key = entity.id,
            postKey = entity.postId,
            uid = entity.authorUid,
            comment = entity.text,
            push_time = entity.timestamp.toString(),
            replyCommentKey = entity.parentCommentId
        )
    }

    private fun parsePushTime(pushTime: String): Long {
        return try {
            pushTime.toLongOrNull() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
