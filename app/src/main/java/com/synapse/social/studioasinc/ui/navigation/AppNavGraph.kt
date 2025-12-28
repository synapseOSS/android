package com.synapse.social.studioasinc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.synapse.social.studioasinc.ui.screens.HomeScreen
import com.synapse.social.studioasinc.ui.screens.ProfileScreen
import com.synapse.social.studioasinc.ui.screens.SearchScreen
import com.synapse.social.studioasinc.ui.screens.ChatScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToChat = { navController.navigate("chat") }
            )
        }
        
        composable("profile") {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("search") {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("chat") {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
