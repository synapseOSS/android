package com.synapse.social.studioasinc.domain

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synapse.social.studioasinc.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages tutorial missions, progress tracking, and completion
 */
class TutorialManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _missions = MutableStateFlow<List<TutorialMission>>(emptyList())
    val missions: StateFlow<List<TutorialMission>> = _missions.asStateFlow()
    
    private val _currentMission = MutableStateFlow<TutorialMission?>(null)
    val currentMission: StateFlow<TutorialMission?> = _currentMission.asStateFlow()
    
    private val _userProgress = MutableStateFlow(UserProgress())
    val userProgress: StateFlow<UserProgress> = _userProgress.asStateFlow()
    
    init {
        loadMissions()
        loadUserProgress()
    }
    
    /**
     * Load all available missions
     */
    private fun loadMissions() {
        val savedMissions = loadMissionsFromPrefs()
        _missions.value = if (savedMissions.isEmpty()) {
            getDefaultMissions()
        } else {
            savedMissions
        }
    }
    
    /**
     * Load user progress from preferences
     */
    private fun loadUserProgress() {
        val json = prefs.getString(KEY_USER_PROGRESS, null)
        _userProgress.value = if (json != null) {
            gson.fromJson(json, UserProgress::class.java)
        } else {
            UserProgress()
        }
    }
    
    /**
     * Save missions to preferences
     */
    private fun saveMissions() {
        val json = gson.toJson(_missions.value)
        prefs.edit().putString(KEY_MISSIONS, json).apply()
    }
    
    /**
     * Save user progress to preferences
     */
    private fun saveUserProgress() {
        val json = gson.toJson(_userProgress.value)
        prefs.edit().putString(KEY_USER_PROGRESS, json).apply()
    }
    
    /**
     * Load missions from preferences
     */
    private fun loadMissionsFromPrefs(): List<TutorialMission> {
        val json = prefs.getString(KEY_MISSIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<TutorialMission>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * Start a mission
     */
    fun startMission(missionId: String) {
        val updatedMissions = _missions.value.map { mission ->
            if (mission.id == missionId && mission.startedAt == null) {
                mission.copy(startedAt = System.currentTimeMillis())
            } else {
                mission
            }
        }
        _missions.value = updatedMissions
        _currentMission.value = updatedMissions.find { it.id == missionId }
        saveMissions()
    }
    
    /**
     * Complete a mission step
     */
    fun completeStep(missionId: String, stepId: String) {
        val updatedMissions = _missions.value.map { mission ->
            if (mission.id == missionId) {
                val updatedSteps = mission.steps.map { step ->
                    if (step.id == stepId && !step.isCompleted) {
                        step.copy(
                            isCompleted = true,
                            completedAt = System.currentTimeMillis()
                        )
                    } else {
                        step
                    }
                }
                
                val completedSteps = updatedSteps.count { it.isCompleted }
                val allCompleted = completedSteps == updatedSteps.size
                
                mission.copy(
                    steps = updatedSteps,
                    currentStep = completedSteps,
                    isCompleted = allCompleted,
                    completionTime = if (allCompleted) System.currentTimeMillis() else null
                )
            } else {
                mission
            }
        }
        
        _missions.value = updatedMissions
        
        // Check if mission is completed and award rewards
        val completedMission = updatedMissions.find { it.id == missionId && it.isCompleted }
        if (completedMission != null) {
            awardRewards(completedMission.rewards)
        }
        
        saveMissions()
    }
    
    /**
     * Award rewards to user
     */
    private fun awardRewards(rewards: MissionRewards) {
        val currentProgress = _userProgress.value
        _userProgress.value = currentProgress.copy(
            totalXp = currentProgress.totalXp + rewards.xp,
            badges = if (rewards.badge != null) {
                currentProgress.badges + rewards.badge
            } else {
                currentProgress.badges
            },
            unlockedFeatures = if (rewards.unlockFeature != null) {
                currentProgress.unlockedFeatures + rewards.unlockFeature
            } else {
                currentProgress.unlockedFeatures
            }
        )
        saveUserProgress()
    }
    
    /**
     * Reset all missions (for testing)
     */
    fun resetAllMissions() {
        _missions.value = getDefaultMissions()
        _userProgress.value = UserProgress()
        _currentMission.value = null
        saveMissions()
        saveUserProgress()
    }
    
    /**
     * Get missions by category
     */
    fun getMissionsByCategory(category: MissionCategory): List<TutorialMission> {
        return _missions.value.filter { it.category == category }
    }
    
    /**
     * Get completed missions
     */
    fun getCompletedMissions(): List<TutorialMission> {
        return _missions.value.filter { it.isCompleted }
    }
    
    /**
     * Get in-progress missions
     */
    fun getInProgressMissions(): List<TutorialMission> {
        return _missions.value.filter { it.isInProgress() }
    }
    
    /**
     * Get available missions (not started)
     */
    fun getAvailableMissions(): List<TutorialMission> {
        return _missions.value.filter { it.startedAt == null }
    }
    
    companion object {
        private const val PREFS_NAME = "tutorial_prefs"
        private const val KEY_MISSIONS = "missions"
        private const val KEY_USER_PROGRESS = "user_progress"
        
        @Volatile
        private var instance: TutorialManager? = null
        
        fun getInstance(context: Context): TutorialManager {
            return instance ?: synchronized(this) {
                instance ?: TutorialManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * User progress data
 */
data class UserProgress(
    val totalXp: Int = 0,
    val badges: List<String> = emptyList(),
    val unlockedFeatures: List<String> = emptyList(),
    val completedMissionsCount: Int = 0
) {
    fun getLevel(): Int = (totalXp / 100) + 1
    fun getXpForNextLevel(): Int = (getLevel() * 100) - totalXp
}
