package com.synapse.social.studioasinc.ui.profile.utils

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

object NetworkOptimizer {
    
    private val requestCache = mutableMapOf<String, Pair<Long, Any>>()
    private const val CACHE_DURATION_MS = 60_000L // 1 minute
    
    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 10000,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(maxRetries - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                delay(currentDelay)
                currentDelay = min((currentDelay * 2.0.pow(attempt + 1)).toLong(), maxDelayMs)
            }
        }
        return block()
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T> getCached(key: String): T? {
        val (timestamp, value) = requestCache[key] ?: return null
        return if (System.currentTimeMillis() - timestamp < CACHE_DURATION_MS) {
            value as? T
        } else {
            requestCache.remove(key)
            null
        }
    }
    
    fun <T> cache(key: String, value: T) {
        requestCache[key] = System.currentTimeMillis() to value as Any
    }
    
    fun clearCache() {
        requestCache.clear()
    }
}
