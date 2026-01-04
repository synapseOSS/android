package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.synapse.social.studioasinc.R
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized error handling utility for message actions
 * Maps technical errors to user-friendly messages and provides consistent error display
 */
object ErrorHandler {

    private const val TAG = "ErrorHandler"

    /**
     * Error types for message actions
     */
    enum class ErrorType {
        REPLY,
        FORWARD,
        EDIT,
        DELETE,
        AI_SUMMARY,
        NETWORK,
        GENERIC
    }

    /**
     * Map technical exception to user-friendly error message
     * 
     * @param context Android context for string resources
     * @param errorType The type of action that failed
     * @param exception The exception that occurred
     * @param messageId Optional message ID for logging context
     * @param userId Optional user ID for logging context
     * @return User-friendly error message string
     */
    fun getErrorMessage(
        context: Context,
        errorType: ErrorType,
        exception: Throwable,
        messageId: String? = null,
        userId: String? = null
    ): String {
        // Log the technical error with full context
        logError(errorType, exception, messageId, userId)

        // Map to user-friendly message
        return when (errorType) {
            ErrorType.REPLY -> getReplyErrorMessage(context, exception)
            ErrorType.FORWARD -> getForwardErrorMessage(context, exception)
            ErrorType.EDIT -> getEditErrorMessage(context, exception)
            ErrorType.DELETE -> getDeleteErrorMessage(context, exception)
            ErrorType.AI_SUMMARY -> getAISummaryErrorMessage(context, exception)
            ErrorType.NETWORK -> getNetworkErrorMessage(context, exception)
            ErrorType.GENERIC -> getGenericErrorMessage(context, exception)
        }
    }

    /**
     * Display error message in a Snackbar with retry action if applicable
     * 
     * @param view The view to attach the Snackbar to
     * @param message The error message to display
     * @param duration Snackbar duration (default: LENGTH_LONG)
     * @param retryAction Optional retry action callback
     */
    fun showErrorSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_LONG,
        retryAction: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, duration)
        
        if (retryAction != null) {
            snackbar.setAction(view.context.getString(R.string.error_retry_action)) {
                retryAction.invoke()
            }
        }
        
        snackbar.show()
    }

    /**
     * Display success message in a Snackbar
     * 
     * @param view The view to attach the Snackbar to
     * @param message The success message to display
     * @param duration Snackbar duration (default: LENGTH_SHORT)
     */
    fun showSuccessSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        Snackbar.make(view, message, duration).show()
    }

    /**
     * Check if error is a network-related error
     */
    fun isNetworkError(exception: Throwable): Boolean {
        return exception is IOException ||
                exception is SocketTimeoutException ||
                exception is UnknownHostException ||
                exception.message?.contains("network", ignoreCase = true) == true ||
                exception.message?.contains("connection", ignoreCase = true) == true ||
                exception.message?.contains("timeout", ignoreCase = true) == true
    }

    /**
     * Check if error is a rate limit error
     */
    fun isRateLimitError(exception: Throwable): Boolean {
        return exception.message?.contains("rate limit", ignoreCase = true) == true ||
                exception.message?.contains("429", ignoreCase = true) == true
    }

    /**
     * Check if error is retryable
     */
    fun isRetryableError(exception: Throwable): Boolean {
        return isNetworkError(exception) || 
                exception.message?.contains("timeout", ignoreCase = true) == true ||
                exception.message?.contains("temporary", ignoreCase = true) == true
    }

    // ==================== Private Helper Methods ====================

    private fun getReplyErrorMessage(context: Context, exception: Throwable): String {
        return when {
            exception.message?.contains("deleted", ignoreCase = true) == true ->
                context.getString(R.string.error_reply_deleted)
            else ->
                context.getString(R.string.error_reply_generic)
        }
    }

    private fun getForwardErrorMessage(context: Context, exception: Throwable): String {
        return when {
            isNetworkError(exception) ->
                context.getString(R.string.error_forward_no_network)
            exception.message?.contains("permission", ignoreCase = true) == true -> {
                // Extract chat name if available
                val chatName = extractChatName(exception.message)
                if (chatName != null) {
                    context.getString(R.string.error_forward_no_permission, chatName)
                } else {
                    context.getString(R.string.error_forward_generic)
                }
            }
            else ->
                context.getString(R.string.error_forward_generic)
        }
    }

    private fun getEditErrorMessage(context: Context, exception: Throwable): String {
        return when {
            isNetworkError(exception) ->
                context.getString(R.string.error_edit_no_network)
            exception.message?.contains("too old", ignoreCase = true) == true ->
                context.getString(R.string.error_edit_too_old)
            exception.message?.contains("empty", ignoreCase = true) == true ->
                context.getString(R.string.error_edit_empty)
            exception.message?.contains("not found", ignoreCase = true) == true ->
                context.getString(R.string.error_edit_not_found)
            else ->
                context.getString(R.string.error_edit_generic)
        }
    }

    private fun getDeleteErrorMessage(context: Context, exception: Throwable): String {
        return when {
            isNetworkError(exception) ->
                context.getString(R.string.error_delete_no_network)
            exception.message?.contains("already deleted", ignoreCase = true) == true ->
                context.getString(R.string.error_delete_already_deleted)
            else ->
                context.getString(R.string.error_delete_generic)
        }
    }

    private fun getAISummaryErrorMessage(context: Context, exception: Throwable): String {
        return when {
            isRateLimitError(exception) -> {
                val minutes = extractMinutesFromRateLimitError(exception.message)
                context.getString(R.string.error_ai_summary_rate_limit, minutes)
            }
            isNetworkError(exception) ->
                context.getString(R.string.error_ai_summary_network)
            exception is SocketTimeoutException ->
                context.getString(R.string.error_ai_summary_timeout)
            exception.message?.contains("too short", ignoreCase = true) == true ->
                context.getString(R.string.error_ai_summary_too_short)
            exception.message?.contains("API key", ignoreCase = true) == true ->
                context.getString(R.string.error_ai_summary_api_key)
            exception.message?.contains("unavailable", ignoreCase = true) == true ->
                context.getString(R.string.error_ai_summary_unavailable)
            else ->
                context.getString(R.string.error_ai_summary_generic)
        }
    }

    private fun getNetworkErrorMessage(context: Context, exception: Throwable): String {
        return when (exception) {
            is SocketTimeoutException ->
                context.getString(R.string.error_network_timeout)
            is UnknownHostException ->
                context.getString(R.string.error_network_unavailable)
            else ->
                context.getString(R.string.error_network_connection)
        }
    }

    private fun getGenericErrorMessage(context: Context, exception: Throwable): String {
        return exception.message ?: context.getString(R.string.error_unexpected)
    }

    /**
     * Log error with full context for debugging
     */
    private fun logError(
        errorType: ErrorType,
        exception: Throwable,
        messageId: String?,
        userId: String?
    ) {
        val contextInfo = buildString {
            append("ErrorType: $errorType")
            if (messageId != null) append(", MessageId: $messageId")
            if (userId != null) append(", UserId: $userId")
        }
        
        Log.e(TAG, "Message action error - $contextInfo", exception)
    }

    /**
     * Extract chat name from error message if available
     */
    private fun extractChatName(message: String?): String? {
        if (message == null) return null
        
        // Try to extract chat name from error message
        // Example: "You don't have permission to send messages in [Chat Name]"
        val regex = "\\[([^\\]]+)\\]".toRegex()
        val match = regex.find(message)
        return match?.groupValues?.getOrNull(1)
    }

    /**
     * Extract minutes from rate limit error message
     */
    private fun extractMinutesFromRateLimitError(message: String?): Int {
        if (message == null) return 60 // Default to 60 minutes
        
        // Try to extract number from error message
        val regex = "(\\d+)\\s*minute".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(message)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 60
    }
}
