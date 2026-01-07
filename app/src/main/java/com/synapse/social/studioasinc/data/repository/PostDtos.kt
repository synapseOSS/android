package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.Post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostInsertDto(
    val id: String,
    val key: String? = null,
    @SerialName("author_uid") val authorUid: String,
    @SerialName("post_text") val postText: String? = null,
    @SerialName("post_image") val postImage: String? = null,
    @SerialName("post_type") val postType: String? = null,
    @SerialName("post_visibility") val postVisibility: String? = null,
    @SerialName("post_hide_views_count") val postHideViewsCount: String? = null,
    @SerialName("post_hide_like_count") val postHideLikeCount: String? = null,
    @SerialName("post_hide_comments_count") val postHideCommentsCount: String? = null,
    @SerialName("post_disable_comments") val postDisableComments: String? = null,
    @SerialName("publish_date") val publishDate: String? = null,
    val timestamp: Long,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("reshares_count") val resharesCount: Int = 0,
    @SerialName("media_items") val mediaItems: List<MediaItem>? = null,
    @SerialName("has_poll") val hasPoll: Boolean? = null,
    @SerialName("poll_question") val pollQuestion: String? = null,
    @SerialName("poll_options") val pollOptions: List<PollOption>? = null,
    @SerialName("poll_end_time") val pollEndTime: String? = null,
    @SerialName("poll_allow_multiple") val pollAllowMultiple: Boolean? = null,
    @SerialName("has_location") val hasLocation: Boolean? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("location_address") val locationAddress: String? = null,
    @SerialName("location_latitude") val locationLatitude: Double? = null,
    @SerialName("location_longitude") val locationLongitude: Double? = null,
    @SerialName("location_place_id") val locationPlaceId: String? = null,
    @SerialName("youtube_url") val youtubeUrl: String? = null
)

@Serializable
data class UserSummaryDto(
    val uid: String,
    val username: String? = null,
    @SerialName("avatar") val avatarUrl: String? = null,
    @SerialName("verify") val isVerified: Boolean? = false
)

@Serializable
data class CommentUserDto(
    val username: String? = null
)

@Serializable
data class CommentSelectDto(
    val id: String,
    @SerialName("content") val comment: String? = null, // Mapped to 'content' column
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("users") val user: CommentUserDto? = null
)

@Serializable
data class PostSelectDto(
    val id: String,
    val key: String? = null,
    @SerialName("author_uid") val authorUid: String,
    @SerialName("post_text") val postText: String? = null,
    @SerialName("post_image") val postImage: String? = null,
    @SerialName("post_type") val postType: String? = null,
    @SerialName("post_visibility") val postVisibility: String? = null,
    @SerialName("post_hide_views_count") val postHideViewsCount: String? = null,
    @SerialName("post_hide_like_count") val postHideLikeCount: String? = null,
    @SerialName("post_hide_comments_count") val postHideCommentsCount: String? = null,
    @SerialName("post_disable_comments") val postDisableComments: String? = null,
    @SerialName("publish_date") val publishDate: String? = null,
    val timestamp: Long,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("reshares_count") val resharesCount: Int = 0,
    @SerialName("media_items") val mediaItems: List<MediaItem>? = null,
    @SerialName("has_poll") val hasPoll: Boolean? = null,
    @SerialName("poll_question") val pollQuestion: String? = null,
    @SerialName("poll_options") val pollOptions: List<PollOption>? = null,
    @SerialName("poll_end_time") val pollEndTime: String? = null,
    @SerialName("poll_allow_multiple") val pollAllowMultiple: Boolean? = null,
    @SerialName("has_location") val hasLocation: Boolean? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("location_address") val locationAddress: String? = null,
    @SerialName("location_latitude") val locationLatitude: Double? = null,
    @SerialName("location_longitude") val locationLongitude: Double? = null,
    @SerialName("location_place_id") val locationPlaceId: String? = null,
    @SerialName("youtube_url") val youtubeUrl: String? = null,

    // Nested user data from join
    @SerialName("users") val user: UserSummaryDto? = null,

    // Nested latest comment
    @SerialName("latest_comments") val comments: List<CommentSelectDto>? = null
)

// Mappers

fun Post.toInsertDto(): PostInsertDto {
    return PostInsertDto(
        id = this.id,
        key = this.key,
        authorUid = this.authorUid,
        postText = this.postText,
        postImage = this.postImage,
        postType = this.postType,
        postVisibility = this.postVisibility,
        postHideViewsCount = this.postHideViewsCount,
        postHideLikeCount = this.postHideLikeCount,
        postHideCommentsCount = this.postHideCommentsCount,
        postDisableComments = this.postDisableComments,
        publishDate = this.publishDate,
        timestamp = this.timestamp,
        likesCount = 0, // Reset counts for new post
        commentsCount = 0,
        viewsCount = 0,
        resharesCount = 0,
        mediaItems = this.mediaItems,
        hasPoll = this.hasPoll,
        pollQuestion = this.pollQuestion,
        pollOptions = this.pollOptions,
        pollEndTime = this.pollEndTime,
        pollAllowMultiple = this.pollAllowMultiple,
        hasLocation = this.hasLocation,
        locationName = this.locationName,
        locationAddress = this.locationAddress,
        locationLatitude = this.locationLatitude,
        locationLongitude = this.locationLongitude,
        locationPlaceId = this.locationPlaceId,
        youtubeUrl = this.youtubeUrl
    )
}

fun Post.toUpdateDto(): PostInsertDto {
    // For update, we use the same DTO but include all current values
    return PostInsertDto(
        id = this.id,
        key = this.key,
        authorUid = this.authorUid,
        postText = this.postText,
        postImage = this.postImage,
        postType = this.postType,
        postVisibility = this.postVisibility,
        postHideViewsCount = this.postHideViewsCount,
        postHideLikeCount = this.postHideLikeCount,
        postHideCommentsCount = this.postHideCommentsCount,
        postDisableComments = this.postDisableComments,
        publishDate = this.publishDate,
        timestamp = this.timestamp, // Original timestamp
        likesCount = this.likesCount,
        commentsCount = this.commentsCount,
        viewsCount = this.viewsCount,
        resharesCount = this.resharesCount,
        mediaItems = this.mediaItems,
        hasPoll = this.hasPoll,
        pollQuestion = this.pollQuestion,
        pollOptions = this.pollOptions,
        pollEndTime = this.pollEndTime,
        pollAllowMultiple = this.pollAllowMultiple,
        hasLocation = this.hasLocation,
        locationName = this.locationName,
        locationAddress = this.locationAddress,
        locationLatitude = this.locationLatitude,
        locationLongitude = this.locationLongitude,
        locationPlaceId = this.locationPlaceId,
        youtubeUrl = this.youtubeUrl
    )
}

fun PostSelectDto.toDomain(constructMediaUrl: (String) -> String, constructAvatarUrl: (String) -> String): Post {
    val post = Post(
        id = this.id,
        key = this.key,
        authorUid = this.authorUid,
        postText = this.postText,
        postImage = this.postImage?.let { constructMediaUrl(it) },
        postType = this.postType,
        postHideViewsCount = this.postHideViewsCount,
        postHideLikeCount = this.postHideLikeCount,
        postHideCommentsCount = this.postHideCommentsCount,
        postDisableComments = this.postDisableComments,
        postVisibility = this.postVisibility,
        publishDate = this.publishDate,
        timestamp = this.timestamp,
        likesCount = this.likesCount,
        commentsCount = this.commentsCount,
        viewsCount = this.viewsCount,
        resharesCount = this.resharesCount,
        mediaItems = this.mediaItems?.map {
            it.copy(
                url = constructMediaUrl(it.url),
                thumbnailUrl = it.thumbnailUrl?.let { thumb -> constructMediaUrl(thumb) }
            )
        }?.toMutableList(),
        hasPoll = this.hasPoll,
        pollQuestion = this.pollQuestion,
        pollOptions = this.pollOptions,
        pollEndTime = this.pollEndTime,
        pollAllowMultiple = this.pollAllowMultiple,
        hasLocation = this.hasLocation,
        locationName = this.locationName,
        locationAddress = this.locationAddress,
        locationLatitude = this.locationLatitude,
        locationLongitude = this.locationLongitude,
        locationPlaceId = this.locationPlaceId,
        youtubeUrl = this.youtubeUrl
    )

    // Populate user data from join
    this.user?.let { u ->
        post.username = u.username
        post.avatarUrl = u.avatarUrl?.let { constructAvatarUrl(it) }
        post.isVerified = u.isVerified ?: false
    }

    // Populate latest comment data - Sort by createdAt desc to get the latest since we can't limit/order in embedding
    this.comments?.sortedByDescending { it.createdAt }?.firstOrNull()?.let { comment ->
        post.latestCommentText = comment.comment
        post.latestCommentAuthor = comment.user?.username
    }

    return post
}
