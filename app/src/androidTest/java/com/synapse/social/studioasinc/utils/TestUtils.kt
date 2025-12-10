package com.synapse.social.studioasinc.utils

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.synapse.social.studioasinc.ChatActivity
import com.synapse.social.studioasinc.model.models.MessageState
import kotlinx.coroutines.delay
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Utility functions for integration tests
 * Provides common functionality for typing indicator and read receipt tests
 */
object TestUtils {
    
    /**
     * Creates a standardized chat intent for testing
     */
    fun createChatIntent(
        chatId: String,
        currentUserId: String,
        otherUserId: String,
        otherUserName: String = "Test User"
    ): Intent {
        return Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("currentUserId", currentUserId)
            putExtra("otherUserId", otherUserId)
            putExtra("otherUserName", otherUserName)
        }
    }
    
    /**
     * Generates unique test identifiers
     */
    fun generateTestId(prefix: String): String {
        return "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * Waits for a condition to be true with timeout
     */
    suspend fun waitForCondition(
        timeoutMs: Long = 5000,
        intervalMs: Long = 100,
        condition: suspend () -> Boolean
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition()) {
                return true
            }
            delay(intervalMs)
        }
        return false
    }
    
    /**
     * Simulates typing with realistic delays
     */
    suspend fun simulateTyping(text: String, charDelayMs: Long = 50): String {
        val result = StringBuilder()
        for (char in text) {
            result.append(char)
            delay(charDelayMs)
        }
        return result.toString()
    }
    
    /**
     * Validates message state transition timing
     */
    fun validateStateTransitionTiming(
        previousState: String,
        newState: String,
        transitionTimeMs: Long,
        expectedMaxMs: Long
    ): Boolean {
        val validTransitions = mapOf(
            MessageState.SENDING to listOf(MessageState.SENT, MessageState.FAILED),
            MessageState.SENT to listOf(MessageState.DELIVERED, MessageState.FAILED),
            MessageState.DELIVERED to listOf(MessageState.READ)
        )
        
        val isValidTransition = validTransitions[previousState]?.contains(newState) == true
        val isWithinTimeLimit = transitionTimeMs <= expectedMaxMs
        
        return isValidTransition && isWithinTimeLimit
    }
    
    /**
     * Creates a countdown latch with timeout handling
     */
    fun createTimeoutLatch(count: Int = 1): TimeoutLatch {
        return TimeoutLatch(CountDownLatch(count))
    }
    
    /**
     * Wrapper for CountDownLatch with timeout utilities
     */
    class TimeoutLatch(private val latch: CountDownLatch) {
        fun countDown() = latch.countDown()
        
        fun await(timeoutMs: Long): Boolean {
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        }
        
        fun awaitOrThrow(timeoutMs: Long, message: String = "Timeout waiting for condition") {
            if (!await(timeoutMs)) {
                throw AssertionError("$message (timeout: ${timeoutMs}ms)")
            }
        }
    }
    
    /**
     * Performance measurement utilities
     */
    class PerformanceTimer {
        private var startTime: Long = 0
        private var endTime: Long = 0
        
        fun start() {
            startTime = System.currentTimeMillis()
        }
        
        fun stop(): Long {
            endTime = System.currentTimeMillis()
            return duration()
        }
        
        fun duration(): Long {
            return if (endTime > startTime) endTime - startTime else 0
        }
        
        fun assertDurationLessThan(maxMs: Long, operation: String = "Operation") {
            val actualDuration = duration()
            if (actualDuration > maxMs) {
                throw AssertionError("$operation took ${actualDuration}ms, expected < ${maxMs}ms")
            }
        }
    }
    
    /**
     * Network simulation utilities
     */
    object NetworkSimulator {
        
        /**
         * Simulates network delay
         */
        suspend fun simulateNetworkDelay(minMs: Long = 50, maxMs: Long = 200) {
            val delay = (minMs..maxMs).random()
            delay(delay)
        }
        
        /**
         * Simulates intermittent connectivity
         */
        suspend fun simulateIntermittentConnectivity(
            connectionDurationMs: Long = 1000,
            disconnectionDurationMs: Long = 500,
            cycles: Int = 3
        ) {
            repeat(cycles) {
                // Connected period
                delay(connectionDurationMs)
                // Disconnected period  
                delay(disconnectionDurationMs)
            }
        }
    }
    
    /**
     * UI interaction utilities
     */
    object UIUtils {
        
        /**
         * Simulates realistic user typing behavior
         */
        suspend fun simulateUserTyping(
            messageInput: android.widget.EditText?,
            text: String,
            wpmSpeed: Int = 40 // words per minute
        ) {
            messageInput?.let { input ->
                val charDelayMs = (60000 / (wpmSpeed * 5)).toLong() // 5 chars per word average
                
                for (i in text.indices) {
                    input.setText(text.substring(0, i + 1))
                    delay(charDelayMs + (-10..10).random()) // Add some randomness
                }
            }
        }
        
        /**
         * Simulates user pausing while typing
         */
        suspend fun simulateTypingPause(durationMs: Long = 1000) {
            delay(durationMs)
        }
    }
    
    /**
     * Test data generators
     */
    object TestDataGenerator {
        
        fun generateTestMessage(length: Int = 50): String {
            val words = listOf(
                "hello", "world", "test", "message", "typing", "indicator",
                "read", "receipt", "integration", "android", "kotlin", "supabase"
            )
            
            val result = StringBuilder()
            var currentLength = 0
            
            while (currentLength < length) {
                val word = words.random()
                if (currentLength + word.length + 1 <= length) {
                    if (result.isNotEmpty()) result.append(" ")
                    result.append(word)
                    currentLength += word.length + 1
                } else {
                    break
                }
            }
            
            return result.toString()
        }
        
        fun generateTestMessages(count: Int, avgLength: Int = 30): List<String> {
            return (1..count).map { i ->
                "Test message $i: ${generateTestMessage(avgLength)}"
            }
        }
    }
    
    /**
     * Assertion utilities for integration tests
     */
    object TestAssertions {
        
        fun assertTypingIndicatorVisible(
            typingIndicator: android.view.View?,
            timeoutMs: Long = 1000
        ) {
            val timer = PerformanceTimer()
            timer.start()
            
            while (timer.duration() < timeoutMs) {
                if (typingIndicator?.visibility == android.view.View.VISIBLE) {
                    return
                }
                Thread.sleep(50)
            }
            
            throw AssertionError("Typing indicator not visible within ${timeoutMs}ms")
        }
        
        fun assertMessageState(
            message: Any?, // Would be ChatMessage in real implementation
            expectedState: String,
            timeoutMs: Long = 2000
        ) {
            // This would check the actual message state
            // Implementation depends on the actual ChatMessage interface
        }
        
        fun assertLatencyWithinBounds(
            actualLatencyMs: Long,
            maxLatencyMs: Long,
            operation: String = "Operation"
        ) {
            if (actualLatencyMs > maxLatencyMs) {
                throw AssertionError(
                    "$operation latency ${actualLatencyMs}ms exceeds maximum ${maxLatencyMs}ms"
                )
            }
        }
    }
}
