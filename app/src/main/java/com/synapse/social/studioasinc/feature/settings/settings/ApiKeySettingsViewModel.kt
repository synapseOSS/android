package com.synapse.social.studioasinc.ui.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.settings.ApiKeySettingsService
import com.synapse.social.studioasinc.settings.ApiKeyInfo
import com.synapse.social.studioasinc.settings.ProviderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiKeySettingsViewModel @Inject constructor(
    private val apiKeySettingsService: ApiKeySettingsService
) : ViewModel() {

    private val _apiKeys = mutableStateOf<List<ApiKeyInfo>>(emptyList())
    val apiKeys: State<List<ApiKeyInfo>> = _apiKeys

    private val _providerSettings = mutableStateOf(ProviderSettings())
    val providerSettings: State<ProviderSettings> = _providerSettings

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val keys = apiKeySettingsService.getUserApiKeys()
                _apiKeys.value = keys
                
                val settings = apiKeySettingsService.getProviderSettings()
                _providerSettings.value = settings
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addApiKey(provider: String, keyName: String, apiKey: String) {
        viewModelScope.launch {
            try {
                apiKeySettingsService.addApiKey(provider, keyName, apiKey)
                loadSettings() // Refresh
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteApiKey(keyId: String) {
        viewModelScope.launch {
            try {
                apiKeySettingsService.deleteApiKey(keyId)
                loadSettings() // Refresh
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updatePreferredProvider(provider: String) {
        viewModelScope.launch {
            try {
                apiKeySettingsService.updatePreferredProvider(provider)
                loadSettings() // Refresh
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateFallbackSetting(fallbackToPlatform: Boolean) {
        viewModelScope.launch {
            try {
                apiKeySettingsService.updateFallbackSetting(fallbackToPlatform)
                loadSettings() // Refresh
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getProviderDisplayName(provider: String): String {
        return apiKeySettingsService.getProviderDisplayName(provider)
    }

    fun getAvailableProviders(): List<String> {
        // Filter out platform since users don't add keys for it
        return apiKeySettingsService.getAvailableProviders().filter { it != "platform" }
    }
}
