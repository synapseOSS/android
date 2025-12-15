package com.synapse.social.studioasinc.data.repository.deletion

import com.synapse.social.studioasinc.data.model.deletion.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ChatHistoryManagerImpl
 * Requirements: 1.4, 1.5, 6.5
 */
class ChatHistoryManagerTest {
    
    @Mock
    private lateinit var deletionCoordinator: DeletionCoordinator
    
    @Mock
    private lateinit var errorRecoveryManager: ErrorRecoveryManager
    
    @Mock
    private lateinit var userNotificationManager: UserNotificationManager
    
    @Mock
    private lateinit var retryQueueManager: RetryQueueManager
    
    private lateinit var chatHistoryManager: ChatHistoryManagerImpl
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        chatHistoryManager = ChatHistoryManagerImpl(
            deletionCoordinator,
            errorRecoveryManager,
            userNotificationManager,
            retryQueueManager
        )
    }
    
    @Test
    fun `deleteAllHistory should coordinate full deletion and return success result`() = runTest {
        // Given
        val userId = "test-user-123"
        val expectedResult = DeletionResult(
            success = true,
            completedOperations = listOf(
                DeletionOperation(
                    id = "op-1",
                    storageType = StorageType.LOCAL_DATABASE,
                    status = OperationStatus.COMPLETED,
                    chatIds = null,
                    messagesAffected = 100,
                    timestamp = System.currentTimeMillis()
                )
            ),
            failedOperations = emptyList(),
            totalMessagesDeleted = 100,
            errors = emptyList()
        )
        
        whenever(deletionCoordinator.coordinateFullDeletion(userId)).thenReturn(expectedResult)
        
        // When
        val result = chatHistoryManager.deleteAllHistory(userId)
        
        // Then
        assertTrue(result.success)
        assertEquals(100, result.totalMessagesDeleted)
        assertEquals(1, result.completedOperations.size)
        assertEquals(0, result.failedOperations.size)
        
        verify(deletionCoordinator).coordinateFullDeletion(userId)
        verify(userNotificationManager).notifyDeletionStarted(any())
        verify(userNotificationManager).notifyDeletionCompleted(any(), any())
    }
    
    @Test
    fun `deleteSpecificChats should validate input and coordinate selective deletion`() = runTest {
        // Given
        val userId = "test-user-123"
        val chatIds = listOf("chat-1", "chat-2", "chat-3")
        val expectedResult = DeletionResult(
            success = true,
            completedOperations = listOf(
                DeletionOperation(
                    id = "op-1",
                    storageType = StorageType.LOCAL_DATABASE,
                    status = OperationStatus.COMPLETED,
                    chatIds = chatIds,
                    messagesAffected = 50,
                    timestamp = System.currentTimeMillis()
                )
            ),
            failedOperations = emptyList(),
            totalMessagesDeleted = 50,
            errors = emptyList()
        )
        
        whenever(deletionCoordinator.coordinateSelectiveDeletion(userId, chatIds)).thenReturn(expectedResult)
        
        // When
        val result = chatHistoryManager.deleteSpecificChats(userId, chatIds)
        
        // Then
        assertTrue(result.success)
        assertEquals(50, result.totalMessagesDeleted)
        assertEquals(chatIds, result.completedOperations.first().chatIds)
        
        verify(deletionCoordinator).coordinateSelectiveDeletion(userId, chatIds)
        verify(userNotificationManager).notifyDeletionStarted(any())
        verify(userNotificationManager).notifyDeletionCompleted(any(), any())
    }
    
    @Test
    fun `deleteSpecificChats should return validation error for empty chat IDs`() = runTest {
        // Given
        val userId = "test-user-123"
        val emptyChatIds = emptyList<String>()
        
        // When
        val result = chatHistoryManager.deleteSpecificChats(userId, emptyChatIds)
        
        // Then
        assertFalse(result.success)
        assertEquals(0, result.totalMessagesDeleted)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors.first() is DeletionError.ValidationError)
    }
    
    @Test
    fun `getDeleteProgress should return progress from coordinator`() = runTest {
        // Given
        val progressFlow = MutableStateFlow(
            DeletionProgress(
                totalOperations = 3,
                completedOperations = 1,
                currentOperation = "Deleting local messages...",
                estimatedTimeRemaining = 5000L,
                canCancel = true
            )
        )
        
        whenever(deletionCoordinator.getProgress()).thenReturn(progressFlow)
        
        // When
        val result = chatHistoryManager.getDeleteProgress()
        
        // Then
        assertEquals(progressFlow, result)
        verify(deletionCoordinator).getProgress()
    }
    
    @Test
    fun `cancelDeletion should delegate to coordinator and notify user`() = runTest {
        // Given
        whenever(deletionCoordinator.cancelDeletion()).thenReturn(true)
        
        // When
        val result = chatHistoryManager.cancelDeletion()
        
        // Then
        assertTrue(result)
        verify(deletionCoordinator).cancelDeletion()
    }
    
    @Test
    fun `retryFailedOperations should handle empty failed operations list`() = runTest {
        // Given
        val userId = "test-user-123"
        whenever(retryQueueManager.getFailedOperations(userId)).thenReturn(emptyList())
        
        // When
        val result = chatHistoryManager.retryFailedOperations(userId)
        
        // Then
        assertTrue(result is RecoveryResult.Success)
        assertEquals(0, (result as RecoveryResult.Success).recoveredOperations.size)
        
        verify(retryQueueManager).getFailedOperations(userId)
    }
}