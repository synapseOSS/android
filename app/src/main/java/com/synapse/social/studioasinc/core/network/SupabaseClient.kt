package com.synapse.social.studioasinc.core.network

import android.util.Log
import com.synapse.social.studioasinc.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.MalformedURLException

/**
 * Exception thrown when Supabase configuration is invalid or missing.
 */
class ConfigurationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Supabase client singleton for the application.
 * Provides centralized access to Supabase services including Auth, Postgrest, Realtime, and Storage.
 */
object SupabaseClient {
    private const val TAG = "SupabaseClient"

    /**
     * Callback to open a URL in the browser.
     * Must be set by the active Activity (e.g., AuthActivity).
     */
    var openUrl: ((String) -> Unit)? = null
    
    /**
     * Lazy-initialized Supabase client instance.
     * Automatically configures all required modules and handles configuration errors gracefully.
     */
    val client by lazy {
        // Check if credentials are properly configured
        if (BuildConfig.SUPABASE_URL.isBlank() || 
            BuildConfig.SUPABASE_URL == "https://your-project.supabase.co" ||
            BuildConfig.SUPABASE_ANON_KEY.isBlank() || 
            BuildConfig.SUPABASE_ANON_KEY == "your-anon-key-here") {
            
            Log.e(TAG, "CRITICAL: Supabase credentials not configured!")
            Log.e(TAG, "Please update gradle.properties with your actual Supabase URL and key")
            
            // Fail fast - don't create dummy client that silently fails
            throw ConfigurationException(
                "Supabase not configured. Please set SUPABASE_URL and SUPABASE_ANON_KEY in gradle.properties"
            )
        }
        
        try {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth) {
                    // Configure URL opener for OAuth flows
                }
                install(Postgrest)
                install(Realtime)
                install(Storage) {
                    if (BuildConfig.SUPABASE_SYNAPSE_S3_ENDPOINT_URL.isNotBlank()) {
                        customUrl = BuildConfig.SUPABASE_SYNAPSE_S3_ENDPOINT_URL
                    }
                }
                httpEngine = OkHttp.create()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client: ${e.message}", e)
            // Fail fast instead of creating dummy client
            throw ConfigurationException("Failed to initialize Supabase client", e)
        }
    }
    
    /**
     * Check if Supabase is properly configured with valid credentials.
     * @return true if both URL and API key are configured, false otherwise
     */
    fun isConfigured(): Boolean {
        return BuildConfig.SUPABASE_URL.isNotBlank() && 
               BuildConfig.SUPABASE_URL != "https://your-project.supabase.co" &&
               BuildConfig.SUPABASE_ANON_KEY.isNotBlank() && 
               BuildConfig.SUPABASE_ANON_KEY != "your-anon-key-here"
    }
    
    /**
     * Get the configured Supabase URL.
     * @return The Supabase project URL
     */
    fun getUrl(): String = BuildConfig.SUPABASE_URL

    const val BUCKET_POST_MEDIA = "posts"
    const val BUCKET_USER_AVATARS = "avatars"
    const val BUCKET_USER_COVERS = "covers"

    /**
     * Construct a full Supabase Storage URL from a storage path.
     *
     * @param bucket The storage bucket name
     * @param path The path within the storage bucket
     * @return Full public URL for the storage object
     */
    fun constructStorageUrl(bucket: String, path: String): String {
        // Validate URL format - Ensure BuildConfig.SUPABASE_URL is a valid URL before concatenation
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        val supabaseUrl = BuildConfig.SUPABASE_URL
        try {
            URL(supabaseUrl)
        } catch (e: MalformedURLException) {
            throw ConfigurationException("Invalid Supabase URL configured: $supabaseUrl", e)
        }
        return "$supabaseUrl/storage/v1/object/public/$bucket/$path"
    }

    /**
     * Constructs URL for post media storage
     */
    fun constructMediaUrl(storagePath: String): String {
        return constructStorageUrl(BUCKET_POST_MEDIA, storagePath)
    }

    /**
     * Constructs URL for user avatar storage
     */
    fun constructAvatarUrl(storagePath: String): String {
        return constructStorageUrl(BUCKET_USER_AVATARS, storagePath)
    }
}
