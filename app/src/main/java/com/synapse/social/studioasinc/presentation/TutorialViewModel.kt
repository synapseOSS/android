package com.synapse.social.studioasinc.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.TutorialManager
import com.synapse.social.studioasinc.domain.UserProgress
import com.synapse.social.studioasinc.model.MissionCategory
import com.synapse.social.studioasinc.model.TutorialMission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing tutorial missions and user progress
 */
class TutorialViewModel(application: Application) : AndroidViewModel(application) {
    
    private val tutorialManager = TutorialManager.getInstance(application)
    
    private val _uiState = MutableStateFlow<TutorialUiState>(TutorialUiState.Loading)
    val uiState: StateFlow<TutorialUiState> = _uiState.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<MissionCategory?>(null)
    val selectedCategory: StateFlow<MissionCategory?> = _selectedCategory.asStateFlow()
    
    val missions: StateFlow<List<TutorialMission>> = tutorialManager.missions
    val currentMission: StateFlow<TutorialMission?> = tutorialManager.currentMission
    val userProgress: StateFlow<UserProgress> = tutorialManager.userProgress
    
    init {
        loadMissions()
    }
    
    /**
     * Load missions and update UI state
     */
    private fun loadMissions() {
        viewModelScope.launch {
            try {
                tutorialManager.missions.collect { missions ->
                    if (missions.isEmpty()) {
                        _uiState.value = TutorialUiState.Empty
                    } else {
                        _uiState.value = TutorialUiState.Success(missions)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = TutorialUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Start a mission
     */
    fun startMission(missionId: String) {
        viewModelScope.launch {
            try {
                tutorialManager.startMission(missionId)
            } catch (e: Exception) {
                _uiState.value = TutorialUiState.Error("Failed to start mission: ${e.message}")
            }
        }
    }
    
    /**
     * Complete a mission step
     */
    fun completeStep(missionId: String, stepId: String) {
        viewModelScope.launch {
            try {
                tutorialManager.completeStep(missionId, stepId)
            } catch (e: Exception) {
                _uiState.value = TutorialUiState.Error("Failed to complete step: ${e.message}")
            }
        }
    }
    
    /**
     * Filter missions by category
     */
    fun filterByCategory(category: MissionCategory?) {
        _selectedCategory.value = category
    }
    
    /**
     * Get filtered missions based on selected category
     */
    fun getFilteredMissions(): List<TutorialMission> {
        val allMissions = missions.value
        val category = _selectedCategory.value
        
        return if (category != null) {
            tutorialManager.getMissionsByCategory(category)
        } else {
            allMissions
        }
    }
    
    /**
     * Get missions by status
     */
    fun getCompletedMissions(): List<TutorialMission> {
        return tutorialManager.getCompletedMissions()
    }
    
    fun getInProgressMissions(): List<TutorialMission> {
        return tutorialManager.getInProgressMissions()
    }
    
    fun getAvailableMissions(): List<TutorialMission> {
        return tutorialManager.getAvailableMissions()
    }
    
    /**
     * Reset all missions (for testing)
     */
    fun resetAllMissions() {
        viewModelScope.launch {
            tutorialManager.resetAllMissions()
        }
    }
    
    /**
     * Get statistics
     */
    fun getStatistics(): TutorialStatistics {
        val allMissions = missions.value
        return TutorialStatistics(
            totalMissions = allMissions.size,
            completedMissions = allMissions.count { it.isCompleted },
            inProgressMissions = allMissions.count { it.isInProgress() },
            totalXp = userProgress.value.totalXp,
            level = userProgress.value.getLevel(),
            badges = userProgress.value.badges.size
        )
    }
}

/**
 * UI state for tutorial screen
 */
sealed class TutorialUiState {
    object Loading : TutorialUiState()
    object Empty : TutorialUiState()
    data class Success(val missions: List<TutorialMission>) : TutorialUiState()
    data class Error(val message: String) : TutorialUiState()
}

/**
 * Tutorial statistics data
 */
data class TutorialStatistics(
    val totalMissions: Int,
    val completedMissions: Int,
    val inProgressMissions: Int,
    val totalXp: Int,
    val level: Int,
    val badges: Int
) {
    fun getCompletionPercentage(): Int {
        return if (totalMissions > 0) {
            (completedMissions * 100) / totalMissions
        } else {
            0
        }
    }
}
