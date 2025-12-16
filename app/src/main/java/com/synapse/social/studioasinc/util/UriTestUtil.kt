package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.synapse.social.studioasinc.FileUtil

/**
 * Utility for testing and debugging URI conversion
 */
object UriTestUtil {
    
    private const val TAG = "UriTestUtil"
    
    /**
     * Test URI conversion and log the results
     */
    fun testUriConversion(context: Context, uri: Uri): String? {
        Log.d(TAG, "Testing URI conversion for: $uri")
        
        val result = FileUtil.convertUriToFilePath(context, uri)
        
        when {
            result == null -> {
                Log.w(TAG, "URI conversion failed - returned null")
            }
            result.startsWith("content://") -> {
                Log.e(TAG, "URI conversion failed - still a content URI: $result")
            }
            result.startsWith("/") -> {
                Log.i(TAG, "URI conversion successful - file path: $result")
            }
            else -> {
                Log.w(TAG, "URI conversion returned unexpected result: $result")
            }
        }
        
        return result
    }
    
    /**
     * Validate that a URL is not a content URI
     */
    fun isValidFileUrl(url: String): Boolean {
        return !url.startsWith("content://")
    }
    
    /**
     * Check if a list of media items contains any content URIs
     */
    fun validateMediaItems(mediaItems: List<com.synapse.social.studioasinc.model.MediaItem>): List<String> {
        val invalidUrls = mutableListOf<String>()
        
        mediaItems.forEach { item ->
            if (item.url.startsWith("content://")) {
                invalidUrls.add(item.url)
                Log.e(TAG, "Found invalid content URI in media item: ${item.url}")
            }
        }
        
        return invalidUrls
    }
}
