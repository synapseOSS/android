/*
 * ImageUploader - Modern Android image uploader with Multi-Provider Support
 * Copyright (c) 2025 Ashik (StudioAs Inc.)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.synapse.social.studioasinc

import android.content.Context
import com.synapse.social.studioasinc.data.local.AppSettingsManager
import com.synapse.social.studioasinc.data.local.StorageConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

object ImageUploader {


    private const val IMGBB_DEFAULT_KEY = "faa85ffbac0217ff67b5f3c4baa7fb29"
    private val client = OkHttpClient()

    enum class MediaType {
        PHOTO, VIDEO, OTHER
    }

    interface UploadCallback {
        fun onUploadComplete(imageUrl: String)
        fun onUploadError(errorMessage: String)
    }

    /**
     * Uploads an image using the configured storage provider.
     * @param context Context required to access AppSettingsManager
     * @param filePath Path to the image file
     * @param callback Callback for result
     */
    fun uploadImage(context: Context, filePath: String, callback: UploadCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get current configuration
                val appSettingsManager = AppSettingsManager.getInstance(context)
                val config = appSettingsManager.storageConfigFlow.first()
                
                val result = uploadImageSuspend(config, filePath)
                withContext(Dispatchers.Main) {
                    callback.onUploadComplete(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onUploadError(e.message ?: "Unknown error")
                }
            }
        }
    }

    private suspend fun uploadImageSuspend(config: StorageConfig, filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) throw IOException("File not found: $filePath")

        // Detect media type from file extension
        val mediaType = detectMediaType(file)
        
        // Get providers for this media type
        val providers = getProvidersForMediaType(config, mediaType)
        
        if (providers.isEmpty()) {
            throw IOException("No storage providers configured for ${mediaType.name.lowercase()} files. Please configure at least one provider in Settings.")
        }

        // Try each provider in sequence with fallback
        val errors = mutableListOf<String>()
        
        for ((index, provider) in providers.withIndex()) {
            try {
                android.util.Log.d("ImageUploader", "Attempting upload with provider: $provider (${index + 1}/${providers.size})")
                
                val result = when (provider) {
                    "ImgBB" -> uploadToImgBB(config.imgBBConfig.apiKey, file)
                    "Cloudinary" -> uploadToCloudinary(config.cloudinaryConfig.cloudName, config.cloudinaryConfig.apiKey, file)
                    "Supabase" -> uploadToSupabase(config.supabaseConfig.url, config.supabaseConfig.apiKey, config.supabaseConfig.bucketName, file)
                    "Cloudflare R2" -> throw IOException("Cloudflare R2 upload is not yet implemented.")
                    else -> throw IOException("Unknown provider: $provider")
                }
                
                android.util.Log.i("ImageUploader", "Upload successful using provider: $provider")
                return@withContext result
                
            } catch (e: Exception) {
                val errorMsg = "$provider: ${e.message}"
                errors.add(errorMsg)
                android.util.Log.w("ImageUploader", "Upload failed with $provider: ${e.message}")
                
                // If this is the last provider, throw an aggregate error
                if (index == providers.size - 1) {
                    throw IOException("All upload attempts failed:\n${errors.joinToString("\n")}")
                }
                // Otherwise, continue to next provider
            }
        }
        
        // This should never be reached, but just in case
        throw IOException("Upload failed with all configured providers")
    }

    /**
     * Detects the media type based on file extension
     */
    private fun detectMediaType(file: File): MediaType {
        return when (file.extension.lowercase()) {
            in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico") -> MediaType.PHOTO
            in listOf("mp4", "mov", "avi", "mkv", "webm", "flv", "wmv", "m4v") -> MediaType.VIDEO
            else -> MediaType.OTHER
        }
    }

    /**
     * Returns the list of providers configured for the given media type
     * With single provider selection, this returns a list with one item if configured
     */
    private fun getProvidersForMediaType(config: StorageConfig, mediaType: MediaType): List<String> {
        val provider = when (mediaType) {
            MediaType.PHOTO -> config.photoProvider
            MediaType.VIDEO -> config.videoProvider
            MediaType.OTHER -> config.otherProvider
        }
        return listOfNotNull(provider)
    }

    private fun uploadToImgBB(apiKey: String, file: File): String {
        val key = if (apiKey.isBlank()) IMGBB_DEFAULT_KEY else apiKey
        
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload?expiration=0&key=$key")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("ImgBB Error: ${response.code} ${response.message}")
            
            val responseBody = response.body?.string() ?: throw IOException("Empty response from ImgBB")
            try {
                val json = JSONObject(responseBody)
                return json.getJSONObject("data").getString("url")
            } catch (e: JSONException) {
                throw IOException("Invalid JSON from ImgBB: ${e.message}")
            }
        }
    }

    private fun uploadToCloudinary(cloudName: String, uploadPreset: String, file: File): String {
        // Implementation for Unsigned Upload (simpler)
        // URL: https://api.cloudinary.com/v1_1/<cloud_name>/image/upload
        // Param: upload_preset (We are using the apiKey field for this in Settings)
        
        if (cloudName.isBlank()) throw IOException("Cloudinary Cloud Name is missing")
        
        val preset = if (uploadPreset.isNotBlank()) uploadPreset else "ml_default" 
        
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", preset) 
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                 val errorBody = response.body?.string()
                 throw IOException("Cloudinary Error: ${response.code} $errorBody")
            }
            
            val responseBody = response.body?.string() ?: throw IOException("Empty response from Cloudinary")
            try {
                val json = JSONObject(responseBody)
                return json.getString("secure_url")
            } catch (e: JSONException) {
                throw IOException("Invalid JSON from Cloudinary")
            }
        }
    }
    
    private fun uploadToSupabase(url: String, apiKey: String, bucket: String, file: File): String {
        if (url.isBlank() || apiKey.isBlank() || bucket.isBlank()) {
            throw IOException("Supabase configuration missing (URL, Key, or Bucket)")
        }

        // Clean URL ensure no trailing slash
        val baseUrl = url.trimEnd('/')
        // Generate unique filename
        val fileName = "${UUID.randomUUID()}_${file.name}"
        // Supabase Storage API: POST /storage/v1/object/<bucket>/<path>
        val storageUrl = "$baseUrl/storage/v1/object/$bucket/$fileName"

        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(storageUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            // Supabase API usually requires 'apikey' header as well for the anon key, 
            // but Authorization: Bearer is for the user. 
            // If using service_role or anon key for the bucket access, stick to Authorization.
            // Let's add apikey just in case as it's common in their REST API.
            .addHeader("apikey", apiKey) 
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
             if (!response.isSuccessful) {
                 val errorBody = response.body?.string()
                 throw IOException("Supabase Error: ${response.code} $errorBody")
            }
            // Supabase returns metadata.
            // Construct public URL manually since we can't easily parse the JSON without a model and public URL is predictable.
            // Public URL: <url>/storage/v1/object/public/<bucket>/<filename>
            return "$baseUrl/storage/v1/object/public/$bucket/$fileName"
        }
    }
}
