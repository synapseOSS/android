package com.synapse.social.studioasinc.domain.model

data class Notification(
    var from: String? = null,
    var message: String? = null,
    var type: String? = null,
    var postId: String? = null,
    var commentId: String? = null,
    var timestamp: Long = 0
)
