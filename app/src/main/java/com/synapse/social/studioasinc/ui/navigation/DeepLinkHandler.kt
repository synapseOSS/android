package com.synapse.social.studioasinc.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController

object DeepLinkHandler {
    
    fun handleDeepLink(intent: Intent, navController: NavController): Boolean {
        val uri = intent.data ?: return false
        
        return when (uri.scheme) {
            "synapse" -> handleSynapseDeepLink(uri, navController)
            else -> false
        }
    }
    
    private fun handleSynapseDeepLink(uri: Uri, navController: NavController): Boolean {
        return when (uri.host) {
            "home" -> {
                navController.navigate(AppDestination.Home.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                true
            }
            
            "profile" -> {
                val userId = uri.getQueryParameter("uid") 
                    ?: uri.pathSegments.getOrNull(0) 
                    ?: return false
                navController.navigate(AppDestination.Profile.createRoute(userId))
                true
            }
            
            "post" -> {
                val postId = uri.getQueryParameter("id") 
                    ?: uri.pathSegments.getOrNull(0) 
                    ?: return false
                navController.navigate(AppDestination.PostDetail.createRoute(postId))
                true
            }
            
            "login" -> {
                navController.navigate(AppDestination.Auth.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                true
            }
            
            "chat" -> {
                navController.navigate(AppDestination.Inbox.route)
                true
            }
            
            else -> false
        }
    }
    
    fun createDeepLink(destination: AppDestination, vararg params: Pair<String, String>): Uri {
        val builder = Uri.Builder()
            .scheme("synapse")
        
        when (destination) {
            is AppDestination.Home -> builder.authority("home")
            is AppDestination.Profile -> {
                builder.authority("profile")
                params.find { it.first == "userId" }?.let { 
                    builder.appendQueryParameter("uid", it.second)
                }
            }
            is AppDestination.PostDetail -> {
                builder.authority("post")
                params.find { it.first == "postId" }?.let { 
                    builder.appendQueryParameter("id", it.second)
                }
            }
            is AppDestination.Auth -> builder.authority("login")
            is AppDestination.Inbox -> builder.authority("chat")
            else -> builder.authority("home")
        }
        
        return builder.build()
    }
}