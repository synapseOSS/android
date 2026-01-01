package com.synapse.social.studioasinc.model

import androidx.annotation.DrawableRes

data class PostActionItem(
    val label: String,
    @get:DrawableRes val icon: Int,
    val isDestructive: Boolean = false,
    val action: () -> Unit
)
