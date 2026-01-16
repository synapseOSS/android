package com.synapse.social.studioasinc.data.model

enum class PrivacyLevel(val value: String) {
    PUBLIC("public"),
    FRIENDS("friends"),
    ONLY_ME("only_me");

    companion object {
        fun fromValue(value: String?): PrivacyLevel = when (value) {
            "friends" -> FRIENDS
            "only_me" -> ONLY_ME
            else -> PUBLIC
        }
    }
}
