package com.synapse.social.studioasinc.core.storage.providers

import android.webkit.MimeTypeMap
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.core.storage.MediaStorageService
import com.synapse.social.studioasinc.core.storage.MediaUploadStrategy
import com.synapse.social.studioasinc.data.local.database.StorageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class ImgBBProvider : MediaUploadStrategy {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 30000
        private const val READ_TIMEOUT_MS = 60000
        private val DEFAULT_IMGBB_API_KEY = BuildConfig.IMGBB_API_KEY
    }

    override suspend fun upload(
        file: File,
        config: StorageConfig,
        bucketName: String?,
        callback: MediaStorageService.UploadCallback
    ) = withContext(Dispatchers.IO) {

        // Use custom API key if provided, otherwise use default
        val apiKey = if (config.imgBBConfig.apiKey.isNotBlank()) {
            config.imgBBConfig.apiKey
        } else {
            DEFAULT_IMGBB_API_KEY
        }

        val boundary = "*****${System.currentTimeMillis()}*****"
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        try {
            val url = URL("https://api.imgbb.com/1/upload?expiration=0&key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.doInput = true
            conn.doOutput = true
            conn.useCaches = false
            conn.requestMethod = "POST"
            conn.setRequestProperty("Connection", "Keep-Alive")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS

            DataOutputStream(BufferedOutputStream(conn.outputStream)).use { dos ->
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"${file.name}\"$lineEnd")
                dos.writeBytes("Content-Type: ${getMimeType(file.extension)}$lineEnd$lineEnd")

                FileInputStream(file).use { fileInputStream ->
                    val buffer = ByteArray(8192)
                    val totalBytes = file.length()
                    var bytesSent = 0L
                    var bytesRead: Int

                    while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                        dos.write(buffer, 0, bytesRead)
                        bytesSent += bytesRead
                        val progress = ((bytesSent * 100) / totalBytes).toInt()
                        withContext(Dispatchers.Main) {
                            callback.onProgress(progress)
                        }
                    }
                }

                dos.writeBytes("$lineEnd$twoHyphens$boundary$twoHyphens$lineEnd")
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                val data = jsonResponse.getJSONObject("data")
                val imageUrl = data.getString("url")

                withContext(Dispatchers.Main) {
                    callback.onSuccess(imageUrl)
                }
            } else {
                val errorResponse = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                withContext(Dispatchers.Main) {
                    callback.onError("ImgBB upload failed: $errorResponse")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback.onError("ImgBB upload error: ${e.message}")
            }
        }
    }

    private fun getMimeType(extension: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "application/octet-stream"
    }
}
