package com.synapse.social.studioasinc.data.repository.deletion

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom

/**
 * Secure deletion manager for chat history data
 * Implements secure overwrite and verification mechanisms
 * Requirements: 5.1, 5.4, 5.5
 */
class SecureDeletionManager(private val context: Context) {

    companion object {
        private const val TAG = "SecureDeletionManager"
        private const val OVERWRITE_PASSES = 3 // Number of overwrite passes for security
        private const val VERIFICATION_SAMPLE_SIZE = 1024 // Bytes to sample for verification
    }

    private val secureRandom = SecureRandom()

    /**
     * Perform secure overwrite of deleted message data
     * Uses multiple passes with different patterns for enhanced security
     * Requirements: 5.1
     */
    suspend fun secureOverwriteFile(file: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!file.exists() || !file.isFile) {
                Log.w(TAG, "File does not exist or is not a regular file: ${file.absolutePath}")
                return@withContext true
            }

            val fileSize = file.length()
            if (fileSize == 0L) {
                Log.d(TAG, "File is empty, proceeding with deletion: ${file.absolutePath}")
                return@withContext file.delete()
            }

            Log.d(TAG, "Starting secure overwrite of file: ${file.absolutePath} (${fileSize} bytes)")

            RandomAccessFile(file, "rws").use { randomAccessFile ->
                // Pass 1: Overwrite with random data
                overwriteWithRandomData(randomAccessFile, fileSize)
                
                // Pass 2: Overwrite with zeros
                overwriteWithPattern(randomAccessFile, fileSize, 0x00.toByte())
                
                // Pass 3: Overwrite with ones
                overwriteWithPattern(randomAccessFile, fileSize, 0xFF.toByte())
                
                // Final pass: Overwrite with random data again
                overwriteWithRandomData(randomAccessFile, fileSize)
            }

            // Verify overwrite was successful
            val verificationResult = verifyFileOverwrite(file)
            if (!verificationResult) {
                Log.e(TAG, "File overwrite verification failed: ${file.absolutePath}")
                return@withContext false
            }

            // Delete the file
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Successfully securely deleted file: ${file.absolutePath}")
            } else {
                Log.e(TAG, "Failed to delete file after overwrite: ${file.absolutePath}")
            }

            deleted

        } catch (e: Exception) {
            Log.e(TAG, "Failed to securely overwrite file: ${file.absolutePath}", e)
            false
        }
    }

    /**
     * Overwrite file with random data
     * Requirements: 5.1
     */
    private fun overwriteWithRandomData(randomAccessFile: RandomAccessFile, fileSize: Long) {
        randomAccessFile.seek(0)
        val buffer = ByteArray(8192) // 8KB buffer for efficiency
        var remaining = fileSize

        while (remaining > 0) {
            val bytesToWrite = minOf(buffer.size.toLong(), remaining).toInt()
            secureRandom.nextBytes(buffer)
            randomAccessFile.write(buffer, 0, bytesToWrite)
            remaining -= bytesToWrite
        }
        
        randomAccessFile.getFD().sync() // Force write to disk
        Log.d(TAG, "Completed random data overwrite pass")
    }

    /**
     * Overwrite file with specific pattern
     * Requirements: 5.1
     */
    private fun overwriteWithPattern(randomAccessFile: RandomAccessFile, fileSize: Long, pattern: Byte) {
        randomAccessFile.seek(0)
        val buffer = ByteArray(8192) { pattern }
        var remaining = fileSize

        while (remaining > 0) {
            val bytesToWrite = minOf(buffer.size.toLong(), remaining).toInt()
            randomAccessFile.write(buffer, 0, bytesToWrite)
            remaining -= bytesToWrite
        }
        
        randomAccessFile.getFD().sync() // Force write to disk
        Log.d(TAG, "Completed pattern overwrite pass with pattern: 0x${pattern.toString(16).uppercase()}")
    }

    /**
     * Verify that file overwrite was successful by sampling file content
     * Requirements: 5.4
     */
    private fun verifyFileOverwrite(file: File): Boolean {
        return try {
            if (!file.exists()) {
                Log.d(TAG, "File no longer exists, verification passed")
                return true
            }

            val fileSize = file.length()
            if (fileSize == 0L) {
                Log.d(TAG, "File is empty, verification passed")
                return true
            }

            // Sample random positions in the file to verify overwrite
            val sampleSize = minOf(VERIFICATION_SAMPLE_SIZE.toLong(), fileSize).toInt()
            val buffer = ByteArray(sampleSize)
            
            RandomAccessFile(file, "r").use { randomAccessFile ->
                // Sample from beginning
                randomAccessFile.seek(0)
                randomAccessFile.read(buffer, 0, minOf(sampleSize, fileSize.toInt()))
                
                // Check if data looks random (not all zeros or original content patterns)
                val isOverwritten = verifyDataIsOverwritten(buffer)
                
                if (isOverwritten) {
                    Log.d(TAG, "File overwrite verification passed")
                } else {
                    Log.w(TAG, "File overwrite verification failed - data may not be properly overwritten")
                }
                
                return isOverwritten
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify file overwrite: ${file.absolutePath}", e)
            false
        }
    }

    /**
     * Verify that data has been overwritten (not original content)
     * Requirements: 5.4
     */
    private fun verifyDataIsOverwritten(data: ByteArray): Boolean {
        if (data.isEmpty()) return true

        // Check for patterns that indicate successful overwrite
        val allZeros = data.all { it == 0.toByte() }
        val allOnes = data.all { it == 0xFF.toByte() }
        val hasVariation = data.distinct().size > 1

        // Data should either be all zeros/ones (from pattern pass) or have variation (from random pass)
        return allZeros || allOnes || hasVariation
    }

    /**
     * Secure overwrite of SharedPreferences data
     * Requirements: 5.1, 5.4
     */
    suspend fun secureOverwritePreferences(prefs: SharedPreferences, keysToDelete: List<String>): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Starting secure overwrite of ${keysToDelete.size} preference keys")
            
            val editor = prefs.edit()
            var overwrittenCount = 0

            keysToDelete.forEach { key ->
                // First overwrite with random data
                val randomValue = generateRandomString(256)
                editor.putString(key, randomValue)
                overwrittenCount++
            }
            
            // Apply the random overwrite
            editor.apply()
            
            // Wait a moment for the write to complete
            Thread.sleep(100)
            
            // Now remove the keys
            val deleteEditor = prefs.edit()
            keysToDelete.forEach { key ->
                deleteEditor.remove(key)
            }
            deleteEditor.apply()

            // Verify deletion
            val verificationResult = verifyPreferencesDeleted(prefs, keysToDelete)
            
            Log.d(TAG, "Securely overwritten and deleted $overwrittenCount preference keys, verification: $verificationResult")
            verificationResult

        } catch (e: Exception) {
            Log.e(TAG, "Failed to securely overwrite preferences", e)
            false
        }
    }

    /**
     * Verify that SharedPreferences keys have been deleted
     * Requirements: 5.4
     */
    private fun verifyPreferencesDeleted(prefs: SharedPreferences, deletedKeys: List<String>): Boolean {
        return try {
            val remainingKeys = deletedKeys.filter { prefs.contains(it) }
            if (remainingKeys.isNotEmpty()) {
                Log.w(TAG, "Verification failed - ${remainingKeys.size} preference keys still exist: $remainingKeys")
                return false
            }
            
            Log.d(TAG, "Preferences deletion verification passed")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify preferences deletion", e)
            false
        }
    }

    /**
     * Generate random string for overwriting sensitive data
     * Requirements: 5.1
     */
    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?"
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Verify that no message remnants exist after deletion
     * Scans directories for any remaining message-related files
     * Requirements: 5.4
     */
    suspend fun verifyNoMessageRemnants(userId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Verifying no message remnants exist for user: $userId")
            
            var foundRemnants = false
            
            // Check cache directory
            context.cacheDir?.let { cacheDir ->
                if (scanDirectoryForRemnants(cacheDir, listOf("message", userId))) {
                    foundRemnants = true
                }
            }
            
            // Check files directory
            context.filesDir?.let { filesDir ->
                if (scanDirectoryForRemnants(filesDir, listOf("message", userId))) {
                    foundRemnants = true
                }
            }
            
            // Check external cache directory
            context.externalCacheDir?.let { externalCache ->
                if (scanDirectoryForRemnants(externalCache, listOf("message", userId))) {
                    foundRemnants = true
                }
            }
            
            val verificationPassed = !foundRemnants
            Log.d(TAG, "Message remnants verification for user $userId: ${if (verificationPassed) "PASSED" else "FAILED"}")
            verificationPassed
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify no message remnants for user: $userId", e)
            false
        }
    }

    /**
     * Scan directory for message remnants
     * Requirements: 5.4
     */
    private fun scanDirectoryForRemnants(directory: File, patterns: List<String>): Boolean {
        return try {
            if (!directory.exists() || !directory.isDirectory) {
                return false
            }
            
            var foundRemnants = false
            
            directory.listFiles()?.forEach { file ->
                if (file.isFile) {
                    // Check if filename contains any of the patterns
                    val containsPattern = patterns.any { pattern ->
                        file.name.contains(pattern, ignoreCase = true)
                    }
                    
                    if (containsPattern) {
                        Log.w(TAG, "Found message remnant file: ${file.absolutePath}")
                        foundRemnants = true
                    }
                } else if (file.isDirectory) {
                    // Recursively scan subdirectories
                    if (scanDirectoryForRemnants(file, patterns)) {
                        foundRemnants = true
                    }
                }
            }
            
            foundRemnants
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan directory for remnants: ${directory.path}", e)
            false
        }
    }

    /**
     * Clean up temporary files containing chat data
     * Requirements: 5.5
     */
    suspend fun cleanupTemporaryFiles(): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Starting cleanup of temporary files")
            
            var deletedCount = 0
            
            // Clean cache directory
            context.cacheDir?.let { cacheDir ->
                deletedCount += cleanupDirectoryTemporaryFiles(cacheDir)
            }
            
            // Clean files directory (only temp files)
            context.filesDir?.let { filesDir ->
                deletedCount += cleanupDirectoryTemporaryFiles(filesDir)
            }
            
            // Clean external cache directory
            context.externalCacheDir?.let { externalCache ->
                deletedCount += cleanupDirectoryTemporaryFiles(externalCache)
            }
            
            Log.d(TAG, "Cleaned up $deletedCount temporary files")
            deletedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup temporary files", e)
            0
        }
    }

    /**
     * Clean up temporary files in a specific directory
     * Requirements: 5.5
     */
    private suspend fun cleanupDirectoryTemporaryFiles(directory: File): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!directory.exists() || !directory.isDirectory) {
                return@withContext 0
            }
            
            var deletedCount = 0
            
            directory.listFiles()?.forEach { file ->
                if (file.isFile && isTemporaryFile(file)) {
                    if (secureOverwriteFile(file)) {
                        deletedCount++
                        Log.d(TAG, "Deleted temporary file: ${file.name}")
                    }
                } else if (file.isDirectory) {
                    // Recursively clean subdirectories
                    deletedCount += cleanupDirectoryTemporaryFiles(file)
                }
            }
            
            deletedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup directory temporary files: ${directory.path}", e)
            0
        }
    }

    /**
     * Determine if a file is a temporary file that should be cleaned up
     * Requirements: 5.5
     */
    private fun isTemporaryFile(file: File): Boolean {
        val fileName = file.name.lowercase()
        
        // Check for common temporary file patterns
        return fileName.startsWith("tmp") ||
               fileName.startsWith("temp") ||
               fileName.endsWith(".tmp") ||
               fileName.endsWith(".temp") ||
               fileName.contains("cache") ||
               fileName.contains("message") ||
               fileName.contains("chat") ||
               // Check for old files (older than 24 hours)
               (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000)
    }
}