package com.synapse.social.studioasinc.backend

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.util.RetryHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service for integrating with Google Gemini API for AI-powered message summarization.
 * 
 * Features:
 * - Generate concise summaries of long messages
 * - Rate limiting detection and handling
 * - Retry logic with exponential backoff
 * - Character count and reading time estimation
 */
class GeminiAIService(context: Context) {

    companion object {
        private const val TAG = "GeminiAIService"
        
        // API Configuration
        private const val GEMINI_API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
        private const val REQUEST_TIMEOUT_SECONDS = 10L
        
        // Rate Limiting
        private const val PREF_NAME = "gemini_ai_prefs"
        private const val KEY_RATE_LIMIT_RESET_TIME = "rate_limit_reset_time"
        
        // Retry Configuration
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        
        // Reading Time Calculation
        private const val AVERAGE_WORDS_PER_MINUTE = 200
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Data class for summary response including metadata
     */
    data class SummaryResult(
        val summary: String,
        val characterCount: Int,
        val estimatedReadTimeMinutes: Int
    )

    /**
     * Generate a concise summary of the provided text using Gemini API.
     * 
     * @param text The message text to summarize
     * @param maxLength Maximum length of the summary in tokens (default 100)
     * @return Result containing SummaryResult on success or error message on failure
     */
    suspend fun generateSummary(text: String, maxLength: Int = 100): Result<SummaryResult> = withContext(Dispatchers.IO) {
        // Check if rate limited
        if (isRateLimited()) {
            val resetTime = getRateLimitResetTime()
            val minutesRemaining = ((resetTime - System.currentTimeMillis()) / 60000).toInt()
            Log.w(TAG, "Rate limited - Minutes remaining: $minutesRemaining")
            return@withContext Result.failure(
                Exception("Rate limit reached. Try again in $minutesRemaining minutes")
            )
        }

        // Use RetryHandler for automatic retry with exponential backoff
        val retryConfig = RetryHandler.RetryConfig(
            maxAttempts = MAX_RETRY_ATTEMPTS,
            initialDelayMs = INITIAL_RETRY_DELAY_MS,
            maxDelayMs = 4000L,
            exponentialBase = 2.0
        )

        try {
            val result = RetryHandler.executeWithRetryResult(retryConfig) { attemptNumber ->
                Log.d(TAG, "Generating summary - Attempt: $attemptNumber, TextLength: ${text.length}")
                
                try {
                    val summaryText = performSummaryRequest(text, maxLength)
                    
                    // Calculate metadata
                    val characterCount = text.length
                    val estimatedReadTime = calculateReadingTime(text)
                    
                    SummaryResult(
                        summary = summaryText,
                        characterCount = characterCount,
                        estimatedReadTimeMinutes = estimatedReadTime
                    )
                } catch (e: RateLimitException) {
                    // Store rate limit reset time
                    val resetTime = System.currentTimeMillis() + (e.retryAfterMinutes * 60 * 1000)
                    prefs.edit().putLong(KEY_RATE_LIMIT_RESET_TIME, resetTime).apply()
                    
                    Log.e(TAG, "Rate limit exceeded - Reset time: $resetTime, RetryAfter: ${e.retryAfterMinutes}min", e)
                    // Don't retry rate limit errors
                    throw e
                }
            }
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Summary generation failed after all retries - TextLength: ${text.length}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Perform the actual HTTP request to Gemini API
     */
    private fun performSummaryRequest(text: String, maxLength: Int): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey == "your-gemini-api-key-here") {
            throw IllegalStateException("Gemini API key not configured. Please set GEMINI_API_KEY in gradle.properties")
        }

        // Build request JSON
        val requestJson = buildRequestJson(text, maxLength)
        
        val requestBody = requestJson.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$GEMINI_API_ENDPOINT?key=$apiKey")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()

        return when (response.code) {
            200 -> {
                val responseBody = response.body?.string() 
                    ?: throw IOException("Empty response body")
                parseSuccessResponse(responseBody)
            }
            429 -> {
                // Rate limit exceeded
                val retryAfter = response.header("Retry-After")?.toIntOrNull() ?: 60
                throw RateLimitException("Rate limit exceeded", retryAfter)
            }
            else -> {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Gemini API error: ${response.code} - $errorBody")
                throw IOException("API request failed with code ${response.code}: $errorBody")
            }
        }
    }

    /**
     * Build the JSON request for Gemini API
     */
    private fun buildRequestJson(text: String, maxLength: Int): JSONObject {
        val prompt = """
            Summarize the following message in 2-3 concise sentences, capturing the key points:
            
            $text
            
            Summary:
        """.trimIndent()

        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("maxOutputTokens", maxLength)
            })
        }
    }

    /**
     * Parse successful response from Gemini API
     */
    private fun parseSuccessResponse(responseBody: String): String {
        try {
            val json = JSONObject(responseBody)
            return json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini API response", e)
            throw IOException("Failed to parse API response: ${e.message}")
        }
    }

    /**
     * Check if the service is currently rate limited
     */
    fun isRateLimited(): Boolean {
        val resetTime = prefs.getLong(KEY_RATE_LIMIT_RESET_TIME, 0L)
        return System.currentTimeMillis() < resetTime
    }

    /**
     * Get the timestamp when rate limit will be reset
     */
    fun getRateLimitResetTime(): Long {
        return prefs.getLong(KEY_RATE_LIMIT_RESET_TIME, 0L)
    }

    /**
     * Calculate estimated reading time in minutes
     * Based on average reading speed of 200 words per minute
     */
    private fun calculateReadingTime(text: String): Int {
        val wordCount = text.split("\\s+".toRegex()).size
        val minutes = (wordCount.toDouble() / AVERAGE_WORDS_PER_MINUTE).toInt()
        return if (minutes < 1) 1 else minutes
    }

    /**
     * Custom exception for rate limiting
     */
    class RateLimitException(message: String, val retryAfterMinutes: Int) : Exception(message)
}
