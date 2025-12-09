package com.synapse.social.studioasinc.home

data class User(
    val uid: String = "",
    val avatar: String? = null,
    val nickname: String? = null,
    val username: String? = null,
    val gender: String? = null,
    val verify: String? = null,
    val account_type: String? = null,
    val banned: String? = null
)
