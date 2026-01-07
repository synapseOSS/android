package com.synapse.social.studioasinc.domain.model

/**
 * Represents a user's story collection with associated user data.
 * Used for displaying in the story tray.
 */
data class StoryWithUser(
    val user: User,
    val stories: List<Story>,
    val hasUnseenStories: Boolean = true,
    val latestStoryTime: String? = null
) {
    /**
     * Returns the total number of story segments
     */
    val segmentCount: Int get() = stories.size
    
    /**
     * Returns the first unseen story index, or 0 if all seen
     */
    fun getFirstUnseenIndex(): Int = 0 // Will be updated when we track seen status
    
    /**
     * Checks if this is the current user's own story
     */
    fun isOwnStory(currentUserId: String): Boolean = user.uid == currentUserId
}

/**
 * State holder for the Story Tray
 */
data class StoryTrayState(
    val myStory: StoryWithUser? = null,
    val friendStories: List<StoryWithUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val hasMyActiveStory: Boolean get() = myStory != null && myStory.stories.isNotEmpty()
    
    val allStories: List<StoryWithUser> get() = buildList {
        myStory?.let { add(it) }
        addAll(friendStories)
    }
}
