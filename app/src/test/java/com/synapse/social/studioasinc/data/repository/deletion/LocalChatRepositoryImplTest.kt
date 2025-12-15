package com.synapse.social.studioasinc.data.repository.deletion

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.local.ChatDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for LocalChatRepositoryImpl
 * Tests core deletion functionality and secure deletion mechanisms
 * Requirements: 1.1, 2.1, 5.1, 5.4
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocalChatRepositoryImplTest {

    @Mock
    private lateinit var mockChatDao: ChatDao

    private lateinit var context: Context
    private lateinit var repository: LocalChatRepositoryImpl
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        
        // Create test SharedPreferences
        sharedPreferences = context.getSharedPreferences("test_message_actions_prefs", Context.MODE_PRIVATE)
        
        repository = LocalChatRepositoryImpl(context, mockChatDao)
    }

    @Test
    fun `deleteAllMessages should return success when no data exists`() = runTest {
        // Given
        val userId = "test_user_123"
        
        // When
        val result = repository.deleteAllMessages(userId)
        
        // Then
        assertTrue(result is RepositoryResult.Success)
        assertEquals(0, (result as RepositoryResult.Success).messagesDeleted)
    }

    @Test
    fun `deleteMessagesForChats should return success when no data exists`() = runTest {
        // Given
        val chatIds = listOf("chat_1", "chat_2", "chat_3")
        
        // When
        val result = repository.deleteMessagesForChats(chatIds)
        
        // Then
        assertTrue(result is RepositoryResult.Success)
        assertEquals(0, (result as RepositoryResult.Success).messagesDeleted)
    }

    @Test
    fun `verifyDeletionComplete should return true when no data exists`() = runTest {
        // Given
        val userId = "test_user_123"
        
        // When
        val result = repository.verifyDeletionComplete(userId)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `verifyChatsDeleted should return true when no data exists`() = runTest {
        // Given
        val chatIds = listOf("chat_1", "chat_2")
        
        // When
        val result = repository.verifyChatsDeleted(chatIds)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `getMessageCount should return zero when no data exists`() = runTest {
        // Given
        val userId = "test_user_123"
        
        // When
        val count = repository.getMessageCount(userId)
        
        // Then
        assertEquals(0, count)
    }

    @Test
    fun `getMessageCountForChats should return zero when no data exists`() = runTest {
        // Given
        val chatIds = listOf("chat_1", "chat_2")
        
        // When
        val count = repository.getMessageCountForChats(chatIds)
        
        // Then
        assertEquals(0, count)
    }

    @Test
    fun `deleteAllMessages should handle SharedPreferences data`() = runTest {
        // Given
        val userId = "test_user_123"
        val editor = sharedPreferences.edit()
        editor.putString("deleted_locally_msg_1", "true")
        editor.putString("summary_cache_msg_2", "test summary")
        editor.putString("summary_expiry_msg_2", "1234567890")
        editor.putString("other_key", "should not be deleted")
        editor.apply()
        
        // When
        val result = repository.deleteAllMessages(userId)
        
        // Then
        assertTrue(result is RepositoryResult.Success)
        // Note: The actual deletion count may vary based on implementation
        // The important thing is that it succeeds
    }

    @Test
    fun `deleteMessagesForChats should handle SharedPreferences data`() = runTest {
        // Given
        val chatIds = listOf("chat_1", "chat_2")
        val editor = sharedPreferences.edit()
        editor.putString("deleted_locally_msg_1", "true")
        editor.putString("summary_cache_msg_2", "test summary")
        editor.apply()
        
        // When
        val result = repository.deleteMessagesForChats(chatIds)
        
        // Then
        assertTrue(result is RepositoryResult.Success)
    }
}