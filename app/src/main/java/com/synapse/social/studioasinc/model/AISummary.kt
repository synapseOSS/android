package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AISummary model for storing AI-generated message summaries
 */
@Serializable
data class AISummary(
    val id: String = "",
    @SerialName("message_id")
    val messageId: String = "",
    @SerialName("summary_text")
    val summaryText: String = "",
    @SerialName("generated_at")
    val generatedAt: Long = 0L,
    @SerialName("generated_by")
    val generatedBy: String = "",
    @SerialName("model_version")
    val modelVersion: String = "gemini-pro",
    @SerialName("character_count")
    val characterCount: Int = 0
) {
    /**
     * Calculate estimated reading time in minutes
     * Average reading speed: 200 words per minute
     * Average word length: 5 characters
     */
    fun getEstimatedReadTime(): Int {
        val wordCount = characterCount / 5
        val minutes = wordCount / 200
        return if (minutes < 1) 1 else minutes
    }
}

/**
 * Extension function to convert HashMap to AISummary object
 */
fun HashMap<String, Any>.toAISummary(): AISummary {
    return AISummary(
        id = this["id"] as? String ?: "",
        messageId = this["message_id"] as? String ?: "",
        summaryText = this["summary_text"] as? String ?: "",
        generatedAt = (this["generated_at"] as? String)?.toLongOrNull() ?: 0L,
        generatedBy = this["generated_by"] as? String ?: "",
        modelVersion = this["model_version"] as? String ?: "gemini-pro",
        characterCount = (this["character_count"] as? String)?.toIntOrNull() ?: 0
    )
}

/**
 * Extension function to convert AISummary to HashMap for database operations
 */
fun AISummary.toHashMap(): HashMap<String, Any?> {
    return hashMapOf(
        "id" to id,
        "message_id" to messageId,
        "summary_text" to summaryText,
        "generated_at" to generatedAt.toString(),
        "generated_by" to generatedBy,
        "model_version" to modelVersion,
        "character_count" to characterCount.toString()
    )
}
