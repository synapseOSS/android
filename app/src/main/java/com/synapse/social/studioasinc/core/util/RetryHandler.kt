package com.synapse.social.studioasinc.core.util

import android.util.Log
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility for handling retry logic with exponential backoff for network failures
 * Implements automatic retry for transient errors with configurable parameters
 */
object RetryHandler {

    private const val TAG = "RetryHandler"

    /**
     * Configuration for retry behavior
     */
    data class RetryConfig(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 1000L,
        val maxDelayMs: Long = 4000L,
        val exponentialBase: Double = 2.0,
        val retryableExceptions: List<Class<out Throwable>> = listOf(
            IOException::class.java,
            SocketTimeoutException::class.java,
            UnknownHostException::class.java
        )
    )

    /**
     * Result of a retry operation
     */
    sealed class RetryResult<T> {
        data class Success<T>(val value: T, val attemptNumber: Int) : RetryResult<T>()
        data class Failure<T>(
            val exception: Throwable,
            val attemptNumber: Int,
            val isRetryable: Boolean
        ) : RetryResult<T>()
    }

    /**
     * Execute an operation with automatic retry on network failures
     * Uses exponential backoff: 1s, 2s, 4s
     * 
     * @param config Retry configuration
     * @param operation The suspend function to execute
     * @return Result of the operation after all retry attempts
     */
    suspend fun <T> executeWithRetry(
        config: RetryConfig = RetryConfig(),
        operation: suspend (attemptNumber: Int) -> T
    ): RetryResult<T> {
        var currentAttempt = 1
        var lastException: Throwable? = null

        while (currentAttempt <= config.maxAttempts) {
            try {
                Log.d(TAG, "Executing operation - Attempt: $currentAttempt/${config.maxAttempts}")
                
                val result = operation(currentAttempt)
                
                if (currentAttempt > 1) {
                    Log.d(TAG, "Operation succeeded after $currentAttempt attempts")
                }
                
                return RetryResult.Success(result, currentAttempt)
            } catch (e: Exception) {
                lastException = e
                
                val isRetryable = isRetryableException(e, config)
                
                Log.w(TAG, "Operation failed - Attempt: $currentAttempt/${config.maxAttempts}, Retryable: $isRetryable, Error: ${e.message}")
                
                // If not retryable or last attempt, return failure
                if (!isRetryable || currentAttempt >= config.maxAttempts) {
                    Log.e(TAG, "Operation failed permanently after $currentAttempt attempts", e)
                    return RetryResult.Failure(e, currentAttempt, isRetryable)
                }
                
                // Calculate delay with exponential backoff
                val delayMs = calculateBackoffDelay(
                    attemptNumber = currentAttempt,
                    initialDelayMs = config.initialDelayMs,
                    maxDelayMs = config.maxDelayMs,
                    exponentialBase = config.exponentialBase
                )
                
                Log.d(TAG, "Retrying after ${delayMs}ms delay")
                delay(delayMs)
                
                currentAttempt++
            }
        }

        // Should never reach here, but handle it just in case
        return RetryResult.Failure(
            lastException ?: Exception("Unknown error"),
            currentAttempt,
            false
        )
    }

    /**
     * Execute an operation with retry and return Result type
     * Convenience method that wraps executeWithRetry
     * 
     * @param config Retry configuration
     * @param operation The suspend function to execute
     * @return Result.success on success, Result.failure on failure
     */
    suspend fun <T> executeWithRetryResult(
        config: RetryConfig = RetryConfig(),
        operation: suspend (attemptNumber: Int) -> T
    ): Result<T> {
        return when (val result = executeWithRetry(config, operation)) {
            is RetryResult.Success -> Result.success(result.value)
            is RetryResult.Failure -> Result.failure(result.exception)
        }
    }

    /**
     * Check if an exception is retryable based on configuration
     */
    private fun isRetryableException(
        exception: Throwable,
        config: RetryConfig
    ): Boolean {
        // Check if exception type is in retryable list
        val isRetryableType = config.retryableExceptions.any { retryableClass ->
            retryableClass.isInstance(exception)
        }

        if (isRetryableType) {
            return true
        }

        // Check exception message for retryable indicators
        val message = exception.message?.lowercase() ?: ""
        val retryableKeywords = listOf(
            "timeout",
            "connection",
            "network",
            "temporary",
            "unavailable"
        )

        return retryableKeywords.any { keyword -> message.contains(keyword) }
    }

    /**
     * Calculate exponential backoff delay
     * Formula: min(initialDelay * (base ^ (attempt - 1)), maxDelay)
     * 
     * @param attemptNumber Current attempt number (1-indexed)
     * @param initialDelayMs Initial delay in milliseconds
     * @param maxDelayMs Maximum delay in milliseconds
     * @param exponentialBase Base for exponential calculation
     * @return Delay in milliseconds
     */
    fun calculateBackoffDelay(
        attemptNumber: Int,
        initialDelayMs: Long = 1000L,
        maxDelayMs: Long = 4000L,
        exponentialBase: Double = 2.0
    ): Long {
        val exponentialDelay = (initialDelayMs * Math.pow(exponentialBase, (attemptNumber - 1).toDouble())).toLong()
        return exponentialDelay.coerceAtMost(maxDelayMs)
    }

    /**
     * Check if an error should trigger a retry
     * Public method for external use
     */
    fun shouldRetry(exception: Throwable, attemptNumber: Int, maxAttempts: Int = 3): Boolean {
        if (attemptNumber >= maxAttempts) {
            return false
        }

        return isRetryableException(exception, RetryConfig())
    }

    /**
     * Get user-friendly retry message
     */
    fun getRetryMessage(attemptNumber: Int, maxAttempts: Int): String {
        return when {
            attemptNumber < maxAttempts -> "Retrying... (Attempt $attemptNumber of $maxAttempts)"
            else -> "All retry attempts failed"
        }
    }
}
