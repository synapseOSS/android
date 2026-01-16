package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import coil.request.Disposable
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.R

/**
 * Utility class for loading images with retry logic and proper authentication.
 * Implements exponential backoff retry strategy for failed image loads.
 */
object ImageLoader {
    private const val TAG = "ImageLoader"
    private const val MAX_RETRIES = 2
    private const val INITIAL_RETRY_DELAY_MS = 100L
    
    /**
     * Load an image into an ImageView with retry logic and authentication headers.
     * 
     * @param context Android context
     * @param url Image URL to load
     * @param imageView Target ImageView
     * @param placeholder Placeholder drawable resource ID (optional)
     * @param onSuccess Callback invoked when image loads successfully (optional)
     * @param onFailure Callback invoked when all retries fail (optional)
     */
    fun loadImage(
        context: Context,
        url: String?,
        imageView: ImageView,
        placeholder: Int = R.drawable.default_image,
        onSuccess: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null
    ) {
        if (url.isNullOrBlank()) {
            imageView.setImageResource(placeholder)
            onFailure?.invoke()
            return
        }
        
        loadImageWithRetry(
            context = context,
            url = url,
            imageView = imageView,
            placeholder = placeholder,
            retryCount = 0,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
    
    /**
     * Internal method to load image with retry logic.
     */
    private fun loadImageWithRetry(
        context: Context,
        url: String,
        imageView: ImageView,
        placeholder: Int,
        retryCount: Int,
        onSuccess: (() -> Unit)?,
        onFailure: (() -> Unit)?
    ) {
        // Use imageView.load extension to correctly handle View recycling and lifecycle
        imageView.load(url) {
            placeholder(placeholder)
            error(placeholder)

            // Add Supabase Auth headers if needed
            addSupabaseAuthHeaders(this, url)

            listener(
                onSuccess = { _, _ ->
                    Log.d(TAG, "Image loaded successfully: $url")
                    onSuccess?.invoke()
                },
                onError = { _, _ ->
                    Log.w(TAG, "Image load failed (attempt ${retryCount + 1}/${MAX_RETRIES + 1}): $url")
                    
                    if (retryCount < MAX_RETRIES) {
                        // Calculate exponential backoff delay
                        val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl retryCount)
                        
                        Log.d(TAG, "Retrying image load after ${delayMs}ms...")
                        
                        // Schedule retry with exponential backoff
                        // Note: This relies on the ImageView still being active.
                        // If it was recycled, the new load call on it would cancel this postDelayed or the load itself.
                        imageView.postDelayed({
                            loadImageWithRetry(
                                context = context,
                                url = url,
                                imageView = imageView,
                                placeholder = placeholder,
                                retryCount = retryCount + 1,
                                onSuccess = onSuccess,
                                onFailure = onFailure
                            )
                        }, delayMs)
                    } else {
                        Log.e(TAG, "All retry attempts exhausted for: $url")
                        // Ensure placeholder is set on failure (though error() usually handles it)
                        imageView.setImageResource(placeholder)
                        onFailure?.invoke()
                    }
                }
            )
        }
    }
    
    /**
     * Add proper authentication headers for Supabase Storage.
     */
    private fun addSupabaseAuthHeaders(builder: ImageRequest.Builder, url: String) {
        // Check if this is a Supabase Storage URL that needs authentication
        val needsAuth = url.contains("supabase.co/storage") && 
                       !url.contains("/public/")
        
        if (needsAuth) {
            builder.addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            builder.addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
        }
    }

    /**
     * Helper to create an ImageRequest with authentication headers for use in Compose.
     */
    fun buildImageRequest(context: Context, url: String?): ImageRequest {
        val builder = ImageRequest.Builder(context)
            .data(url)
            
        if (!url.isNullOrBlank()) {
            addSupabaseAuthHeaders(builder, url)
        }

        return builder.build()
    }
    
    /**
     * Preload an image into Coil's cache without displaying it.
     * Useful for preloading images that will be displayed soon.
     * 
     * @param context Android context
     * @param url Image URL to preload
     */
    fun preloadImage(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        
        val requestBuilder = ImageRequest.Builder(context)
            .data(url)

        addSupabaseAuthHeaders(requestBuilder, url)

        context.imageLoader.enqueue(requestBuilder.build())
    }
    
    /**
     * Clear Coil's memory cache.
     * Should be called on the main thread.
     */
    fun clearMemoryCache(context: Context) {
        context.imageLoader.memoryCache?.clear()
    }
    
    /**
     * Clear Coil's disk cache.
     * Should be called on a background thread.
     */
    suspend fun clearDiskCache(context: Context) {
        context.imageLoader.diskCache?.clear()
    }
}
