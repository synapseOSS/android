package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.local.auth.TokenManager
import com.synapse.social.studioasinc.data.local.database.AppDatabase
import com.synapse.social.studioasinc.data.local.database.AppSettingsManager
import com.synapse.social.studioasinc.data.local.database.CloudflareR2Config
import com.synapse.social.studioasinc.data.local.database.CloudinaryConfig
import com.synapse.social.studioasinc.data.local.database.ImgBBConfig
import com.synapse.social.studioasinc.data.local.database.StorageConfig
import com.synapse.social.studioasinc.data.local.database.SupabaseConfig
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appSettingsManager = AppSettingsManager.getInstance(application)
    private val tokenManager = TokenManager(application)
    private val authRepository = AuthRepository(tokenManager)
    private val userRepository = UserRepository(AppDatabase.getDatabase(application).userDao())

    val storageConfig: StateFlow<StorageConfig> = appSettingsManager.storageConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StorageConfig(
                photoProvider = null,
                videoProvider = null,
                otherProvider = null,
                imgBBConfig = ImgBBConfig(""),
                cloudinaryConfig = CloudinaryConfig("", "", ""),
                r2Config = CloudflareR2Config("", "", "", ""),
                supabaseConfig = SupabaseConfig("", "", "")
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

    fun updatePhotoProvider(provider: String?) {
        viewModelScope.launch {
            appSettingsManager.updatePhotoProvider(provider)
        }
    }

    fun updateVideoProvider(provider: String?) {
        viewModelScope.launch {
            appSettingsManager.updateVideoProvider(provider)
        }
    }

    fun updateOtherProvider(provider: String?) {
        viewModelScope.launch {
            appSettingsManager.updateOtherProvider(provider)
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
            appSettingsManager.updateSupabaseConfig(SupabaseConfig(url, apiKey, bucketName))
        }
    }
}
