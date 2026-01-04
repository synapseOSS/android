package com.synapse.social.studioasinc.ui.postdetail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.synapse.social.studioasinc.ui.postdetail.PostDetailScreen

const val postDetailRoute = "post_detail/{postId}"

fun NavController.navigateToPostDetail(postId: String) {
    this.navigate("post_detail/$postId")
}

fun NavGraphBuilder.postDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    composable(
        route = postDetailRoute,
        arguments = listOf(navArgument("postId") { type = NavType.StringType })
    ) { backStackEntry ->
        val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
        PostDetailScreen(
            postId = postId,
            onNavigateBack = onNavigateBack,
            onNavigateToProfile = onNavigateToProfile
        )
    }
}
