package com.synapse.social.studioasinc.chat.service

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks performance metrics for Supabase Realtime operations.
 * Monitors typing events, read receipts, reconnections, and latency.
 * 
 * Requirements: 6.1, 6.5
 */
class RealtimeMetrics {
    
    companion object {
        private const val TAG = "RealtimeMetrics"
        private const val LATENCY_SAMPLE_SIZE = 100
    }
    
    // Counters for events sent
    private val _typingEventsSent = AtomicInteger(0)
    private val _readReceiptsSent = AtomicInteger(0)
    private val _reconnectionCount = AtomicInteger(0)
    
    // Latency tracking
    private val latencySamples = mutableListOf<Long>()
    private val _averageLatency = AtomicLong(0L)
    
    // Additional metrics
    private val _totalEventsProcessed = AtomicInteger(0)
    private val _failedEvents = AtomicInteger(0)
    private val _pollingFallbackActivations = AtomicInteger(0)
    private val _queuedEventsCount = AtomicInteger(0)
    
    // Connection uptime tracking
    private var connectionStartTime = 0L
    private val _totalConnectionTime = AtomicLong(0L)
    
    // StateFlow for reactive metrics updates
    private val _metricsState = MutableStateFlow(MetricsSnapshot())
    val metricsState: StateFlow<MetricsSnapshot> = _metricsState.asStateFlow()
    
    /**
     * Record a typing event being sent.
     * 
     * @param latencyMs Optional latency measurement in milliseconds
     */
    fun recordTypingEventSent(latencyMs: Long? = null) {
        _typingEventsSent.incrementAndGet()
        _totalEventsProcessed.incrementAndGet()
        
        latencyMs?.let { recordLatency(it) }
        
        updateMetricsState()
        Log.d(TAG, "Typing event sent. Total: ${_typingEventsSent.get()}")
    }
    
    /**
     * Record a read receipt being sent.
     * 
     * @param messageCount Number of messages in the read receipt batch
     * @param latencyMs Optional latency measurement in milliseconds
     */
    fun recordReadReceiptSent(messageCount: Int = 1, latencyMs: Long? = null) {
        _readReceiptsSent.incrementAndGet()
        _totalEventsProcessed.incrementAndGet()
        
        latencyMs?.let { recordLatency(it) }
        
        updateMetricsState()
        Log.d(TAG, "Read receipt sent for $messageCount messages. Total: ${_readReceiptsSent.get()}")
    }
    
    /**
     * Record a reconnection attempt.
     * 
     * @param successful Whether the reconnection was successful
     */
    fun recordReconnection(successful: Boolean = true) {
        _reconnectionCount.incrementAndGet()
        
        if (!successful) {
            _failedEvents.incrementAndGet()
        }
        
        updateMetricsState()
        Log.d(TAG, "Reconnection recorded (successful: $successful). Total: ${_reconnectionCount.get()}")
    }
    
    /**
     * Record event latency for average calculation.
     * 
     * @param latencyMs Latency in milliseconds
     */
    fun recordLatency(latencyMs: Long) {
        synchronized(latencySamples) {
            latencySamples.add(latencyMs)
            
            // Keep only the most recent samples to prevent memory growth
            if (latencySamples.size > LATENCY_SAMPLE_SIZE) {
                latencySamples.removeAt(0)
            }
            
            // Calculate new average
            val average = latencySamples.average().toLong()
            _averageLatency.set(average)
        }
        
        updateMetricsState()
        Log.v(TAG, "Latency recorded: ${latencyMs}ms. Average: ${_averageLatency.get()}ms")
    }
    
    /**
     * Record a failed event (typing or read receipt).
     * 
     * @param eventType Type of event that failed ("typing" or "read_receipt")
     */
    fun recordFailedEvent(eventType: String) {
        _failedEvents.incrementAndGet()
        updateMetricsState()
        Log.w(TAG, "Failed event recorded: $eventType. Total failures: ${_failedEvents.get()}")
    }
    
    /**
     * Record activation of polling fallback mode.
     */
    fun recordPollingFallbackActivation() {
        _pollingFallbackActivations.incrementAndGet()
        updateMetricsState()
        Log.i(TAG, "Polling fallback activated. Total activations: ${_pollingFallbackActivations.get()}")
    }
    
    /**
     * Update the count of queued events waiting to be sent.
     * 
     * @param count Current number of queued events
     */
    fun updateQueuedEventsCount(count: Int) {
        _queuedEventsCount.set(count)
        updateMetricsState()
        Log.v(TAG, "Queued events count updated: $count")
    }
    
    /**
     * Record when a connection is established.
     */
    fun recordConnectionStart() {
        connectionStartTime = System.currentTimeMillis()
        Log.d(TAG, "Connection start recorded")
    }
    
    /**
     * Record when a connection is lost and calculate uptime.
     */
    fun recordConnectionEnd() {
        if (connectionStartTime > 0) {
            val connectionDuration = System.currentTimeMillis() - connectionStartTime
            _totalConnectionTime.addAndGet(connectionDuration)
            connectionStartTime = 0L
            
            updateMetricsState()
            Log.d(TAG, "Connection end recorded. Duration: ${connectionDuration}ms")
        }
    }
    
    /**
     * Get current metrics snapshot.
     * 
     * @return Current metrics data
     */
    fun getCurrentMetrics(): MetricsSnapshot {
        return MetricsSnapshot(
            typingEventsSent = _typingEventsSent.get(),
            readReceiptsSent = _readReceiptsSent.get(),
            reconnectionCount = _reconnectionCount.get(),
            averageLatency = _averageLatency.get(),
            totalEventsProcessed = _totalEventsProcessed.get(),
            failedEvents = _failedEvents.get(),
            pollingFallbackActivations = _pollingFallbackActivations.get(),
            queuedEventsCount = _queuedEventsCount.get(),
            totalConnectionTime = _totalConnectionTime.get(),
            successRate = calculateSuccessRate()
        )
    }
    
    /**
     * Reset all metrics to zero.
     * Useful for testing or periodic resets.
     */
    fun reset() {
        _typingEventsSent.set(0)
        _readReceiptsSent.set(0)
        _reconnectionCount.set(0)
        _averageLatency.set(0)
        _totalEventsProcessed.set(0)
        _failedEvents.set(0)
        _pollingFallbackActivations.set(0)
        _queuedEventsCount.set(0)
        _totalConnectionTime.set(0)
        connectionStartTime = 0L
        
        synchronized(latencySamples) {
            latencySamples.clear()
        }
        
        updateMetricsState()
        Log.i(TAG, "All metrics reset")
    }
    
    /**
     * Log current metrics to console.
     * Useful for debugging and monitoring.
     */
    fun logMetrics() {
        val metrics = getCurrentMetrics()
        
        Log.i(TAG, "=== Realtime Metrics ===")
        Log.i(TAG, "Typing events sent: ${metrics.typingEventsSent}")
        Log.i(TAG, "Read receipts sent: ${metrics.readReceiptsSent}")
        Log.i(TAG, "Reconnections: ${metrics.reconnectionCount}")
        Log.i(TAG, "Average latency: ${metrics.averageLatency}ms")
        Log.i(TAG, "Total events processed: ${metrics.totalEventsProcessed}")
        Log.i(TAG, "Failed events: ${metrics.failedEvents}")
        Log.i(TAG, "Success rate: ${String.format("%.2f", metrics.successRate * 100)}%")
        Log.i(TAG, "Polling fallback activations: ${metrics.pollingFallbackActivations}")
        Log.i(TAG, "Queued events: ${metrics.queuedEventsCount}")
        Log.i(TAG, "Total connection time: ${metrics.totalConnectionTime}ms")
        Log.i(TAG, "========================")
    }
    
    /**
     * Get metrics formatted as a human-readable string.
     * 
     * @return Formatted metrics string
     */
    fun getMetricsString(): String {
        val metrics = getCurrentMetrics()
        
        return buildString {
            appendLine("Realtime Metrics:")
            appendLine("• Typing events: ${metrics.typingEventsSent}")
            appendLine("• Read receipts: ${metrics.readReceiptsSent}")
            appendLine("• Reconnections: ${metrics.reconnectionCount}")
            appendLine("• Avg latency: ${metrics.averageLatency}ms")
            appendLine("• Success rate: ${String.format("%.1f", metrics.successRate * 100)}%")
            appendLine("• Polling fallbacks: ${metrics.pollingFallbackActivations}")
            appendLine("• Queued events: ${metrics.queuedEventsCount}")
        }
    }
    
    // Private helper methods
    
    private fun updateMetricsState() {
        _metricsState.value = getCurrentMetrics()
    }
    
    private fun calculateSuccessRate(): Double {
        val total = _totalEventsProcessed.get()
        val failed = _failedEvents.get()
        
        return if (total > 0) {
            (total - failed).toDouble() / total.toDouble()
        } else {
            1.0 // 100% success rate when no events processed
        }
    }
}

/**
 * Immutable snapshot of current metrics state.
 * Used for reactive updates and data export.
 */
data class MetricsSnapshot(
    val typingEventsSent: Int = 0,
    val readReceiptsSent: Int = 0,
    val reconnectionCount: Int = 0,
    val averageLatency: Long = 0L,
    val totalEventsProcessed: Int = 0,
    val failedEvents: Int = 0,
    val pollingFallbackActivations: Int = 0,
    val queuedEventsCount: Int = 0,
    val totalConnectionTime: Long = 0L,
    val successRate: Double = 1.0,
    val timestamp: Long = System.currentTimeMillis()
)
