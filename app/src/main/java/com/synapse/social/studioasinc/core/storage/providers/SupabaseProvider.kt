package com.synapse.social.studioasinc.core.storage.providers

import com.synapse.social.studioasinc.core.storage.MediaStorageService
import com.synapse.social.studioasinc.core.storage.MediaUploadStrategy
import com.synapse.social.studioasinc.data.local.database.StorageConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SupabaseProvider : MediaUploadStrategy {

    override suspend fun upload(
        file: File,
        config: StorageConfig,
        bucketName: String?,
        callback: MediaStorageService.UploadCallback
    ) {
        val url = config.supabaseConfig.url
        val apiKey = config.supabaseConfig.apiKey
        val targetBucketName = bucketName ?: config.supabaseConfig.bucketName

        var client: io.github.jan.supabase.SupabaseClient? = null
        try {
            // Create a scoped client for this upload since credentials might differ from global instance
            client = createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = apiKey
            ) {
                install(Storage)
            }

            val bucket = client.storage.from(targetBucketName)
            val fileName = "${System.currentTimeMillis()}_${file.name}"

            // Initial progress
            withContext(Dispatchers.Main) {
                callback.onProgress(10)
            }

            // Upload the file
            bucket.upload(fileName, file.readBytes()) {
                 upsert = true
            }

            // Final progress
            withContext(Dispatchers.Main) {
                callback.onProgress(100)
            }

            // Get public URL
            val publicUrl = bucket.publicUrl(fileName)

            withContext(Dispatchers.Main) {
                callback.onSuccess(publicUrl, fileName)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback.onError("Supabase upload failed: ${e.message}")
            }
        } finally {
             client?.close()
        }
    }
}
