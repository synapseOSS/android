package com.synapse.social.studioasinc.domain.model

/**
 * Sealed class representing different types of items in post detail view
 */
sealed class PostDetailItem {
    data class Caption(
        val postId: String,
        val text: String,
        val likesCount: Int = 0,
        val commentsCount: Int = 0,
        val userHasLiked: Boolean = false
    ) : PostDetailItem()

    data class Image(
        val mediaItem: MediaItem,
        val postId: String
    ) : PostDetailItem()

    data class Video(
        val mediaItem: MediaItem,
        val postId: String
    ) : PostDetailItem()
}
