package com.synapse.social.studioasinc.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.domain.TutorialManager
import com.synapse.social.studioasinc.model.MissionStep
import com.synapse.social.studioasinc.model.TutorialMission
import kotlinx.coroutines.launch

/**
 * Fragment displaying detailed view of a single tutorial mission
 */
class MissionDetailFragment : Fragment() {
    
    private lateinit var tutorialManager: TutorialManager
    private var missionId: String? = null
    private var currentMission: TutorialMission? = null
    
    private lateinit var scrollView: ScrollView
    private lateinit var containerLayout: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        missionId = arguments?.getString(ARG_MISSION_ID)
        tutorialManager = TutorialManager.getInstance(requireContext())
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val density = resources.displayMetrics.density
        
        // Create ScrollView
        scrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Create container
        containerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val padding = (16 * density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        
        scrollView.addView(containerLayout)
        return scrollView
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeMission()
    }
    
    /**
     * Observe mission data
     */
    private fun observeMission() {
        viewLifecycleOwner.lifecycleScope.launch {
            tutorialManager.missions.collect { missions ->
                currentMission = missions.find { it.id == missionId }
                currentMission?.let { mission ->
                    displayMission(mission)
                }
            }
        }
    }
    
    /**
     * Display mission details
     */
    private fun displayMission(mission: TutorialMission) {
        containerLayout.removeAllViews()
        val density = resources.displayMetrics.density
        
        // Mission header
        addMissionHeader(mission, density)
        
        // Progress section
        addProgressSection(mission, density)
        
        // Rewards section
        addRewardsSection(mission, density)
        
        // Steps section
        addStepsSection(mission, density)
        
        // Action button
        addActionButton(mission, density)
    }
    
    /**
     * Add mission header
     */
    private fun addMissionHeader(mission: TutorialMission, density: Float) {
        // Category and difficulty
        val headerText = TextView(requireContext()).apply {
            text = "${mission.category.getIcon()} ${mission.category.getDisplayName()} • ${mission.difficulty.getDisplayName()}"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
        }
        containerLayout.addView(headerText)
        
        // Title
        val titleText = TextView(requireContext()).apply {
            text = mission.title
            textSize = 24f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
        }
        containerLayout.addView(titleText)
        
        // Description
        val descriptionText = TextView(requireContext()).apply {
            text = mission.description
            textSize = 16f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
        }
        containerLayout.addView(descriptionText)
    }
    
    /**
     * Add progress section
     */
    private fun addProgressSection(mission: TutorialMission, density: Float) {
        val sectionTitle = TextView(requireContext()).apply {
            text = "Progress"
            textSize = 18f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (24 * density).toInt()
            }
        }
        containerLayout.addView(sectionTitle)
        
        val progressBar = ProgressBar(
            requireContext(),
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = mission.getProgressPercentage()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
        }
        containerLayout.addView(progressBar)
        
        val completedSteps = mission.steps.count { it.isCompleted }
        val totalSteps = mission.steps.size
        val progressText = TextView(requireContext()).apply {
            text = "$completedSteps of $totalSteps steps completed (${mission.getProgressPercentage()}%)"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (4 * density).toInt()
            }
        }
        containerLayout.addView(progressText)
    }
    
    /**
     * Add rewards section
     */
    private fun addRewardsSection(mission: TutorialMission, density: Float) {
        val sectionTitle = TextView(requireContext()).apply {
            text = "Rewards"
            textSize = 18f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (24 * density).toInt()
            }
        }
        containerLayout.addView(sectionTitle)
        
        val rewardsText = buildString {
            append("🌟 ${mission.rewards.xp} XP")
            if (mission.rewards.badge != null) {
                append("\n🏆 Badge: ${mission.rewards.badge}")
            }
            if (mission.rewards.title != null) {
                append("\n👑 Title: ${mission.rewards.title}")
            }
            if (mission.rewards.unlockFeature != null) {
                append("\n🔓 Unlock: ${mission.rewards.unlockFeature}")
            }
        }
        
        val rewardsTextView = TextView(requireContext()).apply {
            text = rewardsText
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
        }
        containerLayout.addView(rewardsTextView)
    }
    
    /**
     * Add steps section
     */
    private fun addStepsSection(mission: TutorialMission, density: Float) {
        val sectionTitle = TextView(requireContext()).apply {
            text = "Steps"
            textSize = 18f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (24 * density).toInt()
            }
        }
        containerLayout.addView(sectionTitle)
        
        mission.steps.forEachIndexed { index, step ->
            addStepItem(step, index + 1, density)
        }
    }
    
    /**
     * Add individual step item
     */
    private fun addStepItem(step: MissionStep, stepNumber: Int, density: Float) {
        val stepContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (16 * density).toInt()
            }
            val padding = (12 * density).toInt()
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(if (step.isCompleted) {
                Color.parseColor("#E8F5E9")
            } else {
                Color.parseColor("#F5F5F5")
            })
        }
        
        val stepTitle = TextView(requireContext()).apply {
            text = if (step.isCompleted) {
                "✓ Step $stepNumber: ${step.title}"
            } else {
                "Step $stepNumber: ${step.title}"
            }
            textSize = 16f
            setTextColor(if (step.isCompleted) {
                Color.parseColor("#4CAF50")
            } else {
                Color.BLACK
            })
        }
        stepContainer.addView(stepTitle)
        
        val stepDescription = TextView(requireContext()).apply {
            text = step.description
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (4 * density).toInt()
            }
        }
        stepContainer.addView(stepDescription)
        
        if (!step.isCompleted) {
            val instructionText = TextView(requireContext()).apply {
                text = "📝 ${step.instruction}"
                textSize = 14f
                setTextColor(Color.parseColor("#2196F3"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (8 * density).toInt()
                }
            }
            stepContainer.addView(instructionText)
            
            // Add manual complete button for manual verification
            if (step.verificationType.name == "MANUAL") {
                val completeButton = Button(requireContext()).apply {
                    text = "Mark as Complete"
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (8 * density).toInt()
                    }
                    setOnClickListener {
                        currentMission?.let { mission ->
                            tutorialManager.completeStep(mission.id, step.id)
                        }
                    }
                }
                stepContainer.addView(completeButton)
            }
        }
        
        containerLayout.addView(stepContainer)
    }
    
    /**
     * Add action button
     */
    private fun addActionButton(mission: TutorialMission, density: Float) {
        val button = Button(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (24 * density).toInt()
            }
            
            when {
                mission.isCompleted -> {
                    text = "✓ Mission Completed!"
                    isEnabled = false
                    setBackgroundColor(Color.parseColor("#4CAF50"))
                }
                mission.isInProgress() -> {
                    text = "Continue Mission"
                    setBackgroundColor(Color.parseColor("#2196F3"))
                    setOnClickListener {
                        // Navigate to next step
                    }
                }
                else -> {
                    text = "Start Mission"
                    setBackgroundColor(Color.parseColor("#2196F3"))
                    setOnClickListener {
                        tutorialManager.startMission(mission.id)
                    }
                }
            }
        }
        containerLayout.addView(button)
    }
    
    companion object {
        private const val ARG_MISSION_ID = "mission_id"
        
        fun newInstance(missionId: String): MissionDetailFragment {
            return MissionDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MISSION_ID, missionId)
                }
            }
        }
    }
}
