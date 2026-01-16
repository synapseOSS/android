package com.synapse.social.studioasinc.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.synapse.social.studioasinc.ui.home.FeedScreen
import com.synapse.social.studioasinc.feature.shared.reels.ReelsScreen
import com.synapse.social.studioasinc.ui.notifications.NotificationsScreen
import com.synapse.social.studioasinc.ui.postdetail.PostDetailScreen
import com.synapse.social.studioasinc.feature.stories.creator.StoryCreatorActivity

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
    onNavigateToStoryViewer: (String) -> Unit = {},
    startDestination: String = HomeDestinations.Feed.route,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    
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
                onMediaClick = { },
                onStoryClick = { userId ->
                    // Navigate to story viewer
                    onNavigateToStoryViewer(userId)
                },
                onAddStoryClick = {
                    // Launch Story Creator Activity
                    context.startActivity(Intent(context, StoryCreatorActivity::class.java))
                },
                contentPadding = PaddingValues(bottom = bottomPadding)
            )
        }

        composable(HomeDestinations.Reels.route) {
             ReelsScreen(
                 onUserClick = { userId -> onNavigateToProfile(userId) },
                 onCommentClick = { },
                 contentPadding = PaddingValues(bottom = bottomPadding)
             )
        }

        composable(HomeDestinations.Notifications.route) {
             NotificationsScreen(
                 onNotificationClick = { notification -> },
                 onUserClick = { userId -> onNavigateToProfile(userId) },
                 contentPadding = PaddingValues(bottom = bottomPadding)
             )
        }

        composable(HomeDestinations.PostDetail.route) { backStackEntry ->
             val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
             // PostDetail typically doesn't show the bottom nav bar (or it slides away).
             // Since we removed it from Scaffold and it's overlaid in HomeActivity/HomeScreen,
             // it will slide away based on logic in HomeScreen.
             // PostDetail takes full screen. We might not need to add padding here if the bar is hidden.
             // But if the bar IS visible during transition, we might want it?
             // Usually detail screens don't have the bottom bar.
             // We don't pass contentPadding here.
             PostDetailScreen(
                 postId = postId,
                 onNavigateBack = { navController.popBackStack() },
                 onNavigateToProfile = onNavigateToProfile
             )
        }
    }
}

