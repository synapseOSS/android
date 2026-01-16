package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Post model aligned with Supabase schema
 * 
 * Schema notes:
 * - media_items: JSONB column (not separate table)
 * - author_uid: FK to users.uid
 * - publish_date: timestamp with time zone
 */
@Serializable
data class Post(
    val id: String = "",
    val key: String? = null,
    @SerialName("author_uid")
    val authorUid: String = "",
    @SerialName("post_text")
    val postText: String? = null,
    @SerialName("post_image")
    var postImage: String? = null,
    @SerialName("post_type")
    var postType: String? = null,
    @SerialName("post_hide_views_count")
    val postHideViewsCount: String? = null,
    @SerialName("post_hide_like_count")
    val postHideLikeCount: String? = null,
    @SerialName("post_hide_comments_count")
    val postHideCommentsCount: String? = null,
    @SerialName("post_disable_comments")
    val postDisableComments: String? = null,
    @SerialName("post_visibility")
    val postVisibility: String? = null,
    @SerialName("publish_date")
    val publishDate: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("comments_count")
    val commentsCount: Int = 0,
    @SerialName("views_count")
    val viewsCount: Int = 0,
    @SerialName("reshares_count")
    val resharesCount: Int = 0,
    // Caption-level interactions (when caption is treated as separate entity)
    @Transient
    val captionLikesCount: Int = 0,
    @Transient
    val captionCommentsCount: Int = 0,
    @Transient
    val userHasLikedCaption: Boolean = false,
    // Media stored as JSONB in posts table
    @SerialName("media_items")
    var mediaItems: MutableList<MediaItem>? = null,
    // Encryption fields
    @SerialName("is_encrypted")
    val isEncrypted: Boolean? = null,
    @SerialName("encrypted_content")
    @Transient
    val encryptedContent: Map<String, String>? = null,
    val nonce: String? = null,
    @SerialName("encryption_key_id")
    val encryptionKeyId: String? = null,
    // Edit/Delete tracking
    @SerialName("is_deleted")
    val isDeleted: Boolean? = null,
    @SerialName("is_edited")
    val isEdited: Boolean? = null,
    @SerialName("edited_at")
    val editedAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null,
    // Poll fields
    @SerialName("has_poll")
    val hasPoll: Boolean? = null,
    @SerialName("poll_question")
    val pollQuestion: String? = null,
    @SerialName("poll_options")
    val pollOptions: List<PollOption>? = null,
    @SerialName("poll_end_time")
    val pollEndTime: String? = null,
    @SerialName("poll_allow_multiple")
    val pollAllowMultiple: Boolean? = null,
    // Location fields
    @SerialName("has_location")
    val hasLocation: Boolean? = null,
    @SerialName("location_name")
    val locationName: String? = null,
    @SerialName("location_address")
    val locationAddress: String? = null,
    @SerialName("location_latitude")
    val locationLatitude: Double? = null,
    @SerialName("location_longitude")
    val locationLongitude: Double? = null,
    @SerialName("location_place_id")
    val locationPlaceId: String? = null,
    // YouTube embed
    @SerialName("youtube_url")
    val youtubeUrl: String? = null,
    // Transient fields (populated from joins/reactions)
    @Transient
    var reactions: Map<ReactionType, Int>? = null,
    @Transient
    var userReaction: ReactionType? = null,
    @SerialName("author_username")
    var username: String? = null,
    @SerialName("author_avatar_url")
    var avatarUrl: String? = null,
    @SerialName("author_is_verified")
    var isVerified: Boolean = false,
    @Transient
    var userPollVote: Int? = null,
    @Transient
    var latestCommentText: String? = null,
    @Transient
    var latestCommentAuthor: String? = null
) {
    fun determinePostType() {
        postType = when {
            mediaItems?.any { it.type == MediaType.VIDEO } == true -> "VIDEO"
            mediaItems?.any { it.type == MediaType.IMAGE } == true -> "IMAGE"
            !postText.isNullOrEmpty() -> "TEXT"
            else -> "TEXT"
        }
    }

    fun getTotalReactionsCount(): Int = reactions?.values?.sum() ?: likesCount

    fun getTopReactions(): List<Pair<ReactionType, Int>> =
        reactions?.entries?.sortedByDescending { it.value }?.take(3)?.map { it.key to it.value } ?: emptyList()

    fun hasUserReacted(): Boolean = userReaction != null

    fun getReactionSummary(): String {
        val topReactions = getTopReactions()
        if (topReactions.isEmpty()) return ""
        val emojis = topReactions.take(2).joinToString(" ") { it.first.emoji }
        val total = getTotalReactionsCount()
        return when {
            total == 0 -> ""
            total == 1 -> "$emojis 1 person"
            else -> "$emojis $total"
        }
    }

    fun hasMultipleMedia(): Boolean = (mediaItems?.size ?: 0) > 1

    fun toDetailItems(): List<PostDetailItem> {
        val items = mutableListOf<PostDetailItem>()
        
        // Add caption if present
        if (!postText.isNullOrEmpty()) {
            items.add(
                PostDetailItem.Caption(
                    postId = id,
                    text = postText,
                    likesCount = captionLikesCount,
                    commentsCount = captionCommentsCount,
                    userHasLiked = userHasLikedCaption
                )
            )
        }
        
        // Add media items
        mediaItems?.forEach { media ->
            when (media.type) {
                MediaType.IMAGE -> items.add(PostDetailItem.Image(media, id))
                MediaType.VIDEO -> items.add(PostDetailItem.Video(media, id))
            }
        }
        
        return items
    }
}

fun HashMap<String, Any>.toPost(): Post = Post(
    id = this["id"] as? String ?: "",
    key = this["key"] as? String,
    authorUid = this["author_uid"] as? String ?: "",
    postText = this["post_text"] as? String,
    postImage = this["post_image"] as? String,
    postType = this["post_type"] as? String,
    publishDate = this["publish_date"] as? String,
    timestamp = (this["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    likesCount = (this["likes_count"] as? Number)?.toInt() ?: 0,
    commentsCount = (this["comments_count"] as? Number)?.toInt() ?: 0,
    viewsCount = (this["views_count"] as? Number)?.toInt() ?: 0,
    resharesCount = (this["reshares_count"] as? Number)?.toInt() ?: 0,
    postHideViewsCount = this["post_hide_views_count"] as? String,
    postHideLikeCount = this["post_hide_like_count"] as? String,
    postHideCommentsCount = this["post_hide_comments_count"] as? String,
    postDisableComments = this["post_disable_comments"] as? String,
    postVisibility = this["post_visibility"] as? String,
    avatarUrl = this["author_avatar_url"] as? String,
    username = this["author_username"] as? String,
    isVerified = this["author_is_verified"] as? Boolean ?: false
)
