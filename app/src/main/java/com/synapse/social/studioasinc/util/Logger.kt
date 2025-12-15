package com.synapse.social.studioasinc.util

import android.util.Log
import com.synapse.social.studioasinc.BuildConfig

object Logger {
    private const val TAG = "Synapse"
    
    fun d(message: String, tag: String = TAG) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
    
    fun i(message: String, tag: String = TAG) {
        Log.i(tag, message)
    }
    
    fun w(message: String, tag: String = TAG) {
        Log.w(tag, message)
    }
    
    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        Log.e(tag, message, throwable)
    }
}

// Extension functions for classes
fun Any.logd(message: String) = Logger.d(message, this::class.java.simpleName)
fun Any.logi(message: String) = Logger.i(message, this::class.java.simpleName)
fun Any.logw(message: String) = Logger.w(message, this::class.java.simpleName)
fun Any.loge(message: String, throwable: Throwable? = null) = Logger.e(message, throwable, this::class.java.simpleName)
