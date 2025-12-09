package com.synapse.social.studioasinc.chat.service

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.chat.models.ReadReceiptEvent
import com.synapse.social.studioasinc.chat.models.TypingStatus
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing Supabase Realtime WebSocket connections and events.
 * Handles channel lifecycle, typing indicators, read receipts, and connection state.
 */
class SupabaseRealtimeService {
    
    companion object {
        private const val TAG = "SupabaseRealtimeService"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val INITIAL_RECONNECT_DELAY = 2000L
        private const val POLLING_INTERVAL = 5000L
        private const val CONNECTION_TIMEOUT = 10000L
    }
    
    // Performance metrics tracking
    private val metrics = RealtimeMetrics()
    
    // Thread-safe channel map for concurrent access
    private val channels = ConcurrentHashMap<String, RealtimeChannel>()
    
    // Connection state management per chat
    private val _connectionState = MutableStateFlow<RealtimeState>(RealtimeState.Disconnected)
    val connectionState: StateFlow<RealtimeState> = _connectionState.asStateFlow()
    
    private val chatReconnectAttempts = ConcurrentHashMap<String, Int>()
    private val chatPollingFallback = ConcurrentHashMap<String, Boolean>()
    
    // Callbacks for connection status updates
    private val connectionCallbacks = mutableListOf<(RealtimeState) -> Unit>()
    
    // Track last successful connection time
    private var lastSuccessfulConnection = 0L
    
    // Event queuing for graceful degradation
    private val queuedTypingEvents = ConcurrentHashMap<String, MutableList<TypingStatus>>()
    private val queuedReadReceiptEvents = ConcurrentHashMap<String, MutableList<ReadReceiptEvent>>()
    
    // Polling fallback jobs
    private val pollingJobs = ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    
    // Backend service for polling fallback
    private val chatService = com.synapse.social.studioasinc.backend.SupabaseChatService()
    
    /**
     * Subscribe to a chat room's Realtime channel.
     * Creates and manages a channel for the specified chat ID.
     * 
     * @param chatId The unique identifier for the chat room
     * @return The created or existing RealtimeChannel
     */
    suspend fun subscribeToChat(chatId: String): RealtimeChannel {
        Log.d(TAG, "Subscribing to chat: $chatId")
        
        // Return existing channel if already subscribed
        channels[chatId]?.let {
            Log.d(TAG, "Reusing existing channel for chat: $chatId")
            return it
        }
        
        return try {
            updateConnectionState(RealtimeState.Connecting)
            
            val channelName = "chat:$chatId"
            val channel = SupabaseClient.client.realtime.channel(channelName)
            
            // Subscribe to the channel with timeout handling
            channel.subscribe()
            
            // Store the channel
            channels[chatId] = channel
            
            // Reset reconnection attempts on success
            chatReconnectAttempts[chatId] = 0
            chatPollingFallback[chatId] = false
            lastSuccessfulConnection = System.currentTimeMillis()
            
            // Record successful connection
            metrics.recordConnectionStart()
            
            updateConnectionState(RealtimeState.Connected)
            
            Log.d(TAG, "Successfully subscribed to chat: $chatId")
            channel
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to chat: $chatId", e)
            handleSubscriptionError(chatId, e)
            throw e
        }
    }
    
    /**
     * Broadcast a typing event to the chat room.
     * Falls back to queuing if WebSocket is unavailable.
     * 
     * @param chatId The chat room identifier
     * @param userId The user who is typing
     * @param isTyping Whether the user is currently typing
     */
    suspend fun broadcastTyping(chatId: String, userId: String, isTyping: Boolean) {
        Log.d(TAG, "Broadcasting typing event - chatId: $chatId, userId: $userId, isTyping: $isTyping")
        
        val typingStatus = TypingStatus(
            userId = userId,
            chatId = chatId,
            isTyping = isTyping,
            timestamp = System.currentTimeMillis()
        )
        
        // Check if using polling fallback
        if (chatPollingFallback.getOrDefault(chatId, false)) {
            Log.d(TAG, "Using polling fallback - queuing typing event")
            queueTypingEvent(chatId, typingStatus)
            return
        }
        
        val channel = channels[chatId]
        if (channel == null) {
            Log.w(TAG, "No channel found for chatId: $chatId. Subscribing first.")
            try {
                subscribeToChat(chatId)
                return broadcastTyping(chatId, userId, isTyping)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe, queuing typing event", e)
                queueTypingEvent(chatId, typingStatus)
                return
            }
        }
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Broadcast the typing event using the correct API
            channel.broadcast(
                event = "typing",
                message = buildJsonObject {
                    put("user_id", userId)
                    put("chat_id", chatId)
                    put("is_typing", isTyping)
                    put("timestamp", typingStatus.timestamp)
                }
            )
            
            // Record successful typing event with latency
            val latency = System.currentTimeMillis() - startTime
            metrics.recordTypingEventSent(latency)
            
            Log.d(TAG, "Typing event broadcasted successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast typing event, queuing for later", e)
            metrics.recordFailedEvent("typing")
            queueTypingEvent(chatId, typingStatus)
            handleBroadcastError(chatId, e)
        }
    }
    
    /**
     * Broadcast a read receipt event to the chat room.
     * Falls back to queuing if WebSocket is unavailable.
     * 
     * @param chatId The chat room identifier
     * @param userId The user who read the messages
     * @param messageIds List of message IDs that were read
     */
    suspend fun broadcastReadReceipt(chatId: String, userId: String, messageIds: List<String>) {
        Log.d(TAG, "Broadcasting read receipt - chatId: $chatId, userId: $userId, messageCount: ${messageIds.size}")
        
        val readReceiptEvent = ReadReceiptEvent(
            chatId = chatId,
            userId = userId,
            messageIds = messageIds,
            timestamp = System.currentTimeMillis()
        )
        
        // Check if using polling fallback
        if (chatPollingFallback.getOrDefault(chatId, false)) {
            Log.d(TAG, "Using polling fallback - queuing read receipt event")
            queueReadReceiptEvent(chatId, readReceiptEvent)
            return
        }
        
        val channel = channels[chatId]
        if (channel == null) {
            Log.w(TAG, "No channel found for chatId: $chatId. Subscribing first.")
            try {
                subscribeToChat(chatId)
                return broadcastReadReceipt(chatId, userId, messageIds)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe, queuing read receipt event", e)
                queueReadReceiptEvent(chatId, readReceiptEvent)
                return
            }
        }
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Broadcast the read receipt event using the correct API
            channel.broadcast(
                event = "read_receipt",
                message = buildJsonObject {
                    put("chat_id", chatId)
                    put("user_id", userId)
                    put("message_ids", Json.encodeToJsonElement(ListSerializer(String.serializer()), messageIds))
                    put("timestamp", readReceiptEvent.timestamp)
                }
            )
            
            // Record successful read receipt with latency
            val latency = System.currentTimeMillis() - startTime
            metrics.recordReadReceiptSent(messageIds.size, latency)
            
            Log.d(TAG, "Read receipt broadcasted successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast read receipt, queuing for later", e)
            metrics.recordFailedEvent("read_receipt")
            queueReadReceiptEvent(chatId, readReceiptEvent)
            handleBroadcastError(chatId, e)
        }
    }
    
    /**
     * Unsubscribe from a chat room's Realtime channel and clean up resources.
     * 
     * @param chatId The chat room identifier
     */
    suspend fun unsubscribeFromChat(chatId: String) {
        Log.d(TAG, "Unsubscribing from chat: $chatId")
        
        // Stop polling fallback if active
        stopPollingFallback(chatId)
        
        // Clear queued events
        clearQueuedEvents(chatId)
        
        val channel = channels.remove(chatId)
        if (channel != null) {
            try {
                SupabaseClient.client.realtime.removeChannel(channel)
                Log.d(TAG, "Successfully unsubscribed from chat: $chatId")
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing from chat: $chatId", e)
            }
        } else {
            Log.w(TAG, "No channel found for chatId: $chatId")
        }
        
        // Update connection state if no channels remain
        if (channels.isEmpty()) {
            metrics.recordConnectionEnd()
            _connectionState.value = RealtimeState.Disconnected
            notifyConnectionCallbacks(RealtimeState.Disconnected)
        }
    }
    
    /**
     * Clean up all channels and resources. This method should be called when the service is
     * no longer needed, such as in the ViewModel's onCleared() method, to prevent memory
     * and resource leaks from active WebSocket connections and polling jobs.
     */
    suspend fun cleanup() {
        Log.d(TAG, "Cleaning up all channels")
        
        // Stop all polling jobs
        pollingJobs.values.forEach { job ->
            job.cancel()
        }
        pollingJobs.clear()
        
        // Unsubscribe from all channels
        channels.keys.toList().forEach { chatId ->
            unsubscribeFromChat(chatId)
        }
        
        // Clear all data structures
        channels.clear()
        connectionCallbacks.clear()
        chatReconnectAttempts.clear()
        chatPollingFallback.clear()
        queuedTypingEvents.clear()
        queuedReadReceiptEvents.clear()
        
        _connectionState.value = RealtimeState.Disconnected
    }
    
    /**
     * Register a callback for connection state changes.
     * 
     * @param callback Function to be called when connection state changes
     */
    fun addConnectionCallback(callback: (RealtimeState) -> Unit) {
        connectionCallbacks.add(callback)
    }
    
    /**
     * Remove a connection state callback.
     * 
     * @param callback The callback to remove
     */
    fun removeConnectionCallback(callback: (RealtimeState) -> Unit) {
        connectionCallbacks.remove(callback)
    }
    
    /**
     * Get the current channel for a chat room.
     * 
     * @param chatId The chat room identifier
     * @return The RealtimeChannel if it exists, null otherwise
     */
    fun getChannel(chatId: String): RealtimeChannel? {
        return channels[chatId]
    }
    
    /**
     * Check if a chat room has an active channel subscription.
     * 
     * @param chatId The chat room identifier
     * @return true if subscribed, false otherwise
     */
    fun isSubscribed(chatId: String): Boolean {
        return channels.containsKey(chatId)
    }
    
    // Private helper methods
    
    private fun notifyConnectionCallbacks(state: RealtimeState) {
        connectionCallbacks.forEach { callback ->
            try {
                callback(state)
            } catch (e: Exception) {
                Log.e(TAG, "Error in connection callback", e)
            }
        }
    }
    
    private suspend fun handleReconnection(chatId: String) {
        val attempts = chatReconnectAttempts.getOrDefault(chatId, 0)
        
        if (attempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnection attempts reached for chat: $chatId. Falling back to polling.")
            enablePollingFallback(chatId)
            return
        }
        
        chatReconnectAttempts[chatId] = attempts + 1
        
        // Exponential backoff: 2s, 4s, 8s, 16s, 32s
        val delayMs = INITIAL_RECONNECT_DELAY * (1 shl attempts)
        
        Log.d(TAG, "Attempting reconnection ${attempts + 1}/$MAX_RECONNECT_ATTEMPTS for chat: $chatId in ${delayMs}ms")
        updateConnectionState(RealtimeState.Connecting)
        
        delay(delayMs)
        
        try {
            // Remove failed channel before retrying
            channels.remove(chatId)?.let { oldChannel ->
                try {
                    SupabaseClient.client.realtime.removeChannel(oldChannel)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing old channel during reconnection", e)
                }
            }
            
            subscribeToChat(chatId)
            metrics.recordReconnection(successful = true)
            Log.i(TAG, "Reconnection successful for chat: $chatId")
            
        } catch (e: Exception) {
            metrics.recordReconnection(successful = false)
            Log.e(TAG, "Reconnection attempt ${attempts + 1} failed for chat: $chatId", e)
            handleReconnection(chatId)
        }
    }
    
    private fun handleSubscriptionError(chatId: String, error: Exception) {
        Log.e(TAG, "Subscription error for chat: $chatId", error)
        
        val errorMessage = when {
            error.message?.contains("timeout", ignoreCase = true) == true -> "Connection timeout"
            error.message?.contains("network", ignoreCase = true) == true -> "Network error"
            error.message?.contains("unauthorized", ignoreCase = true) == true -> "Authentication error"
            else -> error.message ?: "Unknown error"
        }
        
        updateConnectionState(RealtimeState.Error(errorMessage))
    }
    
    private fun handleBroadcastError(chatId: String, error: Exception) {
        Log.e(TAG, "Broadcast error for chat: $chatId", error)
        
        val errorMessage = when {
            error.message?.contains("not subscribed", ignoreCase = true) == true -> {
                "Channel not subscribed"
            }
            error.message?.contains("timeout", ignoreCase = true) == true -> {
                "Broadcast timeout"
            }
            else -> error.message ?: "Broadcast failed"
        }
        
        updateConnectionState(RealtimeState.Error(errorMessage))
        
        // If channel is not subscribed, attempt to resubscribe
        if (!isSubscribed(chatId)) {
            Log.w(TAG, "Channel not subscribed, will attempt reconnection on next broadcast")
        }
    }
    
    private suspend fun enablePollingFallback(chatId: String) {
        chatPollingFallback[chatId] = true
        metrics.recordPollingFallbackActivation()
        updateConnectionState(RealtimeState.Error("Using polling fallback"))
        Log.w(TAG, "Polling fallback enabled for chat: $chatId. Real-time features will poll every ${POLLING_INTERVAL}ms")
        
        // Start polling fallback
        startPollingFallback(chatId)
    }
    
    private fun updateConnectionState(newState: RealtimeState) {
        if (_connectionState.value != newState) {
            _connectionState.value = newState
            notifyConnectionCallbacks(newState)
        }
    }
    
    /**
     * Check if the service is using polling fallback instead of WebSocket for a specific chat.
     * 
     * @param chatId The chat room identifier
     * @return true if using polling, false if using WebSocket
     */
    fun isUsingPollingFallback(chatId: String): Boolean {
        return chatPollingFallback.getOrDefault(chatId, false)
    }
    
    /**
     * Check if any chat is using polling fallback.
     * 
     * @return true if any chat is using polling, false otherwise
     */
    fun isAnyUsingPollingFallback(): Boolean {
        return chatPollingFallback.values.any { it }
    }
    
    /**
     * Get the polling interval in milliseconds.
     * 
     * @return The polling interval
     */
    fun getPollingInterval(): Long {
        return POLLING_INTERVAL
    }
    
    /**
     * Manually trigger a reconnection attempt for a specific chat.
     * Useful for user-initiated retry actions.
     * 
     * @param chatId The chat room identifier
     */
    suspend fun reconnect(chatId: String) {
        Log.d(TAG, "Manual reconnection triggered for chat: $chatId")
        
        // Stop polling fallback if active
        stopPollingFallback(chatId)
        
        // Reset reconnection attempts to allow retry
        chatReconnectAttempts[chatId] = 0
        chatPollingFallback[chatId] = false
        
        // Remove existing channel
        unsubscribeFromChat(chatId)
        
        // Attempt to subscribe again
        try {
            subscribeToChat(chatId)
            
            // Send queued events when connection is restored
            sendQueuedEvents(chatId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Manual reconnection failed for chat: $chatId", e)
            // Re-enable polling fallback if reconnection fails
            enablePollingFallback(chatId)
            throw e
        }
    }
    
    /**
     * Reconnect all active channels.
     * Useful for recovering from network changes or app resume.
     */
    suspend fun reconnectAll() {
        Log.d(TAG, "Reconnecting all channels")
        
        val chatIds = channels.keys.toList()
        chatIds.forEach { chatId ->
            try {
                reconnect(chatId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reconnect chat: $chatId", e)
            }
        }
    }
    
    /**
     * Get the time since last successful connection in milliseconds.
     * 
     * @return Time in milliseconds, or -1 if never connected
     */
    fun getTimeSinceLastConnection(): Long {
        return if (lastSuccessfulConnection > 0) {
            System.currentTimeMillis() - lastSuccessfulConnection
        } else {
            -1
        }
    }
    
    /**
     * Check if the connection is healthy based on recent activity.
     * 
     * @return true if connection is healthy, false otherwise
     */
    fun isConnectionHealthy(): Boolean {
        val timeSinceConnection = getTimeSinceLastConnection()
        return _connectionState.value is RealtimeState.Connected && 
               timeSinceConnection >= 0 && 
               timeSinceConnection < CONNECTION_TIMEOUT
    }
    
    /**
     * Get reconnection attempts for a specific chat.
     * 
     * @param chatId The chat room identifier
     * @return Number of reconnection attempts
     */
    fun getReconnectionAttempts(chatId: String): Int {
        return chatReconnectAttempts.getOrDefault(chatId, 0)
    }
    
    /**
     * Get current performance metrics.
     * 
     * @return Current metrics snapshot
     */
    fun getMetrics(): MetricsSnapshot {
        return metrics.getCurrentMetrics()
    }
    
    /**
     * Get metrics as StateFlow for reactive updates.
     * 
     * @return StateFlow of metrics snapshots
     */
    fun getMetricsFlow(): StateFlow<MetricsSnapshot> {
        return metrics.metricsState
    }
    
    /**
     * Log current performance metrics.
     * Useful for debugging and monitoring.
     */
    fun logMetrics() {
        metrics.logMetrics()
    }
    
    /**
     * Reset all performance metrics.
     * Useful for testing or periodic resets.
     */
    fun resetMetrics() {
        metrics.reset()
    }

    // Graceful degradation methods

    /**
     * Start polling fallback for a specific chat when WebSocket fails.
     * Polls every 5 seconds for typing indicators and read receipts.
     * 
     * Requirements: 6.2
     * 
     * @param chatId The chat room identifier
     */
    private suspend fun startPollingFallback(chatId: String) {
        // Cancel existing polling job if any
        pollingJobs[chatId]?.cancel()
        
        val pollingJob = CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Starting polling fallback for chat: $chatId")
            
            while (chatPollingFallback.getOrDefault(chatId, false)) {
                try {
                    // Poll for typing indicators
                    pollTypingIndicators(chatId)
                    
                    // Poll for read receipts
                    pollReadReceipts(chatId)
                    
                    // Send queued events
                    sendQueuedEvents(chatId)
                    
                    // Wait for next poll
                    delay(POLLING_INTERVAL)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error during polling for chat: $chatId", e)
                    delay(POLLING_INTERVAL)
                }
            }
            
            Log.i(TAG, "Polling fallback stopped for chat: $chatId")
        }
        
        pollingJobs[chatId] = pollingJob
    }

    /**
     * Poll for typing indicators using REST API.
     * 
     * @param chatId The chat room identifier
     */
    private suspend fun pollTypingIndicators(chatId: String) {
        try {
            // This would typically call a REST endpoint to get current typing users
            // For now, we'll simulate this with a placeholder
            // In a real implementation, you'd have an endpoint like:
            // GET /api/chats/{chatId}/typing-users
            
            Log.d(TAG, "Polling typing indicators for chat: $chatId")
            
            // Placeholder: In real implementation, parse response and notify callbacks
            // val typingUsers = chatService.getTypingUsers(chatId)
            // typingUsers.forEach { typingStatus ->
            //     notifyTypingCallbacks(typingStatus)
            // }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll typing indicators for chat: $chatId", e)
        }
    }

    /**
     * Poll for read receipts using REST API.
     * 
     * @param chatId The chat room identifier
     */
    private suspend fun pollReadReceipts(chatId: String) {
        try {
            // This would typically call a REST endpoint to get recent read receipts
            // For now, we'll simulate this with a placeholder
            // In a real implementation, you'd have an endpoint like:
            // GET /api/chats/{chatId}/read-receipts?since={timestamp}
            
            Log.d(TAG, "Polling read receipts for chat: $chatId")
            
            // Placeholder: In real implementation, parse response and notify callbacks
            // val readReceipts = chatService.getReadReceipts(chatId, lastPollTime)
            // readReceipts.forEach { readReceiptEvent ->
            //     notifyReadReceiptCallbacks(readReceiptEvent)
            // }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll read receipts for chat: $chatId", e)
        }
    }

    /**
     * Queue typing event when WebSocket is unavailable.
     * Events will be sent when connection is restored.
     * 
     * Requirements: 6.2
     * 
     * @param chatId The chat room identifier
     * @param typingStatus The typing status to queue
     */
    private fun queueTypingEvent(chatId: String, typingStatus: TypingStatus) {
        val queue = queuedTypingEvents.getOrPut(chatId) { mutableListOf() }
        
        // Remove any existing typing event for the same user to avoid duplicates
        queue.removeAll { it.userId == typingStatus.userId }
        
        // Add new event
        queue.add(typingStatus)
        
        // Limit queue size to prevent memory issues
        if (queue.size > 50) {
            queue.removeAt(0) // Remove oldest event
        }
        
        // Update metrics with total queued events count
        val totalQueued = queuedTypingEvents.values.sumOf { it.size } + 
                         queuedReadReceiptEvents.values.sumOf { it.size }
        metrics.updateQueuedEventsCount(totalQueued)
        
        Log.d(TAG, "Queued typing event for chat: $chatId, user: ${typingStatus.userId}")
    }

    /**
     * Queue read receipt event when WebSocket is unavailable.
     * Events will be batched and sent when connection is restored.
     * 
     * Requirements: 6.2
     * 
     * @param chatId The chat room identifier
     * @param readReceiptEvent The read receipt event to queue
     */
    private fun queueReadReceiptEvent(chatId: String, readReceiptEvent: ReadReceiptEvent) {
        val queue = queuedReadReceiptEvents.getOrPut(chatId) { mutableListOf() }
        
        // Add new event
        queue.add(readReceiptEvent)
        
        // Limit queue size to prevent memory issues
        if (queue.size > 100) {
            queue.removeAt(0) // Remove oldest event
        }
        
        // Update metrics with total queued events count
        val totalQueued = queuedTypingEvents.values.sumOf { it.size } + 
                         queuedReadReceiptEvents.values.sumOf { it.size }
        metrics.updateQueuedEventsCount(totalQueued)
        
        Log.d(TAG, "Queued read receipt event for chat: $chatId, messages: ${readReceiptEvent.messageIds.size}")
    }

    /**
     * Send all queued events when connection is restored.
     * Batches read receipts and sends typing events.
     * 
     * Requirements: 6.2
     * 
     * @param chatId The chat room identifier
     */
    private suspend fun sendQueuedEvents(chatId: String) {
        try {
            // Send queued typing events
            val typingQueue = queuedTypingEvents[chatId]
            if (!typingQueue.isNullOrEmpty()) {
                // Only send the most recent typing status per user
                val latestTypingByUser = typingQueue.groupBy { it.userId }
                    .mapValues { (_, events) -> events.maxByOrNull { it.timestamp } }
                    .values
                    .filterNotNull()
                
                latestTypingByUser.forEach { typingStatus ->
                    try {
                        broadcastTyping(chatId, typingStatus.userId, typingStatus.isTyping)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send queued typing event", e)
                    }
                }
                
                // Clear sent events
                typingQueue.clear()
                
                // Update queued events count
                val totalQueued = queuedTypingEvents.values.sumOf { it.size } + 
                                 queuedReadReceiptEvents.values.sumOf { it.size }
                metrics.updateQueuedEventsCount(totalQueued)
                
                Log.d(TAG, "Sent ${latestTypingByUser.size} queued typing events for chat: $chatId")
            }
            
            // Send queued read receipt events (batched)
            val readReceiptQueue = queuedReadReceiptEvents[chatId]
            if (!readReceiptQueue.isNullOrEmpty()) {
                // Batch all message IDs by user
                val batchedReceipts = readReceiptQueue.groupBy { it.userId }
                    .mapValues { (_, events) -> 
                        events.flatMap { it.messageIds }.distinct()
                    }
                
                batchedReceipts.forEach { (userId, messageIds) ->
                    if (messageIds.isNotEmpty()) {
                        try {
                            broadcastReadReceipt(chatId, userId, messageIds)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send queued read receipt event", e)
                        }
                    }
                }
                
                // Clear sent events
                readReceiptQueue.clear()
                
                // Update queued events count
                val totalQueued = queuedTypingEvents.values.sumOf { it.size } + 
                                 queuedReadReceiptEvents.values.sumOf { it.size }
                metrics.updateQueuedEventsCount(totalQueued)
                
                Log.d(TAG, "Sent batched read receipts for ${batchedReceipts.size} users in chat: $chatId")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending queued events for chat: $chatId", e)
        }
    }

    /**
     * Stop polling fallback for a specific chat.
     * 
     * @param chatId The chat room identifier
     */
    private fun stopPollingFallback(chatId: String) {
        pollingJobs[chatId]?.cancel()
        pollingJobs.remove(chatId)
        chatPollingFallback[chatId] = false
        Log.d(TAG, "Stopped polling fallback for chat: $chatId")
    }

    /**
     * Clear all queued events for a specific chat.
     * 
     * @param chatId The chat room identifier
     */
    private fun clearQueuedEvents(chatId: String) {
        queuedTypingEvents.remove(chatId)
        queuedReadReceiptEvents.remove(chatId)
        Log.d(TAG, "Cleared queued events for chat: $chatId")
    }
}

/**
 * Represents the connection state of the Realtime service.
 */
sealed class RealtimeState {
    object Connected : RealtimeState()
    object Connecting : RealtimeState()
    object Disconnected : RealtimeState()
    data class Error(val message: String) : RealtimeState()
}
