package com.synapse.social.studioasinc.core.util

class ImageUploader {
    fun uploadImage(path: String, callback: UploadCallback) {
        callback.onUploadComplete("")
    }
}

interface UploadCallback {
    fun onUploadComplete(url: String)
    fun onUploadError(error: String)
}
