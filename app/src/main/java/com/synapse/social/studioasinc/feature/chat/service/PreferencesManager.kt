package com.synapse.social.studioasinc.chat.service

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// DataStore singleton - MUST be at top level to avoid multiple instances
private val Context.chatPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "synapse_chat_preferences"
)

/**
 * Manages user preferences for chat features using DataStore.
 * Handles privacy settings for read receipts and typing indicators.
 * 
 * Requirements: 5.1, 5.4
 * 
 * IMPORTANT: Always pass applicationContext to avoid multiple DataStore instances
 */
class PreferencesManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "PreferencesManager"
        
        // Preference keys
        private val KEY_SEND_READ_RECEIPTS = booleanPreferencesKey("send_read_receipts")
        private val KEY_SHOW_TYPING_INDICATORS = booleanPreferencesKey("show_typing_indicators")
        
        // Default values
        private const val DEFAULT_SEND_READ_RECEIPTS = true
        private const val DEFAULT_SHOW_TYPING_INDICATORS = true
        
        // Singleton instance
        @Volatile
        private var INSTANCE: PreferencesManager? = null
        
        /**
         * Get singleton instance of PreferencesManager.
         * Always uses application context to avoid multiple DataStore instances.
         */
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Use the singleton DataStore instance
    private val dataStore: DataStore<Preferences>
        get() = context.chatPreferencesDataStore
    
    /**
     * Check if read receipts are enabled (synchronous version for backward compatibility).
     * 
     * @return true if user wants to send read receipts, false otherwise
     */
    fun isReadReceiptsEnabled(): Boolean {
        return runBlocking {
            dataStore.data.map { preferences ->
                preferences[KEY_SEND_READ_RECEIPTS] ?: DEFAULT_SEND_READ_RECEIPTS
            }.first()
        }
    }
    
    /**
     * Check if read receipts are enabled (suspend version).
     * 
     * @return true if user wants to send read receipts, false otherwise
     */
    suspend fun isReadReceiptsEnabledAsync(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[KEY_SEND_READ_RECEIPTS] ?: DEFAULT_SEND_READ_RECEIPTS
        }.first()
    }
    
    /**
     * Set read receipts preference.
     * 
     * @param enabled true to enable sending read receipts, false to disable
     */
    suspend fun setReadReceiptsEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting read receipts enabled: $enabled")
        dataStore.edit { preferences ->
            preferences[KEY_SEND_READ_RECEIPTS] = enabled
        }
    }
    
    /**
     * Check if typing indicators are enabled (synchronous version for backward compatibility).
     * 
     * @return true if user wants to send typing indicators, false otherwise
     */
    fun isTypingIndicatorsEnabled(): Boolean {
        return runBlocking {
            dataStore.data.map { preferences ->
                preferences[KEY_SHOW_TYPING_INDICATORS] ?: DEFAULT_SHOW_TYPING_INDICATORS
            }.first()
        }
    }
    
    /**
     * Check if typing indicators are enabled (suspend version).
     * 
     * @return true if user wants to send typing indicators, false otherwise
     */
    suspend fun isTypingIndicatorsEnabledAsync(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[KEY_SHOW_TYPING_INDICATORS] ?: DEFAULT_SHOW_TYPING_INDICATORS
        }.first()
    }
    
    /**
     * Set typing indicators preference.
     * 
     * @param enabled true to enable sending typing indicators, false to disable
     */
    suspend fun setTypingIndicatorsEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting typing indicators enabled: $enabled")
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_TYPING_INDICATORS] = enabled
        }
    }
    
    /**
     * Get read receipts preference as Flow for reactive updates.
     * 
     * @return Flow<Boolean> that emits read receipts preference changes
     */
    fun getReadReceiptsEnabledFlow(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[KEY_SEND_READ_RECEIPTS] ?: DEFAULT_SEND_READ_RECEIPTS
        }
    }
    
    /**
     * Get typing indicators preference as Flow for reactive updates.
     * 
     * @return Flow<Boolean> that emits typing indicators preference changes
     */
    fun getTypingIndicatorsEnabledFlow(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[KEY_SHOW_TYPING_INDICATORS] ?: DEFAULT_SHOW_TYPING_INDICATORS
        }
    }
    
    /**
     * Get all chat preferences.
     * 
     * @return ChatPreferences object with current settings
     */
    suspend fun getChatPreferences(): ChatPreferences {
        return dataStore.data.map { preferences ->
            ChatPreferences(
                sendReadReceipts = preferences[KEY_SEND_READ_RECEIPTS] ?: DEFAULT_SEND_READ_RECEIPTS,
                showTypingIndicators = preferences[KEY_SHOW_TYPING_INDICATORS] ?: DEFAULT_SHOW_TYPING_INDICATORS
            )
        }.first()
    }
    
    /**
     * Get all chat preferences as Flow for reactive updates.
     * 
     * @return Flow<ChatPreferences> that emits preference changes
     */
    fun getChatPreferencesFlow(): Flow<ChatPreferences> {
        return dataStore.data.map { preferences ->
            ChatPreferences(
                sendReadReceipts = preferences[KEY_SEND_READ_RECEIPTS] ?: DEFAULT_SEND_READ_RECEIPTS,
                showTypingIndicators = preferences[KEY_SHOW_TYPING_INDICATORS] ?: DEFAULT_SHOW_TYPING_INDICATORS
            )
        }
    }
    
    /**
     * Update all chat preferences at once.
     * 
     * @param preferences ChatPreferences object with new settings
     */
    suspend fun updateChatPreferences(preferences: ChatPreferences) {
        Log.d(TAG, "Updating chat preferences: $preferences")
        dataStore.edit { dataStorePreferences ->
            dataStorePreferences[KEY_SEND_READ_RECEIPTS] = preferences.sendReadReceipts
            dataStorePreferences[KEY_SHOW_TYPING_INDICATORS] = preferences.showTypingIndicators
        }
    }
    
    /**
     * Reset all preferences to default values.
     */
    suspend fun resetToDefaults() {
        Log.d(TAG, "Resetting preferences to defaults")
        dataStore.edit { preferences ->
            preferences[KEY_SEND_READ_RECEIPTS] = DEFAULT_SEND_READ_RECEIPTS
            preferences[KEY_SHOW_TYPING_INDICATORS] = DEFAULT_SHOW_TYPING_INDICATORS
        }
    }
}

/**
 * Data class representing chat preferences.
 */
data class ChatPreferences(
    val sendReadReceipts: Boolean = true,
    val showTypingIndicators: Boolean = true
)
