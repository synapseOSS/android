package com.synapse.social.studioasinc.home

data class Post(
    val key: String = "",
    val uid: String = "",
    val post_text: String? = null,
    val post_type: String = "",
    val post_image: String? = null,
    val post_hide_views_count: String = "false",
    val post_region: String = "none",
    val post_hide_like_count: String = "false",
    val post_hide_comments_count: String = "false",
    val post_visibility: String = "public",
    val post_disable_favorite: String = "false",
    val post_disable_comments: String = "false",
    val publish_date: String = ""
)
