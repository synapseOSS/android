package com.synapse.social.studioasinc.util

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout

/**
 * Helper for managing loading states with skeleton screens
 * Provides smooth transitions between loading and content states
 */
class LoadingStateHelper(
    private val contentView: View,
    private val loadingView: View? = null,
    private val emptyView: View? = null,
    private val errorView: View? = null
) {

    enum class State {
        LOADING,
        CONTENT,
        EMPTY,
        ERROR
    }

    private var currentState: State = State.LOADING

    /**
     * Show loading state with skeleton screen
     */
    fun showLoading() {
        if (currentState == State.LOADING) return
        currentState = State.LOADING
        
        animateTransition {
            contentView.visibility = View.GONE
            emptyView?.visibility = View.GONE
            errorView?.visibility = View.GONE
            loadingView?.visibility = View.VISIBLE
            
            // Start shimmer animation if available
            (loadingView as? ShimmerFrameLayout)?.startShimmer()
        }
    }

    /**
     * Show content state
     */
    fun showContent() {
        if (currentState == State.CONTENT) return
        currentState = State.CONTENT
        
        animateTransition {
            loadingView?.visibility = View.GONE
            emptyView?.visibility = View.GONE
            errorView?.visibility = View.GONE
            contentView.visibility = View.VISIBLE
            
            // Stop shimmer animation if available
            (loadingView as? ShimmerFrameLayout)?.stopShimmer()
        }
    }

    /**
     * Show empty state
     */
    fun showEmpty() {
        if (currentState == State.EMPTY) return
        currentState = State.EMPTY
        
        animateTransition {
            loadingView?.visibility = View.GONE
            contentView.visibility = View.GONE
            errorView?.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
            
            // Stop shimmer animation if available
            (loadingView as? ShimmerFrameLayout)?.stopShimmer()
        }
    }

    /**
     * Show error state
     */
    fun showError() {
        if (currentState == State.ERROR) return
        currentState = State.ERROR
        
        animateTransition {
            loadingView?.visibility = View.GONE
            contentView.visibility = View.GONE
            emptyView?.visibility = View.GONE
            errorView?.visibility = View.VISIBLE
            
            // Stop shimmer animation if available
            (loadingView as? ShimmerFrameLayout)?.stopShimmer()
        }
    }

    /**
     * Get current state
     */
    fun getCurrentState(): State = currentState

    /**
     * Animate transition between states
     */
    private fun animateTransition(action: () -> Unit) {
        // Fade out current view
        val currentView = when (currentState) {
            State.LOADING -> loadingView
            State.CONTENT -> contentView
            State.EMPTY -> emptyView
            State.ERROR -> errorView
        }
        
        currentView?.animate()
            ?.alpha(0f)
            ?.setDuration(150)
            ?.withEndAction {
                action()
                
                // Fade in new view
                val newView = when (currentState) {
                    State.LOADING -> loadingView
                    State.CONTENT -> contentView
                    State.EMPTY -> emptyView
                    State.ERROR -> errorView
                }
                
                newView?.alpha = 0f
                newView?.animate()
                    ?.alpha(1f)
                    ?.setDuration(200)
                    ?.start()
            }
            ?.start()
    }

    /**
     * Builder for creating LoadingStateHelper
     */
    class Builder(private val contentView: View) {
        private var loadingView: View? = null
        private var emptyView: View? = null
        private var errorView: View? = null

        fun setLoadingView(view: View) = apply { this.loadingView = view }
        fun setEmptyView(view: View) = apply { this.emptyView = view }
        fun setErrorView(view: View) = apply { this.errorView = view }

        fun build() = LoadingStateHelper(contentView, loadingView, emptyView, errorView)
    }
}

/**
 * Extension function for RecyclerView to show loading indicator
 */
fun RecyclerView.showLoadingIndicator(show: Boolean, progressBar: ProgressBar) {
    if (show) {
        progressBar.visibility = View.VISIBLE
        this.alpha = 0.5f
    } else {
        progressBar.visibility = View.GONE
        this.alpha = 1f
    }
}

/**
 * Extension function for showing inline loading state
 */
fun TextView.showLoadingText(isLoading: Boolean, loadingText: String, normalText: String) {
    if (isLoading) {
        this.text = loadingText
        this.isEnabled = false
    } else {
        this.text = normalText
        this.isEnabled = true
    }
}
