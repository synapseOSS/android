package com.synapse.social.studioasinc.ui.settings

/**
 * Sealed class defining all navigation destinations for the Settings feature.
 * 
 * This provides type-safe navigation routes for the settings hub-and-spoke
 * navigation model, where the main Settings Hub provides categorized access
 * to dedicated sub-screens.
 * 
 * Requirements: 1.1, 1.2
 */
sealed class SettingsDestination(val route: String) {
    
    /**
     * Main settings hub screen displaying categorized setting groups.
     */
    object Hub : SettingsDestination(ROUTE_HUB)
    
    /**
     * Account settings screen for profile, email, password, and account management.
     */
    object Account : SettingsDestination(ROUTE_ACCOUNT)
    
    /**
     * Privacy and security settings screen for visibility, 2FA, biometric lock, and blocking.
     */
    object Privacy : SettingsDestination(ROUTE_PRIVACY)
    
    /**
     * Appearance settings screen for theme, dynamic color, and font customization.
     */
    object Appearance : SettingsDestination(ROUTE_APPEARANCE)
    
    /**
     * Notification settings screen for push notifications and in-app alerts.
     */
    object Notifications : SettingsDestination(ROUTE_NOTIFICATIONS)
    
    /**
     * Chat settings screen for read receipts, typing indicators, and media download.
     */
    object Chat : SettingsDestination(ROUTE_CHAT)
    
    /**
     * Storage and data settings screen for cache, data saver, and storage providers.
     */
    object Storage : SettingsDestination(ROUTE_STORAGE)
    
    /**
     * Language and region settings screen for language selection and regional preferences.
     */
    object Language : SettingsDestination(ROUTE_LANGUAGE)
    
    /**
     * About and support settings screen for app info, legal, and feedback.
     */
    object About : SettingsDestination(ROUTE_ABOUT)
    
    /**
     * Storage Provider configuration screen.
     */
    object StorageProvider : SettingsDestination(ROUTE_STORAGE_PROVIDER)
    
    /**
     * Chat History Deletion screen for managing chat history deletion operations.
     */
    object ChatHistoryDeletion : SettingsDestination(ROUTE_CHAT_HISTORY_DELETION)

    /**
     * Open Source Licenses screen.
     */
    object Licenses : SettingsDestination(ROUTE_LICENSES)

    /**
     * Synapse Plus settings screen for premium features and verification.
     */
    object SynapsePlus : SettingsDestination(ROUTE_SYNAPSE_PLUS)

    /**
     * Avatar settings screen for creating and editing profile avatars.
     */
    object Avatar : SettingsDestination(ROUTE_AVATAR)

    /**
     * Favourites settings screen for managing favorite contacts and content.
     */
    object Favourites : SettingsDestination(ROUTE_FAVOURITES)

    /**
     * Accessibility settings screen for contrast, animations, and accessibility features.
     */
    object Accessibility : SettingsDestination(ROUTE_ACCESSIBILITY)

    /**
     * Settings search screen for finding specific settings quickly.
     */
    object Search : SettingsDestination(ROUTE_SEARCH)

    /**
     * API Key settings screen for managing AI provider keys.
     */
    object ApiKey : SettingsDestination(ROUTE_API_KEY)

    companion object {
        // Route constants for navigation
        const val ROUTE_HUB = "settings_hub"
        const val ROUTE_ACCOUNT = "settings_account"
        const val ROUTE_PRIVACY = "settings_privacy"
        const val ROUTE_APPEARANCE = "settings_appearance"
        const val ROUTE_NOTIFICATIONS = "settings_notifications"
        const val ROUTE_CHAT = "settings_chat"
        const val ROUTE_STORAGE = "settings_storage"
        const val ROUTE_STORAGE_PROVIDER = "settings_storage_provider"
        const val ROUTE_LANGUAGE = "settings_language"
        const val ROUTE_ABOUT = "settings_about"
        const val ROUTE_CHAT_HISTORY_DELETION = "settings_chat_history_deletion"
        const val ROUTE_CHAT_THEME = "settings_chat_theme"
        const val ROUTE_CHAT_WALLPAPER = "settings_chat_wallpaper"
        const val ROUTE_LICENSES = "settings_licenses"
        const val ROUTE_SYNAPSE_PLUS = "settings_synapse_plus"
        const val ROUTE_AVATAR = "settings_avatar"
        const val ROUTE_FAVOURITES = "settings_favourites"
        const val ROUTE_ACCESSIBILITY = "settings_accessibility"
        const val ROUTE_SEARCH = "settings_search"
        const val ROUTE_API_KEY = "settings_api_key"

        /**
         * Returns all available settings destinations.
         */
        fun allDestinations(): List<SettingsDestination> = listOf(
            Hub,
            Account,
            Privacy,
            Appearance,
            Notifications,
            Chat,
            Storage,
            StorageProvider,
            Language,
            About,
            ChatHistoryDeletion,
            Licenses,
            SynapsePlus,
            Avatar,
            Favourites,
            Accessibility,
            ApiKey
        )
        
        /**
         * Returns a destination by its route string.
         * @param route The route string to look up
         * @return The matching SettingsDestination or null if not found
         */
        fun fromRoute(route: String): SettingsDestination? = when (route) {
            ROUTE_HUB -> Hub
            ROUTE_ACCOUNT -> Account
            ROUTE_PRIVACY -> Privacy
            ROUTE_APPEARANCE -> Appearance
            ROUTE_NOTIFICATIONS -> Notifications
            ROUTE_CHAT -> Chat
            ROUTE_STORAGE -> Storage
            ROUTE_STORAGE_PROVIDER -> StorageProvider
            ROUTE_LANGUAGE -> Language
            ROUTE_ABOUT -> About
            ROUTE_CHAT_HISTORY_DELETION -> ChatHistoryDeletion
            ROUTE_LICENSES -> Licenses
            ROUTE_SYNAPSE_PLUS -> SynapsePlus
            ROUTE_AVATAR -> Avatar
            ROUTE_FAVOURITES -> Favourites
            ROUTE_ACCESSIBILITY -> Accessibility
            ROUTE_API_KEY -> ApiKey
            else -> null
        }
    }
}
