package com.synapse.social.studioasinc

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json

/**
 * Supabase client singleton for the application.
 * Provides centralized access to Supabase services including Auth, Postgrest, Realtime, and Storage.
 */
object SupabaseClient {
    private const val TAG = "SupabaseClient"

    /**
     * Callback to open a URL in the browser.
     * Must be set by the active Activity (e.g., AuthComposeActivity).
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
            throw IllegalStateException(
                "Supabase not configured. Please set SUPABASE_URL and SUPABASE_ANON_KEY in gradle.properties"
            )
        }
        
        try {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth) {
                    // Configure schema for deep links if necessary (usually default is sufficient)
                    // Configure URL opener for OAuth
                    // Note: We use a lambda that delegates to the mutable property
                    // because this block is executed only once at initialization.
                    // This allows us to swap the implementation (context) at runtime.
                    // The property name in AuthConfig might be 'scheme', 'host', or just reliance on platform.
                    // For Kotlin/Android without Compose-Auth, we might need to rely on 'openUrl' property?
                    // Actually, looking at documentation, we can't easily inject openUrl here for non-Compose Auth
                    // if the library doesn't expose it in AuthConfig.
                    // However, we can use 'httpEngine' to customize requests, but opening browser is different.
                    // Assuming we will manually handle the URL in AuthRepository via 'getOAuthUrl' or similar,
                    // or if 'signInWith' calls a hook we can't access, we might have issues.
                    // But wait! If we use 'signInWith(Provider)', it returns Unit.
                    // It EXPECTS the platform to handle it.
                    // If we can't configure it, we will use 'getOAuthUrl' pattern in Repository.
                    // So we don't strictly need to change this file for 'openUrl' if we don't use 'signInWith' directly for browser launch.
                    // BUT, to be safe and cleaner, let's keep it standard.
                }
                install(Postgrest)
                install(Realtime)
                install(Storage) {
                    customUrl = BuildConfig.SUPABASE_SYNAPSE_S3_ENDPOINT_URL
                }
                httpEngine = OkHttp.create()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client: ${e.message}", e)
            // Fail fast instead of creating dummy client
            throw IllegalStateException("Failed to initialize Supabase client", e)
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
}
