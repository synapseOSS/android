package com.synapse.social.studioasinc.data.repository.deletion

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Implementation of ChatCacheManager for managing cached chat data
 * Handles cache clearing, secure overwrite, and verification operations
 * Requirements: 1.3, 2.3, 5.3, 5.4
 */
class ChatCacheManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ChatCacheManager {

    companion object {
        private const val TAG = "ChatCacheManagerImpl"
        private const val PREFS_NAME = "message_actions_prefs"
        private const val CHAT_CACHE_DIR = "chat_cache"
        private const val TEMP_FILES_DIR = "temp_chat_files"
        private const val SUMMARY_CACHE_PREFIX = "summary_cache_"
        private const val SUMMARY_EXPIRY_PREFIX = "summary_expiry_"
        private const val DELETED_LOCALLY_PREFIX = "deleted_locally_"
        private const val OVERWRITE_PASSES = 3 // Number of secure overwrite passes
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, CHAT_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    private val tempFilesDir: File by lazy {
        File(context.cacheDir, TEMP_FILES_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Clear all cached chat data for a user
     * Requirements: 1.3
     */
    override suspend fun clearAllCache(userId: String): CacheResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting clearAllCache for user: $userId")
            
            // Clear SharedPreferences entries for the user
            val editor = sharedPreferences.edit()
            val allPrefs = sharedPreferences.all
            
            var clearedCount = 0
            for ((key, _) in allPrefs) {
                if (key.contains(userId) || 
                    key.startsWith(SUMMARY_CACHE_PREFIX) ||
                    key.startsWith(SUMMARY_EXPIRY_PREFIX) ||
                    key.startsWith(DELETED_LOCALLY_PREFIX)) {
                    editor.remove(key)
                    clearedCount++
                }
            }
            editor.apply()
            
            // Clear cache files for the user
            val userCacheDir = File(cacheDir, userId)
            if (userCacheDir.exists()) {
                val filesDeleted = deleteDirectorySecurely(userCacheDir)
                Log.d(TAG, "Deleted $filesDeleted cache files for user $userId")
            }
            
            // Clear any user-specific temporary files
            clearUserTempFiles(userId)
            
            Log.d(TAG, "Successfully cleared all cache for user $userId. Cleared $clearedCount preferences")
            CacheResult.Success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all cache for user $userId", e)
            CacheResult.Failure("Failed to clear cache: ${e.message}")
        }
    }

    /**
     * Clear cached data for specific chats
     * Requirements: 2.3
     */
    override suspend fun clearCacheForChats(chatIds: List<String>): CacheResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting clearCacheForChats for ${chatIds.size} chats")
            
            // Clear SharedPreferences entries for specific chats
            val editor = sharedPreferences.edit()
            val allPrefs = sharedPreferences.all
            
            var clearedCount = 0
            for ((key, _) in allPrefs) {
                for (chatId in chatIds) {
                    if (key.contains(chatId)) {
                        editor.remove(key)
                        clearedCount++
                        break
                    }
                }
            }
            editor.apply()
            
            // Clear cache files for specific chats
            var totalFilesDeleted = 0
            for (chatId in chatIds) {
                val chatCacheFiles = findCacheFilesForChat(chatId)
                for (file in chatCacheFiles) {
                    if (deleteFileSecurely(file)) {
                        totalFilesDeleted++
                    }
                }
            }
            
            // Clear temporary files for specific chats
            clearChatTempFiles(chatIds)
            
            Log.d(TAG, "Successfully cleared cache for ${chatIds.size} chats. Cleared $clearedCount preferences, $totalFilesDeleted files")
            CacheResult.Success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache for chats: $chatIds", e)
            CacheResult.Failure("Failed to clear chat cache: ${e.message}")
        }
    }

    /**
     * Perform secure overwrite of cached data to prevent recovery
     * Requirements: 5.3
     */
    override suspend fun secureCacheOverwrite(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting secure cache overwrite")
            
            var success = true
            
            // Secure overwrite of all cache files
            val allCacheFiles = getAllCacheFiles()
            for (file in allCacheFiles) {
                if (!overwriteFileSecurely(file)) {
                    success = false
                    Log.w(TAG, "Failed to securely overwrite file: ${file.absolutePath}")
                }
            }
            
            // Secure overwrite of temporary files
            val allTempFiles = getAllTempFiles()
            for (file in allTempFiles) {
                if (!overwriteFileSecurely(file)) {
                    success = false
                    Log.w(TAG, "Failed to securely overwrite temp file: ${file.absolutePath}")
                }
            }
            
            // Clear SharedPreferences (Android handles this securely)
            sharedPreferences.edit().clear().apply()
            
            Log.d(TAG, "Secure cache overwrite completed. Success: $success")
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform secure cache overwrite", e)
            false
        }
    }

    /**
     * Verify that cache was cleared successfully
     * Requirements: 5.4
     */
    override suspend fun verifyCacheCleared(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check SharedPreferences for any remaining user data
            val allPrefs = sharedPreferences.all
            val hasUserPrefs = allPrefs.keys.any { key ->
                key.contains(userId) || 
                key.startsWith(SUMMARY_CACHE_PREFIX) ||
                key.startsWith(SUMMARY_EXPIRY_PREFIX) ||
                key.startsWith(DELETED_LOCALLY_PREFIX)
            }
            
            if (hasUserPrefs) {
                Log.w(TAG, "Found remaining preferences for user $userId")
                return@withContext false
            }
            
            // Check for remaining cache files
            val userCacheDir = File(cacheDir, userId)
            if (userCacheDir.exists() && userCacheDir.listFiles()?.isNotEmpty() == true) {
                Log.w(TAG, "Found remaining cache files for user $userId")
                return@withContext false
            }
            
            // Check for remaining temporary files
            val userTempFiles = findTempFilesForUser(userId)
            if (userTempFiles.isNotEmpty()) {
                Log.w(TAG, "Found remaining temp files for user $userId")
                return@withContext false
            }
            
            Log.d(TAG, "Cache verification successful for user $userId")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify cache cleared for user $userId", e)
            false
        }
    }

    /**
     * Verify that specific chat caches were cleared
     * Requirements: 5.4
     */
    override suspend fun verifyChatCachesCleared(chatIds: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check SharedPreferences for any remaining chat data
            val allPrefs = sharedPreferences.all
            val haschatPrefs = allPrefs.keys.any { key ->
                chatIds.any { chatId -> key.contains(chatId) }
            }
            
            if (haschatPrefs) {
                Log.w(TAG, "Found remaining preferences for chats: $chatIds")
                return@withContext false
            }
            
            // Check for remaining cache files
            for (chatId in chatIds) {
                val chatCacheFiles = findCacheFilesForChat(chatId)
                if (chatCacheFiles.isNotEmpty()) {
                    Log.w(TAG, "Found remaining cache files for chat $chatId")
                    return@withContext false
                }
            }
            
            // Check for remaining temporary files
            val chatTempFiles = findTempFilesForChats(chatIds)
            if (chatTempFiles.isNotEmpty()) {
                Log.w(TAG, "Found remaining temp files for chats: $chatIds")
                return@withContext false
            }
            
            Log.d(TAG, "Cache verification successful for chats: $chatIds")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify cache cleared for chats: $chatIds", e)
            false
        }
    }

    /**
     * Clean up temporary files containing chat data
     * Requirements: 5.5
     */
    override suspend fun cleanupTemporaryFiles(): CacheResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting cleanup of temporary files")
            
            var filesDeleted = 0
            val tempFiles = getAllTempFiles()
            
            for (file in tempFiles) {
                if (deleteFileSecurely(file)) {
                    filesDeleted++
                }
            }
            
            // Also clean up any orphaned cache files
            val orphanedFiles = findOrphanedCacheFiles()
            for (file in orphanedFiles) {
                if (deleteFileSecurely(file)) {
                    filesDeleted++
                }
            }
            
            Log.d(TAG, "Cleanup completed. Deleted $filesDeleted temporary files")
            CacheResult.Success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup temporary files", e)
            CacheResult.Failure("Failed to cleanup temporary files: ${e.message}")
        }
    }

    /**
     * Get size of cached data for progress estimation
     * Requirements: 6.1, 6.2
     */
    override suspend fun getCacheSize(userId: String): Long = withContext(Dispatchers.IO) {
        try {
            var totalSize = 0L
            
            // Calculate size of user cache directory
            val userCacheDir = File(cacheDir, userId)
            if (userCacheDir.exists()) {
                totalSize += calculateDirectorySize(userCacheDir)
            }
            
            // Calculate size of user temp files
            val userTempFiles = findTempFilesForUser(userId)
            totalSize += userTempFiles.sumOf { it.length() }
            
            Log.d(TAG, "Cache size for user $userId: $totalSize bytes")
            totalSize
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate cache size for user $userId", e)
            0L
        }
    }

    /**
     * Get size of cached data for specific chats
     * Requirements: 6.1, 6.2
     */
    override suspend fun getCacheSizeForChats(chatIds: List<String>): Long = withContext(Dispatchers.IO) {
        try {
            var totalSize = 0L
            
            for (chatId in chatIds) {
                // Calculate size of chat cache files
                val chatCacheFiles = findCacheFilesForChat(chatId)
                totalSize += chatCacheFiles.sumOf { it.length() }
            }
            
            // Calculate size of chat temp files
            val chatTempFiles = findTempFilesForChats(chatIds)
            totalSize += chatTempFiles.sumOf { it.length() }
            
            Log.d(TAG, "Cache size for chats $chatIds: $totalSize bytes")
            totalSize
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate cache size for chats: $chatIds", e)
            0L
        }
    }

    // Private helper methods

    private fun deleteDirectorySecurely(directory: File): Int {
        var deletedCount = 0
        if (!directory.exists()) return deletedCount
        
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deletedCount += deleteDirectorySecurely(file)
            } else {
                if (deleteFileSecurely(file)) {
                    deletedCount++
                }
            }
        }
        
        directory.delete()
        return deletedCount
    }

    private fun deleteFileSecurely(file: File): Boolean {
        return try {
            if (!file.exists()) return true
            
            // First overwrite the file securely
            overwriteFileSecurely(file)
            
            // Then delete it
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file securely: ${file.absolutePath}", e)
            false
        }
    }

    private fun overwriteFileSecurely(file: File): Boolean {
        return try {
            if (!file.exists() || !file.isFile) return true
            
            val fileSize = file.length()
            if (fileSize == 0L) return true
            
            RandomAccessFile(file, "rws").use { raf ->
                val random = SecureRandom()
                val buffer = ByteArray(8192) // 8KB buffer
                
                // Perform multiple overwrite passes
                repeat(OVERWRITE_PASSES) {
                    raf.seek(0)
                    var remaining = fileSize
                    
                    while (remaining > 0) {
                        val bytesToWrite = minOf(buffer.size.toLong(), remaining).toInt()
                        random.nextBytes(buffer)
                        raf.write(buffer, 0, bytesToWrite)
                        remaining -= bytesToWrite
                    }
                    
                    raf.fd.sync() // Force write to disk
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to overwrite file securely: ${file.absolutePath}", e)
            false
        }
    }

    private fun getAllCacheFiles(): List<File> {
        val files = mutableListOf<File>()
        if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.forEach { files.add(it) }
        }
        return files
    }

    private fun getAllTempFiles(): List<File> {
        val files = mutableListOf<File>()
        if (tempFilesDir.exists()) {
            tempFilesDir.walkTopDown().filter { it.isFile }.forEach { files.add(it) }
        }
        return files
    }

    private fun findCacheFilesForChat(chatId: String): List<File> {
        val files = mutableListOf<File>()
        if (cacheDir.exists()) {
            cacheDir.walkTopDown()
                .filter { it.isFile && it.name.contains(chatId) }
                .forEach { files.add(it) }
        }
        return files
    }

    private fun findTempFilesForUser(userId: String): List<File> {
        val files = mutableListOf<File>()
        if (tempFilesDir.exists()) {
            tempFilesDir.walkTopDown()
                .filter { it.isFile && it.name.contains(userId) }
                .forEach { files.add(it) }
        }
        return files
    }

    private fun findTempFilesForChats(chatIds: List<String>): List<File> {
        val files = mutableListOf<File>()
        if (tempFilesDir.exists()) {
            tempFilesDir.walkTopDown()
                .filter { file -> 
                    file.isFile && chatIds.any { chatId -> file.name.contains(chatId) }
                }
                .forEach { files.add(it) }
        }
        return files
    }

    private fun findOrphanedCacheFiles(): List<File> {
        val files = mutableListOf<File>()
        if (cacheDir.exists()) {
            // Find files older than 24 hours that might be orphaned
            val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours
            cacheDir.walkTopDown()
                .filter { it.isFile && it.lastModified() < cutoffTime }
                .forEach { files.add(it) }
        }
        return files
    }

    private fun clearUserTempFiles(userId: String) {
        val userTempFiles = findTempFilesForUser(userId)
        userTempFiles.forEach { deleteFileSecurely(it) }
    }

    private fun clearChatTempFiles(chatIds: List<String>) {
        val chatTempFiles = findTempFilesForChats(chatIds)
        chatTempFiles.forEach { deleteFileSecurely(it) }
    }

    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists() && directory.isDirectory) {
            directory.walkTopDown()
                .filter { it.isFile }
                .forEach { size += it.length() }
        }
        return size
    }

    /**
     * Perform comprehensive cache integrity check
     * Requirements: 5.4
     */
    suspend fun performCacheIntegrityCheck(): CacheIntegrityResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting cache integrity check")
            
            val issues = mutableListOf<String>()
            
            // Check for corrupted cache files
            val allCacheFiles = getAllCacheFiles()
            for (file in allCacheFiles) {
                if (!file.canRead()) {
                    issues.add("Cannot read cache file: ${file.absolutePath}")
                } else if (file.length() == 0L && file.name.endsWith(".cache")) {
                    // Empty cache files might indicate corruption
                    issues.add("Empty cache file detected: ${file.absolutePath}")
                }
            }
            
            // Check for orphaned temporary files
            val orphanedFiles = findOrphanedCacheFiles()
            if (orphanedFiles.isNotEmpty()) {
                issues.add("Found ${orphanedFiles.size} orphaned cache files")
            }
            
            // Check SharedPreferences consistency
            val allPrefs = sharedPreferences.all
            val inconsistentPrefs = allPrefs.filter { (key, value) ->
                // Check for malformed preference keys or values
                key.isBlank() || (value is String && value.isBlank())
            }
            
            if (inconsistentPrefs.isNotEmpty()) {
                issues.add("Found ${inconsistentPrefs.size} inconsistent preferences")
            }
            
            // Check cache directory permissions
            if (!cacheDir.canWrite()) {
                issues.add("Cache directory is not writable: ${cacheDir.absolutePath}")
            }
            
            if (!tempFilesDir.canWrite()) {
                issues.add("Temp files directory is not writable: ${tempFilesDir.absolutePath}")
            }
            
            Log.d(TAG, "Cache integrity check completed. Found ${issues.size} issues")
            
            if (issues.isEmpty()) {
                CacheIntegrityResult.Healthy
            } else {
                CacheIntegrityResult.IssuesFound(issues)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform cache integrity check", e)
            CacheIntegrityResult.CheckFailed("Integrity check failed: ${e.message}")
        }
    }

    /**
     * Repair cache integrity issues
     * Requirements: 5.4, 5.5
     */
    suspend fun repairCacheIntegrity(): CacheResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting cache integrity repair")
            
            var repairedCount = 0
            
            // Remove corrupted cache files
            val allCacheFiles = getAllCacheFiles()
            for (file in allCacheFiles) {
                if (!file.canRead() || (file.length() == 0L && file.name.endsWith(".cache"))) {
                    if (deleteFileSecurely(file)) {
                        repairedCount++
                        Log.d(TAG, "Removed corrupted cache file: ${file.absolutePath}")
                    }
                }
            }
            
            // Clean up orphaned files
            val orphanedFiles = findOrphanedCacheFiles()
            for (file in orphanedFiles) {
                if (deleteFileSecurely(file)) {
                    repairedCount++
                }
            }
            
            // Clean up inconsistent preferences
            val editor = sharedPreferences.edit()
            val allPrefs = sharedPreferences.all
            for ((key, value) in allPrefs) {
                if (key.isBlank() || (value is String && value.isBlank())) {
                    editor.remove(key)
                    repairedCount++
                }
            }
            editor.apply()
            
            Log.d(TAG, "Cache integrity repair completed. Repaired $repairedCount issues")
            CacheResult.Success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to repair cache integrity", e)
            CacheResult.Failure("Cache repair failed: ${e.message}")
        }
    }

    /**
     * Get detailed cache statistics for monitoring
     * Requirements: 6.1, 6.2
     */
    suspend fun getCacheStatistics(): CacheStatistics = withContext(Dispatchers.IO) {
        try {
            val allCacheFiles = getAllCacheFiles()
            val allTempFiles = getAllTempFiles()
            val totalPrefs = sharedPreferences.all.size
            
            val totalCacheSize = allCacheFiles.sumOf { it.length() }
            val totalTempSize = allTempFiles.sumOf { it.length() }
            
            CacheStatistics(
                totalCacheFiles = allCacheFiles.size,
                totalTempFiles = allTempFiles.size,
                totalPreferences = totalPrefs,
                totalCacheSize = totalCacheSize,
                totalTempSize = totalTempSize,
                cacheDirectoryPath = cacheDir.absolutePath,
                tempDirectoryPath = tempFilesDir.absolutePath
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache statistics", e)
            CacheStatistics(0, 0, 0, 0L, 0L, "", "")
        }
    }
}

/**
 * Result of cache integrity check
 * Requirements: 5.4
 */
sealed class CacheIntegrityResult {
    object Healthy : CacheIntegrityResult()
    data class IssuesFound(val issues: List<String>) : CacheIntegrityResult()
    data class CheckFailed(val error: String) : CacheIntegrityResult()
}

/**
 * Cache statistics for monitoring and debugging
 * Requirements: 6.1, 6.2
 */
data class CacheStatistics(
    val totalCacheFiles: Int,
    val totalTempFiles: Int,
    val totalPreferences: Int,
    val totalCacheSize: Long,
    val totalTempSize: Long,
    val cacheDirectoryPath: String,
    val tempDirectoryPath: String
)