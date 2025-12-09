package com.synapse.social.studioasinc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.synapse.social.studioasinc.ui.home.FeedScreen
import com.synapse.social.studioasinc.ui.reels.ReelsScreen
import com.synapse.social.studioasinc.ui.notifications.NotificationsScreen
import com.synapse.social.studioasinc.ui.postdetail.PostDetailScreen

sealed class HomeDestinations(val route: String) {
    object Feed : HomeDestinations("feed")
    object Reels : HomeDestinations("reels")
    object Notifications : HomeDestinations("notifications")
    object PostDetail : HomeDestinations("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }
    object Profile : HomeDestinations("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
}

@Composable
fun HomeNavGraph(
    navController: NavHostController,
    onNavigateToProfile: (String) -> Unit,
    startDestination: String = HomeDestinations.Feed.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(HomeDestinations.Feed.route) {
            FeedScreen(
                onPostClick = { postId -> navController.navigate(HomeDestinations.PostDetail.createRoute(postId)) },
                onUserClick = { userId -> onNavigateToProfile(userId) },
                onCommentClick = { postId -> navController.navigate(HomeDestinations.PostDetail.createRoute(postId)) },
                onMediaClick = { }
            )
        }

        composable(HomeDestinations.Reels.route) {
             ReelsScreen(
                 onUserClick = { userId -> onNavigateToProfile(userId) },
                 onCommentClick = { }
             )
        }

        composable(HomeDestinations.Notifications.route) {
             NotificationsScreen(
                 onNotificationClick = { notification -> },
                 onUserClick = { userId -> onNavigateToProfile(userId) }
             )
        }

        composable(HomeDestinations.PostDetail.route) { backStackEntry ->
             val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
             PostDetailScreen(
                 postId = postId,
                 onNavigateBack = { navController.popBackStack() },
                 onNavigateToProfile = onNavigateToProfile
             )
        }
    }
}
