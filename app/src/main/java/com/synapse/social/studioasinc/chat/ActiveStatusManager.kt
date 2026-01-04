package com.synapse.social.studioasinc.chat

import android.os.Handler
import android.os.Looper
import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager for handling user active status and presence tracking
 */
class ActiveStatusManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: ActiveStatusManager? = null
        
        fun getInstance(): ActiveStatusManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ActiveStatusManager().also { INSTANCE = it }
            }
        }
        
        private const val PRESENCE_UPDATE_INTERVAL = 30000L // 30 seconds
        private const val OFFLINE_THRESHOLD = 60000L // 1 minute
    }
    
    private val dbService = SupabaseDatabaseService()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    
    // Cache for user presence data
    private val presenceCache = ConcurrentHashMap<String, UserPresence>()
    private val presenceListeners = ConcurrentHashMap<String, MutableSet<PresenceListener>>()
    
    // Background jobs
    private var heartbeatJob: Job? = null
    private var monitoringJob: Job? = null
    
    data class UserPresence(
        val userId: String,
        val isOnline: Boolean,
        val lastSeen: Long,
        val activityStatus: String,
        val currentChatId: String? = null
    )
    
    interface PresenceListener {
        fun onPresenceChanged(userId: String, presence: UserPresence)
    }
    
    /**
     * Set user online status
     */
    fun setOnline(userId: String) {
        scope.launch {
            try {
                val updateData = mapOf(
                    "is_online" to true,
                    "last_seen" to System.currentTimeMillis(),
                    "activity_status" to "online",
                    "updated_at" to System.currentTimeMillis()
                )
                
                dbService.upsert("user_presence", updateData.plus("user_id" to userId))
                
                // Update cache
                val presence = UserPresence(
                    userId = userId,
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    activityStatus = "online"
                )
                presenceCache[userId] = presence
                notifyPresenceChanged(userId, presence)
                
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    /**
     * Set user offline status
     */
    fun setOffline(userId: String) {
        scope.launch {
            try {
                val updateData = mapOf(
                    "is_online" to false,
                    "last_seen" to System.currentTimeMillis(),
                    "activity_status" to "offline",
                    "current_chat_id" to null,
                    "updated_at" to System.currentTimeMillis()
                )
                
                dbService.upsert("user_presence", updateData.plus("user_id" to userId))
                
                // Update cache
                val presence = UserPresence(
                    userId = userId,
                    isOnline = false,
                    lastSeen = System.currentTimeMillis(),
                    activityStatus = "offline"
                )
                presenceCache[userId] = presence
                notifyPresenceChanged(userId, presence)
                
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    /**
     * Set user activity status (e.g., "chatting", "typing", "away")
     */
    fun setActivityStatus(userId: String, status: String, chatId: String? = null) {
        scope.launch {
            try {
                val updateData = mutableMapOf(
                    "activity_status" to status,
                    "last_seen" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis()
                )
                
                if (chatId != null) {
                    updateData["current_chat_id"] = chatId
                }
                
                dbService.upsert("user_presence", updateData.plus("user_id" to userId))
                
                // Update cache
                val currentPresence = presenceCache[userId]
                val presence = UserPresence(
                    userId = userId,
                    isOnline = currentPresence?.isOnline ?: true,
                    lastSeen = System.currentTimeMillis(),
                    activityStatus = status,
                    currentChatId = chatId
                )
                presenceCache[userId] = presence
                notifyPresenceChanged(userId, presence)
                
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    /**
     * Get user presence
     */
    suspend fun getUserPresence(userId: String): UserPresence? {
        // Check cache first
        presenceCache[userId]?.let { return it }
        
        return try {
            val result = dbService.getSingle("user_presence", "user_id", userId).getOrNull()
            result?.let {
                val presence = UserPresence(
                    userId = userId,
                    isOnline = it["is_online"] as? Boolean ?: false,
                    lastSeen = (it["last_seen"] as? Number)?.toLong() ?: 0L,
                    activityStatus = it["activity_status"] as? String ?: "offline",
                    currentChatId = it["current_chat_id"] as? String
                )
                presenceCache[userId] = presence
                presence
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get multiple user presences
     */
    suspend fun getMultiplePresences(userIds: List<String>): Map<String, UserPresence> {
        val result = mutableMapOf<String, UserPresence>()
        
        // Get from cache first
        userIds.forEach { userId ->
            presenceCache[userId]?.let { result[userId] = it }
        }
        
        // Fetch missing ones from database
        val missingIds = userIds.filter { !result.containsKey(it) }
        if (missingIds.isNotEmpty()) {
            try {
                // Note: This would need to be implemented in SupabaseDatabaseService
                // For now, fetch individually
                missingIds.forEach { userId ->
                    getUserPresence(userId)?.let { result[userId] = it }
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
        
        return result
    }
    
    /**
     * Check if user is currently online
     */
    suspend fun isUserOnline(userId: String): Boolean {
        val presence = getUserPresence(userId)
        return presence?.isOnline == true && 
               (System.currentTimeMillis() - presence.lastSeen) < OFFLINE_THRESHOLD
    }
    
    /**
     * Check if user is in a specific chat
     */
    suspend fun isUserInChat(userId: String, chatId: String): Boolean {
        val presence = getUserPresence(userId)
        return presence?.currentChatId == chatId && presence.isOnline
    }
    
    /**
     * Start heartbeat to maintain online status
     */
    fun startHeartbeat(userId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    setOnline(userId)
                    delay(PRESENCE_UPDATE_INTERVAL)
                } catch (e: Exception) {
                    delay(PRESENCE_UPDATE_INTERVAL * 2) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Stop heartbeat
     */
    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
    
    /**
     * Start monitoring presence changes for specific users
     */
    fun startMonitoring(userIds: List<String>) {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    val presences = getMultiplePresences(userIds)
                    presences.forEach { (userId, presence) ->
                        notifyPresenceChanged(userId, presence)
                    }
                    delay(10000) // Check every 10 seconds
                } catch (e: Exception) {
                    delay(15000) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Add presence listener
     */
    fun addPresenceListener(userId: String, listener: PresenceListener) {
        presenceListeners.getOrPut(userId) { mutableSetOf() }.add(listener)
    }
    
    /**
     * Remove presence listener
     */
    fun removePresenceListener(userId: String, listener: PresenceListener) {
        presenceListeners[userId]?.remove(listener)
        if (presenceListeners[userId]?.isEmpty() == true) {
            presenceListeners.remove(userId)
        }
    }
    
    /**
     * Notify listeners of presence changes
     */
    private fun notifyPresenceChanged(userId: String, presence: UserPresence) {
        presenceListeners[userId]?.forEach { listener ->
            handler.post {
                listener.onPresenceChanged(userId, presence)
            }
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup(userId: String) {
        setOffline(userId)
        stopHeartbeat()
        stopMonitoring()
        presenceCache.remove(userId)
        presenceListeners.remove(userId)
    }
    
    /**
     * Clean up all resources
     */
    fun cleanupAll() {
        stopHeartbeat()
        stopMonitoring()
        presenceCache.clear()
        presenceListeners.clear()
        scope.cancel()
    }
}
