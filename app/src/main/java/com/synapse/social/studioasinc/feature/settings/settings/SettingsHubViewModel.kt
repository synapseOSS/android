package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
@HiltViewModel
class SettingsHubViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

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
        // Group A: Premium & Profile
        val groupA = SettingsGroup(
            id = "group_a",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "synapse_plus",
                    title = "Synapse Plus",
                    subtitle = "Show that your profile is verified",
                    icon = R.drawable.ic_verified,
                    destination = SettingsDestination.SynapsePlus
                ),
                SettingsCategory(
                    id = "account",
                    title = "Account",
                    subtitle = "Security notifications, change number",
                    icon = R.drawable.ic_person,
                    destination = SettingsDestination.Account
                ),
                SettingsCategory(
                    id = "avatar",
                    title = "Avatar",
                    subtitle = "Create, edit, profile photo",
                    icon = R.drawable.ic_face,
                    destination = SettingsDestination.Avatar
                )
            )
        )

        // Group B: Privacy & Personalization
        val groupB = SettingsGroup(
            id = "group_b",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "privacy",
                    title = "Privacy",
                    subtitle = "Block contacts, disappearing messages",
                    icon = R.drawable.ic_shield_lock,
                    destination = SettingsDestination.Privacy
                ),
                SettingsCategory(
                    id = "favourites",
                    title = "Favourites",
                    subtitle = "Add, reorder, remove",
                    icon = R.drawable.ic_favorite,
                    destination = SettingsDestination.Favourites
                ),
                SettingsCategory(
                    id = "appearance",
                    title = "Appearance",
                    subtitle = "Theme, wallpapers, font size",
                    icon = R.drawable.ic_palette,
                    destination = SettingsDestination.Appearance
                )
            )
        )

        // Group C: Communication & Media
        val groupC = SettingsGroup(
            id = "group_c",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "chat",
                    title = "Chats",
                    subtitle = "Theme, wallpapers, chat history",
                    icon = R.drawable.ic_message,
                    destination = SettingsDestination.Chat
                ),
                SettingsCategory(
                    id = "notifications",
                    title = "Notifications",
                    subtitle = "Message, group & call tones",
                    icon = R.drawable.ic_notifications,
                    destination = SettingsDestination.Notifications
                ),
                SettingsCategory(
                    id = "storage",
                    title = "Storage and Data",
                    subtitle = "Network usage, auto-download",
                    icon = R.drawable.data_usage_24px,
                    destination = SettingsDestination.Storage
                )
            )
        )

        // Group D: Accessibility & Support
        val groupD = SettingsGroup(
            id = "group_d",
            title = null,
            categories = listOf(
                SettingsCategory(
                    id = "accessibility",
                    title = "Accessibility",
                    subtitle = "Increase contrast, animation",
                    icon = R.drawable.ic_accessibility,
                    destination = SettingsDestination.Accessibility
                ),
                SettingsCategory(
                    id = "language",
                    title = "App Language",
                    subtitle = "Language selection (e.g., English)",
                    icon = R.drawable.ic_public,
                    destination = SettingsDestination.Language
                ),
                SettingsCategory(
                    id = "about",
                    title = "Help and Feedback",
                    subtitle = "Help center, contact us, privacy policy",
                    icon = R.drawable.ic_info_48px,
                    destination = SettingsDestination.About
                )
            )
        )

        // Group E: AI & Experimental
        val groupE = SettingsGroup(
            id = "group_e",
            title = "AI & Experimental",
            categories = listOf(
                SettingsCategory(
                    id = "storage_provider",
                    title = "Storage Providers",
                    subtitle = "Configure Cloudflare, Cloudinary, Supabase",
                    icon = R.drawable.ic_cloud_upload,
                    destination = SettingsDestination.StorageProvider
                )
            )
        )

        _settingsGroups.value = listOf(groupA, groupB, groupC, groupD, groupE)
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
}
