package com.synapse.social.studioasinc.ui.deletion

import com.synapse.social.studioasinc.data.model.deletion.DeletionOperation
import com.synapse.social.studioasinc.data.model.deletion.OperationStatus
import com.synapse.social.studioasinc.data.model.deletion.StorageType
import com.synapse.social.studioasinc.data.repository.deletion.ChatHistoryManager
import com.synapse.social.studioasinc.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ChatHistoryDeletionViewModelTest {

    private lateinit var viewModel: ChatHistoryDeletionViewModel
    private lateinit var chatHistoryManager: ChatHistoryManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        chatHistoryManager = mockk(relaxed = true)
        val authRepository = mockk<AuthRepository>(relaxed = true)
        viewModel = ChatHistoryDeletionViewModel(chatHistoryManager, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDeletionHistory maps operations correctly`() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()
        val operations = listOf(
            DeletionOperation(
                id = "op1",
                storageType = StorageType.LOCAL_DATABASE,
                status = OperationStatus.COMPLETED,
                chatIds = null,
                messagesAffected = 100,
                timestamp = timestamp
            ),
            DeletionOperation(
                id = "op2",
                storageType = StorageType.LOCAL_DATABASE,
                status = OperationStatus.FAILED,
                chatIds = listOf("chat1"),
                messagesAffected = 50,
                timestamp = timestamp
            )
        )

        coEvery { chatHistoryManager.getDeletionHistory(any()) } returns operations

        // When
        // Re-initialize to trigger init block which calls loadDeletionHistory
        val authRepository = mockk<AuthRepository>(relaxed = true)
        viewModel = ChatHistoryDeletionViewModel(chatHistoryManager, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val history = viewModel.deletionHistory.value
        assertEquals(2, history.size)

        val item1 = history[0]
        assertEquals("op1", item1.id)
        assertEquals("Complete History Deletion", item1.operationType)
        assertEquals("Completed", item1.status)
        assertEquals(100, item1.messagesAffected)
        assertTrue(item1.isSuccess)

        val item2 = history[1]
        assertEquals("op2", item2.id)
        assertEquals("Selective Chat Deletion", item2.operationType)
        assertEquals("Failed", item2.status)
        assertEquals(50, item2.messagesAffected)
        assertTrue(item2.isError)
        assertTrue(item2.canRetry)

        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val expectedDate = sdf.format(Date(timestamp))
        assertEquals(expectedDate, item1.timestamp)
    }
}
