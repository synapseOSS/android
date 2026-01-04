package com.synapse.social.studioasinc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.synapse.social.studioasinc.feature.auth.ui.AuthScreen
import com.synapse.social.studioasinc.ui.home.HomeScreen
import com.synapse.social.studioasinc.ui.profile.ProfileScreen
import com.synapse.social.studioasinc.ui.inbox.InboxScreen
import com.synapse.social.studioasinc.ui.search.SearchScreen
import com.synapse.social.studioasinc.ui.search.SearchViewModel
import com.synapse.social.studioasinc.ui.postdetail.PostDetailScreen
import com.synapse.social.studioasinc.ui.createpost.CreatePostScreen
import com.synapse.social.studioasinc.ui.createpost.CreatePostViewModel
import com.synapse.social.studioasinc.ui.settings.SettingsScreen
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileScreen
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileViewModel
import com.synapse.social.studioasinc.compose.FollowListScreen
import com.synapse.social.studioasinc.ui.chat.DirectChatScreen
import com.synapse.social.studioasinc.ui.settings.ChatPrivacyScreen
import com.synapse.social.studioasinc.ui.settings.ChatPrivacyViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = AppDestination.Auth.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth
        composable(AppDestination.Auth.route) {
            val viewModel: com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.AuthViewModel = hiltViewModel()
            AuthScreen(
                viewModel = viewModel,
                onNavigateToMain = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // Home
        composable(AppDestination.Home.route) {
            HomeScreen(
                onNavigateToSearch = {
                    navController.navigate(AppDestination.Search.route)
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(AppDestination.Profile.createRoute(userId))
                },
                onNavigateToInbox = {
                    navController.navigate(AppDestination.Inbox.route)
                },
                onNavigateToCreatePost = {
                    navController.navigate(AppDestination.CreatePost.route)
                }
            )
        }

        // Profile
        composable(
            route = "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val viewModel: com.synapse.social.studioasinc.ui.profile.ProfileViewModel = hiltViewModel()
            ProfileScreen(
                userId = userId,
                currentUserId = "current_user", // TODO: Get from auth state
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatId ->
                    navController.navigate(AppDestination.Chat.createRoute(chatId))
                },
                onNavigateToFollowers = {
                    navController.navigate(AppDestination.FollowList.createRoute(userId, "followers"))
                },
                onNavigateToFollowing = {
                    navController.navigate(AppDestination.FollowList.createRoute(userId, "following"))
                },
                viewModel = viewModel
            )
        }

        // Inbox
        composable(AppDestination.Inbox.route) {
            val messageDeletionViewModel: com.synapse.social.studioasinc.ui.deletion.MessageDeletionViewModel = hiltViewModel()
            InboxScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatId, userId ->
                    navController.navigate(AppDestination.Chat.createRoute(chatId))
                },
                onNavigateToCreateGroup = {
                    // TODO: Add create group navigation
                },
                messageDeletionViewModel = messageDeletionViewModel
            )
        }

        // Search
        composable(AppDestination.Search.route) {
            val viewModel: SearchViewModel = hiltViewModel()
            SearchScreen(
                viewModel = viewModel,
                onNavigateToProfile = { userId ->
                    navController.navigate(AppDestination.Profile.createRoute(userId))
                },
                onNavigateToPost = { postId ->
                    navController.navigate(AppDestination.PostDetail.createRoute(postId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Post Detail
        composable(
            route = "post_detail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            PostDetailScreen(
                postId = postId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate(AppDestination.Profile.createRoute(userId))
                }
            )
        }

        // Create Post
        composable(AppDestination.CreatePost.route) {
            val viewModel: CreatePostViewModel = hiltViewModel()
            CreatePostScreen(
                viewModel = viewModel,
                onNavigateUp = { navController.popBackStack() }
            )
        }

        // Settings
        composable(AppDestination.Settings.route) {
            val viewModel: com.synapse.social.studioasinc.ui.settings.SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAccountClick = {
                    navController.navigate(AppDestination.EditProfile.route)
                },
                onPrivacyClick = {
                    navController.navigate("chat_privacy")
                },
                onNotificationsClick = {
                    // TODO: Add notifications navigation
                },
                onLogoutClick = {
                    // TODO: Handle logout
                }
            )
        }

        // Edit Profile
        composable(AppDestination.EditProfile.route) {
            val viewModel: EditProfileViewModel = hiltViewModel()
            EditProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegionSelection = { currentRegion ->
                    // TODO: Add region selection navigation
                },
                onNavigateToPhotoHistory = { type ->
                    // TODO: Add photo history navigation
                }
            )
        }

        // Follow List
        composable(
            route = "follow_list/{userId}/{type}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val listType = backStackEntry.arguments?.getString("type") ?: return@composable
            FollowListScreen(
                userId = userId,
                listType = listType,
                onNavigateBack = { navController.popBackStack() },
                onUserClick = { profileUserId ->
                    navController.navigate(AppDestination.Profile.createRoute(profileUserId))
                },
                onMessageClick = { chatId ->
                    navController.navigate(AppDestination.Chat.createRoute(chatId))
                }
            )
        }

        // Chat
        composable(
            route = "chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            DirectChatScreen(
                chatId = chatId,
                otherUserId = "other_user", // TODO: Extract from chatId or pass separately
                onBackClick = { navController.popBackStack() }
            )
        }

        // Chat Privacy Settings
        composable("chat_privacy") {
            val viewModel: ChatPrivacyViewModel = hiltViewModel()
            ChatPrivacyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
