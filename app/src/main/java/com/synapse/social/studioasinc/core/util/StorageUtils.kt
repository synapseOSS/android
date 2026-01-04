package com.synapse.social.studioasinc.core.util

object StorageUtils {
    fun getFilePathFromUri(uri: Any): String = ""
    fun getPathFromUri(uri: Any): String = ""
}

// Extension function for Any to check if it's not empty
fun Any?.isNotEmpty(): Boolean = this != null && this.toString().isNotEmpty()
