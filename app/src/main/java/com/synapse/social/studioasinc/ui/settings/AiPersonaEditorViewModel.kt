package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.data.repository.AiRepository
import com.synapse.social.studioasinc.domain.model.AiPersonaConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AiPersonaEditorViewModel : ViewModel() {

    private val repository = AiRepository()

    private val _personaConfig = MutableStateFlow<AiPersonaConfig?>(null)
    val personaConfig: StateFlow<AiPersonaConfig?> = _personaConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadPersonaConfig()
    }

    private fun loadPersonaConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = UserProfileManager.getCurrentUserProfile()
                if (user != null) {
                    val config = repository.getPersonaConfig(user.uid)
                    _personaConfig.value = config
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load persona config: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun savePersonaConfig(traits: String, schedule: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val user = UserProfileManager.getCurrentUserProfile()
                if (user != null) {
                    // Simple conversion to JsonElement for now.
                    // In a real app, we might want structured input.
                    val traitsJson: JsonElement = try {
                         Json.parseToJsonElement(traits)
                    } catch (e: Exception) {
                        buildJsonObject { put("traits", traits) }
                    }

                    val scheduleJson: JsonElement = try {
                        Json.parseToJsonElement(schedule)
                    } catch (e: Exception) {
                        buildJsonObject { put("schedule", schedule) }
                    }

                    repository.updatePersonaConfig(user.uid, traitsJson, scheduleJson)
                    _successMessage.value = "Persona configuration saved!"
                    loadPersonaConfig()
                } else {
                    _errorMessage.value = "User not logged in"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
