package com.synapse.social.studioasinc.domain.model

sealed class SearchResult {
    data class User(
        val uid: String,
        val username: String,
        val nickname: String?,
        val avatar: String?,
        val gender: String?,
        val accountType: String?,
        val isPremium: Boolean,
        val isVerified: Boolean,
        val isBanned: Boolean,
        val status: String?
    ) : SearchResult()

    data class Post(
        val postId: String,
        val authorId: String,
        val authorName: String,
        val authorAvatar: String?,
        val content: String,
        val timestamp: Long,
        val likesCount: Int,
        val commentsCount: Int
    ) : SearchResult()

    data class Media(
        val postId: String,
        val authorId: String,
        val authorName: String,
        val authorAvatar: String?,
        val mediaUrl: String,
        val mediaType: MediaType,
        val timestamp: Long
    ) : SearchResult()

    enum class MediaType {
        PHOTO, VIDEO
    }
}
