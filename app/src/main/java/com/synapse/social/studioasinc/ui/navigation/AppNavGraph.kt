package com.synapse.social.studioasinc.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.synapse.social.studioasinc.ui.auth.AuthScreen
import com.synapse.social.studioasinc.ui.home.HomeScreen
import com.synapse.social.studioasinc.ui.profile.ProfileScreen
import com.synapse.social.studioasinc.ui.search.SearchScreen
import com.synapse.social.studioasinc.ui.postdetail.PostDetailScreen
import com.synapse.social.studioasinc.ui.createpost.CreatePostScreen
import com.synapse.social.studioasinc.ui.inbox.InboxScreen

sealed class AppDestination(val route: String) {
    object Auth : AppDestination("auth")
    object Home : AppDestination("home")
    object Search : AppDestination("search")
    object Profile : AppDestination("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object PostDetail : AppDestination("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }
    object CreatePost : AppDestination("create_post")
    object Inbox : AppDestination("inbox")
}

object AppTransitions {
    val slideInFromRight = slideInHorizontally(
        initialOffsetX = { it * 30 / 100 },
        animationSpec = tween(350)
    ) + fadeIn(animationSpec = tween(300)) + scaleIn(
        initialScale = 0.95f,
        animationSpec = tween(350)
    )
    
    val slideOutToLeft = slideOutHorizontally(
        targetOffsetX = { -it * 10 / 100 },
        animationSpec = tween(350)
    ) + fadeOut(
        targetAlpha = 0.7f,
        animationSpec = tween(300)
    ) + scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(350)
    )
    
    val slideInFromLeft = slideInHorizontally(
        initialOffsetX = { -it * 10 / 100 },
        animationSpec = tween(300)
    ) + fadeIn(
        initialAlpha = 0.7f,
        animationSpec = tween(250)
    ) + scaleIn(
        initialScale = 0.95f,
        animationSpec = tween(300)
    )
    
    val slideOutToRight = slideOutHorizontally(
        targetOffsetX = { it * 30 / 100 },
        animationSpec = tween(300)
    ) + fadeOut(animationSpec = tween(250)) + scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(300)
    )
}

@Composable
fun AppNavGraph(
    startDestination: String = AppDestination.Auth.route,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { AppTransitions.slideInFromRight },
        exitTransition = { AppTransitions.slideOutToLeft },
        popEnterTransition = { AppTransitions.slideInFromLeft },
        popExitTransition = { AppTransitions.slideOutToRight }
    ) {
        composable(AppDestination.Auth.route) {
            AuthScreen(
                viewModel = hiltViewModel(),
                onNavigateToMain = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        
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
        
        composable(AppDestination.Search.route) {
            SearchScreen(
                viewModel = hiltViewModel(),
                onNavigateToProfile = { userId ->
                    navController.navigate(AppDestination.Profile.createRoute(userId))
                },
                onNavigateToPost = { postId ->
                    navController.navigate(AppDestination.PostDetail.createRoute(postId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            ProfileScreen(
                userId = userId,
                currentUserId = "current_user", // TODO: Get from auth state
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUserProfile = { postId ->
                    navController.navigate(AppDestination.PostDetail.createRoute(postId))
                },
                viewModel = hiltViewModel()
            )
        }
        
        composable("post/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            PostDetailScreen(
                postId = postId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(AppDestination.Profile.createRoute(userId))
                },
                viewModel = hiltViewModel()
            )
        }
        
        composable(AppDestination.CreatePost.route) {
            CreatePostScreen(
                viewModel = hiltViewModel(),
                onNavigateUp = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(AppDestination.Inbox.route) {
            InboxScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { chatId, userId ->
                    // TODO: Navigate to chat screen when implemented
                },
                messageDeletionViewModel = hiltViewModel(),
                viewModel = hiltViewModel()
            )
        }
    }
}