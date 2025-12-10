package com.synapse.social.studioasinc.domain

import com.synapse.social.studioasinc.model.*

/**
 * Default tutorial missions for the app
 */
fun getDefaultMissions(): List<TutorialMission> {
    return listOf(
        createWelcomeMission(),
        createProfileSetupMission(),
        createFirstPostMission(),
        createSocialConnectionsMission(),
        createMessagingMission(),
        createContentDiscoveryMission(),
        createAdvancedFeaturesMission()
    )
}

/**
 * Welcome mission - Introduction to Synapse
 */
private fun createWelcomeMission(): TutorialMission {
    return TutorialMission(
        id = "welcome_to_synapse",
        title = "Welcome to Synapse",
        description = "Learn the basics of Synapse and explore the main features",
        category = MissionCategory.GETTING_STARTED,
        difficulty = MissionDifficulty.BEGINNER,
        steps = listOf(
            MissionStep(
                id = "explore_home",
                title = "Explore Home Feed",
                description = "Navigate to the home feed and see what's happening",
                instruction = "Tap the Home icon in the bottom navigation bar",
                verificationType = VerificationType.NAVIGATION,
                verificationData = "HomeFragment",
                estimatedMinutes = 2,
                hints = listOf(
                    "Look for the house icon at the bottom of the screen",
                    "The home feed shows posts from people you follow"
                )
            ),
            MissionStep(
                id = "check_notifications",
                title = "Check Notifications",
                description = "View your notifications to stay updated",
                instruction = "Tap the Notifications icon to see your activity",
                verificationType = VerificationType.NAVIGATION,
                verificationData = "NotificationsFragment",
                estimatedMinutes = 2,
                hints = listOf(
                    "The bell icon shows your notifications",
                    "You'll see likes, comments, and follows here"
                )
            ),
            MissionStep(
                id = "explore_inbox",
                title = "Open Inbox",
                description = "Check your messages and chat conversations",
                instruction = "Navigate to the Inbox to see your chats",
                verificationType = VerificationType.NAVIGATION,
                verificationData = "InboxChatsFragment",
                estimatedMinutes = 2,
                hints = listOf(
                    "The message icon opens your inbox",
                    "All your conversations are listed here"
                )
            )
        ),
        rewards = MissionRewards(
            xp = 50,
            badge = "first_steps",
            title = "Explorer"
        )
    )
}

/**
 * Profile setup mission
 */
private fun createProfileSetupMission(): TutorialMission {
    return TutorialMission(
        id = "setup_profile",
        title = "Complete Your Profile",
        description = "Make your profile stand out by adding personal information",
        category = MissionCategory.PROFILE,
        difficulty = MissionDifficulty.BEGINNER,
        steps = listOf(
            MissionStep(
                id = "add_profile_photo",
                title = "Add Profile Photo",
                description = "Upload a profile picture to personalize your account",
                instruction = "Go to your profile and tap on the profile picture to upload",
                verificationType = VerificationType.DATA_CREATION,
                verificationData = "profile_photo",
                estimatedMinutes = 5,
                hints = listOf(
                    "Tap your profile icon in the bottom navigation",
                    "Tap the camera icon on your profile picture",
                    "Choose a photo from your gallery or take a new one"
                )
            ),
            MissionStep(
                id = "add_bio",
                title = "Write Your Bio",
                description = "Tell others about yourself with a short bio",
                instruction = "Edit your profile and add a bio description",
                verificationType = VerificationType.DATA_CREATION,
                verificationData = "bio",
                estimatedMinutes = 3,
                hints = listOf(
                    "Tap 'Edit Profile' button",
                    "Add a short description about yourself",
                    "Keep it friendly and authentic"
                )
            ),
            MissionStep(
                id = "add_cover_photo",
                title = "Add Cover Photo",
                description = "Customize your profile with a cover photo",
                instruction = "Upload a cover photo to make your profile unique",
                verificationType = VerificationType.DATA_CREATION,
                verificationData = "cover_photo",
                estimatedMinutes = 3,
                hints = listOf(
                    "Tap the camera icon on your cover photo area",
                    "Choose a landscape-oriented image for best results"
                )
            )
        ),
        rewards = MissionRewards(
            xp = 75,
            badge = "profile_master",
            title = "Profile Complete"
        )
    )
}

/**
 * First post mission
 */
private fun createFirstPostMission(): TutorialMission {
    return TutorialMission(
        id = "create_first_post",
        title = "Share Your First Post",
        description = "Create and share your first post with the community",
        category = MissionCategory.CONTENT_CREATION,
        difficulty = MissionDifficulty.BEGINNER,
        steps = listOf(
            MissionStep(
                id = "open_create_post",
                title = "Open Post Creator",
                description = "Navigate to the post creation screen",
                instruction = "Tap the '+' button to create a new post",
                verificationType = VerificationType.NAVIGATION,
                verificationData = "CreatePostActivity",
                estimatedMinutes = 1,
                hints = listOf(
                    "Look for the '+' icon in the center of the bottom navigation",
                    "This opens the post creation screen"
                )
            ),
            MissionStep(
                id = "write_post_content",
                title = "Write Your Post",
                description = "Add text content to your post",
                instruction = "Type your message in the post text field",
                verificationType = VerificationType.FEATURE_USAGE,
                verificationData = "post_text",
                estimatedMinutes = 3,
                hints = listOf(
                    "Share what's on your mind",
                    "You can add emojis, mentions, and hashtags",
                    "Keep it positive and engaging"
                )
            ),
            MissionStep(
                id = "publish_post",
                title = "Publish Your Post",
                description = "Share your post with the community",
                instruction = "Tap the 'Post' button to publish",
                verificationType = VerificationType.DATA_CREATION,
                verificationData = "post_created",
                estimatedMinutes = 1,
                hints = listOf(
                    "Review your post before publishing",
                    "Tap the 'Post' or 'Share' button",
                    "Your post will appear in your followers' feeds"
                )
            )
        ),
        rewards = MissionRewards(
            xp = 100,
            badge = "content_creator",
            title = "Content Creator"
        )
    )
}

/**
 * Social connections mission
 */
private fun createSocialConnectionsMission(): TutorialMission {
    return TutorialMission(
        id = "build_connections",
        title = "Build Your Network",
        description = "Connect with others by following and interacting",
        category = MissionCategory.SOCIAL,
        difficulty = MissionDifficulty.INTERMEDIATE,
        steps = listOf(
            MissionStep(
                id = "search_users",
                title = "Search for Users",
                description = "Find interesting people to follow",
                instruction = "Use the search feature to discover users",
                verificationType = VerificationType.NAVIGATION,
                verificationData = "SearchActivity",
                estimatedMinutes = 3,
                hints = listOf(
                    "Tap the search icon",
                    "Search by username or name",
                    "Browse suggested users"
                )
            ),
            MissionStep(
                id = "follow_users",
                title = "Follow 3 Users",
                description = "Follow at least 3 users to build your network",
                instruction = "Tap 'Follow' on user profiles you're interested in",
                verificationType = VerificationType.FEATURE_USAGE,
                verificationData = "follow_count:3",
                estimatedMinutes = 5,
                hints = listOf(
                    "Visit user profiles and tap 'Follow'",
                    "Their posts will appear in your home feed",
                    "You can unfollow anytime"
                )
            ),
            MissionStep(
                id = "like_posts",
                title = "Like 5 Posts",
                description = "Show appreciation by liking posts",
                instruction = "Tap the heart icon on posts you enjoy",
                verificationType = VerificationType.FEATURE_USAGE,
                verificationData = "like_count:5",
                estimatedMinutes = 5,
                hints = listOf(
                    "Browse your home feed",
                    "Double-tap or tap the heart icon to like",
                    "Likes help creators know you enjoyed their content"
                )
            ),
            MissionStep(
                id = "comment_post",
                title = "Leave a Comment",
                description = "Engage with the community by commenting",
                instruction = "Write a thoughtful comment on a post",
                verificationType = VerificationType.DATA_CREATION,
                verificationData = "comment_created",
                estimatedMinutes = 3,
                hints = listOf(
                    "Tap the comment icon on a post",
                    "Write a meaningful response",
                    "Be respectful and constructive"
                )
            )
        ),
        rewards = MissionRewards(
            xp = 150,
            badge = "social_butterfly",
            title = "Social Butterfly"
        )
    )
}

/**
 * Messaging mission
 */
private fun createMessagingMission(): TutorialMission {
    return TutorialMission(
        id = "master_messaging",
        title = "Master Messaging",
        description = "Learn to use Synapse's powerful messaging features",
        category = MissionCategory.MESSAGING,
        difficulty = MissionDifficulty.INTERMEDIATE,
        steps = listOf(
            MissionStep(
                id = "start_conversation",
                title = "Start a Conversation",
                description = "Send your first direct message",
                instruction = "Open a user's profile and tap 'Message'",
                verificationType = VerificationType.NAVIGATION,
                verificationData = "ChatActivity",
                estimatedMinutes = 2,
                hints = listOf(
                    "Visit any user's profile",
                    "Tap the 'Message' button",
                    "Or use the '+' icon in your inbox"
                )
            ),
            MissionStep(
                id = "send_message",
                title = "Send a Message",
                description = "Type and send your first message",
                instruction = "Write a message and tap send",
                verificationType = VerificationType.DATA_CREATION,
                verificationData = "message_sent",
                estimatedMinutes = 2,
                hints = listOf(
                    "Type in the message input field",
                    "Tap the send button",
                    "Messages are delivered instantly"
                )
            ),
            MissionStep(
                id = "send_image",
                title = "Share an Image",
                description = "Send a photo in your conversation",
                instruction = "Attach and send an image in chat",
                verificationType = VerificationType.FEATURE_USAGE,
                verificationData = "image_sent",
                estimatedMinutes = 3,
                hints = listOf(
                    "Tap the attachment icon",
                    "Select a photo from your gallery",
                    "Add a caption if you'd like"
                )
            ),
            MissionStep(
                id = "use_reactions",
                title = "React to Messages",
                description = "Use emoji reactions on messages",
                instruction = "Long-press a message and select a reaction",
                verificationType = VerificationType.FEATURE_USAGE,
                verificationData = "reaction_added",
                estimatedMinutes = 2,
                hints = listOf(
                    "Long-press any message",
                    "Choose an emoji reaction",
                    "Quick way to respond without typing"
                )
            )
        ),
        rewards = MissionRewards(
            xp = 125,
            badge = "chat_master",
            title = "Chat Master"
        )
    )
}

/**
 * Content discovery mission
 */
private fun createContentDiscoveryMission(): TutorialMission {
    return TutorialMission(
        id = "discover_content",
        title = "Discover Amazing Content",
        description = "Explore different ways to find interesting content",
        category = MissionCategory.SOCIAL,
        difficulty = MissionDifficulty.BEGINNER,
        steps = listOf(
            MissionStep(
                id = "explore_reels",
                title = "Watch Reels",
                description = "Discover short-form video content",
                instruction = "Navigate to the Reels section",
                verificationType = VerificationType.NAVIGATION,
                verificationData = "ReelsFragment",
                estimatedMinutes = 5,
                hints = listOf(
                    "Tap the Reels icon in bottom navigation",
                    "Swipe up to see more videos",
                    "Double-tap to like"
                )
            ),
            MissionStep(
                id = "use_hashtags",
                title = "Explore Hashtags",
                description = "Find content using hashtags",
                instruction = "Search for a hashtag and browse posts",
                verificationType = VerificationType.FEATURE_USAGE,
                verificationData = "hashtag_search",
                estimatedMinutes = 4,
                hints = listOf(
                    "Use the search feature",
                    "Type '#' followed by a topic",
                    "Discover trending content"
                )
            ),
            MissionStep(
                id = "save_post",
                title = "Save a Post",
                description = "Bookmark posts to view later",
                instruction = "Tap the bookmark icon on a post",
                verificationType = VerificationType.FEATURE_USAGE,
                verificationData = "post_saved",
                estimatedMinutes = 2,
                hints = listOf(
                    "Look for the bookmark icon on posts",
                    "Access saved posts from your profile",
                    "Great for saving recipes, tips, or inspiration"
                )
            )
        ),
        rewards = MissionRewards(
            xp = 100,
            badge = "explorer",
            title = "Content Explorer"
        )
    )
}

/**
 * Advanced features mission
 */
private fun createAdvancedFeaturesMission(): TutorialMission {
    return TutorialMission(
        id = "advanced_features",
        title = "Unlock Advanced Features",
        description = "Master advanced Synapse features for power users",
        category = MissionCategory.ADVANCED,
        difficulty = MissionDifficulty.ADVANCED,
        steps = listOf(
            MissionStep(
                id = "customize_privacy",
                title = "Customize Privacy Settings",
                description = "Control who can see your content",
                instruction = "Review and adjust your privacy settings",
                verificationType = VerificationType.FEATURE_USAGE,
                verificationData = "privacy_settings",
                estimatedMinutes = 5,
                hints = listOf(
                    "Go to Settings > Privacy",
                    "Control post visibility",
                    "Manage who can message you"
                )
            ),
            MissionStep(
                id = "use_markdown",
                title = "Format Posts with Markdown",
                description = "Use markdown to format your posts",
                instruction = "Create a post using markdown formatting",
                verificationType = VerificationType.DATA_CREATION,
                verificationData = "markdown_post",
                estimatedMinutes = 7,
                hints = listOf(
                    "Use **bold** for bold text",
                    "Use *italic* for italic text",
                    "Use # for headers",
                    "Markdown makes posts more readable"
                )
            ),
            MissionStep(
                id = "schedule_post",
                title = "Schedule a Post",
                description = "Plan content ahead with scheduled posts",
                instruction = "Create and schedule a post for later",
                verificationType = VerificationType.FEATURE_USAGE,
                verificationData = "post_scheduled",
                estimatedMinutes = 5,
                hints = listOf(
                    "Create a new post",
                    "Look for scheduling options",
                    "Choose date and time"
                )
            ),
            MissionStep(
                id = "create_poll",
                title = "Create a Poll",
                description = "Engage your audience with polls",
                instruction = "Add a poll to your post",
                verificationType = VerificationType.DATA_CREATION,
                verificationData = "poll_created",
                estimatedMinutes = 5,
                hints = listOf(
                    "Create a new post",
                    "Select poll option",
                    "Add question and answer choices"
                )
            )
        ),
        rewards = MissionRewards(
            xp = 200,
            badge = "power_user",
            unlockFeature = "advanced_analytics",
            title = "Power User"
        )
    )
}
