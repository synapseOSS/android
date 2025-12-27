package com.synapse.social.studioasinc.util

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import com.synapse.social.studioasinc.ui.chat.ChatActivity
import com.synapse.social.studioasinc.StorageUtils
import java.util.ArrayList
import java.util.HashMap

/**
 * Handles activity results for file picking and attachment processing in chat.
 * Migrated to work with Supabase storage and modern Android file handling.
 */
class ActivityResultHandler(private val activity: ChatActivity) {

    companion object {
        private const val TAG = "ActivityResultHandler"
    }

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) { // REQ_CD_IMAGE_PICKER constant
            if (data != null) {
                processSelectedFiles(data)
            }
        }
    }

    private fun processSelectedFiles(data: Intent) {
        val resolvedFilePaths = ArrayList<String>()

        try {
            // Handle multiple files
            if (data.clipData != null) {
                for (i in 0 until data.clipData!!.itemCount) {
                    val fileUri = data.clipData!!.getItemAt(i).uri
                    val path = StorageUtils.getPathFromUri(activity.applicationContext, fileUri)
                    if (path != null && path.isNotEmpty()) {
                        resolvedFilePaths.add(path)
                    } else {
                        Log.w(TAG, "Failed to resolve file path for clip data item $i")
                    }
                }
            } 
            // Handle single file
            else if (data.data != null) {
                val fileUri = data.data
                val path = StorageUtils.getPathFromUri(activity.applicationContext, fileUri)
                if (path != null && path.isNotEmpty()) {
                    resolvedFilePaths.add(path)
                } else {
                    Log.w(TAG, "Failed to resolve file path for single data")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing file picker result: ${e.message}")
            Toast.makeText(activity, "Error processing selected files", Toast.LENGTH_SHORT).show()
            return
        }

        if (resolvedFilePaths.isNotEmpty()) {
            // Process the selected files - this would need to be implemented
            // based on the actual ChatActivity structure
            Log.d(TAG, "Selected ${resolvedFilePaths.size} files for attachment")
            Toast.makeText(activity, "Selected ${resolvedFilePaths.size} files", Toast.LENGTH_SHORT).show()
        } else {
            Log.w(TAG, "No valid file paths resolved from file picker")
            Toast.makeText(activity, "No valid files selected", Toast.LENGTH_SHORT).show()
        }
    }



    /**
     * Handle camera capture results
     */
    fun handleCameraResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Implementation for camera capture if needed
        // This would handle results from camera intents
    }

    /**
     * Handle document picker results
     */
    fun handleDocumentResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Implementation for document picker if needed
        // This would handle results from document picker intents
    }
}
