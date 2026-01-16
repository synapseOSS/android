/*
 * ImageUploader - Refactored to use MediaStorageService
 * Copyright (c) 2025 Ashik (StudioAs Inc.)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.synapse.social.studioasinc.core.storage

import android.content.Context
import com.synapse.social.studioasinc.data.local.database.AppSettingsManager
import kotlinx.coroutines.*

/**
 * ImageUploader - Refactored to use MediaStorageService
 * Maintains backward compatibility while delegating to the unified storage service
 */
object ImageUploader {
    
    interface UploadCallback {
        fun onUploadComplete(imageUrl: String)
        fun onUploadError(errorMessage: String)
    }
    
    /**
     * Upload an image using the configured storage provider with fallback to default
     */
    fun uploadImage(context: Context, filePath: String, callback: UploadCallback) {
        val appSettingsManager = AppSettingsManager.getInstance(context)
        val mediaStorageService = MediaStorageService(context, appSettingsManager)
        
        CoroutineScope(Dispatchers.IO).launch {
            // Explicitly pass arguments to match: suspend fun uploadFile(filePath: String, bucketName: String? = null, callback: UploadCallback)
            mediaStorageService.uploadFile(
                filePath = filePath,
                bucketName = null,
                callback = object : MediaStorageService.UploadCallback {
                    override fun onProgress(percent: Int) {
                        // Progress not exposed in legacy interface
                    }

                    override fun onSuccess(url: String, publicId: String) {
                        callback.onUploadComplete(url)
                    }

                    override fun onError(error: String) {
                        callback.onUploadError(error)
                    }
                }
            )
        }
    }
}
