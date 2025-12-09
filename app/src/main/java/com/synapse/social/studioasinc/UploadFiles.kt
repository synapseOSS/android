package com.synapse.social.studioasinc

import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object UploadFiles {
    
    // --- CALLBACKS ---
    interface UploadCallback {
        fun onProgress(percent: Int)
        fun onSuccess(url: String, publicId: String)
        fun onFailure(error: String)
    }
    
    interface DeleteCallback {
        fun onSuccess()
        fun onFailure(error: String)
    }
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // --- FILE TYPE CONFIGURATION ---
    private val IMAGE_EXTENSIONS = listOf(
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp", "heic", "pdf", "avif"
    )
    
    // --- CLOUDINARY CONFIGURATION ---
    private const val CLOUDINARY_API_KEY = "577882927131931"
    private const val CLOUDINARY_API_SECRET = "M_w_0uQKjnLRUe-u34driUBqUQU"
    private const val CLOUDINARY_CLOUD_NAME = "djw3fgbls"
    private const val CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/auto/upload"
    
    // --- IMGBB CONFIGURATION ---
    private const val IMGBB_API_KEY = "faa85ffbac0217ff67b5f3c4baa7fb29"
    private const val IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload?expiration=0&key=$IMGBB_API_KEY"
    
    // --- POSTIMAGES.ORG CONFIGURATION (Fallback 1 for images) ---
    private const val POSTIMAGES_API_KEY = "7746820ffe9cebd5769618ca22fc9ca8"
    private const val POSTIMAGES_UPLOAD_URL = "https://api.postimage.org/1/upload"
    
    // --- IMGHIPPO CONFIGURATION (Fallback 2 for images) ---
    private const val IMGHIPPO_API_KEY = "8c9f1370410a15045f10c055ed656032"
    private const val IMGHIPPO_UPLOAD_URL = "https://www.imghippo.com/api/1/upload"
    
    // Reasonable network timeouts (ms)
    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 30000
    
    private val activeUploads = mutableMapOf<String, HttpURLConnection>()
    
    // --- R2 (S3-compatible) CONFIGURATION (Fallback for non-images) ---
    @Volatile
    private var R2_ENABLED = false
    @Volatile
    private var R2_ENDPOINT = "https://76bea77fbdac3cdf71e6cf580f270ea6.r2.cloudflarestorage.com"
    @Volatile
    private var R2_BUCKET = "synapse"
    @Volatile
    private var R2_ACCESS_KEY = ""
    @Volatile
    private var R2_SECRET_KEY = ""
    @Volatile
    private var R2_KEY_PREFIX = "uploads/"
    
    private const val R2_REGION = "auto"
    private const val S3_SERVICE = "s3"
    
    @JvmStatic
    fun configureR2(
        endpoint: String?,
        bucket: String?,
        accessKey: String?,
        secretKey: String?,
        keyPrefix: String?
    ) {
        R2_ENDPOINT = endpoint?.takeIf { it.isNotEmpty() } ?: R2_ENDPOINT
        R2_BUCKET = bucket?.takeIf { it.isNotEmpty() } ?: R2_BUCKET
        R2_ACCESS_KEY = accessKey ?: ""
        R2_SECRET_KEY = secretKey ?: ""
        R2_KEY_PREFIX = keyPrefix ?: R2_KEY_PREFIX
        R2_ENABLED = R2_ACCESS_KEY.isNotEmpty() && R2_SECRET_KEY.isNotEmpty()
    }
    
    /**
     * Main upload method. Determines the service based on file extension and starts the upload.
     */
    @JvmStatic
    fun uploadFile(filePath: String, fileName: String, callback: UploadCallback) {
        Thread {
            val extension = getFileExtension(fileName).lowercase()
            if (IMAGE_EXTENSIONS.contains(extension)) {
                uploadToImgBB(filePath, fileName, callback)
            } else {
                uploadToCloudinary(filePath, fileName, callback)
            }
        }.start()
    }
    
    /**
     * Handles uploading image files to ImgBB.
     */
    private fun uploadToImgBB(filePath: String, fileName: String, callback: UploadCallback) {
        val boundary = "*****${System.currentTimeMillis()}*****"
        val lineEnd = "\r\n"
        val twoHyphens = "--"
        
        val file = File(filePath)
        if (!file.exists()) {
            postFailure(callback, "File not found")
            return
        }
        
        try {
            val url = URL(IMGBB_UPLOAD_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.doInput = true
            conn.doOutput = true
            conn.useCaches = false
            conn.requestMethod = "POST"
            conn.setRequestProperty("Connection", "Keep-Alive")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setChunkedStreamingMode(64 * 1024)
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            activeUploads[filePath] = conn
            
            DataOutputStream(BufferedOutputStream(conn.outputStream, 64 * 1024)).use { dos ->
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"$fileName\"$lineEnd")
                dos.writeBytes("Content-Type: ${getMimeType(getFileExtension(fileName))}$lineEnd$lineEnd")
                
                FileInputStream(file).use { fileInputStream ->
                    val buffer = ByteArray(64 * 1024)
                    val totalBytes = file.length()
                    var bytesSent = 0L
                    var bytesRead: Int
                    
                    while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                        dos.write(buffer, 0, bytesRead)
                        bytesSent += bytesRead
                        postProgress(callback, ((bytesSent * 100) / totalBytes).toInt())
                    }
                }
                
                dos.writeBytes(lineEnd)
                dos.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
                dos.flush()
            }
            
            val status = conn.responseCode
            val respStream = if (status == HttpURLConnection.HTTP_OK) conn.inputStream else conn.errorStream
            val response = BufferedReader(InputStreamReader(respStream)).use { reader ->
                reader.readText()
            }
            
            activeUploads.remove(filePath)
            conn.disconnect()
            
            if (status == HttpURLConnection.HTTP_OK) {
                val json = JSONObject(response)
                val data = json.getJSONObject("data")
                val imageUrl = data.getString("url")
                postSuccess(callback, imageUrl, "imgbb|$imageUrl")
            } else {
                uploadToPostImages(filePath, fileName, callback, "ImgBB Upload failed: $response")
            }
        } catch (e: Exception) {
            activeUploads.remove(filePath)
            uploadToPostImages(filePath, fileName, callback, "ImgBB Exception: ${e.message}")
        }
    }
    
    /**
     * Fallback: Upload image to Postimages.org
     */
    private fun uploadToPostImages(filePath: String, fileName: String, callback: UploadCallback, previousError: String) {
        val boundary = "----PostImagesBoundary${System.currentTimeMillis()}"
        val LF = "\r\n"
        
        val file = File(filePath)
        if (!file.exists()) {
            uploadToImgHippo(filePath, fileName, callback, "$previousError | File not found for Postimages")
            return
        }
        
        try {
            val url = URL(POSTIMAGES_UPLOAD_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.useCaches = false
            conn.doOutput = true
            conn.doInput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            
            DataOutputStream(BufferedOutputStream(conn.outputStream, 64 * 1024)).use { out ->
                // API key field
                out.writeBytes("--$boundary$LF")
                out.writeBytes("Content-Disposition: form-data; name=\"key\"$LF$LF")
                out.writeBytes("$POSTIMAGES_API_KEY$LF")
                
                // Image field
                out.writeBytes("--$boundary$LF")
                out.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"$fileName\"$LF")
                out.writeBytes("Content-Type: ${getMimeType(getFileExtension(fileName))}$LF$LF")
                
                FileInputStream(file).use { inputStream ->
                    val buf = ByteArray(64 * 1024)
                    val total = file.length()
                    var sent = 0L
                    var r: Int
                    
                    while (inputStream.read(buf).also { r = it } != -1) {
                        out.write(buf, 0, r)
                        sent += r
                        postProgress(callback, ((sent * 100) / total).toInt())
                    }
                }
                
                out.writeBytes(LF)
                out.writeBytes("--$boundary--$LF")
                out.flush()
            }
            
            val code = conn.responseCode
            val respStream = if (code == 200) conn.inputStream else conn.errorStream
            val resp = BufferedReader(InputStreamReader(respStream)).use { it.readText() }
            conn.disconnect()
            
            if (code == 200) {
                var urlStr: String? = null
                try {
                    val json = JSONObject(resp)
                    urlStr = when {
                        json.has("url") -> json.getString("url")
                        json.has("image") -> json.getJSONObject("image").getString("url")
                        else -> null
                    }
                } catch (ignored: Exception) {}
                
                if (urlStr == null) {
                    urlStr = extractFirstUrl(resp)
                }
                
                if (!urlStr.isNullOrEmpty()) {
                    postSuccess(callback, urlStr, "postimages|$urlStr")
                } else {
                    uploadToImgHippo(filePath, fileName, callback, "$previousError | Postimages success but no URL in response: $resp")
                }
            } else {
                uploadToImgHippo(filePath, fileName, callback, "$previousError | Postimages Upload failed ($code): $resp")
            }
        } catch (e: Exception) {
            uploadToImgHippo(filePath, fileName, callback, "$previousError | Postimages Exception: ${e.message}")
        }
    }
    
    // Fallback 2: Upload to ImgHippo
    private fun uploadToImgHippo(filePath: String, fileName: String, callback: UploadCallback, previousError: String) {
        val boundary = "----ImgHippoBoundary${System.currentTimeMillis()}"
        val LF = "\r\n"
        
        val file = File(filePath)
        if (!file.exists()) {
            postFailure(callback, "$previousError | File not found for ImgHippo")
            return
        }
        
        try {
            val url = URL(IMGHIPPO_UPLOAD_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.useCaches = false
            conn.doOutput = true
            conn.doInput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            
            DataOutputStream(BufferedOutputStream(conn.outputStream, 64 * 1024)).use { out ->
                // API key field
                out.writeBytes("--$boundary$LF")
                out.writeBytes("Content-Disposition: form-data; name=\"api_key\"$LF$LF")
                out.writeBytes("$IMGHIPPO_API_KEY$LF")
                
                // Image field
                out.writeBytes("--$boundary$LF")
                out.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"$fileName\"$LF")
                out.writeBytes("Content-Type: ${getMimeType(getFileExtension(fileName))}$LF$LF")
                
                FileInputStream(file).use { inputStream ->
                    val buf = ByteArray(64 * 1024)
                    val total = file.length()
                    var sent = 0L
                    var r: Int
                    
                    while (inputStream.read(buf).also { r = it } != -1) {
                        out.write(buf, 0, r)
                        sent += r
                        postProgress(callback, ((sent * 100) / total).toInt())
                    }
                }
                
                out.writeBytes(LF)
                out.writeBytes("--$boundary--$LF")
                out.flush()
            }
            
            val code = conn.responseCode
            val respStream = if (code == 200) conn.inputStream else conn.errorStream
            val resp = BufferedReader(InputStreamReader(respStream)).use { it.readText() }
            conn.disconnect()
            
            if (code == 200) {
                var urlStr: String? = null
                try {
                    val json = JSONObject(resp)
                    urlStr = when {
                        json.has("data") -> json.getJSONObject("data").getString("url")
                        json.has("url") -> json.getString("url")
                        else -> null
                    }
                } catch (ignored: Exception) {}
                
                if (!urlStr.isNullOrEmpty()) {
                    postSuccess(callback, urlStr, "imghippo|$urlStr")
                } else {
                    postFailure(callback, "$previousError | ImgHippo success but no URL in response: $resp")
                }
            } else {
                postFailure(callback, "$previousError | ImgHippo Upload failed ($code): $resp")
            }
        } catch (e: Exception) {
            postFailure(callback, "$previousError | ImgHippo Exception: ${e.message}")
        }
    }
    
    private fun extractFirstUrl(text: String): String? {
        return try {
            val i = text.indexOf("http")
            if (i >= 0) {
                val end = text.indexOf('"', i)
                if (end > i) text.substring(i, end) else text.substring(i)
            } else null
        } catch (ignored: Exception) {
            null
        }
    }
    
    /**
     * Handles uploading other file types to Cloudinary.
     */
    private fun uploadToCloudinary(filePath: String, fileName: String, callback: UploadCallback) {
        val boundary = "----CloudinaryBoundary${System.currentTimeMillis()}"
        val LF = "\r\n"
        val file = File(filePath)
        
        if (!file.exists()) {
            postFailure(callback, "File not found")
            return
        }
        
        try {
            val ts = System.currentTimeMillis() / 1000
            val rawSig = "timestamp=$ts$CLOUDINARY_API_SECRET"
            val signature = sha1Hex(rawSig)
            
            val url = URL(CLOUDINARY_UPLOAD_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.useCaches = false
            conn.doOutput = true
            conn.doInput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setChunkedStreamingMode(64 * 1024)
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            activeUploads[filePath] = conn
            
            DataOutputStream(BufferedOutputStream(conn.outputStream, 64 * 1024)).use { out ->
                writeFormField(out, boundary, "api_key", CLOUDINARY_API_KEY)
                writeFormField(out, boundary, "timestamp", ts.toString())
                writeFormField(out, boundary, "signature", signature)
                writeFileField(out, boundary, "file", file, fileName, callback)
                
                out.writeBytes("--$boundary--$LF")
                out.flush()
            }
            
            val status = conn.responseCode
            val respStream = if (status == 200) conn.inputStream else conn.errorStream
            val resp = respStream.bufferedReader().use { it.readText() }
            
            activeUploads.remove(filePath)
            conn.disconnect()
            
            if (status == 200) {
                val json = JSONObject(resp)
                val urlStr = json.getString("secure_url")
                val publicId = json.getString("public_id")
                val resourceType = json.getString("resource_type")
                postSuccess(callback, urlStr, "$publicId|$resourceType")
            } else {
                if (R2_ENABLED) {
                    uploadToR2(filePath, fileName, callback, "Cloudinary Upload failed: $resp")
                } else {
                    postFailure(callback, "Cloudinary Upload failed: $resp")
                }
            }
        } catch (e: Exception) {
            activeUploads.remove(filePath)
            if (R2_ENABLED) {
                uploadToR2(filePath, fileName, callback, "Cloudinary Exception: ${e.message}")
            } else {
                postFailure(callback, "Cloudinary Exception: ${e.message}")
            }
        }
    }
    
    // R2 S3-compatible upload via AWS SigV4
    private fun uploadToR2(filePath: String, fileName: String, callback: UploadCallback, previousError: String) {
        val file = File(filePath)
        if (!file.exists()) {
            postFailure(callback, "$previousError | File not found for R2")
            return
        }
        
        val objectKey = "$R2_KEY_PREFIX${System.currentTimeMillis()}_${safeKey(fileName)}"
        val contentType = "application/octet-stream"
        val host = R2_ENDPOINT.replace("https://", "").replace("http://", "")
        val path = "/$R2_BUCKET/$objectKey"
        val urlStr = "$R2_ENDPOINT$path"
        
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
            signS3(conn, "PUT", path, R2_REGION, host)
            
            // Stream file body
            DataOutputStream(BufferedOutputStream(conn.outputStream, 64 * 1024)).use { out ->
                FileInputStream(file).use { inputStream ->
                    val buf = ByteArray(64 * 1024)
                    val total = file.length()
                    var sent = 0L
                    var r: Int
                    
                    while (inputStream.read(buf).also { r = it } != -1) {
                        out.write(buf, 0, r)
                        sent += r
                        postProgress(callback, ((sent * 100) / total).toInt())
                    }
                }
                out.flush()
            }
            
            val code = conn.responseCode
            if (code == 200 || code == 201) {
                postSuccess(callback, urlStr, "r2|$objectKey|raw")
            } else {
                val err = conn.errorStream
                val resp = err?.bufferedReader()?.use { it.readText() } ?: ""
                postFailure(callback, "$previousError | R2 Upload failed ($code): $resp")
            }
            conn.disconnect()
        } catch (e: Exception) {
            postFailure(callback, "$previousError | R2 Exception: ${e.message}")
        }
    }
    
    private fun safeKey(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
    
    // Minimal SigV4 signer for S3-compatible R2 with UNSIGNED-PAYLOAD
    private fun signS3(conn: HttpURLConnection, method: String, canonicalPath: String, region: String, host: String) {
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
        val signingKey = getSignatureKey(R2_SECRET_KEY, dateStamp, region, S3_SERVICE)
        val signature = bytesToHex(hmacSHA256(signingKey, stringToSign))
        val authorization = "AWS4-HMAC-SHA256 Credential=$R2_ACCESS_KEY/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
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
    
    /**
     * Deletes a file. Only works for Cloudinary uploads.
     */
    @JvmStatic
    fun deleteByPublicId(publicIdWithType: String?, callback: DeleteCallback) {
        if (publicIdWithType == null || 
            publicIdWithType.startsWith("imgbb|") || 
            publicIdWithType.startsWith("postimages|") || 
            publicIdWithType.startsWith("imghippo|")) {
            postDeleteSuccess(callback)
            return
        }
        
        if (publicIdWithType.startsWith("r2|")) {
            val key = publicIdWithType.substring(3)
            val objectKey = if (key.contains("|")) {
                key.split("|", limit = 2)[0]
            } else {
                key
            }
            deleteFromR2(objectKey, callback)
            return
        }
        
        val (publicId, resourceType) = if (publicIdWithType.contains("|")) {
            val parts = publicIdWithType.split("|", limit = 2)
            parts[0] to parts[1]
        } else {
            publicIdWithType to "raw"
        }
        
        Thread {
            try {
                val ts = System.currentTimeMillis() / 1000
                val toSign = "public_id=$publicId&timestamp=$ts$CLOUDINARY_API_SECRET"
                val signature = sha1Hex(toSign)
                
                val destroyUrl = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/$resourceType/destroy"
                
                val conn = URL(destroyUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                
                val body = "public_id=$publicId&timestamp=$ts&api_key=$CLOUDINARY_API_KEY&signature=$signature"
                
                DataOutputStream(conn.outputStream).use { out ->
                    out.writeBytes(body)
                    out.flush()
                }
                
                val code = conn.responseCode
                val inputStream = if (code == 200) conn.inputStream else conn.errorStream
                val resp = inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                
                val js = JSONObject(resp)
                if (code == 200 && "ok" == js.optString("result")) {
                    postDeleteSuccess(callback)
                } else {
                    postDeleteFailure(callback, "Delete failed ($code): $resp")
                }
            } catch (e: Exception) {
                postDeleteFailure(callback, e.message ?: "Unknown error")
            }
        }.start()
    }
    
    private fun deleteFromR2(objectKey: String, callback: DeleteCallback) {
        if (!R2_ENABLED) {
            postDeleteSuccess(callback)
            return
        }
        
        val host = R2_ENDPOINT.replace("https://", "").replace("http://", "")
        val path = "/$R2_BUCKET/$objectKey"
        val urlStr = "$R2_ENDPOINT$path"
        
        Thread {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.setRequestProperty("Host", host)
                conn.setRequestProperty("x-amz-content-sha256", "UNSIGNED-PAYLOAD")
                conn.setRequestProperty("x-amz-date", amzDate())
                signS3(conn, "DELETE", path, R2_REGION, host)
                
                val code = conn.responseCode
                conn.disconnect()
                
                if (code == 204 || code == 200) {
                    postDeleteSuccess(callback)
                } else {
                    postDeleteFailure(callback, "R2 delete failed: $code")
                }
            } catch (e: Exception) {
                postDeleteFailure(callback, "R2 delete exception: ${e.message}")
            }
        }.start()
    }
    
    @JvmStatic
    fun cancelUpload(filePath: String) {
        activeUploads.remove(filePath)?.disconnect()
    }
    
    // --- HELPER METHODS ---
    
    private fun writeFormField(o: DataOutputStream, b: String, name: String, value: String) {
        val LF = "\r\n"
        o.writeBytes("--$b$LF")
        o.writeBytes("Content-Disposition: form-data; name=\"$name\"$LF$LF")
        o.writeBytes("$value$LF")
    }
    
    private fun writeFileField(
        o: DataOutputStream,
        b: String,
        field: String,
        f: File,
        fname: String,
        cb: UploadCallback
    ) {
        val LF = "\r\n"
        o.writeBytes("--$b$LF")
        o.writeBytes("Content-Disposition: form-data; name=\"$field\"; filename=\"$fname\"$LF")
        o.writeBytes("Content-Type: application/octet-stream$LF$LF")
        
        FileInputStream(f).use { inputStream ->
            val buf = ByteArray(64 * 1024)
            val total = f.length()
            var sent = 0L
            var r: Int
            
            while (inputStream.read(buf).also { r = it } != -1) {
                o.write(buf, 0, r)
                sent += r
                postProgress(cb, ((sent * 100) / total).toInt())
            }
        }
        o.writeBytes(LF)
    }
    
    private fun sha1Hex(s: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val d = md.digest(s.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }
    }
    
    private fun getFileExtension(name: String): String {
        val d = name.lastIndexOf('.')
        return if (d >= 0) name.substring(d + 1) else ""
    }
    
    private fun getMimeType(ext: String): String {
        val m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
        return m ?: "application/octet-stream"
    }
    
    // --- MAIN THREAD POSTING ---
    private fun postProgress(cb: UploadCallback, p: Int) {
        mainHandler.post { cb.onProgress(p) }
    }
    
    private fun postSuccess(cb: UploadCallback, url: String, id: String) {
        mainHandler.post { cb.onSuccess(url, id) }
    }
    
    private fun postFailure(cb: UploadCallback, e: String) {
        mainHandler.post { cb.onFailure(e) }
    }
    
    private fun postDeleteSuccess(cb: DeleteCallback) {
        mainHandler.post { cb.onSuccess() }
    }
    
    private fun postDeleteFailure(cb: DeleteCallback, e: String) {
        mainHandler.post { cb.onFailure(e) }
    }
}
