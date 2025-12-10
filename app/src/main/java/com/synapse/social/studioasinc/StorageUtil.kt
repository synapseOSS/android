/*
 * MIT License
 *
 * Copyright (c) 2025 StudioAs Inc. & Synapse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.synapse.social.studioasinc

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object StorageUtil {

    private const val TAG = "StorageUtil"
    private const val BUFFER_SIZE = 8192

    fun pickSingleFile(activity: Activity, mimeType: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        activity.startActivityForResult(intent, requestCode)
    }

    fun pickMultipleFiles(activity: Activity, mimeType: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        activity.startActivityForResult(Intent.createChooser(intent, "Select Files"), requestCode)
    }

    fun pickDirectory(activity: Activity, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity.startActivityForResult(intent, requestCode)
    }

    fun createFile(activity: Activity, mimeType: String, defaultName: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, defaultName)
        }
        activity.startActivityForResult(intent, requestCode)
    }

    fun saveImageToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        subFolder: String?,
        format: Bitmap.CompressFormat
    ): Result<Uri> {
        val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        val extension = if (format == Bitmap.CompressFormat.PNG) ".png" else ".jpg"
        val finalFileName = fileName + extension

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var relativePath = Environment.DIRECTORY_PICTURES
                if (!subFolder.isNullOrEmpty()) {
                    relativePath += File.separator + subFolder
                }
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val itemUri = resolver.insert(collectionUri, values)
            ?: return Result.failure(IOException("Failed to create new MediaStore entry for image."))

        return try {
            resolver.openOutputStream(itemUri)?.use { os ->
                bitmap.compress(format, 95, os)
            } ?: throw IOException("Failed to get output stream for URI: $itemUri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(itemUri, updateValues, null, null)
            }

            Result.success(itemUri)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save bitmap.", e)
            resolver.delete(itemUri, null, null)
            Result.failure(e)
        }
    }

    fun saveVideoToGallery(
        context: Context,
        videoFile: File,
        fileName: String,
        subFolder: String?
    ): Result<Uri> {
        val mimeType = "video/mp4"
        val finalFileName = if (fileName.endsWith(".mp4")) fileName else "$fileName.mp4"

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName)
            put(MediaStore.Video.Media.MIME_TYPE, mimeType)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var relativePath = Environment.DIRECTORY_MOVIES
                if (!subFolder.isNullOrEmpty()) {
                    relativePath += File.separator + subFolder
                }
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val itemUri = resolver.insert(collectionUri, values)
            ?: return Result.failure(IOException("Failed to create new MediaStore entry for video."))

        return try {
            resolver.openOutputStream(itemUri)?.use { os ->
                FileInputStream(videoFile).use { inputStream ->
                    inputStream.copyTo(os, BUFFER_SIZE)
                }
            } ?: throw IOException("Failed to get output stream.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(itemUri, updateValues, null, null)
            }

            Result.success(itemUri)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save video.", e)
            resolver.delete(itemUri, null, null)
            Result.failure(e)
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get file name for URI: $uri", e)
            }
        }
        
        if (result == null) {
            result = uri.path
            result?.let {
                val cut = it.lastIndexOf('/')
                if (cut != -1) {
                    result = it.substring(cut + 1)
                }
            }
        }
        
        return result
    }

    fun getPathFromUri(context: Context, uri: Uri?): String? {
        uri ?: return null

        var path: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            when (uri.authority) {
                "com.android.externalstorage.documents" -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        path = "${Environment.getExternalStorageDirectory()}/${split[1]}"
                    }
                }
                "com.android.providers.downloads.documents" -> {
                    val id = DocumentsContract.getDocumentId(uri)
                    if (id != null && id.startsWith("raw:")) {
                        path = id.substring(4)
                    } else {
                        try {
                            val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                id.toLong()
                            )
                            path = getDataColumn(context, contentUri, null, null)
                        } catch (e: NumberFormatException) {
                            Log.e(TAG, "Could not parse download ID: $id", e)
                            path = null
                        }
                    }
                }
                "com.android.providers.media.documents" -> {
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
                    path = getDataColumn(context, contentUri, selection, selectionArgs)
                }
            }
        }

        if (path == null) {
            path = when {
                ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true) -> {
                    getDataColumn(context, uri, null, null)
                }
                ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true) -> {
                    uri.path
                }
                else -> null
            }
        }

        if (path == null) {
            path = copyToCache(context, uri)
        }

        return path
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        uri ?: return null
        
        val column = MediaStore.MediaColumns.DATA
        val projection = arrayOf(column)
        
        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    cursor.getString(columnIndex)
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "getDataColumn failed for URI: $uri", e)
            null
        }
    }

    private fun copyToCache(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                var fileName = getFileName(context, uri)
                if (fileName.isNullOrEmpty()) {
                    fileName = "temp_file_${System.currentTimeMillis()}"
                }

                val cacheFile = File(context.cacheDir, fileName)
                FileOutputStream(cacheFile).use { outputStream ->
                    inputStream.copyTo(outputStream, BUFFER_SIZE)
                }
                cacheFile.absolutePath
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy URI to cache: $uri", e)
            null
        }
    }

    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false

                context.contentResolver.openInputStream(uri)?.use { inputStream2 ->
                    BitmapFactory.decodeStream(inputStream2, null, options)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to decode bitmap from URI: $uri", e)
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}
