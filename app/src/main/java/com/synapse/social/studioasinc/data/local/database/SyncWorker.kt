package com.synapse.social.studioasinc.data.local.database

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synapse.social.studioasinc.data.repository.PostRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SyncWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        val postRepository = entryPoint.postRepository()

        return try {
            postRepository.refreshPosts(0, 20) // Refresh the first page of posts
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun postRepository(): PostRepository
}
