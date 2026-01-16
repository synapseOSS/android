package com.synapse.social.studioasinc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
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
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileEvent
import com.synapse.social.studioasinc.feature.profile.editprofile.RegionSelectionScreen
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.PhotoHistoryScreen
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.PhotoType
import com.synapse.social.studioasinc.compose.FollowListScreen
// TODO: Re-implement chat feature - chat screen imports
// import com.synapse.social.studioasinc.ui.chat.DirectChatScreen
// import com.synapse.social.studioasinc.ui.settings.ChatPrivacyScreen
// import com.synapse.social.studioasinc.ui.settings.ChatPrivacyViewModel
// TODO: Re-implement chat feature - story viewer imports
// import com.synapse.social.studioasinc.feature.stories.viewer.StoryViewerScreen
// import com.synapse.social.studioasinc.feature.stories.viewer.StoryViewerViewModel
import androidx.compose.runtime.LaunchedEffect

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
                    try {
                        navController.navigate(AppDestination.Inbox.route)
                    } catch (e: IllegalArgumentException) {
                        // Navigation graph not ready, ignore
                    }
                },
                onNavigateToCreatePost = {
                    navController.navigate(AppDestination.CreatePost.route)
                },
                onNavigateToStoryViewer = { userId ->
                    navController.navigate(AppDestination.StoryViewer.createRoute(userId))
                }
            )
        }

        // Profile
        composable(
            route = "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@composable
            val targetUserId = if (userId == "me") currentUserId else userId
            val viewModel: com.synapse.social.studioasinc.ui.profile.ProfileViewModel = hiltViewModel()
            ProfileScreen(
                userId = targetUserId,
                currentUserId = currentUserId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditProfile = {
                    navController.navigate(AppDestination.EditProfile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(AppDestination.Settings.route)
                },
                onNavigateToChat = { targetUserId ->
                    navController.navigate(AppDestination.Chat.createRoute(chatId = "new", userId = targetUserId))
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

        composable(AppDestination.Inbox.route) {
            InboxScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { _, _ -> 
                    // TODO: Re-implement chat feature - chat navigation
                },
                onNavigateToCreateGroup = {
                    // TODO: Re-implement chat feature - group creation
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(AppDestination.Profile.createRoute(userId))
                }
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
            com.synapse.social.studioasinc.ui.settings.SettingsNavHost(
                onBackClick = { navController.popBackStack() },
                onNavigateToProfileEdit = {
                    navController.navigate(AppDestination.EditProfile.route)
                },
                onNavigateToChatPrivacy = {
                    navController.navigate("chat_privacy")
                },
                onLogout = {
                    // Navigate to Auth and clear backstack
                    navController.navigate(AppDestination.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Edit Profile
        composable(AppDestination.EditProfile.route) {
            val viewModel: EditProfileViewModel = hiltViewModel()

            val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
            val selectedRegion = savedStateHandle?.get<String>("selected_region")

            LaunchedEffect(selectedRegion) {
                selectedRegion?.let { region ->
                    viewModel.onEvent(EditProfileEvent.RegionSelected(region))
                    savedStateHandle.remove<String>("selected_region")
                }
            }

            EditProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegionSelection = { _ ->
                    navController.navigate(AppDestination.RegionSelection.route)
                },
                onNavigateToPhotoHistory = { type ->
                    navController.navigate(AppDestination.PhotoHistory.createRoute(type))
                }
            )
        }

        // Photo History
        composable(
            route = AppDestination.PhotoHistory.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val typeStr = backStackEntry.arguments?.getString("type") ?: return@composable
            val photoType = try {
                PhotoType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                return@composable
            }

            PhotoHistoryScreen(
                type = photoType,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Region Selection
        composable(AppDestination.RegionSelection.route) {
            RegionSelectionScreen(
                onBackClick = { navController.popBackStack() },
                onRegionSelected = { region ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_region", region)
                    navController.popBackStack()
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
            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@composable
            val targetUserId = if (userId == "me") currentUserId else userId
            val listType = backStackEntry.arguments?.getString("type") ?: return@composable
            FollowListScreen(
                userId = targetUserId,
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

        // TODO: Re-implement chat feature - direct chat navigation
        /*
        // Chat
        composable(
            route = "chat/{chatId}?userId={userId}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("userId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val userId = backStackEntry.arguments?.getString("userId")
            
            DirectChatScreen(
                chatId = chatId,
                otherUserId = userId ?: "",
                onBackClick = { navController.popBackStack() }
            )
        }
        */

        // TODO: Re-implement chat feature - chat privacy navigation
        /*
        // Chat Privacy Settings
        composable("chat_privacy") {
            val viewModel: ChatPrivacyViewModel = hiltViewModel()
            ChatPrivacyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        */
        
        // TODO: Re-implement chat feature - story viewer navigation
        /*
        // Story Viewer
        composable(
            route = AppDestination.StoryViewer.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val viewModel: StoryViewerViewModel = hiltViewModel()
            
            LaunchedEffect(userId) {
                viewModel.loadStories(userId)
            }
            
            StoryViewerScreen(
                onClose = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
        */
    }
}
