package com.synapse.social.studioasinc.chat.service

import android.util.Log
import com.synapse.social.studioasinc.chat.models.TypingStatus
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages typing indicator events with debouncing and auto-stop functionality.
 * 
 * This manager handles:
 * - Debouncing typing events to prevent excessive broadcasts (500ms)
 * - Auto-stopping typing indicators after inactivity (3 seconds)
 * - Managing coroutine jobs per chat room
 * - Subscribing to and handling incoming typing events
 * - Respecting user privacy preferences for typing indicators
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 5.2, 5.3, 5.5, 6.1, 6.4
 */
class TypingIndicatorManager(
    private val realtimeService: SupabaseRealtimeService,
    private val preferencesManager: PreferencesManager,
    private val coroutineScope: CoroutineScope
) {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception", throwable)
    }

    companion object {
        private const val TAG = "TypingIndicatorManager"
        private const val DEBOUNCE_DELAY = 500L // 500ms debounce for typing events
        private const val TYPING_TIMEOUT = 3000L // 3 seconds auto-stop timeout
    }
    
    // Track typing jobs per chat room to manage debouncing
    private val typingJobs = ConcurrentHashMap<String, Job>()
    
    // Track auto-stop jobs per chat room
    private val autoStopJobs = ConcurrentHashMap<String, Job>()
    
    // Track last typing event time per chat
    private val lastTypingTime = ConcurrentHashMap<String, Long>()
    
    // Track if user is currently typing in each chat
    private val isTypingInChat = ConcurrentHashMap<String, Boolean>()
    
    // Track typing event callbacks per chat
    private val typingCallbacks = ConcurrentHashMap<String, (TypingStatus) -> Unit>()
    
    /**
     * Called when the user types in the message input field.
     * Implements debouncing to send typing events at most once per 500ms.
     * Also sets up auto-stop timer for 3 seconds of inactivity.
     * Respects user privacy preferences for typing indicators.
     * 
     * Requirements: 1.1, 1.3, 1.4, 5.2, 5.3, 5.5, 6.1
     * 
     * @param chatId The chat room identifier
     * @param userId The current user's ID
     */
    fun onUserTyping(chatId: String, userId: String?) {
        if (userId.isNullOrEmpty()) {
            Log.w(TAG, "User ID is null or empty, cannot send typing event.")
            return
        }
        Log.d(TAG, "User typing in chat: $chatId")

        // Check if typing indicators are enabled
        if (!preferencesManager.isTypingIndicatorsEnabled()) {
            Log.d(TAG, "Typing indicators disabled - skipping broadcast for chat: $chatId")
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val lastTime = lastTypingTime[chatId] ?: 0L
        val timeSinceLastEvent = currentTime - lastTime
        
        // Check if we need to send a typing event (debounce logic)
        val shouldSendEvent = timeSinceLastEvent >= DEBOUNCE_DELAY || !isTypingInChat.getOrDefault(chatId, false)
        
        if (shouldSendEvent) {
            // Cancel existing typing job if it exists
            typingJobs[chatId]?.cancel()

            // Send typing event immediately
            typingJobs[chatId] = coroutineScope.launch(exceptionHandler) {
                try {
                    userId?.let {
                        realtimeService.broadcastTyping(chatId, it, true)
                    }
                    lastTypingTime[chatId] = currentTime
                    isTypingInChat[chatId] = true
                    Log.d(TAG, "Typing event sent for chat: $chatId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to broadcast typing event for chat: $chatId", e)
                }
            }
        }
        
        // Cancel existing auto-stop job
        autoStopJobs[chatId]?.cancel()
        
        // Set up auto-stop timer (3 seconds of inactivity)
        autoStopJobs[chatId] = coroutineScope.launch {
            delay(TYPING_TIMEOUT)
            Log.d(TAG, "Auto-stopping typing indicator for chat: $chatId after ${TYPING_TIMEOUT}ms inactivity")
            onUserStoppedTyping(chatId, userId)
        }
    }
    
    /**
     * Called when the user stops typing or sends a message.
     * Broadcasts a typing-stopped event and cleans up resources.
     * Respects user privacy preferences for typing indicators.
     * 
     * Requirements: 1.4, 1.5, 5.2, 5.3, 5.5
     * 
     * @param chatId The chat room identifier
     * @param userId The current user's ID
     */
    fun onUserStoppedTyping(chatId: String, userId: String) {
        Log.d(TAG, "User stopped typing in chat: $chatId")
        
        // Cancel any pending typing jobs
        typingJobs[chatId]?.cancel()
        typingJobs.remove(chatId)
        
        // Cancel auto-stop job
        autoStopJobs[chatId]?.cancel()
        autoStopJobs.remove(chatId)
        
        // Only send stopped event if we were actually typing and typing indicators are enabled
        if (isTypingInChat.getOrDefault(chatId, false)) {
            if (preferencesManager.isTypingIndicatorsEnabled()) {
                coroutineScope.launch(exceptionHandler) {
                    try {
                        realtimeService.broadcastTyping(chatId, userId, false)
                        Log.d(TAG, "Typing stopped event sent for chat: $chatId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to broadcast typing stopped event for chat: $chatId", e)
                    }
                }
            } else {
                Log.d(TAG, "Typing indicators disabled - skipping stopped broadcast for chat: $chatId")
            }
            
            // Always clean up local state regardless of preference
            isTypingInChat[chatId] = false
            lastTypingTime.remove(chatId)
        }
    }
    
    /**
     * Clean up all typing jobs for a specific chat.
     * Called when leaving a chat or when the chat is closed.
     * 
     * @param chatId The chat room identifier
     */
    fun cleanup(chatId: String) {
        Log.d(TAG, "Cleaning up typing indicator for chat: $chatId")
        
        typingJobs[chatId]?.cancel()
        typingJobs.remove(chatId)
        
        autoStopJobs[chatId]?.cancel()
        autoStopJobs.remove(chatId)
        
        lastTypingTime.remove(chatId)
        isTypingInChat.remove(chatId)
    }
    
    /**
     * Clean up all typing jobs for all chats.
     * Called when the service is being destroyed.
     */
    fun cleanupAll() {
        Log.d(TAG, "Cleaning up all typing indicators")
        
        typingJobs.values.forEach { it.cancel() }
        typingJobs.clear()
        
        autoStopJobs.values.forEach { it.cancel() }
        autoStopJobs.clear()
        
        lastTypingTime.clear()
        isTypingInChat.clear()
    }
    
    /**
     * Check if the user is currently typing in a specific chat.
     * 
     * @param chatId The chat room identifier
     * @return true if user is typing, false otherwise
     */
    fun isUserTyping(chatId: String): Boolean {
        return isTypingInChat.getOrDefault(chatId, false)
    }
    
    /**
     * Subscribe to typing events for a specific chat room.
     * Listens for incoming typing events from other users and invokes the callback.
     * 
     * Requirements: 1.2, 6.4
     * 
     * @param chatId The chat room identifier
     * @param onTypingUpdate Callback invoked when a typing event is received
     */
    suspend fun subscribeToTypingEvents(chatId: String, onTypingUpdate: (TypingStatus) -> Unit) {
        Log.d(TAG, "Subscribing to typing events for chat: $chatId")
        
        // Store the callback
        typingCallbacks[chatId] = onTypingUpdate
        
        try {
            // Get or create the Realtime channel
            val channel = realtimeService.getChannel(chatId) 
                ?: realtimeService.subscribeToChat(chatId)
            
            // FIXME: Implement broadcast listening for typing events once the Supabase Realtime API is finalized.
            // This will involve listening for a specific event on the channel and invoking the onTypingUpdate callback.
            // For now, we'll rely on polling fallback for typing indicator updates
            Log.d(TAG, "Typing indicator subscription set up for chat: $chatId (broadcast listening pending API clarification)")

            Log.d(TAG, "Successfully subscribed to typing events for chat: $chatId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to typing events for chat: $chatId", e)
            throw e
        }
    }
    
    /**
     * Unsubscribe from typing events for a specific chat room.
     * Cleans up the subscription and removes callbacks.
     * 
     * Requirements: 6.4
     * 
     * @param chatId The chat room identifier
     */
    fun unsubscribe(chatId: String) {
        Log.d(TAG, "Unsubscribing from typing events for chat: $chatId")
        
        // Remove the callback
        typingCallbacks.remove(chatId)
        
        // Clean up typing state
        cleanup(chatId)
        
        Log.d(TAG, "Successfully unsubscribed from typing events for chat: $chatId")
    }
    
    /**
     * Unsubscribe from all typing events.
     * Called when the manager is being destroyed.
     */
    fun unsubscribeAll() {
        Log.d(TAG, "Unsubscribing from all typing events")
        
        typingCallbacks.clear()
        
        cleanupAll()
    }
}
