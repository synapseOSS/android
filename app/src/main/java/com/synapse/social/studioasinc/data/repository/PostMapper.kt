package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.local.PostEntity
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import com.synapse.social.studioasinc.model.PollOption
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.util.ImageLoader
import kotlinx.serialization.json.*

object PostMapper {

    // Helper to construct URLs (moved from Repository to be reusable)
    private fun constructMediaUrl(storagePath: String): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath
        }
        return ImageLoader.constructStorageUrl(storagePath, "post-media")
    }

    private fun constructAvatarUrl(storagePath: String): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath
        }
        return ImageLoader.constructStorageUrl(storagePath, "user-avatars")
    }

    /**
     * Parses a Supabase JSON object (including joined user data) into a Post model.
     * This centralizes the logic for "fetching"/mapping author details into the Post model.
     */
    fun fromSupabaseJson(data: JsonObject): Post {
        val post = Post(
            id = data["id"]?.jsonPrimitive?.contentOrNull ?: "",
            key = data["key"]?.jsonPrimitive?.contentOrNull,
            authorUid = data["author_uid"]?.jsonPrimitive?.contentOrNull ?: "",
            postText = data["post_text"]?.jsonPrimitive?.contentOrNull,
            postImage = data["post_image"]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) },
            postType = data["post_type"]?.jsonPrimitive?.contentOrNull,
            postHideViewsCount = data["post_hide_views_count"]?.jsonPrimitive?.contentOrNull,
            postHideLikeCount = data["post_hide_like_count"]?.jsonPrimitive?.contentOrNull,
            postHideCommentsCount = data["post_hide_comments_count"]?.jsonPrimitive?.contentOrNull,
            postDisableComments = data["post_disable_comments"]?.jsonPrimitive?.contentOrNull,
            postVisibility = data["post_visibility"]?.jsonPrimitive?.contentOrNull,
            publishDate = data["publish_date"]?.jsonPrimitive?.contentOrNull,
            timestamp = data["timestamp"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
            likesCount = data["likes_count"]?.jsonPrimitive?.intOrNull ?: 0,
            commentsCount = data["comments_count"]?.jsonPrimitive?.intOrNull ?: 0,
            viewsCount = data["views_count"]?.jsonPrimitive?.intOrNull ?: 0,
            resharesCount = data["reshares_count"]?.jsonPrimitive?.intOrNull ?: 0,
            hasPoll = data["has_poll"]?.jsonPrimitive?.booleanOrNull,
            pollQuestion = data["poll_question"]?.jsonPrimitive?.contentOrNull,
            pollOptions = data["poll_options"]?.jsonArray?.mapNotNull {
                val obj = it.jsonObject
                val text = obj["text"]?.jsonPrimitive?.contentOrNull
                val votes = obj["votes"]?.jsonPrimitive?.intOrNull ?: 0
                if (text != null) PollOption(text, votes) else null
            },
            pollEndTime = data["poll_end_time"]?.jsonPrimitive?.contentOrNull,
            hasLocation = data["has_location"]?.jsonPrimitive?.booleanOrNull,
            locationName = data["location_name"]?.jsonPrimitive?.contentOrNull,
            locationAddress = data["location_address"]?.jsonPrimitive?.contentOrNull,
            locationLatitude = data["location_latitude"]?.jsonPrimitive?.doubleOrNull,
            locationLongitude = data["location_longitude"]?.jsonPrimitive?.doubleOrNull,
            youtubeUrl = data["youtube_url"]?.jsonPrimitive?.contentOrNull
        )

        // Parse joined user data
        val userData = data["users"]?.jsonObject
        if (userData != null) {
            post.username = userData["username"]?.jsonPrimitive?.contentOrNull
            post.avatarUrl = userData["avatar"]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) }
            post.isVerified = userData["verify"]?.jsonPrimitive?.booleanOrNull ?: false
        }

        // Parse media items
        val mediaData = data["media_items"]?.takeIf { it !is JsonNull }?.jsonArray
        if (mediaData != null && mediaData.isNotEmpty()) {
            post.mediaItems = mediaData.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap["type"]?.jsonPrimitive?.contentOrNull ?: "IMAGE"
                MediaItem(
                    id = mediaMap["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = constructMediaUrl(url),
                    type = if (typeStr.equals("VIDEO", ignoreCase = true)) MediaType.VIDEO else MediaType.IMAGE,
                    thumbnailUrl = mediaMap["thumbnailUrl"]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) },
                    duration = mediaMap["duration"]?.jsonPrimitive?.longOrNull,
                    size = mediaMap["size"]?.jsonPrimitive?.longOrNull,
                    mimeType = mediaMap["mimeType"]?.jsonPrimitive?.contentOrNull
                )
            }.toMutableList()
        }

        return post
    }

    fun toEntity(post: Post): PostEntity {
        return PostEntity(
            id = post.id,
            key = post.key,
            authorUid = post.authorUid,
            postText = post.postText,
            postImage = post.postImage,
            postType = post.postType,
            postHideViewsCount = post.postHideViewsCount,
            postHideLikeCount = post.postHideLikeCount,
            postHideCommentsCount = post.postHideCommentsCount,
            postDisableComments = post.postDisableComments,
            postVisibility = post.postVisibility,
            publishDate = post.publishDate,
            timestamp = post.timestamp,
            likesCount = post.likesCount,
            commentsCount = post.commentsCount,
            viewsCount = post.viewsCount,
            resharesCount = post.resharesCount,
            mediaItems = post.mediaItems,
            isEncrypted = post.isEncrypted,
            nonce = post.nonce,
            encryptionKeyId = post.encryptionKeyId,
            isDeleted = post.isDeleted,
            isEdited = post.isEdited,
            editedAt = post.editedAt,
            deletedAt = post.deletedAt,
            hasPoll = post.hasPoll,
            pollQuestion = post.pollQuestion,
            pollOptions = post.pollOptions,
            pollEndTime = post.pollEndTime,
            pollAllowMultiple = post.pollAllowMultiple,
            hasLocation = post.hasLocation,
            locationName = post.locationName,
            locationAddress = post.locationAddress,
            locationLatitude = post.locationLatitude,
            locationLongitude = post.locationLongitude,
            locationPlaceId = post.locationPlaceId,
            youtubeUrl = post.youtubeUrl,
            reactions = post.reactions,
            userReaction = post.userReaction,
            username = post.username,
            avatarUrl = post.avatarUrl,
            isVerified = post.isVerified
        )
    }

    fun toModel(entity: PostEntity): Post {
        return Post(
            id = entity.id,
            key = entity.key,
            authorUid = entity.authorUid,
            postText = entity.postText,
            postImage = entity.postImage,
            postType = entity.postType,
            postHideViewsCount = entity.postHideViewsCount,
            postHideLikeCount = entity.postHideLikeCount,
            postHideCommentsCount = entity.postHideCommentsCount,
            postDisableComments = entity.postDisableComments,
            postVisibility = entity.postVisibility,
            publishDate = entity.publishDate,
            timestamp = entity.timestamp,
            likesCount = entity.likesCount,
            commentsCount = entity.commentsCount,
            viewsCount = entity.viewsCount,
            resharesCount = entity.resharesCount,
            mediaItems = entity.mediaItems?.toMutableList(),
            isEncrypted = entity.isEncrypted,
            nonce = entity.nonce,
            encryptionKeyId = entity.encryptionKeyId,
            isDeleted = entity.isDeleted,
            isEdited = entity.isEdited,
            editedAt = entity.editedAt,
            deletedAt = entity.deletedAt,
            hasPoll = entity.hasPoll,
            pollQuestion = entity.pollQuestion,
            pollOptions = entity.pollOptions,
            pollEndTime = entity.pollEndTime,
            pollAllowMultiple = entity.pollAllowMultiple,
            hasLocation = entity.hasLocation,
            locationName = entity.locationName,
            locationAddress = entity.locationAddress,
            locationLatitude = entity.locationLatitude,
            locationLongitude = entity.locationLongitude,
            locationPlaceId = entity.locationPlaceId,
            youtubeUrl = entity.youtubeUrl,
            reactions = entity.reactions,
            userReaction = entity.userReaction,
            username = entity.username,
            avatarUrl = entity.avatarUrl,
            isVerified = entity.isVerified
        )
    }
}
