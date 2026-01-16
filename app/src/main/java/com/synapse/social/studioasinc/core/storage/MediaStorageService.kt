package com.synapse.social.studioasinc.core.storage

import android.content.Context
import com.synapse.social.studioasinc.core.storage.providers.CloudinaryProvider
import com.synapse.social.studioasinc.core.storage.providers.ImgBBProvider
import com.synapse.social.studioasinc.core.storage.providers.R2Provider
import com.synapse.social.studioasinc.core.storage.providers.SupabaseProvider
import com.synapse.social.studioasinc.data.local.database.AppSettingsManager
import com.synapse.social.studioasinc.data.local.database.StorageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Unified media storage service that handles upload/download/link operations
 * for all media types with provider selection and fallback logic
 */
class MediaStorageService(
    private val context: Context,
    private val appSettingsManager: AppSettingsManager
) {
    
    enum class MediaType {
        PHOTO, VIDEO, OTHER
    }
    
    interface UploadCallback {
        fun onProgress(percent: Int)
        fun onSuccess(url: String, publicId: String = "")
        fun onError(error: String)
    }

    private val imgBBProvider = ImgBBProvider()
    private val cloudinaryProvider = CloudinaryProvider()
    private val supabaseProvider = SupabaseProvider()
    private val r2Provider = R2Provider()
    
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
        val providerName = getProviderForMediaType(config, mediaType)
        
        try {
            if (providerName == "Default") {
                uploadWithDefaultProvider(file, mediaType, config, callback)
            } else {
                val strategy = getStrategy(providerName)
                if (strategy != null) {
                    strategy.upload(file, config, bucketName, callback)
                } else {
                    callback.onError("Unknown provider: $providerName")
                }
            }
        } catch (e: Exception) {
            // Fallback to default if custom provider fails
            if (providerName != "Default") {
                android.util.Log.w("MediaStorageService", "Provider $providerName failed, falling back to default: ${e.message}")
                uploadWithDefaultProvider(file, mediaType, config, callback)
            } else {
                callback.onError("Upload failed: ${e.message}")
            }
        }
    }

    private fun getStrategy(providerName: String): MediaUploadStrategy? {
        return when (providerName) {
            "ImgBB" -> imgBBProvider
            "Cloudinary" -> cloudinaryProvider
            "Supabase" -> supabaseProvider
            "Cloudflare R2" -> r2Provider
            else -> null
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
     * Upload using default app-provided credentials or custom if available
     */
    private suspend fun uploadWithDefaultProvider(file: File, mediaType: MediaType, config: StorageConfig, callback: UploadCallback) {
        when (mediaType) {
            MediaType.PHOTO -> {
                android.util.Log.d("MediaStorageService", "Using default provider (ImgBB) for photo")
                imgBBProvider.upload(file, config, null, callback)
            }
            MediaType.VIDEO, MediaType.OTHER -> {
                android.util.Log.d("MediaStorageService", "Using default provider (Cloudinary) for video/other")
                cloudinaryProvider.upload(file, config, null, callback)
            }
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
}
