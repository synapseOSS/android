package com.synapse.social.studioasinc.data.remote.services.moderation

import kotlinx.serialization.Serializable

enum class ContentType {
    POST, MESSAGE, COMMENT, PROFILE
}

data class ModerationResult(
    val flagged: Boolean,
    val violations: Int,
    val actions: List<String>,
    val scores: ModerationScores
)

sealed class ModerationFeedback {
    object Safe : ModerationFeedback()
    data class Warning(val message: String) : ModerationFeedback()
    data class Error(val message: String) : ModerationFeedback()
    data class Info(val message: String) : ModerationFeedback()
}

@Serializable
data class ModerationScores(
    val toxicity: Double = 0.0,
    val spam: Double = 0.0,
    val harassment: Double = 0.0,
    val hate_speech: Double = 0.0,
    val adult_content: Double = 0.0
)

data class ModerationStats(
    val totalFlagged: Int,
    val pending: Int,
    val reviewed: Int,
    val autoActioned: Int
)
