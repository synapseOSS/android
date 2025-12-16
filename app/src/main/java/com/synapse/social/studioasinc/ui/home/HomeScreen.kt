package com.synapse.social.studioasinc.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.navigation.HomeDestinations
import com.synapse.social.studioasinc.ui.navigation.HomeNavGraph
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToCreatePost: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isPostDetail = currentDestination?.route == HomeDestinations.PostDetail.route

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    // Control bottom nav visibility based on scroll
    val isBottomBarVisible = (scrollBehavior.state.collapsedFraction < 0.5f) && !isPostDetail

    // Fetch user profile logic
    val currentUser = com.synapse.social.studioasinc.SupabaseClient.client.auth.currentUserOrNull()
    var userAvatarUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            try {
                val result = com.synapse.social.studioasinc.SupabaseClient.client.from("users")
                    .select(columns = Columns.raw("avatar")) {
                        filter {
                            eq("uid", currentUser.id)
                        }
                    }.decodeSingleOrNull<JsonObject>()

                userAvatarUrl = result?.get("avatar")?.toString()?.replace("\"", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!isPostDetail) {
                TopAppBar(
                    title = { Text(text = "Synapse") },
                    actions = {
                        IconButton(onClick = onNavigateToCreatePost) {
                            Icon(
                                imageVector = Icons.Default.AddBox,
                                contentDescription = "Create Post"
                            )
                        }
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = onNavigateToInbox) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Inbox"
                            )
                        }
                        // Profile Icon with Avatar
                        if (userAvatarUrl != null) {
                            com.synapse.social.studioasinc.ui.components.CircularAvatar(
                                imageUrl = userAvatarUrl,
                                contentDescription = "Profile",
                                size = 32.dp,
                                modifier = Modifier.padding(end = 12.dp),
                                onClick = { onNavigateToProfile("me") }
                            )
                        } else {
                            IconButton(onClick = { onNavigateToProfile("me") }) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile"
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                // navBackStackEntry already defined above

                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == HomeDestinations.Feed.route } == true,
                    onClick = {
                        navController.navigate(HomeDestinations.Feed.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentDestination?.hierarchy?.any { it.route == HomeDestinations.Feed.route } == true) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == HomeDestinations.Reels.route } == true,
                    onClick = {
                        navController.navigate(HomeDestinations.Reels.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentDestination?.hierarchy?.any { it.route == HomeDestinations.Reels.route } == true) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                            contentDescription = "Reels"
                        )
                    },
                    label = { Text("Reels") }
                )

                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == HomeDestinations.Notifications.route } == true,
                    onClick = {
                        navController.navigate(HomeDestinations.Notifications.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                // Add badge logic here
                                // Badge { Text("3") }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentDestination?.hierarchy?.any { it.route == HomeDestinations.Notifications.route } == true) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    },
                    label = { Text("Notifications") }
                )
            }
        }
    }
    ) { innerPadding ->
        HomeNavGraph(
            navController = navController,
            onNavigateToProfile = onNavigateToProfile,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
