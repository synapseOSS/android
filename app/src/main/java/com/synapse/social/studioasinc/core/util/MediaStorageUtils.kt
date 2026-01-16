package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.net.Uri

@Deprecated("Use FileManager instead")
object MediaStorageUtils {

    @Deprecated("Use FileManager.DownloadCallback")
    interface DownloadCallback : FileManager.DownloadCallback

    @Deprecated("Use FileManager.downloadImage")
    fun downloadImage(context: Context?, imageUrl: String?, fileName: String?, callback: DownloadCallback?) {
        val unifiedCallback = if (callback != null) {
            object : FileManager.DownloadCallback {
                override fun onSuccess(savedUri: Uri, fileName: String) = callback.onSuccess(savedUri, fileName)
                override fun onProgress(progress: Int) = callback.onProgress(progress)
                override fun onError(error: String) = callback.onError(error)
            }
        } else null
        FileManager.downloadImage(context, imageUrl, fileName, unifiedCallback)
    }

    @Deprecated("Use FileManager.downloadVideo")
    fun downloadVideo(context: Context?, videoUrl: String?, fileName: String?, callback: DownloadCallback?) {
        val unifiedCallback = if (callback != null) {
            object : FileManager.DownloadCallback {
                override fun onSuccess(savedUri: Uri, fileName: String) = callback.onSuccess(savedUri, fileName)
                override fun onProgress(progress: Int) = callback.onProgress(progress)
                override fun onError(error: String) = callback.onError(error)
            }
        } else null
        FileManager.downloadVideo(context, videoUrl, fileName, unifiedCallback)
    }

    @Deprecated("Use FileManager.shutdown")
    fun shutdown() {
        FileManager.shutdown()
    }
}
