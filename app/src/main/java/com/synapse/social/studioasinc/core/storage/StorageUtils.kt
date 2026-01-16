package com.synapse.social.studioasinc.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.synapse.social.studioasinc.core.util.FileManager
import java.io.File

@Deprecated("Use FileManager instead")
object StorageUtils {

    @Deprecated("Use FileManager.pickSingleFile")
    fun pickSingleFile(launcher: ActivityResultLauncher<String>, mimeType: String) =
        FileManager.pickSingleFile(launcher, mimeType)

    @Deprecated("Use FileManager.pickMultipleFiles")
    fun pickMultipleFiles(launcher: ActivityResultLauncher<String>, mimeType: String) =
        FileManager.pickMultipleFiles(launcher, mimeType)

    @Deprecated("Use FileManager.pickDirectory")
    fun pickDirectory(launcher: ActivityResultLauncher<Uri?>) =
        FileManager.pickDirectory(launcher)

    @Deprecated("Use FileManager.createFile")
    fun createFile(launcher: ActivityResultLauncher<String>, fileName: String) =
        FileManager.createFile(launcher, fileName)

    @Deprecated("Use FileManager.saveImageToGallery")
    fun saveImageToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        subFolder: String?,
        format: Bitmap.CompressFormat
    ): Result<Uri> = FileManager.saveImageToGallery(context, bitmap, fileName, subFolder, format)

    @Deprecated("Use FileManager.saveVideoToGallery")
    fun saveVideoToGallery(
        context: Context,
        videoFile: File,
        fileName: String,
        subFolder: String?
    ): Result<Uri> = FileManager.saveVideoToGallery(context, videoFile, fileName, subFolder)

    @Deprecated("Use FileManager.getFileName")
    fun getFileName(context: Context, uri: Uri): String? = FileManager.getFileName(context, uri)

    @Deprecated("Use FileManager.getPathFromUri")
    fun getPathFromUri(context: Context, uri: Uri?): String? = FileManager.getPathFromUri(context, uri)

    @Deprecated("Use FileManager.decodeSampledBitmapFromUri")
    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? = FileManager.decodeSampledBitmapFromUri(context, uri, reqWidth, reqHeight)
}
