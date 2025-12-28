package com.synapse.social.studioasinc

import android.content.Context
import android.webkit.MimeTypeMap
import com.synapse.social.studioasinc.data.local.AppSettingsManager
import com.synapse.social.studioasinc.data.local.StorageConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Date
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Unified media storage service that handles upload/download/link operations
 * for all media types with provider selection and fallback logic
 */
class MediaStorageService(
    private val context: Context,
    private val appSettingsManager: AppSettingsManager
) {
    
    // Default credentials (from GitHub Actions secrets)
    companion object {
        private val DEFAULT_CLOUDINARY_API_KEY = BuildConfig.CLOUDINARY_API_KEY
        private val DEFAULT_CLOUDINARY_API_SECRET = BuildConfig.CLOUDINARY_API_SECRET
        private val DEFAULT_CLOUDINARY_CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
        private val DEFAULT_IMGBB_API_KEY = BuildConfig.IMGBB_API_KEY
        
        private const val CONNECT_TIMEOUT_MS = 30000
        private const val READ_TIMEOUT_MS = 60000
        private const val R2_REGION = "auto"
        private const val S3_SERVICE = "s3"
    }
    
    enum class MediaType {
        PHOTO, VIDEO, OTHER
    }
    
    interface UploadCallback {
        fun onProgress(percent: Int)
        fun onSuccess(url: String, publicId: String = "")
        fun onError(error: String)
    }
    
    /**
     * Upload a file using the configured provider with fallback to default
     */
    suspend fun uploadFile(filePath: String, bucketName: String? = null, callback: UploadCallback) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            callback.onError("File not found: $filePath")
            return@withContext
        }
        
        val mediaType = detectMediaType(file)
        val config = appSettingsManager.storageConfigFlow.first()
        
        // Get selected provider or use fallback logic
        val provider = getProviderForMediaType(config, mediaType)
        
        try {
            when (provider) {
                "Default" -> uploadWithDefaultProvider(file, mediaType, callback)
                "ImgBB" -> uploadToImgBB(config.imgBBConfig.apiKey, file, callback)
                "Cloudinary" -> uploadToCloudinary(
                    config.cloudinaryConfig.cloudName,
                    config.cloudinaryConfig.apiKey,
                    config.cloudinaryConfig.apiSecret,
                    file,
                    callback
                )
                "Supabase" -> uploadToSupabase(
                    config.supabaseConfig.url,
                    config.supabaseConfig.apiKey,
                    bucketName ?: config.supabaseConfig.bucketName,
                    file,
                    callback
                )
                "Cloudflare R2" -> uploadToR2(
                    config.r2Config.accountId,
                    config.r2Config.accessKeyId,
                    config.r2Config.secretAccessKey,
                    bucketName ?: config.r2Config.bucketName,
                    file,
                    callback
                )
                else -> callback.onError("Unknown provider: $provider")
            }
        } catch (e: Exception) {
            // Fallback to default if custom provider fails
            if (provider != "Default") {
                android.util.Log.w("MediaStorageService", "Provider $provider failed, falling back to default: ${e.message}")
                uploadWithDefaultProvider(file, mediaType, callback)
            } else {
                callback.onError("Upload failed: ${e.message}")
            }
        }
    }
    
    /**
     * Get the appropriate provider for a media type with fallback logic
     */
    private fun getProviderForMediaType(config: StorageConfig, mediaType: MediaType): String {
        val selectedProvider = when (mediaType) {
            MediaType.PHOTO -> config.photoProvider
            MediaType.VIDEO -> config.videoProvider
            MediaType.OTHER -> config.otherProvider
        }
        
        // If no provider selected or provider not configured, use Default
        return if (selectedProvider == null || !config.isProviderConfigured(selectedProvider)) {
            "Default"
        } else {
            selectedProvider
        }
    }
    
    /**
     * Upload using default app-provided credentials
     */
    private suspend fun uploadWithDefaultProvider(file: File, mediaType: MediaType, callback: UploadCallback) {
        when (mediaType) {
            MediaType.PHOTO -> uploadToImgBB(DEFAULT_IMGBB_API_KEY, file, callback)
            MediaType.VIDEO, MediaType.OTHER -> uploadToCloudinary(
                DEFAULT_CLOUDINARY_CLOUD_NAME,
                DEFAULT_CLOUDINARY_API_KEY,
                DEFAULT_CLOUDINARY_API_SECRET,
                file,
                callback
            )
        }
    }
    
    /**
     * Detect media type from file extension
     */
    private fun detectMediaType(file: File): MediaType {
        val extension = file.extension.lowercase()
        return when {
            extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "avif") -> MediaType.PHOTO
            extension in listOf("mp4", "mov", "avi", "mkv", "webm", "3gp") -> MediaType.VIDEO
            else -> MediaType.OTHER
        }
    }
    
    /**
     * Upload to ImgBB
     */
    private suspend fun uploadToImgBB(apiKey: String, file: File, callback: UploadCallback) = withContext(Dispatchers.IO) {
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
    
    /**
     * Upload to Cloudinary
     */
    private suspend fun uploadToCloudinary(
        cloudName: String,
        apiKey: String,
        apiSecret: String,
        file: File,
        callback: UploadCallback
    ) = withContext(Dispatchers.IO) {
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
    
    /**
     * Upload to Supabase Storage
     */
    private suspend fun uploadToSupabase(url: String, apiKey: String, bucketName: String, file: File, callback: UploadCallback) {
        var client: io.github.jan.supabase.SupabaseClient? = null
        try {
            // Create a scoped client for this upload since credentials might differ from global instance
            client = createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = apiKey
            ) {
                install(Storage)
            }

            val bucket = client.storage.from(bucketName)
            val fileName = "${System.currentTimeMillis()}_${file.name}"

            // Initial progress
            withContext(Dispatchers.Main) {
                callback.onProgress(10)
            }

            // Upload the file using the overload that accepts a File or byte array.
            // Using readBytes() is generally safe for typical mobile images (<10MB),
            // but for large videos, chunked upload is preferred.
            // The supabase-kt library's upload method handles byte arrays well.
            // Ideally we would pass the File directly if the library supports it to avoid OOM,
            // but the current version used typically expects bytes.
            // To be safer, we can check file size or just proceed as is for now,
            // but wrapping in try/finally ensures the client is closed.
            bucket.upload(fileName, file.readBytes()) {
                 upsert = true
            }

            // Final progress
            withContext(Dispatchers.Main) {
                callback.onProgress(100)
            }

            // Get public URL
            val publicUrl = bucket.publicUrl(fileName)

            withContext(Dispatchers.Main) {
                callback.onSuccess(publicUrl, fileName)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback.onError("Supabase upload failed: ${e.message}")
            }
        } finally {
             client?.close()
        }
    }
    
    /**
     * Upload to Cloudflare R2 (S3-compatible)
     */
    private suspend fun uploadToR2(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String, file: File, callback: UploadCallback) = withContext(Dispatchers.IO) {
        val objectKey = "${System.currentTimeMillis()}_${safeKey(file.name)}"
        val contentType = getMimeType(file.extension)
        val host = "$accountId.r2.cloudflarestorage.com"
        val path = "/$bucketName/$objectKey"
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

            // Sign request
            signS3(conn, "PUT", path, R2_REGION, host, accessKeyId, secretAccessKey)

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
    
    /**
     * Generate Cloudinary signature
     */
    private fun generateCloudinarySignature(timestamp: String, apiSecret: String): String {
        val toSign = "timestamp=$timestamp"
        val mac = Mac.getInstance("HmacSHA1")
        val secretKeySpec = SecretKeySpec(apiSecret.toByteArray(), "HmacSHA1")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(toSign.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Get MIME type from file extension
     */
    private fun getMimeType(extension: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "application/octet-stream"
    }

    // --- AWS SigV4 Helpers ---

    private fun safeKey(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun signS3(conn: HttpURLConnection, method: String, canonicalPath: String, region: String, host: String, accessKeyId: String, secretAccessKey: String) {
        val amzDate = conn.getRequestProperty("x-amz-date")
        val dateStamp = amzDate.substring(0, 8)
        val signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date"
        val contentType = conn.getRequestProperty("Content-Type")
        val payloadHash = "UNSIGNED-PAYLOAD"
        val canonicalQuery = ""
        val canonicalHeaders = "content-type:$contentType\n" +
                "host:$host\n" +
                "x-amz-content-sha256:$payloadHash\n" +
                "x-amz-date:$amzDate\n"
        val canonicalRequest = "$method\n$canonicalPath\n$canonicalQuery\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
        val credentialScope = "$dateStamp/$region/$S3_SERVICE/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest)}"

        val signingKey = getSignatureKey(secretAccessKey, dateStamp, region, S3_SERVICE)
        val signature = bytesToHex(hmacSHA256(signingKey, stringToSign))
        val authorization = "AWS4-HMAC-SHA256 Credential=$accessKeyId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
        conn.setRequestProperty("Authorization", authorization)
    }

    private fun amzDate(): String {
        val df = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
        df.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return df.format(java.util.Date())
    }

    private fun sha256Hex(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val d = md.digest(s.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSHA256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(key, "HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = "AWS4$key".toByteArray(Charsets.UTF_8)
        val kDate = hmacSHA256(kSecret, dateStamp)
        val kRegion = hmacSHA256(kDate, regionName)
        val kService = hmacSHA256(kRegion, serviceName)
        return hmacSHA256(kService, "aws4_request")
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
