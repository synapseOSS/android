package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Utility class for file operations
 */
object FileUtil {
    
    /**
     * Returns URI as string for ContentResolver access
     * No longer converts to file path to avoid SecurityException
     */
    fun convertUriToFilePath(context: Context, uri: Uri): String? {
        return uri.toString()
    }
    
    /**
     * Gets file size from URI using ContentResolver
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
