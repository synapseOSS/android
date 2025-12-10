package com.synapse.social.studioasinc

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
// import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.domain.TutorialManager
import com.synapse.social.studioasinc.util.getTutorialTracker
import kotlinx.coroutines.launch

/**
 * Demo activity for testing the Tutorial Missions system
 * 
 * This activity provides a simple UI to:
 * - View current progress
 * - Test tracking methods
 * - Launch tutorial UI
 * - Reset progress
 * 
 * Use this for development and testing purposes.
 */
class TutorialDemoActivity : BaseActivity() {
    
    private lateinit var tutorialManager: TutorialManager
    private lateinit var progressText: TextView
    private lateinit var containerLayout: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        tutorialManager = TutorialManager.getInstance(this)
        
        setupUI()
        observeProgress()
        
        // Track navigation to this demo activity
        getTutorialTracker().trackNavigation("TutorialDemoActivity")
    }
    
    /**
     * Setup UI programmatically
     */
    private fun setupUI() {
        val density = resources.displayMetrics.density
        
        // ScrollView container
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Main container
        containerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val padding = (16 * density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        
        scrollView.addView(containerLayout)
        setContentView(scrollView)
        
        // Title
        addTitle("Tutorial Missions Demo", density)
        
        // Progress section
        addSectionTitle("Current Progress", density)
        progressText = addTextView("Loading...", density)
        
        // Actions section
        addSectionTitle("Actions", density)
        addButton("Open Tutorial UI", density) {
            openTutorialUI()
        }
        addButton("Reset All Progress", density) {
            resetProgress()
        }
        
        // Navigation tracking section
        addSectionTitle("Test Navigation Tracking", density)
        addButton("Track Home Navigation", density) {
            getTutorialTracker().trackNavigation("HomeFragment")
            showToast("Tracked: HomeFragment")
        }
        addButton("Track Chat Navigation", density) {
            getTutorialTracker().trackNavigation("ChatActivity")
            showToast("Tracked: ChatActivity")
        }
        addButton("Track Inbox Navigation", density) {
            getTutorialTracker().trackNavigation("InboxChatsFragment")
            showToast("Tracked: InboxChatsFragment")
        }
        
        // Data creation tracking section
        addSectionTitle("Test Data Creation Tracking", density)
        addButton("Track Post Created", density) {
            getTutorialTracker().trackPostCreated()
            showToast("Tracked: Post Created")
        }
        addButton("Track Comment Created", density) {
            getTutorialTracker().trackCommentCreated()
            showToast("Tracked: Comment Created")
        }
        addButton("Track Message Sent", density) {
            getTutorialTracker().trackMessageSent()
            showToast("Tracked: Message Sent")
        }
        
        // Feature usage tracking section
        addSectionTitle("Test Feature Usage Tracking", density)
        addButton("Track 5 Likes", density) {
            getTutorialTracker().trackPostLiked(5)
            showToast("Tracked: 5 Likes")
        }
        addButton("Track 3 Follows", density) {
            getTutorialTracker().trackUserFollowed(3)
            showToast("Tracked: 3 Follows")
        }
        addButton("Track Post Saved", density) {
            getTutorialTracker().trackPostSaved()
            showToast("Tracked: Post Saved")
        }
        addButton("Track Image Sent", density) {
            getTutorialTracker().trackImageSent()
            showToast("Tracked: Image Sent")
        }
        addButton("Track Reaction Added", density) {
            getTutorialTracker().trackReactionAdded()
            showToast("Tracked: Reaction Added")
        }
        
        // Profile tracking section
        addSectionTitle("Test Profile Tracking", density)
        addButton("Track Profile Photo Updated", density) {
            getTutorialTracker().trackProfilePhotoUpdated()
            showToast("Tracked: Profile Photo")
        }
        addButton("Track Bio Updated", density) {
            getTutorialTracker().trackBioUpdated()
            showToast("Tracked: Bio Updated")
        }
        addButton("Track Cover Photo Updated", density) {
            getTutorialTracker().trackCoverPhotoUpdated()
            showToast("Tracked: Cover Photo")
        }
        
        // Advanced tracking section
        addSectionTitle("Test Advanced Tracking", density)
        addButton("Track Markdown Post", density) {
            getTutorialTracker().trackMarkdownPostCreated()
            showToast("Tracked: Markdown Post")
        }
        addButton("Track Poll Created", density) {
            getTutorialTracker().trackPollCreated()
            showToast("Tracked: Poll Created")
        }
        addButton("Track Post Scheduled", density) {
            getTutorialTracker().trackPostScheduled()
            showToast("Tracked: Post Scheduled")
        }
        addButton("Track Privacy Settings", density) {
            getTutorialTracker().trackPrivacySettings()
            showToast("Tracked: Privacy Settings")
        }
        
        // Mission info section
        addSectionTitle("Mission Information", density)
        addButton("Show All Missions", density) {
            showMissionInfo()
        }
    }
    
    /**
     * Observe progress updates
     */
    private fun observeProgress() {
        lifecycleScope.launch {
            tutorialManager.userProgress.collect { progress ->
                updateProgressDisplay(progress)
            }
        }
        
        lifecycleScope.launch {
            tutorialManager.missions.collect { missions ->
                // Update UI when missions change
            }
        }
    }
    
    /**
     * Update progress display
     */
    private fun updateProgressDisplay(progress: com.synapse.social.studioasinc.domain.UserProgress) {
        val missions = tutorialManager.missions.value
        val completed = missions.count { it.isCompleted }
        val inProgress = missions.count { it.isInProgress() }
        val total = missions.size
        
        progressText.text = buildString {
            append("Level: ${progress.getLevel()}\n")
            append("Total XP: ${progress.totalXp}\n")
            append("XP to Next Level: ${progress.getXpForNextLevel()}\n")
            append("Badges: ${progress.badges.size}\n")
            append("Unlocked Features: ${progress.unlockedFeatures.size}\n")
            append("\n")
            append("Missions Completed: $completed / $total\n")
            append("Missions In Progress: $inProgress\n")
            append("Missions Available: ${total - completed - inProgress}")
        }
    }
    
    /**
     * Open tutorial UI
     */
    private fun openTutorialUI() {
        val intent = Intent(this, TutorialActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * Reset all progress
     */
    private fun resetProgress() {
        tutorialManager.resetAllMissions()
        showToast("All progress reset!")
    }
    
    /**
     * Show mission information
     */
    private fun showMissionInfo() {
        val missions = tutorialManager.missions.value
        val info = buildString {
            missions.forEach { mission ->
                append("${mission.category.getIcon()} ${mission.title}\n")
                append("  Category: ${mission.category.getDisplayName()}\n")
                append("  Difficulty: ${mission.difficulty.getDisplayName()}\n")
                append("  Steps: ${mission.steps.size}\n")
                append("  Progress: ${mission.getProgressPercentage()}%\n")
                append("  Status: ${when {
                    mission.isCompleted -> "✓ Completed"
                    mission.isInProgress() -> "⏳ In Progress"
                    else -> "📋 Available"
                }}\n")
                append("  Rewards: ${mission.rewards.xp} XP")
                if (mission.rewards.badge != null) {
                    append(", Badge: ${mission.rewards.badge}")
                }
                append("\n\n")
            }
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle("All Missions")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }
    
    /**
     * Helper: Add title
     */
    private fun addTitle(text: String, density: Float) {
        val textView = TextView(this).apply {
            this.text = text
            textSize = 24f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (16 * density).toInt()
            }
        }
        containerLayout.addView(textView)
    }
    
    /**
     * Helper: Add section title
     */
    private fun addSectionTitle(text: String, density: Float) {
        val textView = TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(Color.parseColor("#2196F3"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (24 * density).toInt()
                bottomMargin = (8 * density).toInt()
            }
        }
        containerLayout.addView(textView)
    }
    
    /**
     * Helper: Add text view
     */
    private fun addTextView(text: String, density: Float): TextView {
        val textView = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
        }
        containerLayout.addView(textView)
        return textView
    }
    
    /**
     * Helper: Add button
     */
    private fun addButton(text: String, density: Float, onClick: () -> Unit) {
        val button = Button(this).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
            setOnClickListener { onClick() }
        }
        containerLayout.addView(button)
    }
    
    /**
     * Helper: Show toast
     */
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
