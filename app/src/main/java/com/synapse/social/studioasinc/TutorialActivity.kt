package com.synapse.social.studioasinc

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
// import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.fragments.MissionDetailFragment
import com.synapse.social.studioasinc.fragments.TutorialMissionsFragment
import com.synapse.social.studioasinc.model.MissionCategory
import com.synapse.social.studioasinc.presentation.TutorialViewModel
import kotlinx.coroutines.launch

/**
 * Activity for displaying and managing tutorial missions
 */
class TutorialActivity : BaseActivity() {
    
    private lateinit var viewModel: TutorialViewModel
    private lateinit var containerLayout: FrameLayout
    private lateinit var headerLayout: LinearLayout
    private lateinit var statsText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[TutorialViewModel::class.java]
        
        setupUI()
        observeViewModel()
        
        // Show missions list by default
        if (savedInstanceState == null) {
            showMissionsList()
        }
    }
    
    /**
     * Setup UI programmatically
     */
    private fun setupUI() {
        val density = resources.displayMetrics.density
        
        // Root layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }
        
        // Header
        headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val padding = (16 * density).toInt()
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.parseColor("#2196F3"))
        }
        
        val titleText = TextView(this).apply {
            text = "Tutorial Missions"
            textSize = 24f
            setTextColor(Color.WHITE)
        }
        headerLayout.addView(titleText)
        
        statsText = TextView(this).apply {
            text = "Loading..."
            textSize = 14f
            setTextColor(Color.parseColor("#E3F2FD"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
        }
        headerLayout.addView(statsText)
        
        rootLayout.addView(headerLayout)
        
        // Category filter buttons
        val filterLayout = createCategoryFilters(density)
        rootLayout.addView(filterLayout)
        
        // Container for fragments
        containerLayout = FrameLayout(this).apply {
            id = android.R.id.content
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        rootLayout.addView(containerLayout)
        
        setContentView(rootLayout)
    }
    
    /**
     * Create category filter buttons
     */
    private fun createCategoryFilters(density: Float): LinearLayout {
        val filterLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val padding = (8 * density).toInt()
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }
        
        // All button
        val allButton = createFilterButton("All", null, density)
        filterLayout.addView(allButton)
        
        // Category buttons
        MissionCategory.values().forEach { category ->
            val button = createFilterButton(
                "${category.getIcon()} ${category.getDisplayName()}",
                category,
                density
            )
            filterLayout.addView(button)
        }
        
        return filterLayout
    }
    
    /**
     * Create a filter button
     */
    private fun createFilterButton(
        text: String,
        category: MissionCategory?,
        density: Float
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
            val padding = (8 * density).toInt()
            setPadding(padding, padding, padding, padding)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = (8 * density).toInt()
            }
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            
            setOnClickListener {
                viewModel.filterByCategory(category)
            }
        }
    }
    
    /**
     * Observe ViewModel
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.userProgress.collect { progress ->
                updateStats(progress.totalXp, progress.getLevel(), progress.badges.size)
            }
        }
    }
    
    /**
     * Update stats display
     */
    private fun updateStats(xp: Int, level: Int, badges: Int) {
        val stats = viewModel.getStatistics()
        statsText.text = buildString {
            append("Level $level • $xp XP • $badges Badges\n")
            append("${stats.completedMissions}/${stats.totalMissions} missions completed")
            if (stats.inProgressMissions > 0) {
                append(" • ${stats.inProgressMissions} in progress")
            }
        }
    }
    
    /**
     * Show missions list
     */
    private fun showMissionsList() {
        val fragment = TutorialMissionsFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(containerLayout.id, fragment)
            .commit()
    }
    
    /**
     * Show mission detail
     */
    fun showMissionDetail(missionId: String) {
        val fragment = MissionDetailFragment.newInstance(missionId)
        supportFragmentManager.beginTransaction()
            .replace(containerLayout.id, fragment)
            .addToBackStack(null)
            .commit()
    }
}
