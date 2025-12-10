package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.local.AIConfig
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.local.AppSettingsManager
import com.synapse.social.studioasinc.data.local.CloudflareR2Config
import com.synapse.social.studioasinc.data.local.CloudinaryConfig
import com.synapse.social.studioasinc.data.local.StorageConfig
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appSettingsManager = AppSettingsManager.getInstance(application)
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository(AppDatabase.getDatabase(application).userDao())

    val aiConfig: StateFlow<AIConfig> = appSettingsManager.aiConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AIConfig("Gemini", "", "")
        )

    val storageConfig: StateFlow<StorageConfig> = appSettingsManager.storageConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StorageConfig(
                provider = "ImgBB",
                imgBBConfig = com.synapse.social.studioasinc.data.local.ImgBBConfig(""),
                cloudinaryConfig = CloudinaryConfig("", "", ""),
                r2Config = CloudflareR2Config("", "", "", ""),
                supabaseConfig = com.synapse.social.studioasinc.data.local.SupabaseConfig("", "", "")
            )
        )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                userRepository.getUserById(userId)
                    .onSuccess { user ->
                        _currentUser.value = user
                    }
                    .onFailure { e ->
                        // Log error or handle failure
                        android.util.Log.e("SettingsViewModel", "Failed to fetch user", e)
                    }
            }
        }
    }

    fun updateAIConfig(provider: String, apiKey: String, endpoint: String) {
        viewModelScope.launch {
            appSettingsManager.updateAIConfig(AIConfig(provider, apiKey, endpoint))
        }
    }

    fun updateStorageProvider(provider: String) {
        viewModelScope.launch {
            appSettingsManager.updateStorageProvider(provider)
        }
    }

    fun updateImgBBConfig(apiKey: String) {
        viewModelScope.launch {
            appSettingsManager.updateImgBBConfig(apiKey)
        }
    }

    fun updateCloudinaryConfig(cloudName: String, apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            appSettingsManager.updateCloudinaryConfig(CloudinaryConfig(cloudName, apiKey, apiSecret))
        }
    }

    fun updateR2Config(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String) {
        viewModelScope.launch {
            appSettingsManager.updateR2Config(CloudflareR2Config(accountId, accessKeyId, secretAccessKey, bucketName))
        }
    }

    fun updateSupabaseConfig(url: String, apiKey: String, bucketName: String) {
        viewModelScope.launch {
            appSettingsManager.updateSupabaseConfig(com.synapse.social.studioasinc.data.local.SupabaseConfig(url, apiKey, bucketName))
        }
    }
}
