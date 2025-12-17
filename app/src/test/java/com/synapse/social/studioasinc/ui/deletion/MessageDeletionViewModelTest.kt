package com.synapse.social.studioasinc.ui.deletion

import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.deletion.ChatHistoryManager
import com.synapse.social.studioasinc.data.model.deletion.DeletionResult
import com.synapse.social.studioasinc.data.model.deletion.DeletionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class MessageDeletionViewModelTest {

    @Mock
    private lateinit var chatHistoryManager: ChatHistoryManager

    @Mock
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: MessageDeletionViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        `when`(chatHistoryManager.getDeleteProgress()).thenReturn(MutableStateFlow(null))

        viewModel = MessageDeletionViewModel(chatHistoryManager, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deleteAllHistory should set error when user ID is mismatched`() = runTest {
        val userId = "user123"
        val authenticatedUserId = "user456"

        `when`(authRepository.getCurrentUserId()).thenReturn(authenticatedUserId)

        viewModel.deleteAllHistory(userId)

        assertEquals("Unauthorized: User ID mismatch or not logged in", viewModel.uiState.value.error)
    }

    @Test
    fun `deleteAllHistory should set error when user is not logged in`() = runTest {
        val userId = "user123"

        `when`(authRepository.getCurrentUserId()).thenReturn(null)

        viewModel.deleteAllHistory(userId)

        assertEquals("Unauthorized: User ID mismatch or not logged in", viewModel.uiState.value.error)
    }

    @Test
    fun `deleteAllHistory should proceed when user ID matches`() = runTest {
        val userId = "user123"
        val deletionResult = DeletionResult(
            success = true,
            completedOperations = emptyList(),
            failedOperations = emptyList(),
            totalMessagesDeleted = 10,
            errors = emptyList()
        )

        `when`(authRepository.getCurrentUserId()).thenReturn(userId)
        `when`(chatHistoryManager.deleteAllHistory(userId)).thenReturn(deletionResult)

        viewModel.deleteAllHistory(userId)

        assertEquals(null, viewModel.uiState.value.error)
        // You might want to verify interactions or other state changes
        verify(chatHistoryManager).deleteAllHistory(userId)
    }
}
