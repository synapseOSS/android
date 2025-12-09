package com.synapse.social.studioasinc.permissionreq

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import java.util.Timer
import java.util.TimerTask

class AskPermission(private val activity: Activity) {
    private val timer = Timer()
    private var permissionCheckCount = 0
    private val maxPermissionChecks = 5 // Max times to check before giving up

    companion object {
        const val PERMISSION_REQUEST_CODE = 1000
    }

    fun checkAndRequestPermissions() {
        if (areAllPermissionsGranted()) {
            startMainActivity()
            return
        }

        requestNeededPermissions()

        // Start checking periodically (fallback for Sketchware's limitation)
        timer.schedule(object : TimerTask() {
            override fun run() {
                activity.runOnUiThread {
                    permissionCheckCount++
                    if (areAllPermissionsGranted()) {
                        timer.cancel()
                        startMainActivity()
                    } else if (permissionCheckCount >= maxPermissionChecks) {
                        timer.cancel()
                        showPermissionDeniedMessage()
                    }
                }
            }
        }, 1000, 1000) // Check every 1 second
    }

    private fun areAllPermissionsGranted(): Boolean {
        val microphoneGranted = ContextCompat.checkSelfPermission(
            activity, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check notification permission
            val notificationGranted = ContextCompat.checkSelfPermission(
                activity, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            // Check media permissions
            val mediaImagesGranted = ContextCompat.checkSelfPermission(
                activity, 
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            
            val mediaVideoGranted = ContextCompat.checkSelfPermission(
                activity, 
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
            
            val mediaAudioGranted = ContextCompat.checkSelfPermission(
                activity, 
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            
            notificationGranted && mediaImagesGranted && mediaVideoGranted && mediaAudioGranted && microphoneGranted
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val readStorageGranted = ContextCompat.checkSelfPermission(
                activity, 
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            
            val writeStorageGranted = ContextCompat.checkSelfPermission(
                activity, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            
            readStorageGranted && writeStorageGranted && microphoneGranted
        } else {
            microphoneGranted
        }
    }

    private fun requestNeededPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ permissions
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12 permissions
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startMainActivity() {
        val intent = Intent(activity, com.synapse.social.studioasinc.MainActivity::class.java)
        activity.startActivity(intent)
        activity.finish()
    }

    private fun showPermissionDeniedMessage() {
        // You can customize this message
     //   SketchwareUtil.showMessage(activity, "App needs permissions to work properly!")
    }

    fun cleanup() {
        timer.cancel()
    }
}
