package com.synapse.social.studioasinc.core.storage.providers

import android.webkit.MimeTypeMap
import com.synapse.social.studioasinc.core.storage.MediaStorageService
import com.synapse.social.studioasinc.core.storage.MediaUploadStrategy
import com.synapse.social.studioasinc.data.local.database.StorageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone

class R2Provider : MediaUploadStrategy {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 30000
        private const val READ_TIMEOUT_MS = 60000
        private const val R2_REGION = "auto"
    }

    override suspend fun upload(
        file: File,
        config: StorageConfig,
        bucketName: String?,
        callback: MediaStorageService.UploadCallback
    ) = withContext(Dispatchers.IO) {

        val accountId = config.r2Config.accountId
        val accessKeyId = config.r2Config.accessKeyId
        val secretAccessKey = config.r2Config.secretAccessKey
        val targetBucketName = bucketName ?: config.r2Config.bucketName

        val objectKey = "${System.currentTimeMillis()}_${safeKey(file.name)}"
        val contentType = getMimeType(file.extension)
        val host = "$accountId.r2.cloudflarestorage.com"
        val path = "/$targetBucketName/$objectKey"
        val urlStr = "https://$host$path"

        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.doOutput = true
            conn.requestMethod = "PUT"
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.setRequestProperty("Host", host)
            conn.setRequestProperty("x-amz-content-sha256", "UNSIGNED-PAYLOAD")
            conn.setRequestProperty("x-amz-date", amzDate())
            conn.setRequestProperty("Content-Type", contentType)

            // Sign request using S3Signer helper
            S3Signer.signS3(
                conn = conn,
                method = "PUT",
                canonicalPath = path,
                region = R2_REGION,
                host = host,
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                amzDate = conn.getRequestProperty("x-amz-date")
            )

            // Upload
            DataOutputStream(BufferedOutputStream(conn.outputStream)).use { dos ->
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
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                withContext(Dispatchers.Main) {
                    callback.onSuccess(urlStr, objectKey)
                }
            } else {
                val errorResponse = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                withContext(Dispatchers.Main) {
                    callback.onError("R2 upload failed ($responseCode): $errorResponse")
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback.onError("R2 upload error: ${e.message}")
            }
        }
    }

    private fun safeKey(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun amzDate(): String {
        val df = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
        df.timeZone = TimeZone.getTimeZone("UTC")
        return df.format(java.util.Date())
    }

    private fun getMimeType(extension: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "application/octet-stream"
    }
}
