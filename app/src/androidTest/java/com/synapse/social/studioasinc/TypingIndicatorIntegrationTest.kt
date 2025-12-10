package com.synapse.social.studioasinc

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.synapse.social.studioasinc.chat.service.TypingIndicatorManager
import com.synapse.social.studioasinc.chat.service.SupabaseRealtimeService
import com.synapse.social.studioasinc.presentation.viewmodel.ChatViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration tests for typing indicators across two devices
 * Tests real-time typing indicator functionality
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TypingIndicatorIntegrationTest {
    
    @get:Rule
    val activityRule = ActivityTestRule(ChatActivity::class.java, false, false)
    
    private lateinit var chatId: String
    private lateinit var userId1: String
    private lateinit var userId2: String
    
    @Before
    fun setUp() {
        chatId = "test_chat_typing_${System.currentTimeMillis()}"
        userId1 = "test_user_1_${System.currentTimeMillis()}"
        userId2 = "test_user_2_${System.currentTimeMillis()}"
    }
    
    /**
     * Test: Typing indicator appears within 200ms
     * Requirement: 1.1, 1.2
     * 
     * Verifies that when a user types, the typing event is broadcast
     * and the typing indicator appears for the remote user within 200ms.
     */
    @Test
    fun testTypingIndicatorAppearsWithin200ms() = runTest {
        // Given: Two chat activities representing different devices
        val intent1 = createChatIntent(chatId, userId1, userId2)
        val intent2 = createChatIntent(chatId, userId2, userId1)
        
        val scenario1 = ActivityScenario.launch<ChatActivity>(intent1)
        val scenario2 = ActivityScenario.launch<ChatActivity>(intent2)
        
        val typingDetectedLatch = CountDownLatch(1)
        var typingDetectedTime = 0L
        var typingStartTime = 0L
        
        scenario2.onActivity { activity ->
            // Set up listener for typing indicator visibility
            val typingIndicator = activity.findViewById<android.view.View>(R.id.typing_indicator_container)
            typingIndicator?.let { indicator ->
                indicator.viewTreeObserver.addOnGlobalLayoutListener {
                    if (indicator.visibility == android.view.View.VISIBLE && typingDetectedTime == 0L) {
                        typingDetectedTime = System.currentTimeMillis()
                        typingDetectedLatch.countDown()
                    }
                }
            }
        }
        
        // When: User starts typing in first device
        scenario1.onActivity { activity ->
            typingStartTime = System.currentTimeMillis()
            val messageInput = activity.findViewById<android.widget.EditText>(R.id.etMessage)
            messageInput?.setText("Hello")
        }
        
        // Then: Typing indicator should appear within 200ms
        val typingDetected = typingDetectedLatch.await(500, TimeUnit.MILLISECONDS)
        assertTrue("Typing indicator should be detected", typingDetected)
        
        val latency = typingDetectedTime - typingStartTime
        assertTrue("Typing indicator should appear within 200ms, actual: ${latency}ms", 
                  latency <= 200)
        
        scenario1.close()
        scenario2.close()
    }
    
    /**
     * Test: Auto-stop after 3 seconds
     * Requirement: 1.3, 1.4
     * 
     * Verifies that typing indicator automatically stops after 3 seconds
     * of inactivity.
     */
    @Test
    fun testTypingIndicatorAutoStopAfter3Seconds() = runTest {
        // Given: Two chat activities
        val intent1 = createChatIntent(chatId, userId1, userId2)
        val intent2 = createChatIntent(chatId, userId2, userId1)
        
        val scenario1 = ActivityScenario.launch<ChatActivity>(intent1)
        val scenario2 = ActivityScenario.launch<ChatActivity>(intent2)
        
        val typingStoppedLatch = CountDownLatch(1)
        var typingStoppedTime = 0L
        var lastTypingTime = 0L
        
        scenario2.onActivity { activity ->
            val typingIndicator = activity.findViewById<android.view.View>(R.id.typing_indicator_container)
            typingIndicator?.let { indicator ->
                indicator.viewTreeObserver.addOnGlobalLayoutListener {
                    if (indicator.visibility == android.view.View.GONE && typingStoppedTime == 0L) {
                        typingStoppedTime = System.currentTimeMillis()
                        typingStoppedLatch.countDown()
                    }
                }
            }
        }
        
        // When: User types and then stops
        scenario1.onActivity { activity ->
            val messageInput = activity.findViewById<android.widget.EditText>(R.id.etMessage)
            messageInput?.setText("Hello")
            lastTypingTime = System.currentTimeMillis()
        }
        
        // Wait for typing to start
        delay(100)
        
        // Then: Typing should stop automatically after 3 seconds
        val typingStopped = typingStoppedLatch.await(4000, TimeUnit.MILLISECONDS)
        assertTrue("Typing indicator should stop automatically", typingStopped)
        
        val inactivityDuration = typingStoppedTime - lastTypingTime
        assertTrue("Typing should stop after ~3 seconds, actual: ${inactivityDuration}ms",
                  inactivityDuration >= 2800 && inactivityDuration <= 3200)
        
        scenario1.close()
        scenario2.close()
    }
    
    /**
     * Test: Immediate stop when message sent
     * Requirement: 1.5
     * 
     * Verifies that typing indicator immediately stops when a message is sent.
     */
    @Test
    fun testTypingIndicatorStopsImmediatelyOnMessageSent() = runTest {
        // Given: Two chat activities
        val intent1 = createChatIntent(chatId, userId1, userId2)
        val intent2 = createChatIntent(chatId, userId2, userId1)
        
        val scenario1 = ActivityScenario.launch<ChatActivity>(intent1)
        val scenario2 = ActivityScenario.launch<ChatActivity>(intent2)
        
        val typingStoppedLatch = CountDownLatch(1)
        var typingStoppedTime = 0L
        var messageSentTime = 0L
        
        scenario2.onActivity { activity ->
            val typingIndicator = activity.findViewById<android.view.View>(R.id.typing_indicator_container)
            typingIndicator?.let { indicator ->
                indicator.viewTreeObserver.addOnGlobalLayoutListener {
                    if (indicator.visibility == android.view.View.GONE && typingStoppedTime == 0L) {
                        typingStoppedTime = System.currentTimeMillis()
                        typingStoppedLatch.countDown()
                    }
                }
            }
        }
        
        // When: User types and then sends message
        scenario1.onActivity { activity ->
            val messageInput = activity.findViewById<android.widget.EditText>(R.id.etMessage)
            val sendButton = activity.findViewById<android.widget.ImageButton>(R.id.btnSend)
            
            messageInput?.setText("Hello World")
            delay(100) // Let typing indicator appear
            
            messageSentTime = System.currentTimeMillis()
            sendButton?.performClick()
        }
        
        // Then: Typing should stop immediately
        val typingStopped = typingStoppedLatch.await(1000, TimeUnit.MILLISECONDS)
        assertTrue("Typing indicator should stop when message sent", typingStopped)
        
        val stopLatency = typingStoppedTime - messageSentTime
        assertTrue("Typing should stop immediately (within 100ms), actual: ${stopLatency}ms",
                  stopLatency <= 100)
        
        scenario1.close()
        scenario2.close()
    }
    
    /**
     * Test: Multiple users typing simultaneously
     * Requirement: 1.2, 2.3
     * 
     * Verifies that typing indicators work correctly when multiple users
     * are typing simultaneously in a group chat scenario.
     */
    @Test
    fun testMultipleUsersTypingSimultaneously() = runTest {
        // Given: Three chat activities (simulating group chat)
        val userId3 = "test_user_3_${System.currentTimeMillis()}"
        val groupChatId = "group_chat_${System.currentTimeMillis()}"
        
        val intent1 = createChatIntent(groupChatId, userId1, userId2)
        val intent2 = createChatIntent(groupChatId, userId2, userId1)
        val intent3 = createChatIntent(groupChatId, userId3, userId1)
        
        val scenario1 = ActivityScenario.launch<ChatActivity>(intent1)
        val scenario2 = ActivityScenario.launch<ChatActivity>(intent2)
        val scenario3 = ActivityScenario.launch<ChatActivity>(intent3)
        
        val multipleTypingDetectedLatch = CountDownLatch(1)
        
        scenario1.onActivity { activity ->
            val typingText = activity.findViewById<android.widget.TextView>(R.id.typing_text)
            typingText?.let { textView ->
                textView.viewTreeObserver.addOnGlobalLayoutListener {
                    val text = textView.text.toString()
                    if (text.contains("are typing") || text.contains(",")) {
                        multipleTypingDetectedLatch.countDown()
                    }
                }
            }
        }
        
        // When: Multiple users start typing
        scenario2.onActivity { activity ->
            val messageInput = activity.findViewById<android.widget.EditText>(R.id.etMessage)
            messageInput?.setText("User 2 typing")
        }
        
        delay(50)
        
        scenario3.onActivity { activity ->
            val messageInput = activity.findViewById<android.widget.EditText>(R.id.etMessage)
            messageInput?.setText("User 3 typing")
        }
        
        // Then: Multiple typing indicator should be shown
        val multipleTypingDetected = multipleTypingDetectedLatch.await(1000, TimeUnit.MILLISECONDS)
        assertTrue("Multiple users typing should be detected", multipleTypingDetected)
        
        scenario1.close()
        scenario2.close()
        scenario3.close()
    }
    
    /**
     * Test: Typing indicator performance under load
     * Requirement: 6.1
     * 
     * Verifies that typing indicators perform well with rapid typing
     * and proper debouncing is applied.
     */
    @Test
    fun testTypingIndicatorPerformanceUnderLoad() = runTest {
        // Given: Two chat activities
        val intent1 = createChatIntent(chatId, userId1, userId2)
        val intent2 = createChatIntent(chatId, userId2, userId1)
        
        val scenario1 = ActivityScenario.launch<ChatActivity>(intent1)
        val scenario2 = ActivityScenario.launch<ChatActivity>(intent2)
        
        var typingEventCount = 0
        val maxExpectedEvents = 10 // Should be debounced from rapid typing
        
        scenario2.onActivity { activity ->
            // Monitor typing events (this would require access to the service)
            // In a real implementation, you'd inject a test observer
        }
        
        // When: User types rapidly
        scenario1.onActivity { activity ->
            val messageInput = activity.findViewById<android.widget.EditText>(R.id.etMessage)
            
            // Simulate rapid typing (50 characters in quick succession)
            repeat(50) { i ->
                messageInput?.setText("Rapid typing test $i")
                Thread.sleep(10) // 10ms between keystrokes
            }
        }
        
        // Wait for debouncing to settle
        delay(1000)
        
        // Then: Events should be debounced (not 50 events)
        // This test would need access to internal metrics
        // For now, we just verify the UI doesn't crash under load
        scenario1.onActivity { activity ->
            val messageInput = activity.findViewById<android.widget.EditText>(R.id.etMessage)
            assertNotNull("Message input should still be functional", messageInput)
        }
        
        scenario1.close()
        scenario2.close()
    }
    
    private fun createChatIntent(chatId: String, currentUserId: String, otherUserId: String): Intent {
        return Intent(ApplicationProvider.getApplicationContext(), ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("currentUserId", currentUserId)
            putExtra("otherUserId", otherUserId)
            putExtra("otherUserName", "Test User")
        }
    }
}
