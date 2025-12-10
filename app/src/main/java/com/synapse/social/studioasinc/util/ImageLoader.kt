package com.synapse.social.studioasinc.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.R
import kotlinx.coroutines.delay

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
        val glideUrl = buildGlideUrlWithAuth(url)
        
        Glide.with(context)
            .load(glideUrl)
            .placeholder(placeholder)
            .error(placeholder)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.w(TAG, "Image load failed (attempt ${retryCount + 1}/${MAX_RETRIES + 1}): $url", e)
                    
                    if (retryCount < MAX_RETRIES) {
                        // Calculate exponential backoff delay
                        val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl retryCount)
                        
                        Log.d(TAG, "Retrying image load after ${delayMs}ms...")
                        
                        // Schedule retry with exponential backoff
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
                        
                        // Return true to prevent Glide from setting error drawable
                        // We'll handle it in the retry or final failure
                        return true
                    } else {
                        Log.e(TAG, "All retry attempts exhausted for: $url")
                        onFailure?.invoke()
                        // Return false to let Glide set the error drawable
                        return false
                    }
                }
                
                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d(TAG, "Image loaded successfully: $url")
                    onSuccess?.invoke()
                    // Return false to let Glide handle the drawable
                    return false
                }
            })
            .into(imageView)
    }
    
    /**
     * Build a GlideUrl with proper authentication headers for Supabase Storage.
     * 
     * @param url The image URL
     * @return GlideUrl with authentication headers if needed
     */
    private fun buildGlideUrlWithAuth(url: String): GlideUrl {
        // Check if this is a Supabase Storage URL that needs authentication
        val needsAuth = url.contains("supabase.co/storage") && 
                       !url.contains("/public/")
        
        return if (needsAuth) {
            val headers = LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .build()
            
            GlideUrl(url, headers)
        } else {
            GlideUrl(url)
        }
    }
    
    /**
     * Construct a full Supabase Storage URL from a storage path.
     * 
     * @param storagePath The path within the storage bucket (e.g., "user-avatars/123.jpg")
     * @param bucketName The storage bucket name (default: "post-media")
     * @return Full public URL for the storage object
     */
    fun constructStorageUrl(storagePath: String, bucketName: String = "post-media"): String {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        return "$supabaseUrl/storage/v1/object/public/$bucketName/$storagePath"
    }
    
    /**
     * Preload an image into Glide's cache without displaying it.
     * Useful for preloading images that will be displayed soon.
     * 
     * @param context Android context
     * @param url Image URL to preload
     */
    fun preloadImage(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        
        val glideUrl = buildGlideUrlWithAuth(url)
        Glide.with(context)
            .load(glideUrl)
            .preload()
    }
    
    /**
     * Clear Glide's memory cache.
     * Should be called on the main thread.
     */
    fun clearMemoryCache(context: Context) {
        Glide.get(context).clearMemory()
    }
    
    /**
     * Clear Glide's disk cache.
     * Should be called on a background thread.
     */
    suspend fun clearDiskCache(context: Context) {
        Glide.get(context).clearDiskCache()
    }
}
