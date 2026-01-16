package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

@Deprecated("Use FileManager instead")
object FileUtils {

    @Deprecated("Use FileManager.getTmpFileUri")
    fun getTmpFileUri(context: Context, extension: String = ".png"): Uri =
        FileManager.getTmpFileUri(context, extension)

    @Deprecated("Use FileManager.readFile")
    fun readFile(path: String): String = FileManager.readFile(path)

    @Deprecated("Use FileManager.writeFile")
    fun writeFile(path: String, str: String) = FileManager.writeFile(path, str)

    @Deprecated("Use FileManager.copyFile")
    fun copyFile(sourcePath: String, destPath: String) = FileManager.copyFile(sourcePath, destPath)

    @Deprecated("Use FileManager.copyDir")
    fun copyDir(oldPath: String, newPath: String) = FileManager.copyDir(oldPath, newPath)

    @Deprecated("Use FileManager.moveFile")
    fun moveFile(sourcePath: String, destPath: String) = FileManager.moveFile(sourcePath, destPath)

    @Deprecated("Use FileManager.deleteFile")
    fun deleteFile(path: String) = FileManager.deleteFile(path)

    @Deprecated("Use FileManager.isExistFile")
    fun isExistFile(path: String): Boolean = FileManager.isExistFile(path)

    @Deprecated("Use FileManager.makeDir")
    fun makeDir(path: String) = FileManager.makeDir(path)

    @Deprecated("Use FileManager.listDir")
    fun listDir(path: String, list: ArrayList<String>?) = FileManager.listDir(path, list)

    @Deprecated("Use FileManager.isDirectory")
    fun isDirectory(path: String): Boolean = FileManager.isDirectory(path)

    @Deprecated("Use FileManager.isFile")
    fun isFile(path: String): Boolean = FileManager.isFile(path)

    @Deprecated("Use FileManager.getFileLength")
    fun getFileLength(path: String): Long = FileManager.getFileLength(path)

    @Deprecated("Use FileManager.getExternalStorageDir")
    fun getExternalStorageDir(): String = FileManager.getExternalStorageDir()

    @Deprecated("Use FileManager.getPackageDataDir")
    fun getPackageDataDir(context: Context): String = FileManager.getPackageDataDir(context)

    @Deprecated("Use FileManager.getPublicDir")
    fun getPublicDir(type: String): String = FileManager.getPublicDir(type)

    @Deprecated("Use FileManager.getPathFromUri")
    fun convertUriToFilePath(context: Context, uri: Uri): String? =
        FileManager.getPathFromUri(context, uri)

    @Deprecated("Use FileManager.saveBitmap")
    private fun saveBitmap(bitmap: Bitmap, destPath: String) = FileManager.saveBitmap(bitmap, destPath)

    @Deprecated("Use FileManager.getScaledBitmap")
    fun getScaledBitmap(path: String, max: Int): Bitmap = FileManager.getScaledBitmap(path, max)

    @Deprecated("Use FileManager.calculateInSampleSize")
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int =
        FileManager.calculateInSampleSize(options, reqWidth, reqHeight)

    @Deprecated("Use FileManager.decodeSampleBitmapFromPath")
    @JvmStatic
    fun decodeSampleBitmapFromPath(path: String, reqWidth: Int, reqHeight: Int): Bitmap =
        FileManager.decodeSampleBitmapFromPath(path, reqWidth, reqHeight)

    @Deprecated("Use FileManager.resizeBitmapFileRetainRatio")
    fun resizeBitmapFileRetainRatio(fromPath: String, destPath: String, max: Int) =
        FileManager.resizeBitmapFileRetainRatio(fromPath, destPath, max)

    @Deprecated("Use FileManager.resizeBitmapFileToSquare")
    fun resizeBitmapFileToSquare(fromPath: String, destPath: String, max: Int) =
        FileManager.resizeBitmapFileToSquare(fromPath, destPath, max)

    @Deprecated("Use FileManager.resizeBitmapFileToCircle")
    fun resizeBitmapFileToCircle(fromPath: String, destPath: String) =
        FileManager.resizeBitmapFileToCircle(fromPath, destPath)

    @Deprecated("Use FileManager.resizeBitmapFileWithRoundedBorder")
    fun resizeBitmapFileWithRoundedBorder(fromPath: String, destPath: String, pixels: Int) =
        FileManager.resizeBitmapFileWithRoundedBorder(fromPath, destPath, pixels)

    @Deprecated("Use FileManager.cropBitmapFileFromCenter")
    fun cropBitmapFileFromCenter(fromPath: String, destPath: String, w: Int, h: Int) =
        FileManager.cropBitmapFileFromCenter(fromPath, destPath, w, h)

    @Deprecated("Use FileManager.rotateBitmapFile")
    fun rotateBitmapFile(fromPath: String, destPath: String, angle: Float) =
        FileManager.rotateBitmapFile(fromPath, destPath, angle)

    @Deprecated("Use FileManager.scaleBitmapFile")
    fun scaleBitmapFile(fromPath: String, destPath: String, x: Float, y: Float) =
        FileManager.scaleBitmapFile(fromPath, destPath, x, y)

    @Deprecated("Use FileManager.skewBitmapFile")
    fun skewBitmapFile(fromPath: String, destPath: String, x: Float, y: Float) =
        FileManager.skewBitmapFile(fromPath, destPath, x, y)

    @Deprecated("Use FileManager.setBitmapFileColorFilter")
    fun setBitmapFileColorFilter(fromPath: String, destPath: String, color: Int) =
        FileManager.setBitmapFileColorFilter(fromPath, destPath, color)

    @Deprecated("Use FileManager.setBitmapFileBrightness")
    fun setBitmapFileBrightness(fromPath: String, destPath: String, brightness: Float) =
        FileManager.setBitmapFileBrightness(fromPath, destPath, brightness)

    @Deprecated("Use FileManager.setBitmapFileContrast")
    fun setBitmapFileContrast(fromPath: String, destPath: String, contrast: Float) =
        FileManager.setBitmapFileContrast(fromPath, destPath, contrast)

    @Deprecated("Use FileManager.getJpegRotate")
    fun getJpegRotate(filePath: String): Int = FileManager.getJpegRotate(filePath)

    @Deprecated("Use FileManager.createNewPictureFile")
    fun createNewPictureFile(context: Context): File = FileManager.createNewPictureFile(context)
}
