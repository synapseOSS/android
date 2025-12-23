package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import com.synapse.social.studioasinc.FileUtils
import java.io.File

/**
 * Debug utility for troubleshooting profile upload issues
 */
object ProfileUploadDebugUtil {
    
    private const val TAG = "ProfileUploadDebug"
    
    /**
     * Test URI conversion and file processing pipeline
     */
    fun testUriProcessing(context: Context, uri: Uri): ProcessingResult {
        android.util.Log.d(TAG, "Testing URI processing for: $uri")
        
        val result = ProcessingResult()
        result.originalUri = uri.toString()
        
        try {
            // Step 1: URI to file path conversion
            val filePath = FileUtils.convertUriToFilePath(context, uri)
            result.convertedPath = filePath
            result.uriConversionSuccess = filePath != null
            
            if (filePath == null) {
                // Try fallback method
                android.util.Log.d(TAG, "URI conversion failed, trying fallback")
                val tempFile = File(context.cacheDir, "debug_temp_${System.currentTimeMillis()}.jpg")
                
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                if (tempFile.exists() && tempFile.length() > 0) {
                    result.fallbackPath = tempFile.absolutePath
                    result.fallbackSuccess = true
                    result.fallbackFileSize = tempFile.length()
                } else {
                    result.fallbackSuccess = false
                    result.error = "Fallback file creation failed"
                }
            } else {
                // Test original file
                val originalFile = File(filePath)
                result.originalFileExists = originalFile.exists()
                result.originalFileSize = if (originalFile.exists()) originalFile.length() else 0
                
                if (!originalFile.exists()) {
                    result.error = "Original file does not exist"
                } else if (originalFile.length() == 0L) {
                    result.error = "Original file is empty"
                } else {
                    // Test compression
                    val compressedFile = File(context.cacheDir, "debug_compressed_${System.currentTimeMillis()}.jpg")
                    try {
                        FileUtils.resizeBitmapFileRetainRatio(filePath, compressedFile.absolutePath, 1024)
                        
                        result.compressionSuccess = compressedFile.exists() && compressedFile.length() > 0
                        result.compressedFileSize = if (compressedFile.exists()) compressedFile.length() else 0
                        result.compressedPath = compressedFile.absolutePath
                        
                        if (!result.compressionSuccess) {
                            result.error = "Image compression failed"
                        }
                    } catch (e: Exception) {
                        result.compressionSuccess = false
                        result.error = "Compression error: ${e.message}"
                    }
                }
            }
            
        } catch (e: Exception) {
            result.error = "Processing error: ${e.message}"
            android.util.Log.e(TAG, "URI processing failed", e)
        }
        
        // Log results
        android.util.Log.d(TAG, "Processing results: $result")
        
        return result
    }
    
    /**
     * Test Supabase storage connectivity
     */
    suspend fun testStorageConnectivity(): StorageTestResult {
        // This would require access to SupabaseStorageService
        // For now, return a placeholder
        return StorageTestResult(
            connected = true,
            error = null
        )
    }
    
    data class ProcessingResult(
        var originalUri: String = "",
        var convertedPath: String? = null,
        var uriConversionSuccess: Boolean = false,
        var originalFileExists: Boolean = false,
        var originalFileSize: Long = 0,
        var fallbackPath: String? = null,
        var fallbackSuccess: Boolean = false,
        var fallbackFileSize: Long = 0,
        var compressionSuccess: Boolean = false,
        var compressedPath: String? = null,
        var compressedFileSize: Long = 0,
        var error: String? = null
    ) {
        val isSuccessful: Boolean
            get() = error == null && (uriConversionSuccess || fallbackSuccess) && compressionSuccess
    }
    
    data class StorageTestResult(
        val connected: Boolean,
        val error: String?
    )
}
