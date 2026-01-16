package com.synapse.social.studioasinc

import kotlinx.serialization.Serializable

/**
 * Represents a user mention in posts or comments
 */
data class UserMention(
    val uid: String,
    val username: String,
    val displayName: String? = null,
    val avatar: String? = null,
    val startIndex: Int = 0,
    val endIndex: Int = 0
) {
    /**
     * Gets the mention text (e.g., "@username")
     */
    fun getMentionText(): String = "@$username"
    
    /**
     * Gets the display text for the mention
     */
    fun getDisplayText(): String = displayName ?: username
}
