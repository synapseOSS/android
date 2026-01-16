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
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class CloudinaryProvider : MediaUploadStrategy {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 30000
        private const val READ_TIMEOUT_MS = 60000
        private val DEFAULT_CLOUDINARY_API_KEY = BuildConfig.CLOUDINARY_API_KEY
        private val DEFAULT_CLOUDINARY_API_SECRET = BuildConfig.CLOUDINARY_API_SECRET
        private val DEFAULT_CLOUDINARY_CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
    }

    override suspend fun upload(
        file: File,
        config: StorageConfig,
        bucketName: String?,
        callback: MediaStorageService.UploadCallback
    ) = withContext(Dispatchers.IO) {

        // Use custom credentials ONLY if ALL are provided, otherwise use defaults
        val hasCustomCloudinary = config.cloudinaryConfig.cloudName.isNotBlank() &&
                                 config.cloudinaryConfig.apiKey.isNotBlank() &&
                                 config.cloudinaryConfig.apiSecret.isNotBlank()

        val cloudName: String
        val apiKey: String
        val apiSecret: String

        if (hasCustomCloudinary) {
             cloudName = config.cloudinaryConfig.cloudName
             apiKey = config.cloudinaryConfig.apiKey
             apiSecret = config.cloudinaryConfig.apiSecret
        } else {
             cloudName = DEFAULT_CLOUDINARY_CLOUD_NAME
             apiKey = DEFAULT_CLOUDINARY_API_KEY
             apiSecret = DEFAULT_CLOUDINARY_API_SECRET
        }

        val boundary = "*****${System.currentTimeMillis()}*****"
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        try {
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val signature = generateCloudinarySignature(timestamp, apiSecret)

            val url = URL("https://api.cloudinary.com/v1_1/$cloudName/auto/upload")
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
                // Add file
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"$lineEnd")
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

                // Add other parameters
                dos.writeBytes("$lineEnd$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"api_key\"$lineEnd$lineEnd$apiKey")

                dos.writeBytes("$lineEnd$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"timestamp\"$lineEnd$lineEnd$timestamp")

                dos.writeBytes("$lineEnd$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"signature\"$lineEnd$lineEnd$signature")

                dos.writeBytes("$lineEnd$twoHyphens$boundary$twoHyphens$lineEnd")
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                val secureUrl = jsonResponse.getString("secure_url")
                val publicId = jsonResponse.optString("public_id", "")

                withContext(Dispatchers.Main) {
                    callback.onSuccess(secureUrl, publicId)
                }
            } else {
                val errorResponse = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                withContext(Dispatchers.Main) {
                    callback.onError("Cloudinary upload failed: $errorResponse")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback.onError("Cloudinary upload error: ${e.message}")
            }
        }
    }

    private fun generateCloudinarySignature(timestamp: String, apiSecret: String): String {
        val toSign = "timestamp=$timestamp$apiSecret"
        val md = MessageDigest.getInstance("SHA-1")
        val hash = md.digest(toSign.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun getMimeType(extension: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "application/octet-stream"
    }
}
