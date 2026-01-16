package com.synapse.social.studioasinc.ui.profile.utils

import android.content.ComponentCallbacks2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object MemoryManager {
    
    private val _memoryPressure = MutableStateFlow(MemoryPressure.NORMAL)
    val memoryPressure: StateFlow<MemoryPressure> = _memoryPressure
    
    const val MAX_CACHED_POSTS = 50
    const val MAX_CACHED_IMAGES = 100
    
    fun handleMemoryTrim(level: Int) {
        _memoryPressure.value = when (level) {
            @Suppress("DEPRECATION")
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            @Suppress("DEPRECATION")
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> MemoryPressure.CRITICAL
            @Suppress("DEPRECATION")
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            @Suppress("DEPRECATION")
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> MemoryPressure.LOW
            else -> MemoryPressure.NORMAL
        }
    }
    
    fun <T> limitCacheSize(list: List<T>, maxSize: Int = MAX_CACHED_POSTS): List<T> {
        return if (list.size > maxSize) list.takeLast(maxSize) else list
    }
}

enum class MemoryPressure {
    NORMAL, LOW, CRITICAL
}
