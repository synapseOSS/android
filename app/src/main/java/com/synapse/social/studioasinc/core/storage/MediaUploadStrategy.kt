package com.synapse.social.studioasinc.core.storage

import com.synapse.social.studioasinc.data.local.database.StorageConfig
import java.io.File

/**
 * Interface defining a contract for file uploads.
 */
interface MediaUploadStrategy {
    suspend fun upload(
        file: File,
        config: StorageConfig,
        bucketName: String? = null,
        callback: MediaStorageService.UploadCallback
    )
}
