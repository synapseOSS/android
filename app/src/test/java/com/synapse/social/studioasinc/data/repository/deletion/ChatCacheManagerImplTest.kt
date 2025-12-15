package com.synapse.social.studioasinc.data.repository.deletion

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for ChatCacheManagerImpl
 * Requirements: 1.3, 2.3, 5.3, 5.4
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChatCacheManagerImplTest {

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    @Mock
    private lateinit var mockCacheDir: File
    
    private lateinit var context: Context
    private lateinit var cacheManager: ChatCacheManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        `when`(mockEditor.clear()).thenReturn(mockEditor)
        `when`(mockCacheDir.exists()).thenReturn(true)
        `when`(mockCacheDir.mkdirs()).thenReturn(true)

        cacheManager = ChatCacheManagerImpl(context)
    }

    @Test
    fun `clearAllCache should succeed with real context`() = runTest {
        // Arrange
        val userId = "test_user_123"

        // Act
        val result = cacheManager.clearAllCache(userId)

        // Assert
        assertTrue("clearAllCache should succeed", result is CacheResult.Success)
    }

    @Test
    fun `clearCacheForChats should succeed with real context`() = runTest {
        // Arrange
        val chatIds = listOf("chat_1", "chat_2")

        // Act
        val result = cacheManager.clearCacheForChats(chatIds)

        // Assert
        assertTrue("clearCacheForChats should succeed", result is CacheResult.Success)
    }

    @Test
    fun `verifyCacheCleared should return true for clean cache`() = runTest {
        // Arrange
        val userId = "test_user_123"

        // Act
        val result = cacheManager.verifyCacheCleared(userId)

        // Assert
        assertTrue("verifyCacheCleared should return true for clean cache", result)
    }

    @Test
    fun `verifyChatCachesCleared should return true for clean cache`() = runTest {
        // Arrange
        val chatIds = listOf("chat_1", "chat_2")

        // Act
        val result = cacheManager.verifyChatCachesCleared(chatIds)

        // Assert
        assertTrue("verifyChatCachesCleared should return true for clean cache", result)
    }

    @Test
    fun `getCacheSize should return zero for empty cache`() = runTest {
        // Arrange
        val userId = "test_user_123"

        // Act
        val result = cacheManager.getCacheSize(userId)

        // Assert
        assertEquals("getCacheSize should return zero for empty cache", 0L, result)
    }

    @Test
    fun `cleanupTemporaryFiles should succeed`() = runTest {
        // Act
        val result = cacheManager.cleanupTemporaryFiles()

        // Assert
        assertTrue("cleanupTemporaryFiles should succeed", result is CacheResult.Success)
    }

    @Test
    fun `secureCacheOverwrite should succeed`() = runTest {
        // Act
        val result = cacheManager.secureCacheOverwrite()

        // Assert
        assertTrue("secureCacheOverwrite should succeed", result)
    }
}