package com.synapse.social.studioasinc

import android.content.ContentResolver
import android.content.ContentUris
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtil {

    private fun createNewFile(path: String) {
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
            e.printStackTrace()
        }
    }

    fun readFile(path: String): String {
        createNewFile(path)
        
        return try {
            File(path).readText()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun writeFile(path: String, str: String) {
        createNewFile(path)
        
        try {
            File(path).writeText(str)
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
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

    fun convertUriToFilePath(context: Context, uri: Uri): String? {
        var path: String? = null
        
        when {
            DocumentsContract.isDocumentUri(context, uri) -> {
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
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        val type = split[0]

                        when {
                            "raw".equals(type, ignoreCase = true) -> {
                                return split[1]
                            }
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && "msf".equals(type, ignoreCase = true) -> {
                                val selection = "_id=?"
                                val selectionArgs = arrayOf(split[1])
                                path = getDataColumn(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)
                            }
                            else -> {
                                val contentUri = ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"),
                                    docId.toLong()
                                )
                                path = getDataColumn(context, contentUri, null, null)
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
            ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true) -> {
                path = getDataColumn(context, uri, null, null)
            }
            ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true) -> {
                path = uri.path
            }
        }

        return path?.let {
            try {
                URLDecoder.decode(it, "UTF-8")
            } catch (e: Exception) {
                null
            }
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

    private fun saveBitmap(bitmap: Bitmap, destPath: String) {
        createNewFile(destPath)
        try {
            FileOutputStream(File(destPath)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getScaledBitmap(path: String, max: Int): Bitmap {
        val src = BitmapFactory.decodeFile(path)
        
        var width = src.width
        var height = src.height
        
        val rate = if (width > height) {
            max / width.toFloat()
        } else {
            max / height.toFloat()
        }
        
        width = (width * rate).toInt()
        height = (height * rate).toInt()
        
        return Bitmap.createScaledBitmap(src, width, height, true)
    }

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
        val paint = Paint().apply {
            isAntiAlias = true
        }
        val rect = Rect(0, 0, src.width, src.height)

        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawCircle(
            src.width / 2f,
            src.height / 2f,
            src.width / 2f,
            paint
        )
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
        val paint = Paint().apply {
            isAntiAlias = true
        }
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
        val matrix = Matrix().apply {
            postRotate(angle)
        }
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    fun scaleBitmapFile(fromPath: String, destPath: String, x: Float, y: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val matrix = Matrix().apply {
            postScale(x, y)
        }

        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    fun skewBitmapFile(fromPath: String, destPath: String, x: Float, y: Float) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val matrix = Matrix().apply {
            postSkew(x, y)
        }

        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    fun setBitmapFileColorFilter(fromPath: String, destPath: String, color: Int) {
        if (!isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width - 1, src.height - 1)
        val paint = Paint().apply {
            colorFilter = LightingColorFilter(color, 1)
        }
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
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
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
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
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
}
