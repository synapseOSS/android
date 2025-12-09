package com.synapse.social.studioasinc.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.model.TutorialMission

/**
 * Adapter for displaying tutorial missions in a RecyclerView
 */
class TutorialMissionsAdapter(
    private val onMissionClick: (TutorialMission) -> Unit,
    private val onStartMission: (TutorialMission) -> Unit
) : ListAdapter<TutorialMission, TutorialMissionsAdapter.MissionViewHolder>(MissionDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissionViewHolder {
        // Create view programmatically for now
        // In production, inflate from XML layout
        val view = createMissionItemView(parent)
        return MissionViewHolder(view, onMissionClick, onStartMission)
    }
    
    override fun onBindViewHolder(holder: MissionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    /**
     * Create mission item view programmatically
     */
    private fun createMissionItemView(parent: ViewGroup): View {
        val context = parent.context
        val density = context.resources.displayMetrics.density
        
        // Create container
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = (16 * density).toInt()
                setMargins(margin, margin / 2, margin, margin / 2)
            }
            setPadding(
                (16 * density).toInt(),
                (16 * density).toInt(),
                (16 * density).toInt(),
                (16 * density).toInt()
            )
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            elevation = 4 * density
        }
        
        // Category and difficulty row
        val headerRow = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        val categoryText = TextView(context).apply {
            id = View.generateViewId()
            tag = "category"
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
        }
        
        val difficultyText = TextView(context).apply {
            id = View.generateViewId()
            tag = "difficulty"
            textSize = 12f
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (8 * density).toInt()
            }
        }
        
        headerRow.addView(categoryText)
        headerRow.addView(difficultyText)
        container.addView(headerRow)
        
        // Title
        val titleText = TextView(context).apply {
            id = View.generateViewId()
            tag = "title"
            textSize = 18f
            setTextColor(Color.BLACK)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
        }
        container.addView(titleText)
        
        // Description
        val descriptionText = TextView(context).apply {
            id = View.generateViewId()
            tag = "description"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (4 * density).toInt()
            }
        }
        container.addView(descriptionText)
        
        // Progress bar
        val progressBar = ProgressBar(
            context,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            id = View.generateViewId()
            tag = "progress"
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * density).toInt()
            }
            max = 100
        }
        container.addView(progressBar)
        
        // Progress text
        val progressText = TextView(context).apply {
            id = View.generateViewId()
            tag = "progress_text"
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (4 * density).toInt()
            }
        }
        container.addView(progressText)
        
        // Action button
        val actionButton = Button(context).apply {
            id = View.generateViewId()
            tag = "action_button"
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * density).toInt()
            }
        }
        container.addView(actionButton)
        
        return container
    }
    
    /**
     * ViewHolder for mission items
     */
    class MissionViewHolder(
        itemView: View,
        private val onMissionClick: (TutorialMission) -> Unit,
        private val onStartMission: (TutorialMission) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val categoryText: TextView = itemView.findViewWithTag("category")
        private val difficultyText: TextView = itemView.findViewWithTag("difficulty")
        private val titleText: TextView = itemView.findViewWithTag("title")
        private val descriptionText: TextView = itemView.findViewWithTag("description")
        private val progressBar: ProgressBar = itemView.findViewWithTag("progress")
        private val progressText: TextView = itemView.findViewWithTag("progress_text")
        private val actionButton: Button = itemView.findViewWithTag("action_button")
        
        fun bind(mission: TutorialMission) {
            categoryText.text = "${mission.category.getIcon()} ${mission.category.getDisplayName()}"
            
            difficultyText.text = mission.difficulty.getDisplayName()
            difficultyText.setTextColor(Color.parseColor(mission.difficulty.getColor()))
            
            titleText.text = mission.title
            descriptionText.text = mission.description
            
            val progress = mission.getProgressPercentage()
            progressBar.progress = progress
            
            val completedSteps = mission.steps.count { it.isCompleted }
            val totalSteps = mission.steps.size
            progressText.text = "$completedSteps / $totalSteps steps completed • ${mission.getEstimatedTime()} min"
            
            // Setup action button
            when {
                mission.isCompleted -> {
                    actionButton.text = "✓ Completed"
                    actionButton.isEnabled = false
                    actionButton.setBackgroundColor(Color.parseColor("#4CAF50"))
                }
                mission.isInProgress() -> {
                    actionButton.text = "Continue"
                    actionButton.isEnabled = true
                    actionButton.setBackgroundColor(Color.parseColor("#2196F3"))
                    actionButton.setOnClickListener { onMissionClick(mission) }
                }
                else -> {
                    actionButton.text = "Start Mission"
                    actionButton.isEnabled = true
                    actionButton.setBackgroundColor(Color.parseColor("#2196F3"))
                    actionButton.setOnClickListener { onStartMission(mission) }
                }
            }
            
            itemView.setOnClickListener { onMissionClick(mission) }
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates
     */
    private class MissionDiffCallback : DiffUtil.ItemCallback<TutorialMission>() {
        override fun areItemsTheSame(oldItem: TutorialMission, newItem: TutorialMission): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TutorialMission, newItem: TutorialMission): Boolean {
            return oldItem == newItem
        }
    }
}
