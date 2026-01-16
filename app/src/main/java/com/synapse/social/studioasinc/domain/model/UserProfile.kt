package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val uid: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    val email: String? = null,
    val bio: String? = null,
    val avatar: String? = null,
    @SerialName("profile_cover_image") val profileCoverImage: String? = null,
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("posts_count") val postsCount: Int = 0,
    val status: String = "offline",
    @SerialName("account_type") val account_type: String = "user",
    val gender: String? = null,
    val region: String? = null,
    val verify: Boolean = false,
    val banned: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val isVerified: Boolean get() = verify
    val isPremium: Boolean get() = account_type == "premium"
}
