/**
 * CONFIDENTIAL AND PROPRIETARY
 *
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 *
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc.core.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Modern utility class for downloading and saving media files using MediaStore API.
 * Replaces deprecated DownloadManager external storage methods.
 */
object MediaStorageUtils {

    private const val TAG = "MediaStorageUtils"
    private val executor: ExecutorService = Executors.newFixedThreadPool(3)

    /**
     * Callback interface for download operations.
     */
    interface DownloadCallback {
        fun onSuccess(savedUri: Uri, fileName: String)
        fun onProgress(progress: Int)
        fun onError(error: String)
    }

    /**
     * Helper class to store file information.
     */
    private data class FileInfo(
        val fileName: String,
        val mimeType: String,
        val extension: String
    )

    /**
     * Downloads an image from URL and saves it to the device's Pictures directory
     * using the modern MediaStore API.
     *
     * @param context The context
     * @param imageUrl The URL of the image to download
     * @param fileName The desired filename (without extension)
     * @param callback Callback to handle success/error
     */
    fun downloadImage(context: Context?, imageUrl: String?, fileName: String?, callback: DownloadCallback?) {
        if (context == null || imageUrl.isNullOrEmpty()) {
            callback?.onError("Invalid parameters")
            return
        }

        executor.execute {
            try {
                // Detect file extension and mime type from URL or content
                val fileInfo = detectImageFileInfo(imageUrl, fileName ?: "image")
                val finalFileName = fileInfo.fileName
                val mimeType = fileInfo.mimeType

                // Create content values for MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // For Android 10+ (API 29+), use relative path
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Synapse")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    } else {
                        // For older versions, use DATA column (deprecated but necessary)
                        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                        @Suppress("DEPRECATION")
                        put(MediaStore.Images.Media.DATA, "$picturesDir/Synapse/$fileName")
                    }
                }

                val resolver = context.contentResolver
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (imageUri == null) {
                    callback?.onError("Failed to create media entry")
                    return@execute
                }

                // Download and save the image
                try {
                    resolver.openOutputStream(imageUri)?.use { outputStream ->
                        downloadToStream(imageUrl, outputStream, callback)

                        // Mark as not pending (for Android 10+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(imageUri, contentValues, null, null)
                        }

                        callback?.onSuccess(imageUri, fileName ?: "image")
                    } ?: throw IOException("Failed to open output stream")
                } catch (e: Exception) {
                    // Clean up failed entry
                    resolver.delete(imageUri, null, null)
                    throw e
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image: " + e.message, e)
                callback?.onError("Download failed: " + e.message)
            }
        }
    }

    /**
     * Downloads a video from URL and saves it to the device's Movies directory.
     *
     * @param context The context
     * @param videoUrl The URL of the video to download
     * @param fileName The desired filename (without extension)
     * @param callback Callback to handle success/error
     */
    fun downloadVideo(context: Context?, videoUrl: String?, fileName: String?, callback: DownloadCallback?) {
        if (context == null || videoUrl.isNullOrEmpty()) {
            callback?.onError("Invalid parameters")
            return
        }

        executor.execute {
            try {
                // Detect file extension and mime type from URL or content
                val fileInfo = detectVideoFileInfo(videoUrl, fileName ?: "video")
                val finalFileName = fileInfo.fileName
                val mimeType = fileInfo.mimeType

                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Synapse")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    } else {
                        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
                        @Suppress("DEPRECATION")
                        put(MediaStore.Video.Media.DATA, "$moviesDir/Synapse/$fileName")
                    }
                }

                val resolver = context.contentResolver
                val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (videoUri == null) {
                    callback?.onError("Failed to create media entry")
                    return@execute
                }

                try {
                    resolver.openOutputStream(videoUri)?.use { outputStream ->
                        downloadToStream(videoUrl, outputStream, callback)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                            resolver.update(videoUri, contentValues, null, null)
                        }

                        callback?.onSuccess(videoUri, fileName ?: "video")
                    } ?: throw IOException("Failed to open output stream")
                } catch (e: Exception) {
                    resolver.delete(videoUri, null, null)
                    throw e
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error downloading video: " + e.message, e)
                callback?.onError("Download failed: " + e.message)
            }
        }
    }

    /**
     * Downloads content from URL to the provided OutputStream with progress reporting.
     *
     * @param urlString The URL to download from
     * @param outputStream The OutputStream to write to
     * @param callback Callback for progress updates
     * @throws IOException If download fails
     */
    @Throws(IOException::class)
    private fun downloadToStream(urlString: String, outputStream: OutputStream, callback: DownloadCallback?) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server response: " + connection.responseCode + " " + connection.responseMessage)
            }

            val fileLength = connection.contentLength

            connection.inputStream.use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    // Report progress
                    if (callback != null && fileLength > 0) {
                        val progress = (totalBytesRead * 100L / fileLength).toInt()
                        callback.onProgress(progress)
                    }
                }

                outputStream.flush()
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Detects file extension and mime type from image URL.
     *
     * @param imageUrl The image URL
     * @param baseFileName The base filename without extension
     * @return FileInfo with detected information
     */
    private fun detectImageFileInfo(imageUrl: String, baseFileName: String): FileInfo {
        var extension = ".jpg" // Default
        var mimeType = "image/jpeg" // Default

        try {
            // First, try to detect from URL
            val urlLower = imageUrl.lowercase()
            if (urlLower.contains(".png")) {
                extension = ".png"
                mimeType = "image/png"
            } else if (urlLower.contains(".gif")) {
                extension = ".gif"
                mimeType = "image/gif"
            } else if (urlLower.contains(".webp")) {
                extension = ".webp"
                mimeType = "image/webp"
            } else if (urlLower.contains(".bmp")) {
                extension = ".bmp"
                mimeType = "image/bmp"
            } else if (urlLower.contains(".jpeg") || urlLower.contains(".jpg")) {
                extension = ".jpg"
                mimeType = "image/jpeg"
            }

            // Try to get more accurate info from URL path
            val uri = Uri.parse(imageUrl)
            val path = uri.path
            if (path != null) {
                val lastDot = path.lastIndexOf('.')
                if (lastDot > 0 && lastDot < path.length - 1) {
                    val urlExtension = path.substring(lastDot).lowercase()
                    val detectedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(urlExtension.substring(1))
                    if (detectedMimeType != null && detectedMimeType.startsWith("image/")) {
                        extension = urlExtension
                        mimeType = detectedMimeType
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error detecting file type from URL, using defaults: " + e.message)
        }

        // Ensure filename has the correct extension
        var finalFileName = baseFileName
        if (!finalFileName.lowercase().endsWith(extension.lowercase())) {
            finalFileName += extension
        }

        return FileInfo(finalFileName, mimeType, extension)
    }

    /**
     * Detects file extension and mime type from video URL.
     *
     * @param videoUrl The video URL
     * @param baseFileName The base filename without extension
     * @return FileInfo with detected information
     */
    private fun detectVideoFileInfo(videoUrl: String, baseFileName: String): FileInfo {
        var extension = ".mp4" // Default
        var mimeType = "video/mp4" // Default

        try {
            // Try to detect from URL
            val urlLower = videoUrl.lowercase()
            if (urlLower.contains(".mov")) {
                extension = ".mov"
                mimeType = "video/quicktime"
            } else if (urlLower.contains(".avi")) {
                extension = ".avi"
                mimeType = "video/x-msvideo"
            } else if (urlLower.contains(".mkv")) {
                extension = ".mkv"
                mimeType = "video/x-matroska"
            } else if (urlLower.contains(".webm")) {
                extension = ".webm"
                mimeType = "video/webm"
            } else if (urlLower.contains(".mp4")) {
                extension = ".mp4"
                mimeType = "video/mp4"
            }

            // Try to get more accurate info from URL path
            val uri = Uri.parse(videoUrl)
            val path = uri.path
            if (path != null) {
                val lastDot = path.lastIndexOf('.')
                if (lastDot > 0 && lastDot < path.length - 1) {
                    val urlExtension = path.substring(lastDot).lowercase()
                    val detectedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(urlExtension.substring(1))
                    if (detectedMimeType != null && detectedMimeType.startsWith("video/")) {
                        extension = urlExtension
                        mimeType = detectedMimeType
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error detecting file type from URL, using defaults: " + e.message)
        }

        // Ensure filename has the correct extension
        var finalFileName = baseFileName
        if (!finalFileName.lowercase().endsWith(extension.lowercase())) {
            finalFileName += extension
        }

        return FileInfo(finalFileName, mimeType, extension)
    }

    /**
     * Shuts down the executor service. Call this when the application is terminating.
     */
    fun shutdown() {
        executor.shutdown()
    }
}
