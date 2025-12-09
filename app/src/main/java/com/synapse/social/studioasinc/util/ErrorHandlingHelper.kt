package com.synapse.social.studioasinc.util

import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.synapse.social.studioasinc.R

/**
 * Enhanced error handling helper with retry mechanisms
 * Provides user-friendly error messages and recovery options
 */
object ErrorHandlingHelper {

    /**
     * Error types for categorization
     */
    enum class ErrorType {
        NETWORK,
        AUTHENTICATION,
        PERMISSION,
        VALIDATION,
        SERVER,
        UNKNOWN
    }

    /**
     * Show error with retry option
     */
    fun showErrorWithRetry(
        view: View,
        message: String,
        onRetry: () -> Unit
    ) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                onRetry()
            }
            .setActionTextColor(view.context.getColor(R.color.md_theme_primary))
            .show()
    }

    /**
     * Show error without retry
     */
    fun showError(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(view, message, duration).show()
    }

    /**
     * Get user-friendly error message based on error type
     */
    fun getUserFriendlyMessage(context: Context, error: Throwable): String {
        val errorType = categorizeError(error)
        
        return when (errorType) {
            ErrorType.NETWORK -> context.getString(R.string.error_network)
            ErrorType.AUTHENTICATION -> context.getString(R.string.error_authentication)
            ErrorType.PERMISSION -> context.getString(R.string.error_permission)
            ErrorType.VALIDATION -> context.getString(R.string.error_validation)
            ErrorType.SERVER -> context.getString(R.string.error_server)
            ErrorType.UNKNOWN -> context.getString(R.string.error_unknown)
        }
    }

    /**
     * Categorize error based on exception type and message
     */
    private fun categorizeError(error: Throwable): ErrorType {
        val message = error.message?.lowercase() ?: ""
        
        return when {
            message.contains("network") || 
            message.contains("connection") || 
            message.contains("timeout") -> ErrorType.NETWORK
            
            message.contains("unauthorized") || 
            message.contains("authentication") || 
            message.contains("token") -> ErrorType.AUTHENTICATION
            
            message.contains("permission") || 
            message.contains("denied") -> ErrorType.PERMISSION
            
            message.contains("invalid") || 
            message.contains("validation") -> ErrorType.VALIDATION
            
            message.contains("server") || 
            message.contains("500") || 
            message.contains("503") -> ErrorType.SERVER
            
            else -> ErrorType.UNKNOWN
        }
    }

    /**
     * Show success message
     */
    fun showSuccess(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(view.context.getColor(R.color.md_theme_primaryContainer))
            .setTextColor(view.context.getColor(R.color.md_theme_onPrimaryContainer))
            .show()
    }

    /**
     * Show warning message
     */
    fun showWarning(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(view.context.getColor(R.color.md_theme_errorContainer))
            .setTextColor(view.context.getColor(R.color.md_theme_onErrorContainer))
            .show()
    }

    /**
     * Retry with exponential backoff
     */
    suspend fun retryWithBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> Unit
    ) {
        var currentDelay = initialDelay
        repeat(maxRetries) { attempt ->
            try {
                block()
                return
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    throw e
                }
                kotlinx.coroutines.delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
    }
}
