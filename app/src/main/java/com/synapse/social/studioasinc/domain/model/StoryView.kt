package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a view of a story by a user.
 * Used for tracking "Seen by" list.
 */
@Serializable
data class StoryView(
    val id: String? = null,
    @SerialName("story_id")
    val storyId: String,
    @SerialName("viewer_id")
    val viewerId: String,
    @SerialName("viewed_at")
    val viewedAt: String? = null
)

/**
 * StoryView with associated viewer user data for UI display
 */
data class StoryViewWithUser(
    val storyView: StoryView,
    val viewer: User?
)
