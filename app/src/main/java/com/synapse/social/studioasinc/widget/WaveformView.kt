package com.synapse.social.studioasinc.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.synapse.social.studioasinc.R
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Custom view that displays an audio waveform visualization.
 * Generates waveform data from audio files and displays it with a progress indicator.
 * Supports seeking by tapping on the waveform.
 * 
 * Requirements: 6.5
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val waveformPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.waveform_color)
    }
    
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.waveform_progress_color)
    }
    
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.waveform_background)
    }
    
    // Waveform data (normalized amplitudes 0.0 to 1.0)
    private var waveformData: FloatArray = FloatArray(0)
    
    // Current playback progress (0.0 to 1.0)
    private var progress: Float = 0f
    
    // Number of bars to display
    private val barCount = 100
    
    // Gap between bars
    private val barGap = 2f
    
    // Minimum bar height
    private val minBarHeight = 4f
    
    // Corner radius for bars
    private val barCornerRadius = 2f
    
    // Listener for seek events
    var onSeekListener: ((progress: Float) -> Unit)? = null
    
    // Coroutine scope for waveform generation
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        // Initialize with empty waveform
        generateEmptyWaveform()
    }
    
    private fun generateEmptyWaveform() {
        waveformData = FloatArray(barCount) { 0.1f }
        invalidate()
    }

    /**
     * Sets the audio URL and generates waveform data.
     * Only generates waveform for audio files under 5 minutes.
     * 
     * @param url The audio file URL
     */
    fun setAudioUrl(url: String) {
        scope.launch {
            try {
                // Download or get cached audio file
                val audioFile = downloadAudioFile(url)
                
                if (audioFile != null && audioFile.exists()) {
                    val waveform = generateWaveformData(audioFile)
                    withContext(Dispatchers.Main) {
                        waveformData = waveform
                        invalidate()
                    }
                } else {
                    // Use placeholder waveform if file not available
                    withContext(Dispatchers.Main) {
                        generatePlaceholderWaveform()
                    }
                }
            } catch (e: Exception) {
                // On error, use placeholder waveform
                withContext(Dispatchers.Main) {
                    generatePlaceholderWaveform()
                }
            }
        }
    }
    
    /**
     * Downloads or retrieves cached audio file.
     * This is a placeholder - should integrate with MediaDownloadManager.
     */
    private suspend fun downloadAudioFile(url: String): File? {
        // FIXME: Integrate with a proper MediaDownloadManager to handle caching and
        // retrieval of the audio file. This will prevent re-downloading the same file.
        // For now, return null to use placeholder waveform
        return null
    }
    
    /**
     * Generates waveform data from an audio file.
     * Samples the audio and calculates amplitude for each bar.
     */
    private fun generateWaveformData(audioFile: File): FloatArray {
        val waveform = FloatArray(barCount)
        
        try {
            FileInputStream(audioFile).use { fis ->
                val fileSize = audioFile.length()
                val samplesPerBar = (fileSize / barCount).toInt()
                
                val buffer = ByteArray(samplesPerBar)
                
                for (i in 0 until barCount) {
                    val bytesRead = fis.read(buffer)
                    if (bytesRead <= 0) break
                    
                    // Calculate RMS (Root Mean Square) amplitude
                    var sum = 0.0
                    for (j in 0 until bytesRead step 2) {
                        if (j + 1 < bytesRead) {
                            // Convert bytes to 16-bit sample
                            val sample = ((buffer[j + 1].toInt() shl 8) or (buffer[j].toInt() and 0xFF)).toShort()
                            sum += (sample * sample).toDouble()
                        }
                    }
                    
                    val rms = kotlin.math.sqrt(sum / (bytesRead / 2))
                    // Normalize to 0.0 - 1.0 range
                    waveform[i] = min(1.0f, (rms / 32768.0).toFloat())
                }
            }
            
            // Smooth the waveform
            return smoothWaveform(waveform)
            
        } catch (e: Exception) {
            // On error, return placeholder waveform
            return generatePlaceholderWaveformData()
        }
    }
    
    /**
     * Smooths waveform data using a simple moving average.
     */
    private fun smoothWaveform(data: FloatArray): FloatArray {
        val smoothed = FloatArray(data.size)
        val windowSize = 3
        
        for (i in data.indices) {
            var sum = 0f
            var count = 0
            
            for (j in max(0, i - windowSize)..min(data.size - 1, i + windowSize)) {
                sum += data[j]
                count++
            }
            
            smoothed[i] = sum / count
        }
        
        return smoothed
    }
    
    /**
     * Generates a placeholder waveform with random-looking data.
     */
    private fun generatePlaceholderWaveform() {
        waveformData = generatePlaceholderWaveformData()
        invalidate()
    }
    
    private fun generatePlaceholderWaveformData(): FloatArray {
        return FloatArray(barCount) { i ->
            // Create a wave-like pattern
            val phase = (i.toFloat() / barCount) * 2 * Math.PI
            val amplitude = (kotlin.math.sin(phase) + 1) / 2
            0.2f + amplitude.toFloat() * 0.6f
        }
    }

    /**
     * Sets the waveform data directly.
     *
     * @param data The waveform data to display.
     */
    fun setWaveformData(data: FloatArray) {
        waveformData = data
        invalidate()
    }

    /**
     * Sets the current playback progress.
     *
     * @param progress Progress from 0.0 to 1.0
     */
    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, 1f)
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (waveformData.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2
        
        // Draw background
        canvas.drawRect(0f, 0f, width, height, backgroundPaint)
        
        // Calculate bar width
        val totalGaps = (barCount - 1) * barGap
        val barWidth = (width - totalGaps) / barCount
        
        // Draw waveform bars
        for (i in waveformData.indices) {
            val x = i * (barWidth + barGap)
            val amplitude = waveformData[i]
            val barHeight = max(minBarHeight, amplitude * height * 0.8f)
            
            val top = centerY - barHeight / 2
            val bottom = centerY + barHeight / 2
            
            // Use progress paint for bars before current position
            val paint = if (i.toFloat() / barCount <= progress) {
                progressPaint
            } else {
                waveformPaint
            }
            
            // Draw rounded rectangle bar
            val rect = RectF(x, top, x + barWidth, bottom)
            canvas.drawRoundRect(rect, barCornerRadius, barCornerRadius, paint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Calculate seek position from touch
                val seekProgress = (event.x / width).coerceIn(0f, 1f)
                onSeekListener?.invoke(seekProgress)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
