package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {
    fun getTmpFileUri(context: Context): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", context.cacheDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
    }
}
