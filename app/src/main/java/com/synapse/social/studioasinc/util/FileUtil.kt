package com.synapse.social.studioasinc.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder

/**
 * Utility class for file operations
 */
object FileUtil {
    
    /**
     * Converts content URI to actual file path or copies to temp file if needed
     */
    fun convertUriToFilePath(context: Context, uri: Uri): String? {
        return when {
            DocumentsContract.isDocumentUri(context, uri) -> {
                when {
                    isExternalStorageDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        val type = split[0]

                        if ("primary".equals(type, ignoreCase = true)) {
                            "${Environment.getExternalStorageDirectory()}/${split[1]}"
                        } else null
                    }
                    isDownloadsDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        val type = split[0]

                        when {
                            "raw".equals(type, ignoreCase = true) -> split[1]
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && "msf".equals(type, ignoreCase = true) -> {
                                val selection = "_id=?"
                                val selectionArgs = arrayOf(split[1])
                                getDataColumn(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)
                            }
                            else -> {
                                try {
                                    val contentUri = ContentUris.withAppendedId(
                                        Uri.parse("content://downloads/public_downloads"),
                                        docId.toLong()
                                    )
                                    getDataColumn(context, contentUri, null, null)
                                } catch (e: Exception) {
                                    // If direct access fails, copy to temp file
                                    copyUriToTempFile(context, uri)
                                }
                            }
                        }
                    }
                    isMediaDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        val type = split[0]

                        val contentUri = when (type) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> null
                        }

                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])
                        getDataColumn(context, contentUri, selection, selectionArgs) 
                            ?: copyUriToTempFile(context, uri)
                    }
                    else -> copyUriToTempFile(context, uri)
                }
            }
            ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true) -> {
                getDataColumn(context, uri, null, null) ?: copyUriToTempFile(context, uri)
            }
            ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true) -> {
                uri.path
            }
            else -> null
        }?.let { path ->
            try {
                URLDecoder.decode(path, "UTF-8")
            } catch (e: Exception) {
                path
            }
        }
    }
    
    /**
     * Copy URI content to a temporary file and return the file path
     */
    private fun copyUriToTempFile(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            // Get file extension from MIME type or URI
            val mimeType = context.contentResolver.getType(uri)
            val extension = when {
                mimeType?.startsWith("image/") == true -> when {
                    mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
                    mimeType.contains("png") -> ".png"
                    mimeType.contains("gif") -> ".gif"
                    mimeType.contains("webp") -> ".webp"
                    else -> ".jpg"
                }
                mimeType?.startsWith("video/") == true -> ".mp4"
                else -> {
                    // Try to get extension from URI path
                    val path = uri.path
                    if (path?.contains(".") == true) {
                        path.substring(path.lastIndexOf("."))
                    } else ".tmp"
                }
            }
            
            // Create temp file
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}$extension")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        uri ?: return null
        
        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf(column)

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    cursor.getString(columnIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
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
