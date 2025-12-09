package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a tutorial mission that guides users through app features
 */
@Serializable
data class TutorialMission(
    val id: String,
    val title: String,
    val description: String,
    val category: MissionCategory,
    val difficulty: MissionDifficulty,
    val steps: List<MissionStep>,
    val rewards: MissionRewards,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("current_step")
    val currentStep: Int = 0,
    @SerialName("completion_time")
    val completionTime: Long? = null,
    @SerialName("started_at")
    val startedAt: Long? = null
) {
    /**
     * Calculate mission progress percentage
     */
    fun getProgressPercentage(): Int {
        if (steps.isEmpty()) return 0
        val completedSteps = steps.count { it.isCompleted }
        return (completedSteps * 100) / steps.size
    }
    
    /**
     * Check if mission is in progress
     */
    fun isInProgress(): Boolean {
        return startedAt != null && !isCompleted && currentStep > 0
    }
    
    /**
     * Get estimated time to complete in minutes
     */
    fun getEstimatedTime(): Int {
        return steps.sumOf { it.estimatedMinutes }
    }
    
    /**
     * Get next incomplete step
     */
    fun getNextStep(): MissionStep? {
        return steps.firstOrNull { !it.isCompleted }
    }
}

/**
 * Represents a single step within a tutorial mission
 */
@Serializable
data class MissionStep(
    val id: String,
    val title: String,
    val description: String,
    val instruction: String,
    @SerialName("verification_type")
    val verificationType: VerificationType,
    @SerialName("verification_data")
    val verificationData: String? = null,
    @SerialName("estimated_minutes")
    val estimatedMinutes: Int = 5,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("completed_at")
    val completedAt: Long? = null,
    val hints: List<String> = emptyList()
)

/**
 * Mission rewards for completion
 */
@Serializable
data class MissionRewards(
    val xp: Int = 0,
    val badge: String? = null,
    @SerialName("unlock_feature")
    val unlockFeature: String? = null,
    val title: String? = null
)

/**
 * Mission categories
 */
@Serializable
enum class MissionCategory {
    @SerialName("getting_started")
    GETTING_STARTED,
    
    @SerialName("social")
    SOCIAL,
    
    @SerialName("messaging")
    MESSAGING,
    
    @SerialName("content_creation")
    CONTENT_CREATION,
    
    @SerialName("profile")
    PROFILE,
    
    @SerialName("advanced")
    ADVANCED;
    
    fun getDisplayName(): String = when (this) {
        GETTING_STARTED -> "Getting Started"
        SOCIAL -> "Social Features"
        MESSAGING -> "Messaging"
        CONTENT_CREATION -> "Content Creation"
        PROFILE -> "Profile Management"
        ADVANCED -> "Advanced Features"
    }
    
    fun getIcon(): String = when (this) {
        GETTING_STARTED -> "🚀"
        SOCIAL -> "👥"
        MESSAGING -> "💬"
        CONTENT_CREATION -> "✨"
        PROFILE -> "👤"
        ADVANCED -> "⚡"
    }
}

/**
 * Mission difficulty levels
 */
@Serializable
enum class MissionDifficulty {
    @SerialName("beginner")
    BEGINNER,
    
    @SerialName("intermediate")
    INTERMEDIATE,
    
    @SerialName("advanced")
    ADVANCED;
    
    fun getDisplayName(): String = when (this) {
        BEGINNER -> "Beginner"
        INTERMEDIATE -> "Intermediate"
        ADVANCED -> "Advanced"
    }
    
    fun getColor(): String = when (this) {
        BEGINNER -> "#4CAF50"
        INTERMEDIATE -> "#FF9800"
        ADVANCED -> "#F44336"
    }
}

/**
 * Types of verification for mission steps
 */
@Serializable
enum class VerificationType {
    @SerialName("manual")
    MANUAL,
    
    @SerialName("action")
    ACTION,
    
    @SerialName("navigation")
    NAVIGATION,
    
    @SerialName("data_creation")
    DATA_CREATION,
    
    @SerialName("feature_usage")
    FEATURE_USAGE
}
