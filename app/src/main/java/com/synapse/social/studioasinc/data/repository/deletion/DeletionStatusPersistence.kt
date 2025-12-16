package com.synapse.social.studioasinc.data.repository.deletion

import android.content.Context
import android.content.SharedPreferences
import com.synapse.social.studioasinc.data.model.deletion.DeletionOperation
import com.synapse.social.studioasinc.data.model.deletion.DeletionProgress
import com.synapse.social.studioasinc.data.model.deletion.DeletionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persistence of deletion status across app restarts.
 * Stores ongoing deletion operations and progress in SharedPreferences.
 * Requirements: All requirements - deletion status persistence
 */
@Singleton
class DeletionStatusPersistence @Inject constructor(
    private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "deletion_status", 
        Context.MODE_PRIVATE
    )
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val _ongoingOperations = MutableStateFlow<List<DeletionOperation>>(emptyList())
    val ongoingOperations: StateFlow<List<DeletionOperation>> = _ongoingOperations.asStateFlow()
    
    private val _lastDeletionResult = MutableStateFlow<DeletionResult?>(null)
    val lastDeletionResult: StateFlow<DeletionResult?> = _lastDeletionResult.asStateFlow()
    
    init {
        loadPersistedState()
    }
    
    /**
     * Saves ongoing deletion operations to persistent storage
     */
    fun saveOngoingOperations(operations: List<DeletionOperation>) {
        try {
            val operationsJson = json.encodeToString(operations)
            prefs.edit()
                .putString(KEY_ONGOING_OPERATIONS, operationsJson)
                .apply()
            
            _ongoingOperations.value = operations
        } catch (e: Exception) {
            android.util.Log.e("DeletionStatusPersistence", "Failed to save ongoing operations", e)
        }
    }
    
    /**
     * Saves deletion progress to persistent storage
     */
    fun saveDeletionProgress(progress: DeletionProgress?) {
        try {
            if (progress != null) {
                val progressJson = json.encodeToString(progress)
                prefs.edit()
                    .putString(KEY_DELETION_PROGRESS, progressJson)
                    .apply()
            } else {
                prefs.edit()
                    .remove(KEY_DELETION_PROGRESS)
                    .apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("DeletionStatusPersistence", "Failed to save deletion progress", e)
        }
    }
    
    /**
     * Saves last deletion result to persistent storage
     */
    fun saveLastDeletionResult(result: DeletionResult?) {
        try {
            if (result != null) {
                val resultJson = json.encodeToString(result)
                prefs.edit()
                    .putString(KEY_LAST_DELETION_RESULT, resultJson)
                    .apply()
            } else {
                prefs.edit()
                    .remove(KEY_LAST_DELETION_RESULT)
                    .apply()
            }
            
            _lastDeletionResult.value = result
        } catch (e: Exception) {
            android.util.Log.e("DeletionStatusPersistence", "Failed to save deletion result", e)
        }
    }
    
    /**
     * Loads persisted deletion state from storage
     */
    private fun loadPersistedState() {
        try {
            // Load ongoing operations
            val operationsJson = prefs.getString(KEY_ONGOING_OPERATIONS, null)
            if (operationsJson != null) {
                val operations = json.decodeFromString<List<DeletionOperation>>(operationsJson)
                _ongoingOperations.value = operations
            }
            
            // Load last deletion result
            val resultJson = prefs.getString(KEY_LAST_DELETION_RESULT, null)
            if (resultJson != null) {
                val result = json.decodeFromString<DeletionResult>(resultJson)
                _lastDeletionResult.value = result
            }
        } catch (e: Exception) {
            android.util.Log.e("DeletionStatusPersistence", "Failed to load persisted state", e)
            clearPersistedState()
        }
    }
    
    /**
     * Gets persisted deletion progress
     */
    fun getPersistedDeletionProgress(): DeletionProgress? {
        return try {
            val progressJson = prefs.getString(KEY_DELETION_PROGRESS, null)
            if (progressJson != null) {
                json.decodeFromString<DeletionProgress>(progressJson)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("DeletionStatusPersistence", "Failed to load deletion progress", e)
            null
        }
    }
    
    /**
     * Clears all persisted deletion state
     */
    fun clearPersistedState() {
        prefs.edit()
            .remove(KEY_ONGOING_OPERATIONS)
            .remove(KEY_DELETION_PROGRESS)
            .remove(KEY_LAST_DELETION_RESULT)
            .apply()
        
        _ongoingOperations.value = emptyList()
        _lastDeletionResult.value = null
    }
    
    /**
     * Marks an operation as completed and removes it from ongoing operations
     */
    fun markOperationCompleted(operationId: String) {
        val currentOperations = _ongoingOperations.value
        val updatedOperations = currentOperations.filter { it.id != operationId }
        saveOngoingOperations(updatedOperations)
    }
    
    /**
     * Checks if there are any ongoing deletion operations
     */
    fun hasOngoingOperations(): Boolean {
        return _ongoingOperations.value.isNotEmpty()
    }
    
    companion object {
        private const val KEY_ONGOING_OPERATIONS = "ongoing_operations"
        private const val KEY_DELETION_PROGRESS = "deletion_progress"
        private const val KEY_LAST_DELETION_RESULT = "last_deletion_result"
    }
}