package com.synapse.social.studioasinc.util

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.synapse.social.studioasinc.TutorialActivity

/**
 * Example integration patterns for the Tutorial Missions system
 * 
 * This file demonstrates how to integrate tutorial tracking into existing activities and fragments.
 * Copy these patterns into your actual implementation files.
 */

// ============================================================================
// EXAMPLE 1: Activity with Navigation Tracking
// ============================================================================

/**
 * Example: HomeActivity with tutorial tracking
 */
class HomeActivityExample : AppCompatActivity() {
    
    private lateinit var tutorialTracker: TutorialTracker
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize tutorial tracker
        tutorialTracker = getTutorialTracker()
        
        // Track navigation to this screen
        tutorialTracker.trackNavigation("HomeActivity")
    }
}

// ============================================================================
// EXAMPLE 2: Fragment with Navigation Tracking
// ============================================================================

/**
 * Example: HomeFragment with tutorial tracking
 */
class HomeFragmentExample : Fragment() {
    
    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Track navigation to this fragment
        requireContext().getTutorialTracker()
            .trackNavigation("HomeFragment")
    }
}

// ============================================================================
// EXAMPLE 3: Post Creation with Data Tracking
// ============================================================================

/**
 * Example: CreatePostActivity with post creation tracking
 */
class CreatePostActivityExample : AppCompatActivity() {
    
    private lateinit var tutorialTracker: TutorialTracker
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        tutorialTracker = getTutorialTracker()
        
        // Track navigation
        tutorialTracker.trackNavigation("CreatePostActivity")
    }
    
    private fun publishPost() {
        // ... post creation logic ...
        
        // Track post creation after successful publish
        tutorialTracker.trackPostCreated()
        
        // If post contains markdown
        if (postContainsMarkdown()) {
            tutorialTracker.trackMarkdownPostCreated()
        }
        
        // If post is a poll
        if (postIsPoll()) {
            tutorialTracker.trackPollCreated()
        }
    }
    
    private fun postContainsMarkdown(): Boolean {
        // Check if post uses markdown formatting
        return false // Placeholder
    }
    
    private fun postIsPoll(): Boolean {
        // Check if post is a poll
        return false // Placeholder
    }
}

// ============================================================================
// EXAMPLE 4: Chat Activity with Messaging Tracking
// ============================================================================

/**
 * Example: ChatActivity with messaging tracking
 */
class ChatActivityExample : AppCompatActivity() {
    
    private lateinit var tutorialTracker: TutorialTracker
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        tutorialTracker = getTutorialTracker()
        
        // Track navigation to chat
        tutorialTracker.trackNavigation("ChatActivity")
    }
    
    private fun sendMessage(messageText: String) {
        // ... send message logic ...
        
        // Track message sent
        tutorialTracker.trackMessageSent()
    }
    
    private fun sendImage() {
        // ... send image logic ...
        
        // Track image sent
        tutorialTracker.trackImageSent()
    }
    
    private fun addReaction() {
        // ... add reaction logic ...
        
        // Track reaction added
        tutorialTracker.trackReactionAdded()
    }
}

// ============================================================================
// EXAMPLE 5: Profile Activity with Profile Updates
// ============================================================================

/**
 * Example: ProfileEditActivity with profile tracking
 */
class ProfileEditActivityExample : AppCompatActivity() {
    
    private lateinit var tutorialTracker: TutorialTracker
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        tutorialTracker = getTutorialTracker()
    }
    
    private fun updateProfilePhoto() {
        // ... update profile photo logic ...
        
        // Track profile photo update
        tutorialTracker.trackProfilePhotoUpdated()
    }
    
    private fun updateBio(newBio: String) {
        // ... update bio logic ...
        
        // Track bio update
        tutorialTracker.trackBioUpdated()
    }
    
    private fun updateCoverPhoto() {
        // ... update cover photo logic ...
        
        // Track cover photo update
        tutorialTracker.trackCoverPhotoUpdated()
    }
}

// ============================================================================
// EXAMPLE 6: Social Interactions with Count Tracking
// ============================================================================

/**
 * Example: Social interactions with cumulative tracking
 */
class SocialInteractionsExample(private val context: Context) {
    
    private val tutorialTracker = context.getTutorialTracker()
    private var totalLikes = 0
    private var totalFollows = 0
    
    fun likePost(postId: String) {
        // ... like post logic ...
        
        // Increment and track
        totalLikes++
        tutorialTracker.trackPostLiked(totalLikes)
    }
    
    fun followUser(userId: String) {
        // ... follow user logic ...
        
        // Increment and track
        totalFollows++
        tutorialTracker.trackUserFollowed(totalFollows)
    }
    
    fun commentOnPost(postId: String, comment: String) {
        // ... comment logic ...
        
        // Track comment creation
        tutorialTracker.trackCommentCreated()
    }
    
    fun savePost(postId: String) {
        // ... save post logic ...
        
        // Track post saved
        tutorialTracker.trackPostSaved()
    }
}

// ============================================================================
// EXAMPLE 7: Search and Discovery
// ============================================================================

/**
 * Example: SearchActivity with search tracking
 */
class SearchActivityExample : AppCompatActivity() {
    
    private lateinit var tutorialTracker: TutorialTracker
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        tutorialTracker = getTutorialTracker()
        
        // Track navigation
        tutorialTracker.trackNavigation("SearchActivity")
    }
    
    private fun searchHashtag(hashtag: String) {
        // ... search logic ...
        
        // Track hashtag search
        if (hashtag.startsWith("#")) {
            tutorialTracker.trackHashtagSearch()
        }
    }
}

// ============================================================================
// EXAMPLE 8: Opening Tutorial from Settings
// ============================================================================

/**
 * Example: Launch tutorial from settings or menu
 */
class SettingsActivityExample : AppCompatActivity() {
    
    private fun openTutorialMissions() {
        val intent = Intent(this, TutorialActivity::class.java)
        startActivity(intent)
    }
    
    private fun setupMenu() {
        // In your menu setup:
        // menuItem.setOnClickListener { openTutorialMissions() }
    }
}

// ============================================================================
// EXAMPLE 9: Advanced Features Tracking
// ============================================================================

/**
 * Example: Advanced features tracking
 */
class AdvancedFeaturesExample(private val context: Context) {
    
    private val tutorialTracker = context.getTutorialTracker()
    
    fun schedulePost(scheduledTime: Long) {
        // ... schedule post logic ...
        
        // Track post scheduled
        tutorialTracker.trackPostScheduled()
    }
    
    fun openPrivacySettings() {
        // ... open privacy settings ...
        
        // Track privacy settings accessed
        tutorialTracker.trackPrivacySettings()
    }
}

// ============================================================================
// EXAMPLE 10: Checking Mission Progress
// ============================================================================

/**
 * Example: Display mission progress in UI
 */
class MissionProgressExample(private val context: Context) {
    
    private val tutorialManager = com.synapse.social.studioasinc.domain.TutorialManager.getInstance(context)
    
    fun displayProgress() {
        // Get user progress
        val progress = tutorialManager.userProgress.value
        
        println("Level: ${progress.getLevel()}")
        println("XP: ${progress.totalXp}")
        println("Badges: ${progress.badges.size}")
        println("XP to next level: ${progress.getXpForNextLevel()}")
        
        // Get mission statistics
        val completed = tutorialManager.getCompletedMissions()
        val inProgress = tutorialManager.getInProgressMissions()
        val available = tutorialManager.getAvailableMissions()
        
        println("Completed missions: ${completed.size}")
        println("In progress: ${inProgress.size}")
        println("Available: ${available.size}")
    }
    
    fun showMissionNotification() {
        val inProgress = tutorialManager.getInProgressMissions()
        
        if (inProgress.isNotEmpty()) {
            val mission = inProgress.first()
            val nextStep = mission.getNextStep()
            
            // Show notification or banner
            println("Continue mission: ${mission.title}")
            println("Next step: ${nextStep?.title}")
        }
    }
}

// ============================================================================
// INTEGRATION CHECKLIST
// ============================================================================

/**
 * Integration Checklist:
 * 
 * 1. ✅ Add navigation tracking to all major screens
 *    - HomeActivity, ChatActivity, ProfileActivity, etc.
 *    - HomeFragment, InboxChatsFragment, NotificationsFragment, etc.
 * 
 * 2. ✅ Add data creation tracking
 *    - Post creation (CreatePostActivity)
 *    - Comment creation (PostDetailActivity)
 *    - Message sending (ChatActivity)
 * 
 * 3. ✅ Add feature usage tracking
 *    - Like posts (PostsAdapter)
 *    - Follow users (ProfileActivity)
 *    - Save posts
 *    - Search hashtags (SearchActivity)
 * 
 * 4. ✅ Add profile update tracking
 *    - Profile photo (ProfileEditActivity)
 *    - Bio updates (ProfileEditActivity)
 *    - Cover photo (ProfileEditActivity)
 * 
 * 5. ✅ Add messaging tracking
 *    - Send messages (ChatActivity)
 *    - Send images (ChatActivity)
 *    - Add reactions (ChatActivity)
 * 
 * 6. ✅ Add advanced feature tracking
 *    - Markdown posts
 *    - Polls
 *    - Scheduled posts
 *    - Privacy settings
 * 
 * 7. ✅ Add tutorial entry points
 *    - Settings menu
 *    - First launch onboarding
 *    - Help section
 * 
 * 8. ✅ Test all tracking
 *    - Use resetAllMissions() for testing
 *    - Verify each step completes correctly
 *    - Check progress persistence
 */
