package com.synapse.social.studioasinc

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File

class DownloadCompletedReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "DownloadReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        when (intent.action) {
            DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == -1L) return
                
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                val uri = manager?.getUriForDownloadedFile(downloadId)
                
                uri?.let { installApk(context, it) }
            }
        }
    }
    
    private fun installApk(context: Context, apkUri: Uri) {
        try {
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    val file = File(apkUri.path ?: return)
                    val legacyUri = Uri.fromFile(file)
                    setDataAndType(legacyUri, "application/vnd.android.package-archive")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
        }
    }
}
