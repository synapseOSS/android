package com.synapse.social.studioasinc.data.repository.deletion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.model.deletion.DeletionRequest
import com.synapse.social.studioasinc.data.model.deletion.DeletionType
import com.synapse.social.studioasinc.data.model.deletion.DeletionError
import javax.inject.Inject

/**
 * Background service for handling large deletion operations.
 * Runs deletion operations in the background with progress notifications.
 * Requirements: 6.1, 6.2
 */
class BackgroundDeletionService : Service() {
    
    @Inject
    lateinit var batchDeletionManager: BatchDeletionManager
    
    @Inject
    lateinit var chatHistoryManager: ChatHistoryManager
    
    @Inject
    lateinit var userNotificationManager: UserNotificationManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentDeletionJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deletionRequest = intent?.getSerializableExtra(EXTRA_DELETION_REQUEST) as? DeletionRequest
        
        if (deletionRequest != null) {
            startForegroundDeletion(deletionRequest)
        } else {
            stopSelf()
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        currentDeletionJob?.cancel()
        serviceScope.cancel()
    }
    
    /**
     * Starts foreground deletion with progress notifications
     */
    private fun startForegroundDeletion(request: DeletionRequest) {
        val notification = createProgressNotification(0, 0, "Starting deletion...")
        startForeground(NOTIFICATION_ID, notification)
        
        currentDeletionJob = serviceScope.launch {
            try {
                // Observe batch progress for notifications
                launch {
                    batchDeletionManager.batchProgress.collectLatest { progress ->
                        progress?.let { updateProgressNotification(it) }
                    }
                }
                
                // Perform the deletion
                val result = when (request.type) {
                    DeletionType.COMPLETE_HISTORY -> {
                        batchDeletionManager.performBatchDeletion(
                            userId = request.userId,
                            chatIds = null
                        )
                    }
                    DeletionType.SELECTIVE_CHATS -> {
                        batchDeletionManager.performBatchDeletion(
                            userId = request.userId,
                            chatIds = request.chatIds
                        )
                    }
                }
                
                // Show completion notification
                showCompletionNotification(result.success, result.totalMessagesDeleted)
                
                // Notify user through notification manager
                if (result.success) {
                    userNotificationManager.notifyDeletionCompleted(request, result)
                } else {
                    val error = DeletionError.SystemError(
                        message = "Deletion completed with errors. ${result.completedOperations.size}/${result.completedOperations.size + result.failedOperations.size} operations succeeded.",
                        recoverable = true
                    )
                    userNotificationManager.notifyDeletionFailed(request, error)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("BackgroundDeletionService", "Deletion failed", e)
                showErrorNotification(e.message ?: "Unknown error occurred")
                
                val error = DeletionError.SystemError(
                    message = "Deletion failed: ${e.message}",
                    recoverable = false
                )
                userNotificationManager.notifyDeletionFailed(request, error)
            } finally {
                stopSelf()
            }
        }
    }
    
    /**
     * Updates progress notification with current batch progress
     */
    private fun updateProgressNotification(progress: BatchProgress) {
        val progressPercent = if (progress.totalOperations > 0) {
            (progress.completedOperations * 100) / progress.totalOperations
        } else {
            0
        }
        
        val timeRemaining = progress.estimatedTimeRemaining?.let { remaining ->
            val minutes = remaining / 60000
            val seconds = (remaining % 60000) / 1000
            if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
        } ?: "Calculating..."
        
        val notification = createProgressNotification(
            progressPercent,
            progress.completedOperations,
            "Batch ${progress.completedBatches}/${progress.totalBatches} • ETA: $timeRemaining"
        )
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Creates progress notification
     */
    private fun createProgressNotification(
        progressPercent: Int,
        completedOperations: Int,
        statusText: String
    ): Notification {
        val cancelIntent = Intent(this, BackgroundDeletionService::class.java).apply {
            action = ACTION_CANCEL_DELETION
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Deleting Chat History")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.auto_delete_24px)
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_close,
                "Cancel",
                cancelPendingIntent
            )
            .build()
    }
    
    /**
     * Shows completion notification
     */
    private fun showCompletionNotification(success: Boolean, messagesDeleted: Int) {
        val title = if (success) "Deletion Completed" else "Deletion Completed with Errors"
        val text = if (success) {
            "Successfully deleted $messagesDeleted messages"
        } else {
            "Deletion completed with some errors. $messagesDeleted messages deleted."
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(if (success) R.drawable.ic_check_circle else R.drawable.ic_warning)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }
    
    /**
     * Shows error notification
     */
    private fun showErrorNotification(errorMessage: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Deletion Failed")
            .setContentText(errorMessage)
            .setSmallIcon(R.drawable.ic_error)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }
    
    /**
     * Creates notification channel for deletion notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat History Deletion",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for chat history deletion operations"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    companion object {
        private const val CHANNEL_ID = "chat_deletion_channel"
        private const val NOTIFICATION_ID = 1001
        private const val COMPLETION_NOTIFICATION_ID = 1002
        private const val ERROR_NOTIFICATION_ID = 1003
        
        const val EXTRA_DELETION_REQUEST = "deletion_request"
        const val ACTION_CANCEL_DELETION = "cancel_deletion"
        
        /**
         * Starts background deletion service
         */
        fun startDeletion(context: Context, request: DeletionRequest) {
            val intent = Intent(context, BackgroundDeletionService::class.java).apply {
                putExtra(EXTRA_DELETION_REQUEST, request)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * Cancels background deletion service
         */
        fun cancelDeletion(context: Context) {
            val intent = Intent(context, BackgroundDeletionService::class.java).apply {
                action = ACTION_CANCEL_DELETION
            }
            context.stopService(intent)
        }
    }
}