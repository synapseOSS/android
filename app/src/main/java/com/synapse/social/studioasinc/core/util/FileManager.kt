package com.synapse.social.studioasinc.core.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LightingColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Unified FileManager serving as the single source of truth for file and media operations.
 * Consolidates logic from FileUtils, StorageUtils, and MediaStorageUtils.
 */
object FileManager {

    private const val TAG = "FileManager"
    private const val BUFFER_SIZE = 8192
    private val executor: ExecutorService = Executors.newFixedThreadPool(3)

    // ============================================================================================
    // File I/O & Path Operations (From FileUtils & StorageUtils)
    // ============================================================================================

    fun getTmpFileUri(context: Context, extension: String = ".png"): Uri {
        val prefix = if (extension == ".mp4") "tmp_video_file" else "tmp_image_file"
        val tmpFile = File.createTempFile(prefix, extension, context.cacheDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
    }

    fun createNewFile(path: String) {
        val lastSep = path.lastIndexOf(File.separator)
        if (lastSep > 0) {
            val dirPath = path.substring(0, lastSep)
            makeDir(dirPath)
        }

        val file = File(path)
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "createNewFile failed: $path", e)
        }
    }

    fun readFile(path: String): String {
        createNewFile(path)
        return try {
            File(path).readText()
        } catch (e: Exception) {
            Log.e(TAG, "readFile failed: $path", e)
            ""
        }
    }

    fun writeFile(path: String, str: String) {
        createNewFile(path)
        try {
            File(path).writeText(str)
        } catch (e: Exception) {
            Log.e(TAG, "writeFile failed: $path", e)
        }
    }

    fun copyFile(sourcePath: String, destPath: String) {
        if (!isExistFile(sourcePath)) return
        createNewFile(destPath)

        try {
            FileInputStream(sourcePath).use { fis ->
                FileOutputStream(destPath).use { fos ->
                    fis.copyTo(fos)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "copyFile failed", e)
        }
    }

    fun copyDir(oldPath: String, newPath: String) {
        val oldFile = File(oldPath)
        val files = oldFile.listFiles() ?: return
        val newFile = File(newPath)

        if (!newFile.exists()) {
            newFile.mkdirs()
        }

        for (file in files) {
            when {
                file.isFile -> copyFile(file.path, "$newPath/${file.name}")
                file.isDirectory -> copyDir(file.path, "$newPath/${file.name}")
            }
        }
    }

    fun moveFile(sourcePath: String, destPath: String) {
        copyFile(sourcePath, destPath)
        deleteFile(sourcePath)
    }

    fun deleteFile(path: String) {
        val file = File(path)
        if (!file.exists()) return

        if (file.isFile) {
            file.delete()
            return
        }

        file.listFiles()?.forEach { subFile ->
            when {
                subFile.isDirectory -> deleteFile(subFile.absolutePath)
                subFile.isFile -> subFile.delete()
            }
        }
        file.delete()
    }

    fun isExistFile(path: String): Boolean {
        return File(path).exists()
    }

    fun makeDir(path: String) {
        if (!isExistFile(path)) {
            File(path).mkdirs()
        }
    }

    fun listDir(path: String, list: ArrayList<String>?) {
        val dir = File(path)
        if (!dir.exists() || dir.isFile) return

        val listFiles = dir.listFiles()
        if (listFiles.isNullOrEmpty()) return

        list?.apply {
            clear()
            addAll(listFiles.map { it.absolutePath })
        }
    }

    fun isDirectory(path: String): Boolean {
        if (!isExistFile(path)) return false
        return File(path).isDirectory
    }

    fun isFile(path: String): Boolean {
        if (!isExistFile(path)) return false
        return File(path).isFile
    }

    fun getFileLength(path: String): Long {
        if (!isExistFile(path)) return 0
        return File(path).length()
    }

    fun getExternalStorageDir(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    fun getPackageDataDir(context: Context): String {
        return context.getExternalFilesDir(null)?.absolutePath ?: ""
    }

    fun getPublicDir(type: String): String {
        return Environment.getExternalStoragePublicDirectory(type).absolutePath
    }

    /**
     * Unified method to get file path from URI.
     * Combines logic from FileUtils and StorageUtils.
     */
    fun getPathFromUri(context: Context, uri: Uri?): String? {
        uri ?: return null
        var path: String? = null

        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            when {
                isExternalStorageDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        path = "${Environment.getExternalStorageDirectory()}/${split[1]}"
                    }
                }
                isDownloadsDocument(uri) -> {
                    val id = DocumentsContract.getDocumentId(uri)
                    if (id != null && id.startsWith("raw:")) {
                        path = id.substring(4)
                    } else {
                        val split = id.split(":")
                        // Check for MSF downloads (specific to FileUtils logic)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && split.isNotEmpty() && "msf".equals(split[0], ignoreCase = true)) {
                            val selection = "_id=?"
                            val selectionArgs = arrayOf(split[1])
                            path = getDataColumn(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)
                        } else {
                            try {
                                val contentUri = ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"),
                                    id.toLong()
                                )
                                path = getDataColumn(context, contentUri, null, null)
                            } catch (e: NumberFormatException) {
                                // Fallback or log
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
                    path = getDataColumn(context, contentUri, selection, selectionArgs)
                }
            }
        }

        // Content Scheme
        if (path == null && "content".equals(uri.scheme, ignoreCase = true)) {
            path = getDataColumn(context, uri, null, null)
        }

        // File Scheme
        if (path == null && "file".equals(uri.scheme, ignoreCase = true)) {
            path = uri.path
        }

        // Decode path if needed (from FileUtils)
        path?.let {
            try {
                path = URLDecoder.decode(it, "UTF-8")
            } catch (e: Exception) {
                // ignore
            }
        }

        // Fallback: Copy to cache (from StorageUtils)
        if (path == null) {
            path = copyToCache(context, uri)
        }

        return path
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

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
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

    // ============================================================================================
    // Bitmap Operations (From FileUtils & StorageUtils)
    // ============================================================================================

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val width = options.outWidth
        val height = options.outHeight
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

    @JvmStatic
    fun decodeSampleBitmapFromPath(path: String, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
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

    fun saveBitmap(bitmap: Bitmap, destPath: String) {
        createNewFile(destPath)
        try {
            FileOutputStream(File(destPath)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveBitmap failed", e)
        }
    }

    fun getScaledBitmap(path: String, max: Int): Bitmap {
        val src = BitmapFactory.decodeFile(path)
        var width = src.width
        var height = src.height
        val rate = if (width > height) max / width.toFloat() else max / height.toFloat()
        width = (width * rate).toInt()
        height = (height * rate).toInt()
        return Bitmap.createScaledBitmap(src, width, height, true)
    }

    // Various Bitmap manipulations from FileUtils

    fun resizeBitmapFileRetainRatio(fromPath: String, destPath: String, max: Int) {
        if (!isExistFile(fromPath)) return
        val bitmap = getScaledBitmap(fromPath, max)
        saveBitmap(bitmap, destPath)
    }

    fun resizeBitmapFileToSquare(fromPath: String, destPath: String, max: Int) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val bitmap = Bitmap.createScaledBitmap(src, max, max, true)
        saveBitmap(bitmap, destPath)
    }

    fun resizeBitmapFileToCircle(fromPath: String, destPath: String) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val bitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val color = 0xff424242.toInt()
        val paint = Paint().apply { isAntiAlias = true }
        val rect = Rect(0, 0, src.width, src.height)

        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawCircle(src.width / 2f, src.height / 2f, src.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, rect, rect, paint)
        saveBitmap(bitmap, destPath)
    }

    fun resizeBitmapFileWithRoundedBorder(fromPath: String, destPath: String, pixels: Int) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val bitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val color = 0xff424242.toInt()
        val paint = Paint().apply { isAntiAlias = true }
        val rect = Rect(0, 0, src.width, src.height)
        val rectF = RectF(rect)
        val roundPx = pixels.toFloat()

        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, rect, rect, paint)
        saveBitmap(bitmap, destPath)
    }

    fun cropBitmapFileFromCenter(fromPath: String, destPath: String, w: Int, h: Int) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val width = src.width
        val height = src.height
        if (width < w && height < h) return

        val x = if (width > w) (width - w) / 2 else 0
        val y = if (height > h) (height - h) / 2 else 0
        val cw = if (w > width) width else w
        val ch = if (h > height) height else h

        val bitmap = Bitmap.createBitmap(src, x, y, cw, ch)
        saveBitmap(bitmap, destPath)
    }

    fun rotateBitmapFile(fromPath: String, destPath: String, angle: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val matrix = Matrix().apply { postRotate(angle) }
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    fun scaleBitmapFile(fromPath: String, destPath: String, x: Float, y: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val matrix = Matrix().apply { postScale(x, y) }
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    fun skewBitmapFile(fromPath: String, destPath: String, x: Float, y: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val matrix = Matrix().apply { postSkew(x, y) }
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    fun setBitmapFileColorFilter(fromPath: String, destPath: String, color: Int) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width - 1, src.height - 1)
        val paint = Paint().apply { colorFilter = LightingColorFilter(color, 1) }
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        saveBitmap(bitmap, destPath)
    }

    fun setBitmapFileBrightness(fromPath: String, destPath: String, brightness: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val cm = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightness,
                0f, 1f, 0f, 0f, brightness,
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val bitmap = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        saveBitmap(bitmap, destPath)
    }

    fun setBitmapFileContrast(fromPath: String, destPath: String, contrast: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, 0f,
                0f, contrast, 0f, 0f, 0f,
                0f, 0f, contrast, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val bitmap = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        saveBitmap(bitmap, destPath)
    }

    fun getJpegRotate(filePath: String): Int {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun createNewPictureFile(context: Context): File {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "${date.format(Date())}.jpg"
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath + File.separator + fileName)
    }

    // ============================================================================================
    // Media Downloading & Saving (From MediaStorageUtils & StorageUtils)
    // ============================================================================================

    interface DownloadCallback {
        fun onSuccess(savedUri: Uri, fileName: String)
        fun onProgress(progress: Int)
        fun onError(error: String)
    }

    private data class FileInfo(val fileName: String, val mimeType: String, val extension: String)

    fun downloadImage(context: Context?, imageUrl: String?, fileName: String?, callback: DownloadCallback?) {
        if (context == null || imageUrl.isNullOrEmpty()) {
            callback?.onError("Invalid parameters")
            return
        }
        executor.execute {
            try {
                val fileInfo = detectImageFileInfo(imageUrl, fileName ?: "image")
                val finalFileName = fileInfo.fileName
                val mimeType = fileInfo.mimeType

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Synapse")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    } else {
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

                try {
                    resolver.openOutputStream(imageUri)?.use { outputStream ->
                        downloadToStream(imageUrl, outputStream, callback)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(imageUri, contentValues, null, null)
                        }
                        callback?.onSuccess(imageUri, fileName ?: "image")
                    } ?: throw IOException("Failed to open output stream")
                } catch (e: Exception) {
                    resolver.delete(imageUri, null, null)
                    throw e
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image: " + e.message, e)
                callback?.onError("Download failed: " + e.message)
            }
        }
    }

    fun downloadVideo(context: Context?, videoUrl: String?, fileName: String?, callback: DownloadCallback?) {
        if (context == null || videoUrl.isNullOrEmpty()) {
            callback?.onError("Invalid parameters")
            return
        }
        executor.execute {
            try {
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

    fun saveImageToGallery(context: Context, bitmap: Bitmap, fileName: String, subFolder: String?, format: Bitmap.CompressFormat): Result<Uri> {
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

        val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return Result.failure(IOException("Failed to create new MediaStore entry for image."))

        return try {
            resolver.openOutputStream(itemUri)?.use { os ->
                bitmap.compress(format, 95, os)
            } ?: throw IOException("Failed to get output stream for URI: $itemUri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(itemUri, updateValues, null, null)
            }
            Result.success(itemUri)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save bitmap.", e)
            resolver.delete(itemUri, null, null)
            Result.failure(e)
        }
    }

    fun saveVideoToGallery(context: Context, videoFile: File, fileName: String, subFolder: String?): Result<Uri> {
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

        val itemUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return Result.failure(IOException("Failed to create new MediaStore entry for video."))

        return try {
            resolver.openOutputStream(itemUri)?.use { os ->
                FileInputStream(videoFile).use { inputStream ->
                    inputStream.copyTo(os, BUFFER_SIZE)
                }
            } ?: throw IOException("Failed to get output stream.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                resolver.update(itemUri, updateValues, null, null)
            }
            Result.success(itemUri)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save video.", e)
            resolver.delete(itemUri, null, null)
            Result.failure(e)
        }
    }

    // Launchers (From StorageUtils)
    fun pickSingleFile(launcher: ActivityResultLauncher<String>, mimeType: String) {
        launcher.launch(mimeType)
    }

    fun pickMultipleFiles(launcher: ActivityResultLauncher<String>, mimeType: String) {
        launcher.launch(mimeType)
    }

    fun pickDirectory(launcher: ActivityResultLauncher<Uri?>) {
        launcher.launch(null)
    }

    fun createFile(launcher: ActivityResultLauncher<String>, fileName: String) {
        launcher.launch(fileName)
    }

    // Helpers for Downloading

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

    private fun detectImageFileInfo(imageUrl: String, baseFileName: String): FileInfo {
        var extension = ".jpg"
        var mimeType = "image/jpeg"
        try {
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
            Log.w(TAG, "Error detecting file type from URL", e)
        }
        var finalFileName = baseFileName
        if (!finalFileName.lowercase().endsWith(extension.lowercase())) {
            finalFileName += extension
        }
        return FileInfo(finalFileName, mimeType, extension)
    }

    private fun detectVideoFileInfo(videoUrl: String, baseFileName: String): FileInfo {
        var extension = ".mp4"
        var mimeType = "video/mp4"
        try {
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
            Log.w(TAG, "Error detecting file type from URL", e)
        }
        var finalFileName = baseFileName
        if (!finalFileName.lowercase().endsWith(extension.lowercase())) {
            finalFileName += extension
        }
        return FileInfo(finalFileName, mimeType, extension)
    }

    fun shutdown() {
        executor.shutdown()
    }
}
