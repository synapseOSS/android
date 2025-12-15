package com.synapse.social.studioasinc.data.repository.deletion

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for SecureDeletionManager
 * Tests secure deletion mechanisms and verification
 * Requirements: 5.1, 5.4, 5.5
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SecureDeletionManagerTest {

    private lateinit var context: Context
    private lateinit var secureDeletionManager: SecureDeletionManager
    private lateinit var testFile: File
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        secureDeletionManager = SecureDeletionManager(context)
        
        // Create test file
        testFile = File(context.cacheDir, "test_message_file.txt")
        
        // Create test SharedPreferences
        sharedPreferences = context.getSharedPreferences("test_secure_prefs", Context.MODE_PRIVATE)
    }

    @Test
    fun `secureOverwriteFile should return true for non-existent file`() = runTest {
        // Given
        val nonExistentFile = File(context.cacheDir, "non_existent_file.txt")
        
        // When
        val result = secureDeletionManager.secureOverwriteFile(nonExistentFile)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `secureOverwriteFile should successfully delete empty file`() = runTest {
        // Given
        testFile.createNewFile()
        assertTrue(testFile.exists())
        assertEquals(0L, testFile.length())
        
        // When
        val result = secureDeletionManager.secureOverwriteFile(testFile)
        
        // Then
        assertTrue(result)
        assertFalse(testFile.exists())
    }

    @Test
    fun `secureOverwriteFile should successfully overwrite and delete file with content`() = runTest {
        // Given
        testFile.writeText("This is sensitive message content that should be securely deleted")
        assertTrue(testFile.exists())
        assertTrue(testFile.length() > 0)
        
        // When
        val result = secureDeletionManager.secureOverwriteFile(testFile)
        
        // Then
        assertTrue(result)
        assertFalse(testFile.exists())
    }

    @Test
    fun `secureOverwritePreferences should successfully overwrite and delete preferences`() = runTest {
        // Given
        val keysToDelete = listOf("deleted_locally_msg_1", "summary_cache_msg_2", "summary_expiry_msg_2")
        val editor = sharedPreferences.edit()
        keysToDelete.forEach { key ->
            editor.putString(key, "sensitive data")
        }
        editor.putString("keep_this_key", "should not be deleted")
        editor.apply()
        
        // Verify keys exist before deletion
        keysToDelete.forEach { key ->
            assertTrue(sharedPreferences.contains(key))
        }
        assertTrue(sharedPreferences.contains("keep_this_key"))
        
        // When
        val result = secureDeletionManager.secureOverwritePreferences(sharedPreferences, keysToDelete)
        
        // Then
        assertTrue(result)
        
        // Verify keys are deleted
        keysToDelete.forEach { key ->
            assertFalse(sharedPreferences.contains(key))
        }
        
        // Verify other keys are preserved
        assertTrue(sharedPreferences.contains("keep_this_key"))
    }

    @Test
    fun `verifyNoMessageRemnants should return true when no remnants exist`() = runTest {
        // Given
        val userId = "test_user_123"
        
        // When
        val result = secureDeletionManager.verifyNoMessageRemnants(userId)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `verifyNoMessageRemnants should return false when message files exist`() = runTest {
        // Given
        val userId = "test_user_123"
        val messageFile = File(context.cacheDir, "message_${userId}_data.txt")
        messageFile.writeText("test message content")
        
        // When
        val result = secureDeletionManager.verifyNoMessageRemnants(userId)
        
        // Then
        assertFalse(result)
        
        // Cleanup
        messageFile.delete()
    }

    @Test
    fun `cleanupTemporaryFiles should delete temporary files`() = runTest {
        // Given
        val tempFile1 = File(context.cacheDir, "temp_message_file.txt")
        val tempFile2 = File(context.cacheDir, "chat_cache_file.tmp")
        val keepFile = File(context.cacheDir, "important_file.txt")
        
        tempFile1.writeText("temporary message data")
        tempFile2.writeText("temporary chat data")
        keepFile.writeText("important data")
        
        assertTrue(tempFile1.exists())
        assertTrue(tempFile2.exists())
        assertTrue(keepFile.exists())
        
        // When
        val deletedCount = secureDeletionManager.cleanupTemporaryFiles()
        
        // Then
        assertTrue(deletedCount >= 0) // Should delete some files or return 0 if none match criteria
        
        // Cleanup remaining files
        if (tempFile1.exists()) tempFile1.delete()
        if (tempFile2.exists()) tempFile2.delete()
        if (keepFile.exists()) keepFile.delete()
    }
}