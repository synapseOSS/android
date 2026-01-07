package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Storage and Data Settings screen.
 * 
 * Manages the state for storage and data-related settings including:
 * - Cache size calculation and display
 * - Cache clearing with size calculation
 * - Data saver mode toggle
 * - Storage statistics
 * - Navigation to storage provider and AI configuration
 * 
 * Requirements: 7.1, 7.2, 7.3
 */
class StorageDataViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ========================================================================
    // State
    // ========================================================================

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    private val _dataSaverEnabled = MutableStateFlow(false)
    val dataSaverEnabled: StateFlow<Boolean> = _dataSaverEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _cacheClearedMessage = MutableStateFlow<String?>(null)
    val cacheClearedMessage: StateFlow<String?> = _cacheClearedMessage.asStateFlow()

    init {
        loadStorageSettings()
        calculateCacheSize()
    }

    // ========================================================================
    // Storage Settings Loading
    // ========================================================================

    /**
     * Loads storage and data settings from the repository.
     * 
     * Requirements: 7.1, 7.3
     */
    private fun loadStorageSettings() {
        viewModelScope.launch {
            try {
                // Load cache size
                settingsRepository.cacheSize.collect { size ->
                    _cacheSize.value = size
                }
            } catch (e: Exception) {
                android.util.Log.e("StorageDataViewModel", "Failed to load cache size", e)
            }
        }

        viewModelScope.launch {
            try {
                // Load data saver setting
                settingsRepository.dataSaverEnabled.collect { enabled ->
                    _dataSaverEnabled.value = enabled
                }
            } catch (e: Exception) {
                android.util.Log.e("StorageDataViewModel", "Failed to load data saver setting", e)
                _error.value = "Failed to load data saver setting"
            }
        }
    }

    // ========================================================================
    // Cache Size Calculation
    // ========================================================================

    /**
     * Calculates the current cache size.
     * 
     * This method triggers a recalculation of the cache size from all cache directories.
     * 
     * Requirements: 7.1
     */
    fun calculateCacheSize() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val size = settingsRepository.calculateCacheSize()
                _cacheSize.value = size
                android.util.Log.d(
                    "StorageDataViewModel",
                    "Cache size calculated: ${formatBytes(size)}"
                )
            } catch (e: Exception) {
                android.util.Log.e("StorageDataViewModel", "Failed to calculate cache size", e)
                _error.value = "Failed to calculate cache size"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Cache Clearing
    // ========================================================================

    /**
     * Clears the app cache and displays the amount of space freed.
     * 
     * This method:
     * 1. Clears all cache directories
     * 2. Calculates the amount of space freed
     * 3. Updates the cache size state
     * 4. Shows a confirmation message with freed space
     * 
     * Requirements: 7.2
     */
    fun clearCache() {
        viewModelScope.launch {
            _isClearingCache.value = true
            _error.value = null
            _cacheClearedMessage.value = null
            try {
                val freedSpace = settingsRepository.clearCache()
                _cacheSize.value = settingsRepository.calculateCacheSize()
                
                val message = if (freedSpace > 0) {
                    "Cache cleared: ${formatBytes(freedSpace)} freed"
                } else {
                    "Cache was already empty"
                }
                
                _cacheClearedMessage.value = message
                
                android.util.Log.d(
                    "StorageDataViewModel",
                    "Cache cleared successfully: ${formatBytes(freedSpace)} freed"
                )
            } catch (e: Exception) {
                android.util.Log.e("StorageDataViewModel", "Failed to clear cache", e)
                _error.value = "Failed to clear cache"
            } finally {
                _isClearingCache.value = false
            }
        }
    }

    // ========================================================================
    // Data Saver Mode
    // ========================================================================

    /**
     * Toggles data saver mode.
     * 
     * When enabled, data saver mode reduces image quality and disables auto-play videos
     * to minimize data consumption.
     * 
     * @param enabled True to enable data saver, false to disable
     * Requirements: 7.3
     */
    fun toggleDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setDataSaverEnabled(enabled)
                android.util.Log.d(
                    "StorageDataViewModel",
                    "Data saver ${if (enabled) "enabled" else "disabled"}"
                )
            } catch (e: Exception) {
                android.util.Log.e("StorageDataViewModel", "Failed to toggle data saver", e)
                _error.value = "Failed to update data saver"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================================================
    // Storage Statistics
    // ========================================================================

    /**
     * Returns formatted storage statistics for display.
     * 
     * Requirements: 7.1
     */
    fun getStorageStatistics(): StorageStatistics {
        val cacheSizeBytes = _cacheSize.value
        return StorageStatistics(
            cacheSize = cacheSizeBytes,
            cacheSizeFormatted = formatBytes(cacheSizeBytes)
        )
    }

    // ========================================================================
    // Navigation Handlers
    // ========================================================================

    /**
     * Handles navigation to storage provider configuration.
     * This will navigate to the existing storage provider settings.
     * 
     * Requirements: 7.4
     */
    fun navigateToStorageProviderConfig() {
        android.util.Log.d("StorageDataViewModel", "Navigate to storage provider configuration")
        // Navigation will be handled by the screen composable
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Formats bytes into a human-readable string (KB, MB, GB).
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Clears any error messages.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clears the cache cleared message.
     */
    fun clearCacheClearedMessage() {
        _cacheClearedMessage.value = null
    }
}

/**
 * Data class for storage statistics.
 */
data class StorageStatistics(
    val cacheSize: Long,
    val cacheSizeFormatted: String
)
