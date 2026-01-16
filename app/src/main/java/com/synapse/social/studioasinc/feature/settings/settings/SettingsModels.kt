package com.synapse.social.studioasinc.ui.settings

import androidx.annotation.DrawableRes
import com.synapse.social.studioasinc.domain.model.ChatThemePreset
import com.synapse.social.studioasinc.domain.model.ChatWallpaper

/**
 * Data models and enums for the Settings feature.
 * 
 * These models define the data structures used across all settings screens,
 * including theme preferences, privacy settings, notification preferences,
 * and chat settings.
 * 
 * Requirements: 3.2, 3.8, 4.1, 4.5, 5.3, 6.4
 */

// ============================================================================
// Theme and Appearance Enums
// ============================================================================

/**
 * Theme mode options for the app appearance.
 * Requirements: 4.1
 */
enum class ThemeMode {
    /** Light theme */
    LIGHT,
    /** Dark theme */
    DARK,
    /** Follow system theme setting */
    SYSTEM
}

/**
 * Font scale options for text size customization.
 * Requirements: 4.5
 */
enum class FontScale {
    /** Small text size */
    SMALL,
    /** Default/medium text size */
    MEDIUM,
    /** Large text size */
    LARGE,
    /** Extra large text size for accessibility */
    EXTRA_LARGE;
    
    /**
     * Returns the scale factor for this font size.
     */
    fun scaleFactor(): Float = when (this) {
        SMALL -> 0.85f
        MEDIUM -> 1.0f
        LARGE -> 1.15f
        EXTRA_LARGE -> 1.3f
    }
    
    /**
     * Returns a human-readable display name.
     */
    fun displayName(): String = when (this) {
        SMALL -> "Small"
        MEDIUM -> "Medium"
        LARGE -> "Large"
        EXTRA_LARGE -> "Extra Large"
    }
}

// ============================================================================
// Privacy Enums
// ============================================================================

/**
 * Profile visibility options controlling who can view the user's profile.
 * Requirements: 3.2
 */
enum class ProfileVisibility {
    /** Profile visible to everyone */
    PUBLIC,
    /** Profile visible only to followers */
    FOLLOWERS_ONLY,
    /** Profile visible only to the user */
    PRIVATE;
    
    /**
     * Returns a human-readable display name.
     */
    fun displayName(): String = when (this) {
        PUBLIC -> "Public"
        FOLLOWERS_ONLY -> "Followers Only"
        PRIVATE -> "Private"
    }
}

/**
 * Content visibility options controlling who can see the user's posts.
 * Requirements: 3.8
 */
enum class ContentVisibility {
    /** Posts visible to everyone */
    EVERYONE,
    /** Posts visible only to followers */
    FOLLOWERS,
    /** Posts visible only to the user */
    ONLY_ME;
    
    /**
     * Returns a human-readable display name.
     */
    fun displayName(): String = when (this) {
        EVERYONE -> "Everyone"
        FOLLOWERS -> "Followers"
        ONLY_ME -> "Only Me"
    }
}

// ============================================================================
// Notification Enums
// ============================================================================

/**
 * Notification category types for granular notification control.
 * Requirements: 5.3
 */
enum class NotificationCategory {
    /** Notifications for likes on posts */
    LIKES,
    /** Notifications for comments on posts */
    COMMENTS,
    /** Notifications for new followers */
    FOLLOWS,
    /** Notifications for direct messages */
    MESSAGES,
    /** Notifications for mentions in posts/comments */
    MENTIONS;
    
    /**
     * Returns a human-readable display name.
     */
    fun displayName(): String = when (this) {
        LIKES -> "Likes"
        COMMENTS -> "Comments"
        FOLLOWS -> "Follows"
        MESSAGES -> "Messages"
        MENTIONS -> "Mentions"
    }
}

// ============================================================================
// Chat Enums
// ============================================================================

/**
 * Media auto-download options for chat media.
 * Requirements: 6.4
 */
enum class MediaAutoDownload {
    /** Always download media automatically */
    ALWAYS,
    /** Download media only on WiFi */
    WIFI_ONLY,
    /** Never auto-download media */
    NEVER;
    
    /**
     * Returns a human-readable display name.
     */
    fun displayName(): String = when (this) {
        ALWAYS -> "Always"
        WIFI_ONLY -> "WiFi Only"
        NEVER -> "Never"
    }
}


// ============================================================================
// Data Classes
// ============================================================================

/**
 * Appearance settings data class.
 * Requirements: 4.1
 */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val fontScale: FontScale = FontScale.MEDIUM,
    val postViewStyle: PostViewStyle = PostViewStyle.SWIPE
)

/**
 * Post view style options.
 */
enum class PostViewStyle {
    /** Horizontal swipeable pager (Instagram-style) */
    SWIPE,
    /** Mosaic/Grid layout (Facebook-style) */
    GRID;

    /**
     * Returns a human-readable display name.
     */
    fun displayName(): String = when (this) {
        SWIPE -> "Swipe"
        GRID -> "Grid"
    }
}

/**
 * Privacy settings data class.
 * Requirements: 3.2, 3.8
 */
data class PrivacySettings(
    val profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC,
    val twoFactorEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val contentVisibility: ContentVisibility = ContentVisibility.EVERYONE,
    val readReceiptsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val chatLockEnabled: Boolean = false
)

/**
 * Notification preferences data class.
 * Requirements: 5.3
 */
data class NotificationPreferences(
    val likesEnabled: Boolean = true,
    val commentsEnabled: Boolean = true,
    val followsEnabled: Boolean = true,
    val messagesEnabled: Boolean = true,
    val mentionsEnabled: Boolean = true,
    val inAppNotificationsEnabled: Boolean = true,
    val remindersEnabled: Boolean = false,
    val highPriorityEnabled: Boolean = true,
    val reactionNotificationsEnabled: Boolean = true
) {
    /**
     * Returns whether notifications are enabled for a specific category.
     */
    fun isEnabled(category: NotificationCategory): Boolean = when (category) {
        NotificationCategory.LIKES -> likesEnabled
        NotificationCategory.COMMENTS -> commentsEnabled
        NotificationCategory.FOLLOWS -> followsEnabled
        NotificationCategory.MESSAGES -> messagesEnabled
        NotificationCategory.MENTIONS -> mentionsEnabled
    }
    
    /**
     * Returns a copy with the specified category enabled/disabled.
     */
    fun withCategory(category: NotificationCategory, enabled: Boolean): NotificationPreferences {
        return when (category) {
            NotificationCategory.LIKES -> copy(likesEnabled = enabled)
            NotificationCategory.COMMENTS -> copy(commentsEnabled = enabled)
            NotificationCategory.FOLLOWS -> copy(followsEnabled = enabled)
            NotificationCategory.MESSAGES -> copy(messagesEnabled = enabled)
            NotificationCategory.MENTIONS -> copy(mentionsEnabled = enabled)
        }
    }
}

/**
 * Chat settings data class.
 * Requirements: 6.4
 */
data class ChatSettings(
    val readReceiptsEnabled: Boolean = true,
    val typingIndicatorsEnabled: Boolean = true,
    val mediaAutoDownload: MediaAutoDownload = MediaAutoDownload.WIFI_ONLY,
    val chatFontScale: Float = 1.0f,
    val themePreset: ChatThemePreset = ChatThemePreset.DEFAULT,
    val wallpaper: ChatWallpaper = ChatWallpaper(),
    val enterIsSendEnabled: Boolean = false,
    val mediaVisibilityEnabled: Boolean = true,
    val voiceTranscriptsEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = true
)

// ============================================================================
// Navigation
// ============================================================================

/**
 * Settings category model for the Settings Hub.
 * Requirements: 1.1, 1.4
 */
data class SettingsCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val icon: Int,
    val destination: SettingsDestination
)

/**
 * Settings Group model for grouping categories.
 */
data class SettingsGroup(
    val id: String,
    val title: String? = null,
    val categories: List<SettingsCategory>
)

/**
 * User profile summary for display in the Settings Hub header.
 * Requirements: 1.5
 */
data class UserProfileSummary(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String?
)
