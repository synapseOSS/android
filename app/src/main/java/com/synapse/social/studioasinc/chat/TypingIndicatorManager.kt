package com.synapse.social.studioasinc.chat

import android.os.Handler
import android.os.Looper
import com.synapse.social.studioasinc.backend.SupabaseChatService
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced manager for handling typing indicators with debouncing and lifecycle management
 */
class TypingIndicatorManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: TypingIndicatorManager? = null
        
        fun getInstance(): TypingIndicatorManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TypingIndicatorManager().also { INSTANCE = it }
            }
        }
        
        private const val TYPING_TIMEOUT = 3000L // 3 seconds
        private const val DEBOUNCE_DELAY = 500L // 500ms
    }
    
    private val chatService = SupabaseChatService()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    
    // Track typing states per chat
    private val typingStates = ConcurrentHashMap<String, Boolean>()
    private val typingTimeouts = ConcurrentHashMap<String, Runnable>()
    private val debounceJobs = ConcurrentHashMap<String, Job>()
    
    // Listeners for typing status changes
    private val typingListeners = ConcurrentHashMap<String, MutableSet<TypingListener>>()
    
    interface TypingListener {
        fun onTypingUsersChanged(chatId: String, typingUsers: List<String>)
    }
    
    /**
     * Start typing in a chat
     */
    fun startTyping(chatId: String, userId: String) {
        val key = "$chatId:$userId"
        
        // Cancel existing debounce job
        debounceJobs[key]?.cancel()
        
        // Create new debounce job
        debounceJobs[key] = scope.launch {
            delay(DEBOUNCE_DELAY)
            
            if (typingStates[key] != true) {
                typingStates[key] = true
                chatService.updateTypingStatus(chatId, userId, true)
            }
            
            // Set timeout to auto-stop typing
            val timeoutRunnable = Runnable {
                stopTyping(chatId, userId)
            }
            
            // Cancel existing timeout
            typingTimeouts[key]?.let { handler.removeCallbacks(it) }
            
            // Set new timeout
            typingTimeouts[key] = timeoutRunnable
            handler.postDelayed(timeoutRunnable, TYPING_TIMEOUT)
        }
    }
    
    /**
     * Stop typing in a chat
     */
    fun stopTyping(chatId: String, userId: String) {
        val key = "$chatId:$userId"
        
        // Cancel debounce job
        debounceJobs[key]?.cancel()
        debounceJobs.remove(key)
        
        // Cancel timeout
        typingTimeouts[key]?.let { handler.removeCallbacks(it) }
        typingTimeouts.remove(key)
        
        // Update state if was typing
        if (typingStates[key] == true) {
            typingStates[key] = false
            scope.launch {
                chatService.updateTypingStatus(chatId, userId, false)
            }
        }
    }
    
    /**
     * Get current typing users for a chat
     */
    suspend fun getTypingUsers(chatId: String, excludeUserId: String): List<String> {
        return chatService.getTypingUsers(chatId, excludeUserId).getOrElse { emptyList() }
    }
    
    /**
     * Add listener for typing status changes
     */
    fun addTypingListener(chatId: String, listener: TypingListener) {
        typingListeners.getOrPut(chatId) { mutableSetOf() }.add(listener)
    }
    
    /**
     * Remove listener for typing status changes
     */
    fun removeTypingListener(chatId: String, listener: TypingListener) {
        typingListeners[chatId]?.remove(listener)
        if (typingListeners[chatId]?.isEmpty() == true) {
            typingListeners.remove(chatId)
        }
    }
    
    /**
     * Notify listeners of typing changes
     */
    private fun notifyTypingChanged(chatId: String, typingUsers: List<String>) {
        typingListeners[chatId]?.forEach { listener ->
            handler.post {
                listener.onTypingUsersChanged(chatId, typingUsers)
            }
        }
    }
    
    /**
     * Start monitoring typing status for a chat
     */
    fun startMonitoring(chatId: String, currentUserId: String) {
        scope.launch {
            while (typingListeners.containsKey(chatId)) {
                try {
                    val typingUsers = getTypingUsers(chatId, currentUserId)
                    notifyTypingChanged(chatId, typingUsers)
                    delay(1000) // Check every second
                } catch (e: Exception) {
                    // Handle error silently
                    delay(2000) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Stop monitoring typing status for a chat
     */
    fun stopMonitoring(chatId: String) {
        typingListeners.remove(chatId)
    }
    
    /**
     * Clean up resources for a specific chat
     */
    fun cleanup(chatId: String, userId: String) {
        stopTyping(chatId, userId)
        stopMonitoring(chatId)
    }
    
    /**
     * Clean up all resources
     */
    fun cleanupAll() {
        // Cancel all debounce jobs
        debounceJobs.values.forEach { it.cancel() }
        debounceJobs.clear()
        
        // Cancel all timeouts
        typingTimeouts.values.forEach { handler.removeCallbacks(it) }
        typingTimeouts.clear()
        
        // Clear states
        typingStates.clear()
        typingListeners.clear()
        
        // Cancel scope
        scope.cancel()
    }
}
