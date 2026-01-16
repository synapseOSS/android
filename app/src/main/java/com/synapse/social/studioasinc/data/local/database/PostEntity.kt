package com.synapse.social.studioasinc.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.ReactionType

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val key: String?,
    val authorUid: String,
    val postText: String?,
    var postImage: String?,
    var postType: String?,
    val postHideViewsCount: String?,
    val postHideLikeCount: String?,
    val postHideCommentsCount: String?,
    val postDisableComments: String?,
    val postVisibility: String?,
    val publishDate: String?,
    val timestamp: Long,
    val likesCount: Int,
    val commentsCount: Int,
    val viewsCount: Int,
    val resharesCount: Int,
    var mediaItems: List<MediaItem>?,
    val isEncrypted: Boolean?,
    val nonce: String?,
    val encryptionKeyId: String?,
    val isDeleted: Boolean?,
    val isEdited: Boolean?,
    val editedAt: String?,
    val deletedAt: String?,
    val hasPoll: Boolean?,
    val pollQuestion: String?,
    val pollOptions: List<PollOption>?,
    val pollEndTime: String?,
    val pollAllowMultiple: Boolean?,
    val hasLocation: Boolean?,
    val locationName: String?,
    val locationAddress: String?,
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val locationPlaceId: String?,
    val youtubeUrl: String?,
    var reactions: Map<ReactionType, Int>?,
    var userReaction: ReactionType?,
    var username: String?,
    var avatarUrl: String?,
    var isVerified: Boolean,
    var userPollVote: Int? = null
)

private val gson = Gson()

class MediaItemConverter {
    @TypeConverter
    fun fromMediaItemList(mediaItems: List<MediaItem>?): String? {
        return mediaItems?.let {
            try {
                gson.toJson(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun toMediaItemList(mediaItemsString: String?): List<MediaItem>? {
        if (mediaItemsString.isNullOrBlank()) return null
        return try {
            val type = object : TypeToken<List<MediaItem>>() {}.type
            gson.fromJson<List<MediaItem>>(mediaItemsString, type)
        } catch (e: Exception) {
            null
        }
    }
}

class PollOptionConverter {
    @TypeConverter
    fun fromPollOptionList(pollOptions: List<PollOption>?): String? {
        return pollOptions?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toPollOptionList(pollOptionsString: String?): List<PollOption>? {
        if (pollOptionsString == null) return null
        return try {
            val type = object : TypeToken<List<PollOption>>() {}.type
            gson.fromJson(pollOptionsString, type)
        } catch (e: Exception) {
            null
        }
    }
}

class ReactionTypeConverter {
    @TypeConverter
    fun fromReactionMap(reactions: Map<ReactionType, Int>?): String? {
        return reactions?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toReactionMap(reactionsString: String?): Map<ReactionType, Int>? {
        if (reactionsString == null) return null
        return try {
            val type = object : TypeToken<Map<ReactionType, Int>>() {}.type
            gson.fromJson(reactionsString, type)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromReactionType(reactionType: ReactionType?): String? {
        return reactionType?.name
    }

    @TypeConverter
    fun toReactionType(reactionTypeString: String?): ReactionType? {
        return reactionTypeString?.let { 
            try {
                ReactionType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}
