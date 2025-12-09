package com.synapse.social.studioasinc.util

import android.content.Context
import com.synapse.social.studioasinc.domain.TutorialManager
import com.synapse.social.studioasinc.model.VerificationType

/**
 * Utility class for automatically tracking tutorial mission progress
 */
class TutorialTracker private constructor(private val context: Context) {
    
    private val tutorialManager = TutorialManager.getInstance(context)
    
    /**
     * Track navigation to a screen
     */
    fun trackNavigation(screenName: String) {
        checkAndCompleteSteps(VerificationType.NAVIGATION, screenName)
    }
    
    /**
     * Track an action performed by user
     */
    fun trackAction(actionName: String) {
        checkAndCompleteSteps(VerificationType.ACTION, actionName)
    }
    
    /**
     * Track data creation (post, comment, etc.)
     */
    fun trackDataCreation(dataType: String) {
        checkAndCompleteSteps(VerificationType.DATA_CREATION, dataType)
    }
    
    /**
     * Track feature usage
     */
    fun trackFeatureUsage(featureName: String, count: Int = 1) {
        val verificationData = if (count > 1) {
            "$featureName:$count"
        } else {
            featureName
        }
        checkAndCompleteSteps(VerificationType.FEATURE_USAGE, verificationData)
    }
    
    /**
     * Check and complete matching steps
     */
    private fun checkAndCompleteSteps(verificationType: VerificationType, verificationData: String) {
        val missions = tutorialManager.missions.value
        
        missions.forEach { mission ->
            if (!mission.isCompleted && mission.startedAt != null) {
                mission.steps.forEach { step ->
                    if (!step.isCompleted && 
                        step.verificationType == verificationType &&
                        matchesVerificationData(step.verificationData, verificationData)) {
                        
                        tutorialManager.completeStep(mission.id, step.id)
                    }
                }
            }
        }
    }
    
    /**
     * Check if verification data matches
     */
    private fun matchesVerificationData(expected: String?, actual: String): Boolean {
        if (expected == null) return false
        
        // Handle count-based verification (e.g., "follow_count:3")
        if (expected.contains(":") && actual.contains(":")) {
            val (expectedType, expectedCount) = expected.split(":")
            val (actualType, actualCount) = actual.split(":")
            
            if (expectedType == actualType) {
                val expectedNum = expectedCount.toIntOrNull() ?: 0
                val actualNum = actualCount.toIntOrNull() ?: 0
                return actualNum >= expectedNum
            }
        }
        
        // Simple string match
        return expected.equals(actual, ignoreCase = true)
    }
    
    /**
     * Track post creation
     */
    fun trackPostCreated() {
        trackDataCreation("post_created")
    }
    
    /**
     * Track comment creation
     */
    fun trackCommentCreated() {
        trackDataCreation("comment_created")
    }
    
    /**
     * Track message sent
     */
    fun trackMessageSent() {
        trackDataCreation("message_sent")
    }
    
    /**
     * Track image sent in chat
     */
    fun trackImageSent() {
        trackFeatureUsage("image_sent")
    }
    
    /**
     * Track reaction added
     */
    fun trackReactionAdded() {
        trackFeatureUsage("reaction_added")
    }
    
    /**
     * Track post liked
     */
    fun trackPostLiked(totalLikes: Int) {
        trackFeatureUsage("like_count", totalLikes)
    }
    
    /**
     * Track user followed
     */
    fun trackUserFollowed(totalFollows: Int) {
        trackFeatureUsage("follow_count", totalFollows)
    }
    
    /**
     * Track post saved
     */
    fun trackPostSaved() {
        trackFeatureUsage("post_saved")
    }
    
    /**
     * Track hashtag search
     */
    fun trackHashtagSearch() {
        trackFeatureUsage("hashtag_search")
    }
    
    /**
     * Track profile photo updated
     */
    fun trackProfilePhotoUpdated() {
        trackDataCreation("profile_photo")
    }
    
    /**
     * Track bio updated
     */
    fun trackBioUpdated() {
        trackDataCreation("bio")
    }
    
    /**
     * Track cover photo updated
     */
    fun trackCoverPhotoUpdated() {
        trackDataCreation("cover_photo")
    }
    
    /**
     * Track markdown post created
     */
    fun trackMarkdownPostCreated() {
        trackDataCreation("markdown_post")
    }
    
    /**
     * Track poll created
     */
    fun trackPollCreated() {
        trackDataCreation("poll_created")
    }
    
    /**
     * Track post scheduled
     */
    fun trackPostScheduled() {
        trackFeatureUsage("post_scheduled")
    }
    
    /**
     * Track privacy settings accessed
     */
    fun trackPrivacySettings() {
        trackFeatureUsage("privacy_settings")
    }
    
    companion object {
        @Volatile
        private var instance: TutorialTracker? = null
        
        fun getInstance(context: Context): TutorialTracker {
            return instance ?: synchronized(this) {
                instance ?: TutorialTracker(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Extension function for easy tracking from Activities/Fragments
 */
fun Context.getTutorialTracker(): TutorialTracker {
    return TutorialTracker.getInstance(this)
}
