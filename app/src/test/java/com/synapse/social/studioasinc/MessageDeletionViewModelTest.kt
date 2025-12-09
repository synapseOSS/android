package com.synapse.social.studioasinc

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.synapse.social.studioasinc.presentation.viewmodel.DeletionState
import com.synapse.social.studioasinc.presentation.viewmodel.MessageDeletionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for MessageDeletionViewModel
 * Tests state management and business logic
 * 
 * Requirements: 1.1, 2.1, 2.3, 5.4, 6.5
 * 
 * NOTE: These tests require Supabase initialization and should be converted to integration tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@org.junit.Ignore("Requires Supabase initialization - convert to integration tests")
class MessageDeletionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MessageDeletionViewModel
    private lateinit var mockApplication: Application
    
    private val testUserId = "test-user-123"
    private val testMessageId = "test-message-456"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = ApplicationProvider.getApplicationContext()
        viewModel = MessageDeletionViewModel(mockApplication)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Test 15.1: Single Message Deletion ====================

    /**
     * Test: Initial state is Idle
     * Validates that ViewModel starts in Idle state
     */
    @Test
    fun testInitialState_IsIdle() {
        // Then: Initial state should be Idle
        assertEquals("Initial state should be Idle", DeletionState.Idle, viewModel.deletionState.value)
    }

    /**
     * Test: Delete for me updates state to Deleting
     * Requirement: 6.5
     */
    @Test
    fun testDeleteForMe_UpdatesStateToDeleting() = runTest {
        // Given: A message to delete
        val messageIds = listOf(testMessageId)
        
        // When: User initiates delete for me
        viewModel.deleteMessagesForMe(messageIds, testUserId)
        
        // Advance dispatcher to process coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: State should eventually update (either Deleting or Success/Error)
        assertNotEquals("State should change from Idle", DeletionState.Idle, viewModel.deletionState.value)
    }

    /**
     * Test: Empty message list validation
     * Validates that empty lists are rejected
     */
    @Test
    fun testDeleteForMe_EmptyList_DoesNotChangeState() = runTest {
        // Given: Empty message list
        val messageIds = emptyList<String>()
        
        // When: User attempts to delete empty list
        viewModel.deleteMessagesForMe(messageIds, testUserId)
        
        // Advance dispatcher
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: State should remain Idle
        assertEquals("State should remain Idle for empty list", DeletionState.Idle, viewModel.deletionState.value)
    }

    /**
     * Test: Blank user ID validation
     * Validates that blank user IDs are rejected
     */
    @Test
    fun testDeleteForMe_BlankUserId_DoesNotChangeState() = runTest {
        // Given: Valid message but blank user ID
        val messageIds = listOf(testMessageId)
        
        // When: User attempts to delete with blank user ID
        viewModel.deleteMessagesForMe(messageIds, "")
        
        // Advance dispatcher
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: State should remain Idle
        assertEquals("State should remain Idle for blank user ID", DeletionState.Idle, viewModel.deletionState.value)
    }

    /**
     * Test: Optimistic updates for delete for me
     * Requirement: 7.2
     */
    @Test
    fun testDeleteForMe_OptimisticUpdates() = runTest {
        // Given: A message to delete
        val messageIds = listOf(testMessageId)
        
        // When: User initiates delete for me
        viewModel.deleteMessagesForMe(messageIds, testUserId)
        
        // Advance dispatcher slightly to allow optimistic update
        testDispatcher.scheduler.advanceTimeBy(100)
        
        // Then: Message should be optimistically marked as deleted
        assertTrue(
            "Message should be optimistically deleted",
            viewModel.isOptimisticallyDeleted(testMessageId)
        )
    }

    /**
     * Test: Reset state functionality
     * Validates that state can be reset to Idle
     */
    @Test
    fun testResetState_SetsStateToIdle() {
        // Given: ViewModel in any state
        // When: State is reset
        viewModel.resetState()
        
        // Then: State should be Idle
        assertEquals("State should be Idle after reset", DeletionState.Idle, viewModel.deletionState.value)
    }

    /**
     * Test: Clear optimistic deletes
     * Validates that optimistic deletes can be cleared
     */
    @Test
    fun testClearOptimisticDeletes_ClearsSet() = runTest {
        // Given: A message optimistically deleted
        val messageIds = listOf(testMessageId)
        viewModel.deleteMessagesForMe(messageIds, testUserId)
        testDispatcher.scheduler.advanceTimeBy(100)
        
        // When: Optimistic deletes are cleared
        viewModel.clearOptimisticDeletes()
        
        // Then: Message should no longer be optimistically deleted
        assertFalse(
            "Message should not be optimistically deleted after clear",
            viewModel.isOptimisticallyDeleted(testMessageId)
        )
    }

    /**
     * Test: Delete for everyone with empty list
     * Validates that empty lists are rejected
     */
    @Test
    fun testDeleteForEveryone_EmptyList_DoesNotChangeState() = runTest {
        // Given: Empty message list
        val messageIds = emptyList<String>()
        
        // When: User attempts to delete empty list for everyone
        viewModel.deleteMessagesForEveryone(messageIds, testUserId)
        
        // Advance dispatcher
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: State should remain Idle
        assertEquals("State should remain Idle for empty list", DeletionState.Idle, viewModel.deletionState.value)
    }

    /**
     * Test: Delete for everyone with blank user ID
     * Validates that blank user IDs are rejected
     */
    @Test
    fun testDeleteForEveryone_BlankUserId_DoesNotChangeState() = runTest {
        // Given: Valid message but blank user ID
        val messageIds = listOf(testMessageId)
        
        // When: User attempts to delete with blank user ID for everyone
        viewModel.deleteMessagesForEveryone(messageIds, "")
        
        // Advance dispatcher
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: State should remain Idle
        assertEquals("State should remain Idle for blank user ID", DeletionState.Idle, viewModel.deletionState.value)
    }

    /**
     * Test: Optimistic updates for delete for everyone
     * Requirement: 7.2
     */
    @Test
    fun testDeleteForEveryone_OptimisticUpdates() = runTest {
        // Given: A message to delete
        val messageIds = listOf(testMessageId)
        
        // When: User initiates delete for everyone
        viewModel.deleteMessagesForEveryone(messageIds, testUserId)
        
        // Advance dispatcher slightly to allow optimistic update
        testDispatcher.scheduler.advanceTimeBy(100)
        
        // Then: Message should be optimistically marked as deleted
        // Note: This will fail ownership validation in real scenario
        // but optimistic update happens before validation completes
        assertTrue(
            "Message should be optimistically deleted",
            viewModel.isOptimisticallyDeleted(testMessageId)
        )
    }

    /**
     * Test: Multiple messages optimistic deletion
     * Validates that multiple messages can be optimistically deleted
     */
    @Test
    fun testOptimisticDeletion_MultipleMessages() = runTest {
        // Given: Multiple messages to delete
        val messageIds = listOf("msg-1", "msg-2", "msg-3")
        
        // When: User initiates delete
        viewModel.deleteMessagesForMe(messageIds, testUserId)
        
        // Advance dispatcher slightly
        testDispatcher.scheduler.advanceTimeBy(100)
        
        // Then: All messages should be optimistically deleted
        assertTrue("First message should be optimistically deleted", viewModel.isOptimisticallyDeleted("msg-1"))
        assertTrue("Second message should be optimistically deleted", viewModel.isOptimisticallyDeleted("msg-2"))
        assertTrue("Third message should be optimistically deleted", viewModel.isOptimisticallyDeleted("msg-3"))
    }
}
