package com.synapse.social.studioasinc.ui.inbox

import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.ui.inbox.models.ChatItemUiModel
import com.synapse.social.studioasinc.ui.inbox.models.InboxAction
import com.synapse.social.studioasinc.ui.inbox.models.InboxUiState
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var chatService: SupabaseChatService

    @Mock
    private lateinit var authService: SupabaseAuthenticationService

    @Mock
    private lateinit var databaseService: SupabaseDatabaseService

    @Mock
    private lateinit var mockUser: UserInfo

    private lateinit var viewModel: InboxViewModel
    private val userId = "user123"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock auth service
        whenever(authService.getCurrentUser()).thenReturn(mockUser)
        whenever(mockUser.id).thenReturn(userId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `archiveChat should update UI state and call backend`() = runTest {
        // Arrange
        val chatId = "chat1"
        val chat = ChatItemUiModel(
            id = chatId,
            otherUserId = "other1",
            displayName = "User 1",
            avatarUrl = null,
            lastMessage = "Hello",
            lastMessageTime = System.currentTimeMillis()
        )

        // Mock initial load
        whenever(chatService.getUserChats(userId)).thenReturn(Result.success(emptyList()))

        viewModel = InboxViewModel(chatService, authService, databaseService)
        testScheduler.advanceUntilIdle()

        // Manually set UI state to Success for testing
        // Since _uiState is private, we rely on loadChats or we can't easily set it.
        // So we should mock getUserChats to return the chat we want to archive.

        val chatMap = mapOf(
            "chat_id" to chatId,
            "other_user_name" to "User 1",
            "last_message" to "Hello",
            "last_message_time" to System.currentTimeMillis().toString(),
            "unread_count" to "0",
            "is_archived" to "false"
        )
        whenever(chatService.getUserChats(userId)).thenReturn(Result.success(listOf(chatMap)))

        // Mock database service for user enrichment
        whenever(databaseService.selectWhere(eq("users"), anyString(), eq("uid"), anyString()))
            .thenReturn(Result.success(emptyList()))

        viewModel.loadChats()
        testScheduler.advanceUntilIdle()

        // Verify initial state
        val initialState = viewModel.uiState.value
        assertTrue(initialState is InboxUiState.Success)
        val successState = initialState as InboxUiState.Success
        assertEquals(1, successState.chats.size)
        assertEquals(0, successState.archivedChats.size)

        // Mock archive backend call
        whenever(chatService.archiveChat(chatId, userId)).thenReturn(Result.success(Unit))

        // Act
        viewModel.onAction(InboxAction.ArchiveChat(chatId))
        testScheduler.advanceUntilIdle()

        // Assert
        verify(chatService).archiveChat(chatId, userId)

        val finalState = viewModel.uiState.value
        assertTrue(finalState is InboxUiState.Success)
        val finalSuccessState = finalState as InboxUiState.Success

        // Check that chat moved from chats to archivedChats
        assertEquals(0, finalSuccessState.chats.size)
        assertEquals(1, finalSuccessState.archivedChats.size)
        assertEquals(chatId, finalSuccessState.archivedChats.first().id)
        assertTrue(finalSuccessState.archivedChats.first().isArchived)
    }
}
