package com.synapse.social.studioasinc.backend.interfaces

interface ICompletionListener<T> {
    fun onComplete(result: T?, error: Exception?)
}
