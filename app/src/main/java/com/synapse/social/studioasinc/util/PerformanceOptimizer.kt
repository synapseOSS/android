package com.synapse.social.studioasinc.util

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Performance optimization utilities for chat
 * Reduces unnecessary updates and improves scrolling performance
 */
object PerformanceOptimizer {

    /**
     * Debounce function calls to reduce frequency
     */
    class Debouncer(
        private val delayMs: Long,
        private val coroutineScope: CoroutineScope
    ) {
        private var debounceJob: Job? = null

        fun debounce(action: () -> Unit) {
            debounceJob?.cancel()
            debounceJob = coroutineScope.launch {
                delay(delayMs)
                action()
            }
        }

        fun cancel() {
            debounceJob?.cancel()
        }
    }

    /**
     * Throttle function calls to limit frequency
     */
    class Throttler(
        private val intervalMs: Long,
        private val coroutineScope: CoroutineScope
    ) {
        private var lastExecutionTime = 0L
        private var throttleJob: Job? = null

        fun throttle(action: () -> Unit) {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastExecution = currentTime - lastExecutionTime

            if (timeSinceLastExecution >= intervalMs) {
                lastExecutionTime = currentTime
                action()
            } else {
                throttleJob?.cancel()
                throttleJob = coroutineScope.launch {
                    delay(intervalMs - timeSinceLastExecution)
                    lastExecutionTime = System.currentTimeMillis()
                    action()
                }
            }
        }

        fun cancel() {
            throttleJob?.cancel()
        }
    }

    /**
     * Optimize RecyclerView performance
     */
    fun optimizeRecyclerView(recyclerView: RecyclerView) {
        recyclerView.apply {
            // Enable view recycling optimizations
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            
            // Reduce overdraw
            clipToPadding = false
            clipChildren = false
            
            // Optimize nested scrolling
            isNestedScrollingEnabled = true
            
            // Prefetch items for smoother scrolling
            recycledViewPool.setMaxRecycledViews(0, 20)
        }
    }

    /**
     * Batch update helper for RecyclerView
     */
    class BatchUpdateHelper(
        private val adapter: RecyclerView.Adapter<*>,
        private val batchDelayMs: Long = 100
    ) {
        private val pendingUpdates = mutableListOf<() -> Unit>()
        private var updateJob: Job? = null

        fun queueUpdate(update: () -> Unit) {
            pendingUpdates.add(update)
        }

        fun executeBatch(coroutineScope: CoroutineScope) {
            updateJob?.cancel()
            updateJob = coroutineScope.launch {
                delay(batchDelayMs)
                if (pendingUpdates.isNotEmpty()) {
                    pendingUpdates.forEach { it() }
                    pendingUpdates.clear()
                }
            }
        }

        fun executeImmediately() {
            updateJob?.cancel()
            if (pendingUpdates.isNotEmpty()) {
                pendingUpdates.forEach { it() }
                pendingUpdates.clear()
            }
        }
    }

    /**
     * View visibility optimizer
     * Only updates views when they're actually visible
     */
    fun updateViewIfVisible(view: View, update: () -> Unit) {
        if (view.isShown && view.visibility == View.VISIBLE) {
            update()
        }
    }

    /**
     * Lazy initialization helper
     */
    class LazyInitializer<T>(private val initializer: () -> T) {
        private var value: T? = null
        private var initialized = false

        fun get(): T {
            if (!initialized) {
                value = initializer()
                initialized = true
            }
            return value!!
        }

        fun reset() {
            value = null
            initialized = false
        }

        fun isInitialized() = initialized
    }

    /**
     * Memory-efficient image loading helper
     */
    fun calculateInSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1

        if (originalHeight > reqHeight || originalWidth > reqWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Reduce layout passes by batching view updates
     */
    fun batchViewUpdates(vararg views: View, updates: () -> Unit) {
        views.forEach { it.visibility = View.INVISIBLE }
        updates()
        views.forEach { it.visibility = View.VISIBLE }
    }
}
