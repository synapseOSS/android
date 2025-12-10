package com.synapse.social.studioasinc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.adapters.TutorialMissionsAdapter
import com.synapse.social.studioasinc.domain.TutorialManager
import com.synapse.social.studioasinc.model.MissionCategory
import com.synapse.social.studioasinc.model.TutorialMission
import kotlinx.coroutines.launch

/**
 * Fragment displaying tutorial missions for users to complete
 */
class TutorialMissionsFragment : Fragment() {
    
    private lateinit var tutorialManager: TutorialManager
    private lateinit var missionsAdapter: TutorialMissionsAdapter
    private lateinit var recyclerView: RecyclerView
    
    private var selectedCategory: MissionCategory? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // For now, create a simple RecyclerView programmatically
        // In production, you'd use ViewBinding with a proper layout
        recyclerView = RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(requireContext())
        }
        return recyclerView
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tutorialManager = TutorialManager.getInstance(requireContext())
        setupRecyclerView()
        observeMissions()
    }
    
    /**
     * Setup RecyclerView with adapter
     */
    private fun setupRecyclerView() {
        missionsAdapter = TutorialMissionsAdapter(
            onMissionClick = { mission ->
                handleMissionClick(mission)
            },
            onStartMission = { mission ->
                startMission(mission)
            }
        )
        
        recyclerView.adapter = missionsAdapter
    }
    
    /**
     * Observe missions from TutorialManager
     */
    private fun observeMissions() {
        viewLifecycleOwner.lifecycleScope.launch {
            tutorialManager.missions.collect { missions ->
                val filteredMissions = if (selectedCategory != null) {
                    missions.filter { it.category == selectedCategory }
                } else {
                    missions
                }
                missionsAdapter.submitList(filteredMissions)
            }
        }
    }
    
    /**
     * Handle mission item click
     */
    private fun handleMissionClick(mission: TutorialMission) {
        // Navigate to mission detail screen
        // TODO: Implement navigation to MissionDetailFragment
    }
    
    /**
     * Start a mission
     */
    private fun startMission(mission: TutorialMission) {
        tutorialManager.startMission(mission.id)
    }
    
    /**
     * Filter missions by category
     */
    fun filterByCategory(category: MissionCategory?) {
        selectedCategory = category
        observeMissions()
    }
    
    companion object {
        fun newInstance(): TutorialMissionsFragment {
            return TutorialMissionsFragment()
        }
    }
}
