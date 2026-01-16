package com.synapse.social.studioasinc.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val name: String? = null,
    val nickname: String? = null,
    val bio: String? = null,
    @SerialName("avatar")
    val avatar: String? = null,
    @SerialName("cover_image_url")
    val coverImageUrl: String? = null,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    @SerialName("is_private")
    val isPrivate: Boolean = false,
    @SerialName("post_count")
    val postCount: Int = 0,
    @SerialName("follower_count")
    val followerCount: Int = 0,
    @SerialName("following_count")
    val followingCount: Int = 0,
    @SerialName("joined_date")
    val joinedDate: Long = 0,
    val location: String? = null,
    @SerialName("relationship_status")
    val relationshipStatus: String? = null,
    val birthday: String? = null,
    val work: String? = null,
    val education: String? = null,
    @SerialName("current_city")
    val currentCity: String? = null,
    val hometown: String? = null,
    val website: String? = null,
    val gender: String? = null,
    val pronouns: String? = null,
    @SerialName("linked_accounts")
    val linkedAccounts: List<LinkedAccount> = emptyList(),
    @SerialName("privacy_settings")
    val privacySettings: Map<String, String> = emptyMap()
)
