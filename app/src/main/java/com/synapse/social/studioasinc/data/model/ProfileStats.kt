package com.synapse.social.studioasinc.data.model

data class ProfileStats(
    val postCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0
) {
    fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "${(count / 1_000_000f).toInt()}M"
        count >= 1_000 -> "${(count / 1_000f).toInt()}K"
        else -> count.toString()
    }
}
