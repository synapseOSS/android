package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class for updating user profile information
 * This ensures proper serialization when updating user data
 */
@Serializable
data class UserUpdateRequest(
    val username: String? = null,
    val nickname: String? = null,
    val bio: String? = null,
    val gender: String? = null,
    val region: String? = null,
    val avatar: String? = null,
    @SerialName("profile_cover_image")
    val profileCoverImage: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val status: String? = null
)
