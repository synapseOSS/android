package com.synapse.social.studioasinc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*

/**
 * Asynchronous upload service with progress notifications for attachments
 */
object AsyncUploadService {
    private const val TAG = "AsyncUploadService"
    private const val CHANNEL_ID = "upload_progress"
    private const val NOTIFICATION_ID_BASE = 1000
    
    private val uploadNotificationIds = mutableMapOf<String, Int>()
    private val uploadFileNames = mutableMapOf<String, String>()
    private val uploadJobs = mutableMapOf<String, Job>()
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    interface UploadProgressListener {
        fun onProgress(filePath: String, percent: Int)
        fun onSuccess(filePath: String, url: String, publicId: String)
        fun onFailure(filePath: String, error: String)
    }
    
    /**
     * Start asynchronous upload with progress notification
     */
    @JvmStatic
    fun uploadWithNotification(
        context: Context?,
        filePath: String?,
        fileName: String?,
        listener: UploadProgressListener?
    ) {
        if (context == null || filePath == null || fileName == null) {
            Log.e(TAG, "Invalid parameters for upload")
            return
        }
        
        // Create notification channel for Android O+
        createNotificationChannel(context)
        
        // Generate unique notification ID for this upload
        val notificationId = NOTIFICATION_ID_BASE + uploadNotificationIds.size
        uploadNotificationIds[filePath] = notificationId
        uploadFileNames[filePath] = fileName
        
        // Show initial notification
        showUploadNotification(context, notificationId, fileName, 0, "Starting upload...")
        
        // Start upload in background coroutine
        val job = serviceScope.launch {
            try {
                UploadFiles.uploadFile(filePath, fileName, object : UploadFiles.UploadCallback {
                    override fun onProgress(percent: Int) {
                        // Update notification and notify listener
                        showUploadNotification(context, notificationId, fileName, percent, "Uploading...")
                        listener?.onProgress(filePath, percent)
                    }
                    
                    override fun onSuccess(url: String, publicId: String) {
                        // Show success notification
                        showSuccessNotification(context, notificationId, fileName)
                        
                        // Clean up
                        uploadNotificationIds.remove(filePath)
                        uploadFileNames.remove(filePath)
                        uploadJobs.remove(filePath)
                        
                        // Notify listener
                        listener?.onSuccess(filePath, url, publicId)
                    }
                    
                    override fun onFailure(error: String) {
                        // Show failure notification
                        showFailureNotification(context, notificationId, fileName, error)
                        
                        // Clean up
                        uploadNotificationIds.remove(filePath)
                        uploadFileNames.remove(filePath)
                        uploadJobs.remove(filePath)
                        
                        // Notify listener
                        listener?.onFailure(filePath, error)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Upload execution error: ${e.message}")
                showFailureNotification(context, notificationId, fileName, "Upload failed: ${e.message}")
                uploadNotificationIds.remove(filePath)
                uploadFileNames.remove(filePath)
                uploadJobs.remove(filePath)
                listener?.onFailure(filePath, "Upload execution error: ${e.message}")
            }
        }
        
        uploadJobs[filePath] = job
    }
    
    /**
     * Cancel upload and remove notification
     */
    @JvmStatic
    fun cancelUpload(context: Context?, filePath: String?) {
        if (context == null || filePath == null) return
        
        // Cancel the coroutine job
        uploadJobs[filePath]?.cancel()
        uploadJobs.remove(filePath)
        
        // Cancel the upload
        UploadFiles.cancelUpload(filePath)
        
        // Remove notification
        uploadNotificationIds.remove(filePath)?.let { notificationId ->
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
        uploadFileNames.remove(filePath)
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Upload Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for file uploads"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show upload progress notification
     */
    private fun showUploadNotification(
        context: Context,
        notificationId: Int,
        fileName: String,
        progress: Int,
        status: String
    ) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle("Uploading: $fileName")
                .setContentText(status)
                .setProgress(100, progress, false)
                .setOngoing(false) // Make cancelable
                .setAutoCancel(true) // Auto cancel when tapped
                .setPriority(NotificationCompat.PRIORITY_LOW)
            
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error showing upload notification: ${e.message}")
        }
    }
    
    /**
     * Show upload success notification
     */
    private fun showSuccessNotification(context: Context, notificationId: Int, fileName: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle("Upload Complete")
                .setContentText("$fileName uploaded successfully")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            
            // Auto-dismiss success notification after 3 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error auto-dismissing success notification: ${e.message}")
                }
            }, 3000)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing success notification: ${e.message}")
        }
    }
    
    /**
     * Show upload failure notification
     */
    private fun showFailureNotification(
        context: Context,
        notificationId: Int,
        fileName: String,
        error: String
    ) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_error)
                .setContentTitle("Upload Failed")
                .setContentText("$fileName failed to upload")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            
            // Auto-dismiss failure notification after 5 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error auto-dismissing failure notification: ${e.message}")
                }
            }, 5000)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing failure notification: ${e.message}")
        }
    }
    
    /**
     * Get current upload count
     */
    @JvmStatic
    fun getActiveUploadCount(): Int = uploadNotificationIds.size
    
    /**
     * Cancel all uploads and clear notifications
     */
    @JvmStatic
    fun cancelAllUploads(context: Context?) {
        if (context == null) return
        
        uploadNotificationIds.keys.toList().forEach { filePath ->
            cancelUpload(context, filePath)
        }
    }
}
