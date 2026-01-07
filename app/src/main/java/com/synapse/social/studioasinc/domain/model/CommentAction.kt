package com.synapse.social.studioasinc.domain.model

sealed class CommentAction {
    data class Share(val commentId: String, val content: String, val postId: String) : CommentAction()
    data class Reply(val commentId: String, val parentUserId: String) : CommentAction()
    data class Copy(val content: String) : CommentAction()
    data class Hide(val commentId: String) : CommentAction()
    data class Report(val commentId: String, val reason: String, val description: String?) : CommentAction()
    data class Pin(val commentId: String, val postId: String) : CommentAction()
    data class Delete(val commentId: String) : CommentAction()
    data class Edit(val commentId: String, val newContent: String) : CommentAction()
}
