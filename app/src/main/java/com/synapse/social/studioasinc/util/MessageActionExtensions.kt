package com.synapse.social.studioasinc.util

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.chat.presentation.MessageActionsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Extension functions for handling message action results in UI components
 * Provides convenient methods for displaying errors and success messages
 */

/**
 * Collect a Flow of MessageActionState and display results in Snackbar
 * 
 * @param view The view to attach Snackbar to
 * @param onSuccess Optional callback for success state
 * @param onError Optional callback for error state
 * @param retryAction Optional retry action for errors
 */
fun Fragment.collectMessageActionState(
    flow: Flow<MessageActionsViewModel.MessageActionState>,
    view: View,
    onSuccess: ((String) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    retryAction: (() -> Unit)? = null
) {
    viewLifecycleOwner.lifecycleScope.launch {
        flow.collect { state ->
            when (state) {
                is MessageActionsViewModel.MessageActionState.Success -> {
                    ErrorHandler.showSuccessSnackbar(view, state.message)
                    onSuccess?.invoke(state.message)
                }
                is MessageActionsViewModel.MessageActionState.Error -> {
                    ErrorHandler.showErrorSnackbar(
                        view = view,
                        message = state.error,
                        retryAction = retryAction
                    )
                    onError?.invoke(state.error)
                }
                is MessageActionsViewModel.MessageActionState.Loading -> {
                    // Handle loading state if needed
                }
                is MessageActionsViewModel.MessageActionState.Idle -> {
                    // Handle idle state if needed
                }
            }
        }
    }
}

/**
 * Collect a Flow of ForwardState and display results in Snackbar
 * 
 * @param view The view to attach Snackbar to
 * @param onSuccess Optional callback for success state with forwarded count
 * @param onError Optional callback for error state
 * @param retryAction Optional retry action for errors
 */
fun Fragment.collectForwardState(
    flow: Flow<MessageActionsViewModel.ForwardState>,
    view: View,
    onSuccess: ((Int) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    retryAction: (() -> Unit)? = null
) {
    viewLifecycleOwner.lifecycleScope.launch {
        flow.collect { state ->
            when {
                state.forwardedCount > 0 && state.error == null -> {
                    val message = requireContext().getString(
                        com.synapse.social.studioasinc.R.string.success_forward_count,
                        state.forwardedCount
                    )
                    ErrorHandler.showSuccessSnackbar(view, message)
                    onSuccess?.invoke(state.forwardedCount)
                }
                state.error != null -> {
                    ErrorHandler.showErrorSnackbar(
                        view = view,
                        message = state.error,
                        retryAction = retryAction
                    )
                    onError?.invoke(state.error)
                }
                state.isForwarding -> {
                    // Handle loading state if needed
                }
            }
        }
    }
}

/**
 * Collect a Flow of AISummaryState and display results in Snackbar
 * 
 * @param view The view to attach Snackbar to
 * @param onSuccess Optional callback for success state with summary
 * @param onError Optional callback for error state
 * @param retryAction Optional retry action for errors
 */
fun Fragment.collectAISummaryState(
    flow: Flow<MessageActionsViewModel.AISummaryState>,
    view: View,
    onSuccess: ((String) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    retryAction: (() -> Unit)? = null
) {
    viewLifecycleOwner.lifecycleScope.launch {
        flow.collect { state ->
            when {
                state.summary != null -> {
                    onSuccess?.invoke(state.summary)
                }
                state.error != null -> {
                    ErrorHandler.showErrorSnackbar(
                        view = view,
                        message = state.error,
                        retryAction = if (state.rateLimitResetTime == 0L) retryAction else null
                    )
                    onError?.invoke(state.error)
                }
                state.isGenerating -> {
                    // Handle loading state if needed
                }
            }
        }
    }
}

/**
 * Example usage in a Fragment:
 * 
 * ```kotlin
 * // Forward message with error handling
 * collectForwardState(
 *     flow = viewModel.forwardMessage(messageId, messageData, targetChatIds),
 *     view = binding.root,
 *     onSuccess = { count ->
 *         // Handle success
 *         dismiss()
 *     },
 *     onError = { error ->
 *         // Handle error
 *     },
 *     retryAction = {
 *         // Retry the operation
 *         viewModel.forwardMessage(messageId, messageData, targetChatIds)
 *     }
 * )
 * 
 * // Edit message with error handling
 * collectMessageActionState(
 *     flow = viewModel.editMessage(messageId, newContent),
 *     view = binding.root,
 *     onSuccess = { message ->
 *         dismiss()
 *     },
 *     retryAction = {
 *         viewModel.editMessage(messageId, newContent)
 *     }
 * )
 * 
 * // Generate AI summary with error handling
 * collectAISummaryState(
 *     flow = viewModel.generateAISummary(messageId, messageText),
 *     view = binding.root,
 *     onSuccess = { summary ->
 *         // Display summary in dialog
 *         showSummaryDialog(summary)
 *     },
 *     retryAction = {
 *         viewModel.generateAISummary(messageId, messageText)
 *     }
 * )
 * ```
 */
