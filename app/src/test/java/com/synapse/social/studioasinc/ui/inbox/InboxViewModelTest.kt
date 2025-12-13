package com.synapse.social.studioasinc.ui.inbox

import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.ui.inbox.models.InboxUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class InboxViewModelTest {

    private lateinit var viewModel: InboxViewModel
    private val chatService = mockk<SupabaseChatService>(relaxed = true)
    private val authService = mockk<SupabaseAuthenticationService>(relaxed = true)
    private val databaseService = mockk<SupabaseDatabaseService>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock current user
        every { authService.getCurrentUser() } returns mockk {
            every { id } returns "user1"
        }

        // Mock initial load
        coEvery { chatService.getUserChats("user1") } returns Result.success(emptyList())

        viewModel = InboxViewModel(chatService, authService, databaseService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deleteChat calls backend and updates state`() = runTest {
        // Given
        val chatId = "chat1"
        val chatMap = mapOf(
            "chat_id" to chatId,
            "other_user_name" to "User 2",
            "last_message" to "Hello",
            "last_message_time" to "1234567890",
            "unread_count" to "0"
        )

        coEvery { chatService.getUserChats("user1") } returns Result.success(listOf(chatMap))
        coEvery { chatService.deleteChat(chatId, "user1") } returns Result.success(Unit)

        // Reload to populate state
        viewModel.loadChats()
        advanceUntilIdle()

        // Verify initial state
        assertTrue(viewModel.uiState.value is InboxUiState.Success)
        assertEquals(1, (viewModel.uiState.value as InboxUiState.Success).chats.size)

        // When
        viewModel.onAction(InboxAction.DeleteChat(chatId))
        advanceUntilIdle()

        // Then
        // Verify backend call
        coVerify { chatService.deleteChat(chatId, "user1") }

        // Verify UI state update (optimistic)
        val state = viewModel.uiState.value as InboxUiState.Success
        assertTrue(state.chats.isEmpty())
    }
}
