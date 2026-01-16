package com.synapse.social.studioasinc.ui.navigation

sealed class AppDestination(val route: String) {
    object Auth : AppDestination("auth")
    object Home : AppDestination("home")
    object Profile : AppDestination("profile") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object Inbox : AppDestination("inbox")
    object Search : AppDestination("search")
    object PostDetail : AppDestination("post_detail") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object CreatePost : AppDestination("create_post")
    object Settings : AppDestination("settings")
    object EditProfile : AppDestination("edit_profile")
    object RegionSelection : AppDestination("region_selection")
    object PhotoHistory : AppDestination("photo_history/{type}") {
        fun createRoute(type: String) = "photo_history/$type"
    }
    object FollowList : AppDestination("follow_list") {
        fun createRoute(userId: String, type: String) = "follow_list/$userId/$type"
    }
    object Chat : AppDestination("chat") {
        fun createRoute(chatId: String, userId: String? = null) = 
            if (userId != null) "chat/$chatId?userId=$userId" else "chat/$chatId"
    }
    
    // Story destinations
    object StoryViewer : AppDestination("story_viewer/{userId}") {
        fun createRoute(userId: String) = "story_viewer/$userId"
    }
    object StoryCreator : AppDestination("story_creator")
}

