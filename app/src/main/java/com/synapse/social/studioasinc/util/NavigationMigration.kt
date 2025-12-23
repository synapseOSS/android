package com.synapse.social.studioasinc.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import com.synapse.social.studioasinc.ui.navigation.AppDestination

object NavigationMigration {
    
    fun navigateWithNavController(
        context: Context,
        navController: NavController?,
        destination: AppDestination,
        vararg params: Pair<String, String>
    ) {
        if (navController != null) {
            val route = when (destination) {
                is AppDestination.Profile -> {
                    val userId = params.find { it.first == "userId" }?.second ?: "me"
                    destination.createRoute(userId)
                }
                is AppDestination.PostDetail -> {
                    val postId = params.find { it.first == "postId" }?.second ?: return
                    destination.createRoute(postId)
                }
                else -> destination.route
            }
            navController.navigate(route)
        } else {
            fallbackToActivityNavigation(context, destination, *params)
        }
    }
    
    private fun fallbackToActivityNavigation(
        context: Context,
        destination: AppDestination,
        vararg params: Pair<String, String>
    ) {
        val intent = when (destination) {
            is AppDestination.Home -> Intent(context, com.synapse.social.studioasinc.HomeActivity::class.java)
            is AppDestination.Search -> Intent(context, com.synapse.social.studioasinc.SearchActivity::class.java)
            is AppDestination.Profile -> {
                Intent(context, com.synapse.social.studioasinc.ProfileActivity::class.java).apply {
                    val userId = params.find { it.first == "userId" }?.second
                    if (userId != null) putExtra("uid", userId)
                }
            }
            is AppDestination.PostDetail -> {
                Intent(context, com.synapse.social.studioasinc.PostDetailActivity::class.java).apply {
                    val postId = params.find { it.first == "postId" }?.second
                    if (postId != null) putExtra("postId", postId)
                }
            }
            is AppDestination.CreatePost -> Intent(context, com.synapse.social.studioasinc.CreatePostActivity::class.java)
            is AppDestination.Inbox -> Intent(context, com.synapse.social.studioasinc.InboxActivity::class.java)
            else -> return
        }
        
        ActivityTransitions.startActivityWithTransition(context, intent)
    }
    
    fun finishWithNavController(
        activity: Activity,
        navController: NavController?
    ) {
        if (navController != null) {
            navController.popBackStack()
        } else {
            ActivityTransitions.finishWithTransition(activity)
        }
    }
}

fun Activity.navigateWithNav3(
    navController: NavController?,
    destination: AppDestination,
    vararg params: Pair<String, String>
) {
    NavigationMigration.navigateWithNavController(this, navController, destination, *params)
}

fun Activity.finishWithNav3(navController: NavController?) {
    NavigationMigration.finishWithNavController(this, navController)
}