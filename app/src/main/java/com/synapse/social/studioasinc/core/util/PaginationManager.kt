package com.synapse.social.studioasinc.core.util

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Generic pagination manager for RecyclerView lists
 * Handles pull-to-refresh and infinite scroll logic
 * 
 * @param T The type of items being paginated
 * @param pageSize Number of items to fetch per page (default: 20)
 * @param scrollThreshold Number of items from end that triggers next page load (default: 5)
 * @param maxCachedItems Maximum number of items to keep in memory (default: 200)
 * @param onLoadPage Callback to fetch a page of data
 * @param onError Callback for error handling
 * @param coroutineScope CoroutineScope for launching coroutines
 */
class PaginationManager<T>(
    private val pageSize: Int = 20,
    private val scrollThreshold: Int = 5,
    private val maxCachedItems: Int = 200,
    private val onLoadPage: suspend (page: Int, pageSize: Int) -> Result<List<T>>,
    private val onError: (String) -> Unit,
    private val coroutineScope: CoroutineScope
) {
    // State management
    private val _paginationState = MutableStateFlow<PaginationState<T>>(PaginationState.Initial)
    val paginationState: StateFlow<PaginationState<T>> = _paginationState.asStateFlow()
    
    // Internal state tracking
    private var currentPage = 0
    private var isLoading = false
    private var hasMoreData = true
    private val loadedItems = mutableListOf<T>()
    
    // Scroll listener and jobs
    private var scrollListener: RecyclerView.OnScrollListener? = null
    private var loadJob: Job? = null
    private var scrollJob: Job? = null
    
    // Performance monitoring
    private var lastLoadStartTime: Long = 0
    private var totalLoadTime: Long = 0
    private var loadCount: Int = 0
    private var errorCount: Int = 0
    
    /**
     * Sealed class representing different pagination states
     */
    sealed class PaginationState<out T> {
        object Initial : PaginationState<Nothing>()
        object Refreshing : PaginationState<Nothing>()
        data class LoadingMore<T>(val currentItems: List<T>) : PaginationState<T>()
        data class Success<T>(val items: List<T>, val hasMore: Boolean) : PaginationState<T>()
        data class Error<T>(val message: String, val currentItems: List<T>) : PaginationState<T>()
        data class EndOfList<T>(val items: List<T>) : PaginationState<T>()
    }
    
    /**
     * Sealed class representing different pagination error types
     */
    sealed class PaginationError {
        data class NetworkError(val message: String) : PaginationError()
        data class ApiError(val code: String, val message: String) : PaginationError()
        data class DataError(val message: String) : PaginationError()
        object NoMoreData : PaginationError()
    }
    
    /**
     * Refresh the list by resetting state and loading the first page
     * Prevents concurrent refresh operations and cancels pending loadNextPage jobs
     */
    fun refresh() {
        // Prevent concurrent refresh operations
        if (isLoading && _paginationState.value is PaginationState.Refreshing) {
            android.util.Log.d("PaginationManager", "Refresh already in progress, skipping")
            return
        }
        
        // Cancel pending loadNextPage jobs when refresh is triggered
        loadJob?.cancel()
        scrollJob?.cancel()
        
        // Transition to Refreshing state
        _paginationState.value = PaginationState.Refreshing
        isLoading = true
        
        // Reset state
        currentPage = 0
        hasMoreData = true
        loadedItems.clear()
        
        // Start performance monitoring
        lastLoadStartTime = System.currentTimeMillis()
        
        // Launch load operation with new job
        loadJob = coroutineScope.launch {
            try {
                // Call onLoadPage callback with page 0
                val result = onLoadPage(0, pageSize)
                
                // Log pagination timing metrics
                val loadDuration = System.currentTimeMillis() - lastLoadStartTime
                totalLoadTime += loadDuration
                loadCount++
                android.util.Log.d("PaginationManager", "Refresh completed in ${loadDuration}ms (avg: ${totalLoadTime / loadCount}ms)")
                
                result.fold(
                    onSuccess = { items ->
                        // Update loaded items
                        loadedItems.clear()
                        loadedItems.addAll(items)
                        
                        // Monitor memory usage with large lists
                        logMemoryUsage()
                        
                        // Determine if there's more data
                        hasMoreData = items.size >= pageSize
                        
                        // Update state to Success
                        _paginationState.value = PaginationState.Success(
                            items = loadedItems.toList(),
                            hasMore = hasMoreData
                        )
                        
                        isLoading = false
                    },
                    onFailure = { error ->
                        // Handle error
                        errorCount++
                        android.util.Log.e("PaginationManager", "Pagination error (total errors: $errorCount): ${error.message}")
                        handleError(error, loadedItems.toList())
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                errorCount++
                android.util.Log.e("PaginationManager", "Pagination error (total errors: $errorCount): ${e.message}")
                handleError(e, loadedItems.toList())
                isLoading = false
            }
        }
    }

    /**
     * Load the next page of data
     * Checks to prevent loading when already loading or at end of list
     */
    fun loadNextPage() {
        // Prevent loading if already loading or at end of list
        if (isLoading || !hasMoreData) {
            return
        }
        
        // Transition to LoadingMore state with current items
        _paginationState.value = PaginationState.LoadingMore(loadedItems.toList())
        isLoading = true
        
        // Calculate next page number
        val nextPage = currentPage + 1
        
        // Start performance monitoring
        lastLoadStartTime = System.currentTimeMillis()
        
        // Launch load operation
        loadJob = coroutineScope.launch {
            try {
                // Call onLoadPage callback with next page
                val result = onLoadPage(nextPage, pageSize)
                
                // Log pagination timing metrics
                val loadDuration = System.currentTimeMillis() - lastLoadStartTime
                totalLoadTime += loadDuration
                loadCount++
                android.util.Log.d("PaginationManager", "LoadNextPage completed in ${loadDuration}ms (avg: ${totalLoadTime / loadCount}ms)")
                
                result.fold(
                    onSuccess = { items ->
                        if (items.isEmpty()) {
                            // No more data available
                            hasMoreData = false
                            _paginationState.value = PaginationState.EndOfList(loadedItems.toList())
                        } else {
                            // Append new items to loadedItems list
                            loadedItems.addAll(items)
                            
                            // Enforce item limit
                            enforceItemLimit()
                            
                            // Monitor memory usage with large lists
                            logMemoryUsage()
                            
                            // Update current page
                            currentPage = nextPage
                            
                            // Determine if there's more data
                            hasMoreData = items.size >= pageSize
                            
                            // Update state to Success with hasMore flag
                            if (hasMoreData) {
                                _paginationState.value = PaginationState.Success(
                                    items = loadedItems.toList(),
                                    hasMore = true
                                )
                            } else {
                                _paginationState.value = PaginationState.EndOfList(loadedItems.toList())
                            }
                        }
                        
                        isLoading = false
                    },
                    onFailure = { error ->
                        // Handle error
                        errorCount++
                        android.util.Log.e("PaginationManager", "Pagination error (total errors: $errorCount): ${error.message}")
                        handleError(error, loadedItems.toList())
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                errorCount++
                android.util.Log.e("PaginationManager", "Pagination error (total errors: $errorCount): ${e.message}")
                handleError(e, loadedItems.toList())
                isLoading = false
            }
        }
    }

    /**
     * Attach pagination manager to a RecyclerView
     * Sets up scroll listener to detect when user approaches end of list
     * 
     * @param recyclerView The RecyclerView to attach to
     */
    fun attachToRecyclerView(recyclerView: RecyclerView) {
        // Remove existing listener if any
        scrollListener?.let { recyclerView.removeOnScrollListener(it) }
        
        // Create new scroll listener
        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                // Only check if scrolling down
                if (dy <= 0) return
                
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                
                // Calculate distance from end
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                val distanceFromEnd = totalItemCount - lastVisibleItem - 1
                
                // Trigger loadNextPage when within scrollThreshold
                if (distanceFromEnd <= scrollThreshold && !isLoading && hasMoreData) {
                    // Add debouncing to prevent rapid triggers (300ms delay)
                    scrollJob?.cancel()
                    scrollJob = coroutineScope.launch {
                        delay(300)
                        if (!isLoading && hasMoreData) {
                            loadNextPage()
                        }
                    }
                }
            }
        }
        
        // Attach listener to RecyclerView
        recyclerView.addOnScrollListener(scrollListener!!)
    }

    /**
     * Enforce item limit to prevent memory issues
     * Removes oldest items when limit exceeded and recalculates current page
     */
    private fun enforceItemLimit() {
        if (loadedItems.size > maxCachedItems) {
            val itemsToRemove = loadedItems.size - maxCachedItems
            
            // Remove oldest items from the beginning
            repeat(itemsToRemove) {
                if (loadedItems.isNotEmpty()) {
                    loadedItems.removeAt(0)
                }
            }
            
            // Recalculate current page after eviction
            currentPage = (loadedItems.size / pageSize)
        }
    }

    /**
     * Handle errors during pagination operations
     * Maps exceptions to PaginationError types and generates user-friendly messages
     * 
     * @param error The exception that occurred
     * @param currentItems The current list of items to preserve
     */
    private fun handleError(error: Throwable, currentItems: List<T>) {
        val paginationError = when {
            error is IOException -> PaginationError.NetworkError(error.message ?: "Network error")
            error.message?.contains("unauthorized", ignoreCase = true) == true -> 
                PaginationError.ApiError("AUTH_ERROR", "Authentication failed")
            error.message?.contains("serialization", ignoreCase = true) == true -> 
                PaginationError.DataError("Failed to parse data")
            error.message?.contains("no more data", ignoreCase = true) == true ->
                PaginationError.NoMoreData
            else -> PaginationError.NetworkError(error.message ?: "Unknown error")
        }
        
        // Generate user-friendly error message
        val userMessage = getUserFriendlyMessage(paginationError)
        
        // Preserve current items in Error state
        _paginationState.value = PaginationState.Error(
            message = userMessage,
            currentItems = currentItems
        )
        
        // Call onError callback with message
        onError(userMessage)
    }
    
    /**
     * Convert PaginationError to user-friendly message
     * 
     * @param error The PaginationError to convert
     * @return User-friendly error message
     */
    private fun getUserFriendlyMessage(error: PaginationError): String {
        return when (error) {
            is PaginationError.NetworkError -> "Check your internet connection and try again"
            is PaginationError.ApiError -> "Unable to load data. Please try again later"
            is PaginationError.DataError -> "Something went wrong. Please try again"
            PaginationError.NoMoreData -> "No more items to load"
        }
    }

    /**
     * Reset all pagination state
     * Clears loaded items, resets page counter, and returns to Initial state
     */
    fun reset() {
        // Cancel any ongoing operations
        loadJob?.cancel()
        scrollJob?.cancel()
        
        // Reset state
        currentPage = 0
        isLoading = false
        hasMoreData = true
        loadedItems.clear()
        
        // Return to Initial state
        _paginationState.value = PaginationState.Initial
    }
    
    /**
     * Detach pagination manager from RecyclerView
     * Removes scroll listener and cancels ongoing operations
     * 
     * @param recyclerView The RecyclerView to detach from
     */
    fun detachFromRecyclerView(recyclerView: RecyclerView) {
        scrollListener?.let { 
            recyclerView.removeOnScrollListener(it)
            scrollListener = null
        }
        
        // Cancel ongoing operations
        loadJob?.cancel()
        scrollJob?.cancel()
    }
    
    /**
     * Get the current list of loaded items
     * 
     * @return Immutable copy of loaded items
     */
    fun getCurrentItems(): List<T> {
        return loadedItems.toList()
    }
    
    /**
     * Check if pagination has reached the end of available data
     * 
     * @return True if no more data is available, false otherwise
     */
    fun isAtEnd(): Boolean {
        return !hasMoreData
    }
    
    /**
     * Log memory usage for performance monitoring
     * Tracks memory usage with large lists
     */
    private fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 // MB
        val maxMemory = runtime.maxMemory() / 1024 / 1024 // MB
        val itemCount = loadedItems.size
        
        android.util.Log.d("PaginationManager", 
            "Memory usage: ${usedMemory}MB / ${maxMemory}MB | Items loaded: $itemCount")
        
        // Warn if memory usage is high
        if (usedMemory > maxMemory * 0.8) {
            android.util.Log.w("PaginationManager", 
                "High memory usage detected: ${usedMemory}MB / ${maxMemory}MB")
        }
    }
    
    /**
     * Get performance metrics for analytics
     * 
     * @return Map of performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any> {
        return mapOf(
            "total_loads" to loadCount,
            "total_errors" to errorCount,
            "average_load_time_ms" to if (loadCount > 0) totalLoadTime / loadCount else 0,
            "items_loaded" to loadedItems.size,
            "current_page" to currentPage,
            "error_rate" to if (loadCount > 0) errorCount.toFloat() / loadCount else 0f
        )
    }
}
