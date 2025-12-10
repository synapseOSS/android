package com.synapse.social.studioasinc.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synapse.social.studioasinc.SynapseApp
import com.synapse.social.studioasinc.data.repository.PostRepository

class SyncWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as SynapseApp
        val postRepository = application.postRepository

        return try {
            postRepository.refreshPosts(0, 20) // Refresh the first page of posts
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
