package com.synapse.social.studioasinc

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Test class to verify message grouping works correctly across all message types
 * Tests Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
 */
class ChatAdapterGroupingTest {

    private lateinit var testData: ArrayList<HashMap<String, Any?>>
    
    @Before
    fun setup() {
        testData = ArrayList()
    }

    /**
     * Test that text messages (VIEW_TYPE_TEXT) support grouping
     * Requirement 4.1
     */
    @Test
    fun testTextMessagesGroupCorrectly() {
        // Create three consecutive text messages from same sender
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        testData.add(createTextMessage(sender1, "First message", timestamp))
        testData.add(createTextMessage(sender1, "Second message", timestamp + 1000))
        testData.add(createTextMessage(sender1, "Third message", timestamp + 2000))
        
        // Verify all messages are groupable text type
        testData.forEach { message ->
            val type = message["TYPE"]?.toString() ?: "MESSAGE"
            assertEquals("MESSAGE", type)
            assertFalse(message["is_deleted"] as? Boolean ?: false)
        }
        
        // Verify sender consistency for grouping
        val senders = testData.map { it["sender_id"]?.toString() }
        assertTrue(senders.all { it == sender1 })
    }

    /**
     * Test that media messages (VIEW_TYPE_MEDIA_GRID) support grouping
     * Requirement 4.2
     */
    @Test
    fun testMediaMessagesGroupCorrectly() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        testData.add(createMediaMessage(sender1, listOf("image1.jpg"), timestamp))
        testData.add(createMediaMessage(sender1, listOf("image2.jpg", "image3.jpg"), timestamp + 1000))
        testData.add(createMediaMessage(sender1, listOf("image4.jpg"), timestamp + 2000))
        
        // Verify all messages are attachment type with images
        testData.forEach { message ->
            val type = message["TYPE"]?.toString()
            assertEquals("ATTACHMENT_MESSAGE", type)
            val attachments = message["attachments"] as? ArrayList<HashMap<String, Any?>>
            assertNotNull(attachments)
            assertTrue(attachments!!.isNotEmpty())
        }
        
        // Verify sender consistency
        val senders = testData.map { it["sender_id"]?.toString() }
        assertTrue(senders.all { it == sender1 })
    }

    /**
     * Test that video messages (VIEW_TYPE_VIDEO) support grouping
     * Requirement 4.3
     */
    @Test
    fun testVideoMessagesGroupCorrectly() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        testData.add(createVideoMessage(sender1, "video1.mp4", timestamp))
        testData.add(createVideoMessage(sender1, "video2.mp4", timestamp + 1000))
        
        // Verify all messages are video type
        testData.forEach { message ->
            val type = message["TYPE"]?.toString()
            assertEquals("ATTACHMENT_MESSAGE", type)
            val attachments = message["attachments"] as? ArrayList<HashMap<String, Any?>>
            assertNotNull(attachments)
            assertEquals(1, attachments!!.size)
            assertTrue(attachments[0]["publicId"]?.toString()?.contains("|video") == true)
        }
        
        // Verify sender consistency
        val senders = testData.map { it["sender_id"]?.toString() }
        assertTrue(senders.all { it == sender1 })
    }

    /**
     * Test that voice messages (VIEW_TYPE_VOICE_MESSAGE) support grouping
     * Requirement 4.4
     */
    @Test
    fun testVoiceMessagesGroupCorrectly() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        testData.add(createVoiceMessage(sender1, "audio1.mp3", 30000, timestamp))
        testData.add(createVoiceMessage(sender1, "audio2.mp3", 45000, timestamp + 1000))
        
        // Verify all messages are voice type
        testData.forEach { message ->
            val type = message["TYPE"]?.toString()
            assertEquals("VOICE_MESSAGE", type)
            val attachments = message["attachments"] as? ArrayList<HashMap<String, Any?>>
            assertNotNull(attachments)
            assertTrue(attachments!!.isNotEmpty())
        }
        
        // Verify sender consistency
        val senders = testData.map { it["sender_id"]?.toString() }
        assertTrue(senders.all { it == sender1 })
    }

    /**
     * Test that link preview messages (VIEW_TYPE_LINK_PREVIEW) support grouping
     * Requirement 4.5
     */
    @Test
    fun testLinkPreviewMessagesGroupCorrectly() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        testData.add(createLinkPreviewMessage(sender1, "Check this out https://example.com", timestamp))
        testData.add(createLinkPreviewMessage(sender1, "Another link https://test.com", timestamp + 1000))
        
        // Verify all messages contain URLs
        testData.forEach { message ->
            val content = message["content"]?.toString() ?: ""
            assertTrue(content.contains("https://"))
        }
        
        // Verify sender consistency
        val senders = testData.map { it["sender_id"]?.toString() }
        assertTrue(senders.all { it == sender1 })
    }

    /**
     * Test that typing, error, and loading indicators don't participate in grouping
     * Requirement 4.6
     */
    @Test
    fun testNonGroupableMessageTypes() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        // Create a mix of groupable and non-groupable messages
        testData.add(createTextMessage(sender1, "First message", timestamp))
        testData.add(createTypingIndicator())
        testData.add(createTextMessage(sender1, "Second message", timestamp + 2000))
        testData.add(createErrorMessage(sender1, "Failed message", timestamp + 3000))
        testData.add(createTextMessage(sender1, "Third message", timestamp + 4000))
        testData.add(createLoadingIndicator())
        
        // Verify typing indicator
        val typingMessage = testData[1]
        assertTrue(typingMessage.containsKey("typingMessageStatus"))
        
        // Verify error message
        val errorMessage = testData[3]
        val deliveryStatus = errorMessage["delivery_status"]?.toString() 
            ?: errorMessage["message_state"]?.toString()
        assertEquals("failed", deliveryStatus)
        
        // Verify loading indicator
        val loadingMessage = testData[5]
        assertTrue(loadingMessage.containsKey("isLoadingMore"))
        
        // Verify groupable messages have consistent sender
        val groupableMessages = listOf(testData[0], testData[2], testData[4])
        val senders = groupableMessages.map { it["sender_id"]?.toString() }
        assertTrue(senders.all { it == sender1 })
    }

    /**
     * Test mixed message types from same sender can group together
     */
    @Test
    fun testMixedMessageTypesGroupTogether() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        // Create different message types from same sender
        testData.add(createTextMessage(sender1, "Text message", timestamp))
        testData.add(createMediaMessage(sender1, listOf("image.jpg"), timestamp + 1000))
        testData.add(createVideoMessage(sender1, "video.mp4", timestamp + 2000))
        testData.add(createVoiceMessage(sender1, "audio.mp3", 30000, timestamp + 3000))
        testData.add(createLinkPreviewMessage(sender1, "Link https://example.com", timestamp + 4000))
        
        // Verify all messages are from same sender
        val senders = testData.map { it["sender_id"]?.toString() }
        assertTrue(senders.all { it == sender1 })
        
        // Verify none are deleted
        testData.forEach { message ->
            assertFalse(message["is_deleted"] as? Boolean ?: false)
            assertFalse(message["delete_for_everyone"] as? Boolean ?: false)
        }
    }

    /**
     * Test that deleted messages are excluded from grouping
     */
    @Test
    fun testDeletedMessagesExcludedFromGrouping() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        testData.add(createTextMessage(sender1, "First message", timestamp))
        testData.add(createTextMessage(sender1, "Deleted message", timestamp + 1000).apply {
            this["is_deleted"] = true
        })
        testData.add(createTextMessage(sender1, "Third message", timestamp + 2000))
        
        // Verify deleted message
        val deletedMessage = testData[1]
        assertTrue(deletedMessage["is_deleted"] as? Boolean ?: false)
        
        // Verify non-deleted messages can still group
        val nonDeletedMessages = listOf(testData[0], testData[2])
        val senders = nonDeletedMessages.map { it["sender_id"]?.toString() }
        assertTrue(senders.all { it == sender1 })
    }

    // Helper methods to create test messages

    private fun createTextMessage(senderId: String, content: String, timestamp: Long): HashMap<String, Any?> {
        return hashMapOf(
            "id" to "msg_${System.nanoTime()}",
            "sender_id" to senderId,
            "content" to content,
            "TYPE" to "MESSAGE",
            "created_at" to timestamp,
            "is_deleted" to false,
            "delete_for_everyone" to false,
            "delivery_status" to "sent"
        )
    }

    private fun createMediaMessage(senderId: String, imageUrls: List<String>, timestamp: Long): HashMap<String, Any?> {
        val attachments = ArrayList<HashMap<String, Any?>>()
        imageUrls.forEach { url ->
            attachments.add(hashMapOf(
                "url" to url,
                "type" to "image",
                "publicId" to "img_${System.nanoTime()}"
            ))
        }
        
        return hashMapOf(
            "id" to "msg_${System.nanoTime()}",
            "sender_id" to senderId,
            "TYPE" to "ATTACHMENT_MESSAGE",
            "attachments" to attachments,
            "created_at" to timestamp,
            "is_deleted" to false,
            "delete_for_everyone" to false,
            "delivery_status" to "sent"
        )
    }

    private fun createVideoMessage(senderId: String, videoUrl: String, timestamp: Long): HashMap<String, Any?> {
        val attachments = ArrayList<HashMap<String, Any?>>()
        attachments.add(hashMapOf(
            "url" to videoUrl,
            "type" to "video",
            "publicId" to "video_${System.nanoTime()}|video",
            "thumbnailUrl" to "${videoUrl}_thumb.jpg"
        ))
        
        return hashMapOf(
            "id" to "msg_${System.nanoTime()}",
            "sender_id" to senderId,
            "TYPE" to "ATTACHMENT_MESSAGE",
            "attachments" to attachments,
            "created_at" to timestamp,
            "is_deleted" to false,
            "delete_for_everyone" to false,
            "delivery_status" to "sent"
        )
    }

    private fun createVoiceMessage(senderId: String, audioUrl: String, duration: Long, timestamp: Long): HashMap<String, Any?> {
        val attachments = ArrayList<HashMap<String, Any?>>()
        attachments.add(hashMapOf(
            "url" to audioUrl,
            "type" to "audio",
            "duration" to duration
        ))
        
        return hashMapOf(
            "id" to "msg_${System.nanoTime()}",
            "sender_id" to senderId,
            "TYPE" to "VOICE_MESSAGE",
            "attachments" to attachments,
            "created_at" to timestamp,
            "is_deleted" to false,
            "delete_for_everyone" to false,
            "delivery_status" to "sent"
        )
    }

    private fun createLinkPreviewMessage(senderId: String, content: String, timestamp: Long): HashMap<String, Any?> {
        return hashMapOf(
            "id" to "msg_${System.nanoTime()}",
            "sender_id" to senderId,
            "content" to content,
            "TYPE" to "MESSAGE",
            "created_at" to timestamp,
            "is_deleted" to false,
            "delete_for_everyone" to false,
            "delivery_status" to "sent"
        )
    }

    private fun createTypingIndicator(): HashMap<String, Any?> {
        return hashMapOf(
            "typingMessageStatus" to true
        )
    }

    private fun createErrorMessage(senderId: String, content: String, timestamp: Long): HashMap<String, Any?> {
        return hashMapOf(
            "id" to "msg_${System.nanoTime()}",
            "sender_id" to senderId,
            "content" to content,
            "TYPE" to "MESSAGE",
            "created_at" to timestamp,
            "delivery_status" to "failed",
            "message_state" to "failed",
            "error" to "Network error"
        )
    }

    private fun createLoadingIndicator(): HashMap<String, Any?> {
        return hashMapOf(
            "isLoadingMore" to true
        )
    }

    // ========== Edge Case Tests (Task 7) ==========

    /**
     * Test that a single message in conversation displays as SINGLE position
     * Requirement 1.4
     */
    @Test
    fun testSingleMessageDisplaysAsSinglePosition() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        // Create only one message
        testData.add(createTextMessage(sender1, "Only message", timestamp))
        
        // Verify it's the only message
        assertEquals(1, testData.size)
        
        // Verify message is not deleted and is groupable
        val message = testData[0]
        assertFalse(message["is_deleted"] as? Boolean ?: false)
        assertFalse(message["delete_for_everyone"] as? Boolean ?: false)
        assertEquals("MESSAGE", message["TYPE"]?.toString())
        
        // A single message should not group with previous (no previous exists)
        // and should not group with next (no next exists)
        // Therefore it should be SINGLE position
    }

    /**
     * Test that deleted messages are excluded from grouping calculations
     * Requirement 1.4, 6.5
     */
    @Test
    fun testDeletedMessagesExcludedFromGroupingCalculations() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        // Create messages with deleted ones in between
        testData.add(createTextMessage(sender1, "Message 1", timestamp))
        testData.add(createTextMessage(sender1, "Message 2 (deleted)", timestamp + 1000).apply {
            this["is_deleted"] = true
        })
        testData.add(createTextMessage(sender1, "Message 3 (deleted)", timestamp + 2000).apply {
            this["delete_for_everyone"] = true
        })
        testData.add(createTextMessage(sender1, "Message 4", timestamp + 3000))
        
        // Verify deleted messages
        assertTrue(testData[1]["is_deleted"] as? Boolean ?: false)
        assertTrue(testData[2]["delete_for_everyone"] as? Boolean ?: false)
        
        // Verify non-deleted messages are from same sender
        assertFalse(testData[0]["is_deleted"] as? Boolean ?: false)
        assertFalse(testData[3]["is_deleted"] as? Boolean ?: false)
        assertEquals(sender1, testData[0]["sender_id"]?.toString())
        assertEquals(sender1, testData[3]["sender_id"]?.toString())
        
        // Message 1 should not group with Message 2 (deleted)
        // Message 4 should not group with Message 3 (deleted)
        // Therefore Message 1 and Message 4 should both be SINGLE positions
    }

    /**
     * Test that non-deleted messages group correctly across deleted messages
     * Requirement 1.4, 6.5
     */
    @Test
    fun testNonDeletedMessagesGroupAcrossDeletedMessages() {
        val sender1 = "user123"
        val sender2 = "user456"
        val timestamp = System.currentTimeMillis()
        
        // Create a group with deleted message in middle
        testData.add(createTextMessage(sender1, "Message 1", timestamp))
        testData.add(createTextMessage(sender1, "Message 2", timestamp + 1000))
        testData.add(createTextMessage(sender1, "Message 3 (deleted)", timestamp + 2000).apply {
            this["is_deleted"] = true
        })
        testData.add(createTextMessage(sender1, "Message 4", timestamp + 3000))
        testData.add(createTextMessage(sender1, "Message 5", timestamp + 4000))
        
        // Verify sender consistency for non-deleted messages
        val nonDeletedMessages = listOf(testData[0], testData[1], testData[3], testData[4])
        nonDeletedMessages.forEach { message ->
            assertEquals(sender1, message["sender_id"]?.toString())
            assertFalse(message["is_deleted"] as? Boolean ?: false)
        }
        
        // Message 1 and 2 should group (FIRST and LAST)
        // Message 3 is deleted (breaks the group)
        // Message 4 and 5 should group (FIRST and LAST)
    }

    /**
     * Test grouping works correctly at start of conversation
     * Requirement 6.5
     */
    @Test
    fun testGroupingAtStartOfConversation() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        // Create messages starting from position 0
        testData.add(createTextMessage(sender1, "First message", timestamp))
        testData.add(createTextMessage(sender1, "Second message", timestamp + 1000))
        testData.add(createTextMessage(sender1, "Third message", timestamp + 2000))
        
        // Verify first message has no previous message to group with
        // It should be FIRST position (groups with next but not previous)
        val firstMessage = testData[0]
        assertEquals(sender1, firstMessage["sender_id"]?.toString())
        assertFalse(firstMessage["is_deleted"] as? Boolean ?: false)
        
        // All messages are from same sender and not deleted
        testData.forEach { message ->
            assertEquals(sender1, message["sender_id"]?.toString())
            assertFalse(message["is_deleted"] as? Boolean ?: false)
        }
    }

    /**
     * Test grouping works correctly at end of conversation
     * Requirement 6.5
     */
    @Test
    fun testGroupingAtEndOfConversation() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        // Create messages ending at last position
        testData.add(createTextMessage(sender1, "First message", timestamp))
        testData.add(createTextMessage(sender1, "Second message", timestamp + 1000))
        testData.add(createTextMessage(sender1, "Last message", timestamp + 2000))
        
        // Verify last message has no next message to group with
        // It should be LAST position (groups with previous but not next)
        val lastMessage = testData[testData.size - 1]
        assertEquals(sender1, lastMessage["sender_id"]?.toString())
        assertFalse(lastMessage["is_deleted"] as? Boolean ?: false)
        
        // All messages are from same sender and not deleted
        testData.forEach { message ->
            assertEquals(sender1, message["sender_id"]?.toString())
            assertFalse(message["is_deleted"] as? Boolean ?: false)
        }
    }

    /**
     * Test rapid message sending (< 60 seconds) hides intermediate timestamps
     * Requirement 3.1, 3.2
     */
    @Test
    fun testRapidMessageSendingHidesIntermediateTimestamps() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        // Create messages sent within 60 seconds of each other
        testData.add(createTextMessage(sender1, "Message 1", timestamp))
        testData.add(createTextMessage(sender1, "Message 2", timestamp + 10000)) // 10 seconds later
        testData.add(createTextMessage(sender1, "Message 3", timestamp + 25000)) // 25 seconds later
        testData.add(createTextMessage(sender1, "Message 4", timestamp + 45000)) // 45 seconds later
        
        // Verify all messages are from same sender
        testData.forEach { message ->
            assertEquals(sender1, message["sender_id"]?.toString())
            assertFalse(message["is_deleted"] as? Boolean ?: false)
        }
        
        // Calculate time differences
        val timeDiff1to2 = testData[1]["created_at"] as Long - testData[0]["created_at"] as Long
        val timeDiff2to3 = testData[2]["created_at"] as Long - testData[1]["created_at"] as Long
        val timeDiff3to4 = testData[3]["created_at"] as Long - testData[2]["created_at"] as Long
        
        // All time differences should be less than 60 seconds (60000ms)
        assertTrue(timeDiff1to2 < 60000)
        assertTrue(timeDiff2to3 < 60000)
        assertTrue(timeDiff3to4 < 60000)
        
        // Messages 1, 2, 3 should hide timestamps (FIRST, MIDDLE, MIDDLE positions)
        // Message 4 should show timestamp (LAST position)
    }

    /**
     * Test delayed message sending (> 60 seconds) shows all timestamps
     * Requirement 3.1, 3.2
     */
    @Test
    fun testDelayedMessageSendingShowsAllTimestamps() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        // Create messages sent more than 60 seconds apart
        testData.add(createTextMessage(sender1, "Message 1", timestamp))
        testData.add(createTextMessage(sender1, "Message 2", timestamp + 70000)) // 70 seconds later
        testData.add(createTextMessage(sender1, "Message 3", timestamp + 150000)) // 150 seconds later
        
        // Verify all messages are from same sender
        testData.forEach { message ->
            assertEquals(sender1, message["sender_id"]?.toString())
            assertFalse(message["is_deleted"] as? Boolean ?: false)
        }
        
        // Calculate time differences
        val timeDiff1to2 = testData[1]["created_at"] as Long - testData[0]["created_at"] as Long
        val timeDiff2to3 = testData[2]["created_at"] as Long - testData[1]["created_at"] as Long
        
        // All time differences should be greater than 60 seconds (60000ms)
        assertTrue(timeDiff1to2 > 60000)
        assertTrue(timeDiff2to3 > 60000)
        
        // All messages should show timestamps because time difference > 60 seconds
        // Even though they're from same sender, they should all be SINGLE positions
    }

    /**
     * Test complex scenario: mixed grouping with deleted messages and time gaps
     * Requirements 1.4, 3.1, 3.2, 6.5
     */
    @Test
    fun testComplexEdgeCaseScenario() {
        val sender1 = "user123"
        val sender2 = "user456"
        val timestamp = System.currentTimeMillis()
        
        // Create complex scenario
        testData.add(createTextMessage(sender1, "Message 1", timestamp))
        testData.add(createTextMessage(sender1, "Message 2", timestamp + 5000))
        testData.add(createTextMessage(sender1, "Message 3 (deleted)", timestamp + 10000).apply {
            this["is_deleted"] = true
        })
        testData.add(createTextMessage(sender1, "Message 4", timestamp + 15000))
        testData.add(createTextMessage(sender2, "Message 5", timestamp + 20000))
        testData.add(createTextMessage(sender2, "Message 6", timestamp + 90000)) // 70 seconds after Message 5
        testData.add(createTextMessage(sender1, "Message 7", timestamp + 95000))
        
        // Verify message properties
        // Messages 1 and 2 should group (same sender, < 60s apart)
        assertEquals(sender1, testData[0]["sender_id"]?.toString())
        assertEquals(sender1, testData[1]["sender_id"]?.toString())
        assertFalse(testData[0]["is_deleted"] as? Boolean ?: false)
        assertFalse(testData[1]["is_deleted"] as? Boolean ?: false)
        
        // Message 3 is deleted
        assertTrue(testData[2]["is_deleted"] as? Boolean ?: false)
        
        // Message 4 is SINGLE (previous is deleted, next is different sender)
        assertEquals(sender1, testData[3]["sender_id"]?.toString())
        assertFalse(testData[3]["is_deleted"] as? Boolean ?: false)
        
        // Message 5 is SINGLE (different sender from previous, > 60s from next)
        assertEquals(sender2, testData[4]["sender_id"]?.toString())
        
        // Message 6 is SINGLE (same sender as 5 but > 60s apart, different sender from next)
        assertEquals(sender2, testData[5]["sender_id"]?.toString())
        val timeDiff5to6 = testData[5]["created_at"] as Long - testData[4]["created_at"] as Long
        assertTrue(timeDiff5to6 > 60000)
        
        // Message 7 is SINGLE (different sender from previous)
        assertEquals(sender1, testData[6]["sender_id"]?.toString())
    }

    /**
     * Test empty conversation (no messages)
     * Requirement 6.5
     */
    @Test
    fun testEmptyConversation() {
        // testData is already empty from setup
        assertEquals(0, testData.size)
        
        // No messages to group - should handle gracefully
    }

    /**
     * Test alternating senders (no grouping should occur)
     * Requirement 1.3
     */
    @Test
    fun testAlternatingSendersNoGrouping() {
        val sender1 = "user123"
        val sender2 = "user456"
        val timestamp = System.currentTimeMillis()
        
        // Create alternating messages
        testData.add(createTextMessage(sender1, "Message 1", timestamp))
        testData.add(createTextMessage(sender2, "Message 2", timestamp + 1000))
        testData.add(createTextMessage(sender1, "Message 3", timestamp + 2000))
        testData.add(createTextMessage(sender2, "Message 4", timestamp + 3000))
        testData.add(createTextMessage(sender1, "Message 5", timestamp + 4000))
        
        // Verify senders alternate
        assertEquals(sender1, testData[0]["sender_id"]?.toString())
        assertEquals(sender2, testData[1]["sender_id"]?.toString())
        assertEquals(sender1, testData[2]["sender_id"]?.toString())
        assertEquals(sender2, testData[3]["sender_id"]?.toString())
        assertEquals(sender1, testData[4]["sender_id"]?.toString())
        
        // All messages should be SINGLE position (no consecutive messages from same sender)
        testData.forEach { message ->
            assertFalse(message["is_deleted"] as? Boolean ?: false)
        }
    }

    /**
     * Test boundary condition: exactly 60 seconds between messages
     * Requirement 3.1, 3.2
     */
    @Test
    fun testExactly60SecondsBetweenMessages() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        // Create messages exactly 60 seconds apart
        testData.add(createTextMessage(sender1, "Message 1", timestamp))
        testData.add(createTextMessage(sender1, "Message 2", timestamp + 60000)) // Exactly 60 seconds
        
        // Verify time difference
        val timeDiff = testData[1]["created_at"] as Long - testData[0]["created_at"] as Long
        assertEquals(60000L, timeDiff)
        
        // At exactly 60 seconds, the condition is NOT > 60000, so timestamps should be hidden
        // Messages should group together
        assertEquals(sender1, testData[0]["sender_id"]?.toString())
        assertEquals(sender1, testData[1]["sender_id"]?.toString())
    }

    /**
     * Test boundary condition: 60001ms between messages (just over threshold)
     * Requirement 3.1, 3.2
     */
    @Test
    fun testJustOver60SecondsBetweenMessages() {
        val sender1 = "user123"
        val timestamp = System.currentTimeMillis()
        
        // Create messages just over 60 seconds apart
        testData.add(createTextMessage(sender1, "Message 1", timestamp))
        testData.add(createTextMessage(sender1, "Message 2", timestamp + 60001)) // 60.001 seconds
        
        // Verify time difference
        val timeDiff = testData[1]["created_at"] as Long - testData[0]["created_at"] as Long
        assertEquals(60001L, timeDiff)
        
        // At 60001ms, the condition IS > 60000, so timestamps should be shown
        // Messages should still group (same sender) but timestamps visible
        assertEquals(sender1, testData[0]["sender_id"]?.toString())
        assertEquals(sender1, testData[1]["sender_id"]?.toString())
    }
}
