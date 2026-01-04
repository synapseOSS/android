package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {
    fun getTmpFileUri(context: Context, extension: String = ".png"): Uri {
        val prefix = if (extension == ".mp4") "tmp_video_file" else "tmp_image_file"
        val tmpFile = File.createTempFile(prefix, extension, context.cacheDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
    }
}
