package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings Hub screen.
 * 
 * Manages the state for the main settings hub, including user profile summary
 * and the list of settings categories. Handles navigation events to sub-screens.
 * 
 * Requirements: 1.5
 */
class SettingsHubViewModel(application: Application) : AndroidViewModel(application) {

    private val _userProfileSummary = MutableStateFlow<UserProfileSummary?>(null)
    val userProfileSummary: StateFlow<UserProfileSummary?> = _userProfileSummary.asStateFlow()

    private val _settingsGroups = MutableStateFlow<List<SettingsGroup>>(emptyList())
    val settingsGroups: StateFlow<List<SettingsGroup>> = _settingsGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUserProfile()
        loadSettingsCategories()
    }

    /**
     * Loads the current user's profile summary from UserProfileManager.
     * 
     * Requirements: 1.5
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = UserProfileManager.getCurrentUserProfile()
                if (currentUser != null) {
                    val displayName = currentUser.displayName?.takeIf { it.isNotBlank() }
                        ?: currentUser.username?.takeIf { it.isNotBlank() }
                        ?: "User"
                    
                    android.util.Log.d("SettingsHubViewModel", "Profile loaded - avatarUrl: ${currentUser.avatar}")
                    _userProfileSummary.value = UserProfileSummary(
                        id = currentUser.uid,
                        displayName = displayName,
                        email = currentUser.email ?: "",
                        avatarUrl = currentUser.avatar
                    )
                } else {
                    // Fallback to Auth Service if profile is missing in DB
                    try {
                        val authService = SupabaseAuthenticationService.getInstance(getApplication())
                        val authUser = authService.getCurrentUser()

                        if (authUser != null) {
                            _userProfileSummary.value = UserProfileSummary(
                                id = authUser.id,
                                displayName = "User", // Fallback name
                                email = authUser.email,
                                avatarUrl = null
                            )
                        } else {
                            // Set default profile if no user found in Auth
                            _userProfileSummary.value = UserProfileSummary(
                                id = "",
                                displayName = "User",
                                email = "",
                                avatarUrl = null
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsHubViewModel", "Failed to load auth user", e)
                         _userProfileSummary.value = UserProfileSummary(
                            id = "",
                            displayName = "User",
                            email = "",
                            avatarUrl = null
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsHubViewModel", "Failed to load user profile", e)
                // Set default profile if loading fails
                _userProfileSummary.value = UserProfileSummary(
                    id = "",
                    displayName = "User",
                    email = "",
                    avatarUrl = null
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads the list of settings categories for the hub.
     * 
     * Requirements: 1.1, 1.4
     */
    private fun loadSettingsCategories() {
        // Group A: Account, Privacy & Security
        val groupA = SettingsGroup(
            id = "group_a",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "account",
                    title = "Account",
                    subtitle = "Profile, email, password, and account management",
                    icon = R.drawable.ic_person,
                    destination = SettingsDestination.Account
                ),
                SettingsCategory(
                    id = "privacy",
                    title = "Privacy & Security",
                    subtitle = "Control who can see your content and secure your account",
                    icon = R.drawable.ic_shield_lock,
                    destination = SettingsDestination.Privacy
                )
            )
        )

        // Group B: Appearance, Notifications, Language & Region
        val groupB = SettingsGroup(
            id = "group_b",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "appearance",
                    title = "Appearance",
                    subtitle = "Theme, colors, and display preferences",
                    icon = R.drawable.ic_palette,
                    destination = SettingsDestination.Appearance
                ),
                SettingsCategory(
                    id = "notifications",
                    title = "Notifications",
                    subtitle = "Manage alerts and notification preferences",
                    icon = R.drawable.ic_notifications,
                    destination = SettingsDestination.Notifications
                ),
                SettingsCategory(
                    id = "language",
                    title = "Language & Region",
                    subtitle = "Language preferences and regional settings",
                    icon = R.drawable.ic_public,
                    destination = SettingsDestination.Language
                )
            )
        )

        // Group C: Chat, AI Settings, Storage & Data
        val groupC = SettingsGroup(
            id = "group_c",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "chat",
                    title = "Chat",
                    subtitle = "Messaging behavior and privacy settings",
                    icon = R.drawable.ic_message,
                    destination = SettingsDestination.Chat
                ),
                SettingsCategory(
                    id = "ai",
                    title = "AI Settings",
                    subtitle = "Configure AI Persona and Assistant",
                    icon = R.drawable.star_shine_24px,
                    destination = SettingsDestination.AI
                ),
                SettingsCategory(
                    id = "storage",
                    title = "Storage & Data",
                    subtitle = "Cache, data usage, and storage providers",
                    icon = R.drawable.data_usage_24px,
                    destination = SettingsDestination.Storage
                )
            )
        )

        // Group D: About & Support
        val groupD = SettingsGroup(
            id = "group_d",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "about",
                    title = "About & Support",
                    subtitle = "App info, help, and legal information",
                    icon = R.drawable.ic_info_48px,
                    destination = SettingsDestination.About
                )
            )
        )

        _settingsGroups.value = listOf(groupA, groupB, groupC, groupD)
    }

    /**
     * Handles navigation to a settings category.
     * 
     * @param destination The destination to navigate to
     */
    fun onNavigateToCategory(destination: SettingsDestination) {
        // Navigation is handled by the composable through callbacks
        // This method can be used for analytics or side effects
        android.util.Log.d("SettingsHubViewModel", "Navigating to: ${destination.route}")
    }

    /**
     * Refreshes the user profile data.
     */
    fun refreshUserProfile() {
        // Clear cache to force fresh data
        UserProfileManager.clearCache()
        loadUserProfile()
    }
    
    /**
     * Force refresh with cache clearing
     */
    fun forceRefreshProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Clear cache first
                UserProfileManager.clearCache()
                
                // Wait a bit to ensure cache is cleared
                kotlinx.coroutines.delay(100)
                
                // Reload profile
                loadUserProfile()
            } catch (e: Exception) {
                android.util.Log.e("SettingsHubViewModel", "Failed to force refresh profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
