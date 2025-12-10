package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object CommentMediaUploader {
    private const val IMGBB_API_KEY = "faa85ffbac0217ff67b5f3c4baa7fb29"
    private const val IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload"
    
    sealed class UploadResult {
        data class Success(val url: String, val mediaType: String) : UploadResult()
        data class Error(val message: String) : UploadResult()
    }
    
    suspend fun uploadPhoto(context: Context, uri: Uri): UploadResult = withContext(Dispatchers.IO) {
        try {
            val imageBytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArrayOutputStream()
                input.copyTo(buffer)
                buffer.toByteArray()
            } ?: return@withContext UploadResult.Error("Failed to read image")
            
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            
            val client = OkHttpClient()
            val requestBody = FormBody.Builder()
                .add("key", IMGBB_API_KEY)
                .add("image", base64Image)
                .build()
            
            val request = Request.Builder()
                .url(IMGBB_UPLOAD_URL)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val imageUrl = json.getJSONObject("data").getString("url")
                UploadResult.Success(imageUrl, "photo")
            } else {
                UploadResult.Error("Upload failed: ${response.code}")
            }
        } catch (e: Exception) {
            UploadResult.Error("Upload error: ${e.message}")
        }
    }
    
    suspend fun uploadVideo(context: Context, uri: Uri): UploadResult = withContext(Dispatchers.IO) {
        UploadResult.Error("Video upload coming soon")
    }
    
    suspend fun uploadAudio(context: Context, uri: Uri): UploadResult = withContext(Dispatchers.IO) {
        UploadResult.Error("Audio upload coming soon")
    }
}
