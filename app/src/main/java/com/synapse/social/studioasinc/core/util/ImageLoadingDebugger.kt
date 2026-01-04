package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class for debugging image loading issues
 */
object ImageLoadingDebugger {
    
    private const val TAG = "ImageLoadingDebugger"
    
    /**
     * Test image loading with detailed logging
     */
    suspend fun testImageLoading(context: Context, imageUrl: String?) {
        withContext(Dispatchers.Main) {
            Log.d(TAG, "Testing image loading for URL: $imageUrl")
            
            if (imageUrl.isNullOrBlank()) {
                Log.w(TAG, "Image URL is null or blank")
                return@withContext
            }
            
            // Check URL format
            when {
                imageUrl.startsWith("http://") -> {
                    Log.w(TAG, "Image URL uses HTTP (not HTTPS): $imageUrl")
                }
                imageUrl.startsWith("https://") -> {
                    Log.d(TAG, "Image URL uses HTTPS: $imageUrl")
                }
                else -> {
                    Log.w(TAG, "Image URL has unexpected format: $imageUrl")
                }
            }
            
            // Test with Glide
            try {
                Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .preload()
                Log.d(TAG, "Glide preload initiated for: $imageUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Exception during Glide preload: $imageUrl", e)
            }
        }
    }
    
    /**
     * Validate image URL format
     */
    fun validateImageUrl(url: String?): ValidationResult {
        return when {
            url.isNullOrBlank() -> ValidationResult.EMPTY
            url.startsWith("http://") -> ValidationResult.HTTP_NOT_HTTPS
            url.startsWith("https://") -> ValidationResult.VALID
            url.startsWith("file://") -> ValidationResult.LOCAL_FILE
            url.startsWith("content://") -> ValidationResult.CONTENT_URI
            else -> ValidationResult.INVALID_FORMAT
        }
    }
    
    enum class ValidationResult {
        VALID,
        EMPTY,
        HTTP_NOT_HTTPS,
        LOCAL_FILE,
        CONTENT_URI,
        INVALID_FORMAT
    }
}