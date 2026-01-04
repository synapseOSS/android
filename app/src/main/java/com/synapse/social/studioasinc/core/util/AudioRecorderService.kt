package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Service for recording audio messages
 * Handles voice recording with proper lifecycle management
 */
class AudioRecorderService(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorderService"
        private const val MAX_DURATION_MS = 300000 // 5 minutes max
        private const val AUDIO_SAMPLE_RATE = 44100
        private const val AUDIO_BIT_RATE = 128000
    }
    
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0
    private var _isRecording = false
    
    val isRecording: Boolean get() = _isRecording
    
    /**
     * Start recording audio
     * @return The file where audio is being saved
     */
    fun startRecording(): File? {
        if (_isRecording) {
            Log.w(TAG, "Already recording")
            return outputFile
        }
        
        try {
            // Create output file
            outputFile = createOutputFile()
            
            recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(AUDIO_SAMPLE_RATE)
                setAudioEncodingBitRate(AUDIO_BIT_RATE)
                setMaxDuration(MAX_DURATION_MS)
                setOutputFile(outputFile?.absolutePath)
                
                prepare()
                start()
            }
            
            startTime = System.currentTimeMillis()
            _isRecording = true
            
            Log.d(TAG, "Recording started: ${outputFile?.absolutePath}")
            return outputFile
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            return null
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for recording", e)
            cleanup()
            return null
        }
    }
    
    /**
     * Stop recording and return the recorded file
     * @return The recorded audio file
     */
    fun stopRecording(): File? {
        if (!_isRecording) {
            Log.w(TAG, "Not recording")
            return null
        }
        
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            _isRecording = false
            
            Log.d(TAG, "Recording stopped: ${outputFile?.absolutePath}")
            outputFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            cleanup()
            null
        }
    }
    
    /**
     * Cancel recording and delete the file
     */
    fun cancelRecording() {
        if (!_isRecording) return
        
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling recording", e)
        }
        
        // Delete the file
        outputFile?.delete()
        cleanup()
        
        Log.d(TAG, "Recording canceled")
    }
    
    /**
     * Get the current recording duration in milliseconds
     */
    fun getRecordingDuration(): Long {
        if (!_isRecording) return 0
        return System.currentTimeMillis() - startTime
    }
    
    /**
     * Get the amplitude of the current recording (for waveform visualization)
     * @return Normalized amplitude value (0.0 to 1.0)
     */
    fun getAmplitude(): Float {
        if (!_isRecording) return 0f
        
        return try {
            val maxAmplitude = recorder?.maxAmplitude ?: 0
            // Normalize to 0-1 range (max amplitude is around 32767)
            (maxAmplitude / 32767f).coerceIn(0f, 1f)
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun createOutputFile(): File {
        val cacheDir = context.cacheDir
        val fileName = "voice_${System.currentTimeMillis()}.m4a"
        return File(cacheDir, fileName)
    }
    
    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }
    
    private fun cleanup() {
        recorder?.release()
        recorder = null
        outputFile = null
        startTime = 0
        _isRecording = false
    }
    
    /**
     * Release resources when done
     */
    fun release() {
        if (_isRecording) {
            cancelRecording()
        }
        cleanup()
    }
}
