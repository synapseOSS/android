package com.synapse.social.studioasinc.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl

/**
 * Navigation host for the Settings feature.
 * 
 * Manages navigation between all settings screens with consistent transitions
 * and state preservation. Uses Jetpack Compose Navigation with Material 3
 * motion design.
 * 
 * Requirements: 1.2, 1.3
 * 
 * @param modifier Modifier to be applied to the NavHost
 * @param navController Navigation controller for managing navigation state
 * @param startDestination Initial destination route (defaults to Settings Hub)
 * @param onBackClick Callback to exit the settings flow (finish activity)
 * @param onNavigateToProfileEdit Callback to navigate to ProfileEditActivity
 * @param onNavigateToChatPrivacy Callback to navigate to ChatPrivacySettingsActivity
 * @param onLogout Callback to perform logout
 */
@Composable
fun SettingsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = SettingsDestination.ROUTE_HUB,
    onBackClick: () -> Unit = {},
    onNavigateToProfileEdit: () -> Unit = {},
    onNavigateToChatPrivacy: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsRepository = SettingsRepositoryImpl.getInstance(context)
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { SettingsAnimations.enterTransition },
        exitTransition = { SettingsAnimations.exitTransition },
        popEnterTransition = { SettingsAnimations.popEnterTransition },
        popExitTransition = { SettingsAnimations.popExitTransition }
    ) {
        // Settings Hub - Main screen with categorized settings
        composable(route = SettingsDestination.ROUTE_HUB) {
            val viewModel: SettingsHubViewModel = viewModel()
            SettingsHubScreen(
                viewModel = viewModel,
                onBackClick = onBackClick,
                onEditProfileClick = onNavigateToProfileEdit,
                onNavigateToCategory = { destination ->
                    navController.navigate(destination.route)
                }
            )
        }

        // Account Settings Screen
        composable(route = SettingsDestination.ROUTE_ACCOUNT) {
            val viewModel: AccountSettingsViewModel = viewModel()
            AccountSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditProfile = onNavigateToProfileEdit,
                onLogout = onLogout
            )
        }

        // Privacy & Security Settings Screen
        composable(route = SettingsDestination.ROUTE_PRIVACY) {
            val viewModel: PrivacySecurityViewModel = viewModel()
            PrivacySecurityScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToBlockedUsers = {
                    // Placeholder - navigate to blocked users screen
                },
                onNavigateToMutedUsers = {
                    // Placeholder - navigate to muted users screen
                },
                onNavigateToActiveSessions = {
                    // Placeholder - navigate to active sessions screen
                }
            )
        }

        // Appearance Settings Screen
        composable(route = SettingsDestination.ROUTE_APPEARANCE) {
            val viewModel: AppearanceViewModel = viewModel()
            AppearanceScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChatCustomization = {
                    // Placeholder - navigate to chat customization screen
                }
            )
        }

        // Notification Settings Screen
        composable(route = SettingsDestination.ROUTE_NOTIFICATIONS) {
            val viewModel: NotificationSettingsViewModel = viewModel(
                factory = NotificationSettingsViewModelFactory(settingsRepository)
            )
            NotificationSettingsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Chat Settings Screen
        composable(route = SettingsDestination.ROUTE_CHAT) {
            val viewModel: ChatSettingsViewModel = viewModel(
                factory = ChatSettingsViewModelFactory(settingsRepository)
            )
            ChatSettingsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToChatPrivacy = onNavigateToChatPrivacy
            )
        }

        // Storage & Data Settings Screen
        composable(route = SettingsDestination.ROUTE_STORAGE) {
            val viewModel: StorageDataViewModel = viewModel(
                factory = StorageDataViewModelFactory(settingsRepository)
            )
            StorageDataScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToStorageProvider = {
                    // Placeholder - navigate to storage provider config
                },
                onNavigateToAIConfig = {
                    // Placeholder - navigate to AI config
                }
            )
        }

        // Language & Region Settings Screen
        composable(route = SettingsDestination.ROUTE_LANGUAGE) {
            val viewModel: LanguageRegionViewModel = viewModel()
            LanguageRegionScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // About & Support Settings Screen
        composable(route = SettingsDestination.ROUTE_ABOUT) {
            val viewModel: AboutSupportViewModel = viewModel()
            AboutSupportScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }
    }
}
