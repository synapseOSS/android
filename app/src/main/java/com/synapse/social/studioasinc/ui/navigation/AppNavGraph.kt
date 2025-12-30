package com.synapse.social.studioasinc.ui.navigation

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.synapse.social.studioasinc.ui.chat.ChatActivity
import com.synapse.social.studioasinc.CreatePostActivity
import com.synapse.social.studioasinc.FollowListActivity
import com.synapse.social.studioasinc.PostDetailActivity
import com.synapse.social.studioasinc.ProfileEditActivity
import com.synapse.social.studioasinc.SearchActivity
import com.synapse.social.studioasinc.SettingsActivity
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.ui.deletion.MessageDeletionViewModel
import com.synapse.social.studioasinc.ui.home.HomeScreen
import com.synapse.social.studioasinc.ui.inbox.InboxScreen
import com.synapse.social.studioasinc.ui.inbox.InboxViewModel
import com.synapse.social.studioasinc.ui.inbox.InboxViewModelFactory
import com.synapse.social.studioasinc.ui.profile.ProfileScreen
import com.synapse.social.studioasinc.ui.profile.ProfileViewModel
import com.synapse.social.studioasinc.ui.profile.ProfileViewModelFactory
import com.synapse.social.studioasinc.ActivityLogActivity
import com.synapse.social.studioasinc.util.ActivityTransitions
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import android.widget.Toast

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
    object CreateGroup : AppDestination("create_group")
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
    startDestination: String = AppDestination.Home.route,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    context: android.content.Context
) {
    val scope = rememberCoroutineScope()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { AppTransitions.slideInFromRight },
        exitTransition = { AppTransitions.slideOutToLeft },
        popEnterTransition = { AppTransitions.slideInFromLeft },
        popExitTransition = { AppTransitions.slideOutToRight }
    ) {
        
        // Home
        composable(AppDestination.Home.route) {
            HomeScreen(
                onNavigateToSearch = {
                    val intent = Intent(context, SearchActivity::class.java)
                    ActivityTransitions.startActivityWithTransition(context as Activity, intent)
                },
                onNavigateToProfile = { userId ->
                    val targetUid = if (userId == "me") SupabaseClient.client.auth.currentUserOrNull()?.id else userId
                    if (targetUid != null) {
                        navController.navigate(AppDestination.Profile.createRoute(targetUid))
                    }
                },
                onNavigateToInbox = {
                    navController.navigate(AppDestination.Inbox.route)
                },
                onNavigateToCreatePost = {
                    val intent = Intent(context, CreatePostActivity::class.java)
                    ActivityTransitions.startActivityWithTransition(context as Activity, intent)
                }
            )
        }
        
        // Profile
        composable(
            route = AppDestination.Profile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id

            if (currentUserId != null) {
                // Use manual factory for ProfileViewModel as it's not Hilt-annotated
                val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(context))

                // Ensure profile exists before loading
                LaunchedEffect(userId) {
                    try {
                        val authRepository = AuthRepository()
                        if (userId == currentUserId) {
                            // For current user, ensure their profile exists
                            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                            if (currentUser != null) {
                                authRepository.ensureProfileExistsPublic(currentUser.id, currentUser.email ?: "")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AppNavGraph", "Failed to ensure profile exists", e)
                    }
                }

                ProfileScreen(
                    userId = userId,
                    currentUserId = currentUserId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEditProfile = {
                         context.startActivity(Intent(context, ProfileEditActivity::class.java))
                    },
                    onNavigateToFollowers = {
                        val intent = Intent(context, FollowListActivity::class.java)
                        intent.putExtra(FollowListActivity.EXTRA_USER_ID, userId)
                        intent.putExtra(FollowListActivity.EXTRA_LIST_TYPE, FollowListActivity.TYPE_FOLLOWERS)
                        context.startActivity(intent)
                    },
                    onNavigateToFollowing = {
                        val intent = Intent(context, FollowListActivity::class.java)
                        intent.putExtra(FollowListActivity.EXTRA_USER_ID, userId)
                        intent.putExtra(FollowListActivity.EXTRA_LIST_TYPE, FollowListActivity.TYPE_FOLLOWING)
                        context.startActivity(intent)
                    },
                    onNavigateToSettings = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    onNavigateToActivityLog = {
                        context.startActivity(Intent(context, ActivityLogActivity::class.java))
                    },
                    onNavigateToUserProfile = { uid ->
                        navController.navigate(AppDestination.Profile.createRoute(uid))
                    },
                    onNavigateToChat = { targetUserId ->
                         scope.launch {
                            try {
                                if (targetUserId == currentUserId) {
                                    Toast.makeText(context, "You cannot message yourself", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                val chatService = SupabaseChatService()
                                val result = chatService.getOrCreateDirectChat(currentUserId, targetUserId)

                                result.fold(
                                    onSuccess = { chatId ->
                                        val intent = Intent(context, ChatActivity::class.java)
                                        intent.putExtra("chatId", chatId)
                                        intent.putExtra("uid", targetUserId)
                                        intent.putExtra("isGroup", false)
                                        context.startActivity(intent)
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Failed to start chat: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error starting chat: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    viewModel = viewModel
                )
            }
        }
        
        // Inbox
        composable(AppDestination.Inbox.route) {
            // MessageDeletionViewModel can be injected via Hilt if MainActivity is @AndroidEntryPoint
            val messageDeletionViewModel: MessageDeletionViewModel = hiltViewModel()
            // InboxViewModel needs manual factory
            val viewModel: InboxViewModel = viewModel(
                factory = InboxViewModelFactory(messageDeletionViewModel = messageDeletionViewModel)
            )

            InboxScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { chatId, userId ->
                    val intent = ChatActivity.createIntent(context, chatId, userId)
                    ActivityTransitions.startActivityWithTransition(context as Activity, intent)
                },
                onNavigateToCreateGroup = {
                    navController.navigate(AppDestination.CreateGroup.route)
                },
                messageDeletionViewModel = messageDeletionViewModel,
                viewModel = viewModel
            )
        }

        // Create Group
        composable(AppDestination.CreateGroup.route) {
            com.synapse.social.studioasinc.ui.inbox.screens.CreateGroupScreen(
                onNavigateBack = { navController.popBackStack() },
                onGroupCreated = { chatId ->
                    // Replace create group screen with chat screen
                    navController.popBackStack()

                    val intent = ChatActivity.createIntent(context, chatId)
                    ActivityTransitions.startActivityWithTransition(context as Activity, intent)
                }
            )
        }

        // Post Detail - Redirect to Activity for now (not used as route, handled in Home/Profile callbacks)
        // But if someone navigates to "post/{postId}", we should handle it or fail.
        // For deep links, we might need it.
        // Let's implement it as a launcher for Activity to maintain graph correctness for deep links
        composable(
            route = AppDestination.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Side-effect to launch activity and pop back
            val postId = backStackEntry.arguments?.getString("postId")
            if (postId != null) {
                // We cannot launch activity from Composable directly without context, which we have.
                // But this will overlay the activity.
                // Ideally we shouldn't have this route if we use Activity.
                // But NavigationMigration uses it.
                // Let's leave it empty or show a loader while redirecting.
                val activity = context as? Activity
                if (activity != null) {
                     androidx.compose.runtime.LaunchedEffect(postId) {
                         PostDetailActivity.start(context, postId)
                         navController.popBackStack()
                     }
                }
            }
        }
    }
}
