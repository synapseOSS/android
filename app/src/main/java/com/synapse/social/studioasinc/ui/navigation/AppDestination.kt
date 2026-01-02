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
    object FollowList : AppDestination("follow_list") {
        fun createRoute(userId: String, type: String) = "follow_list/$userId/$type"
    }
    object Chat : AppDestination("chat") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
}
