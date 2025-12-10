package com.synapse.social.studioasinc.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.ViewAudioPlayerBinding
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * Custom audio player view that wraps ExoPlayer for audio playback.
 * Provides controls for play, pause, seek, and displays audio file information.
 * Optionally displays waveform visualization for audio files under 5 minutes.
 * 
 * Requirements: 6.3, 6.4, 6.5
 */
@UnstableApi
class AudioPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewAudioPlayerBinding
    private var player: ExoPlayer? = null
    private var currentAudioUrl: String? = null
    private var lastPosition: Long = 0L
    private var isUserSeeking: Boolean = false
    
    // Coroutine scope for updating seek bar
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null
    
    // Listener for when this player starts playing (to pause other media)
    var onPlaybackStarted: (() -> Unit)? = null
    
    // Listener for playback state changes
    var onPlaybackStateChanged: ((isPlaying: Boolean) -> Unit)? = null

    companion object {
        // Keep track of currently playing instance to pause others
        private var currentlyPlayingView: AudioPlayerView? = null
        
        // Maximum duration for waveform generation (5 minutes in milliseconds)
        private const val MAX_WAVEFORM_DURATION_MS = 5 * 60 * 1000L
    }

    init {
        binding = ViewAudioPlayerBinding.inflate(LayoutInflater.from(context), this, true)
        initializePlayer()
        setupControls()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(context)
            .build()
            .apply {
                // Add listener for playback state changes
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updatePlayPauseButton(isPlaying)
                        
                        if (isPlaying) {
                            // Pause other media when this audio starts playing
                            pauseOtherMedia()
                            onPlaybackStarted?.invoke()
                            startSeekBarUpdate()
                        } else {
                            stopSeekBarUpdate()
                        }
                        
                        onPlaybackStateChanged?.invoke(isPlaying)
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                binding.loadingIndicator.isVisible = true
                            }
                            Player.STATE_READY -> {
                                binding.loadingIndicator.isVisible = false
                                updateDuration()
                                
                                // Check if waveform should be displayed
                                val duration = getDuration()
                                if (duration > 0 && duration <= MAX_WAVEFORM_DURATION_MS) {
                                    showWaveform()
                                }
                            }
                            Player.STATE_ENDED -> {
                                // Audio playback ended
                                lastPosition = 0L
                                seekTo(0L)
                                updatePlayPauseButton(false)
                                stopSeekBarUpdate()
                            }
                            Player.STATE_IDLE -> {
                                binding.loadingIndicator.isVisible = false
                            }
                        }
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        binding.loadingIndicator.isVisible = false
                        // Error handling - could show a toast or error message
                    }
                })
            }
    }
    
    private fun setupControls() {
        // Play/Pause button
        binding.playPauseButton.setOnClickListener {
            if (isPlaying()) {
                pause()
            } else {
                play()
            }
        }
        
        // Seek bar
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateCurrentTime(progress.toLong())
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                stopSeekBarUpdate()
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                seekBar?.let {
                    seekTo(it.progress.toLong())
                }
                if (isPlaying()) {
                    startSeekBarUpdate()
                }
            }
        })
    }
    
    private fun pauseOtherMedia() {
        // Pause any other currently playing audio
        if (currentlyPlayingView != null && currentlyPlayingView != this) {
            currentlyPlayingView?.pause()
        }
        currentlyPlayingView = this
    }
    
    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.playPauseButton.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }
    
    private fun startSeekBarUpdate() {
        stopSeekBarUpdate()
        updateJob = scope.launch {
            while (isActive && isPlaying()) {
                if (!isUserSeeking) {
                    val position = getCurrentPosition()
                    binding.seekBar.progress = position.toInt()
                    updateCurrentTime(position)
                    
                    // Update waveform progress if visible
                    if (binding.waveformContainer.isVisible) {
                        binding.waveformView.setProgress(position.toFloat() / getDuration())
                    }
                }
                delay(100) // Update every 100ms
            }
        }
    }
    
    private fun stopSeekBarUpdate() {
        updateJob?.cancel()
        updateJob = null
    }
    
    private fun updateDuration() {
        val duration = getDuration()
        binding.seekBar.max = duration.toInt()
        binding.totalDuration.text = formatTime(duration)
    }
    
    private fun updateCurrentTime(positionMs: Long) {
        binding.currentTime.text = formatTime(positionMs)
    }
    
    private fun formatTime(milliseconds: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    private fun showWaveform() {
        binding.waveformContainer.isVisible = true
        currentAudioUrl?.let { url ->
            binding.waveformView.setAudioUrl(url)
            
            // Set up seek listener for waveform
            binding.waveformView.onSeekListener = { progress ->
                val duration = getDuration()
                if (duration > 0) {
                    val seekPosition = (progress * duration).toLong()
                    seekTo(seekPosition)
                }
            }
        }
    }

    /**
     * Sets the audio URL and prepares the player.
     * 
     * @param url The audio URL to load
     * @param fileName Optional file name to display
     */
    fun setAudioUrl(url: String, fileName: String? = null) {
        currentAudioUrl = url
        
        // Update file name display
        binding.audioFileName.text = fileName ?: extractFileNameFromUrl(url)
        
        // Hide waveform initially
        binding.waveformContainer.isVisible = false
        
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .build()
            
        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            
            // Resume from last position if available
            if (lastPosition > 0) {
                seekTo(lastPosition)
            }
        }
    }
    
    private fun extractFileNameFromUrl(url: String): String {
        return url.substringAfterLast('/').substringBefore('?')
    }

    /**
     * Starts audio playback.
     */
    fun play() {
        player?.play()
    }

    /**
     * Pauses audio playback.
     */
    fun pause() {
        player?.pause()
    }

    /**
     * Seeks to a specific position in the audio.
     * 
     * @param positionMs The position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    /**
     * Gets the current playback position.
     * 
     * @return Current position in milliseconds
     */
    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0L
    }

    /**
     * Gets the total duration of the audio.
     * 
     * @return Duration in milliseconds
     */
    fun getDuration(): Long {
        return player?.duration ?: 0L
    }

    /**
     * Checks if the audio is currently playing.
     * 
     * @return True if playing, false otherwise
     */
    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    /**
     * Saves the current playback position for later resumption.
     */
    fun savePosition() {
        lastPosition = getCurrentPosition()
    }
    
    /**
     * Gets the saved playback position.
     * 
     * @return Last saved position in milliseconds
     */
    fun getLastPosition(): Long {
        return lastPosition
    }

    /**
     * Releases the player and frees resources.
     * Should be called when the view is no longer needed.
     */
    fun release() {
        savePosition()
        stopSeekBarUpdate()
        if (currentlyPlayingView == this) {
            currentlyPlayingView = null
        }
        player?.release()
        player = null
        scope.cancel()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Save position but don't release - let the parent manage lifecycle
        savePosition()
        stopSeekBarUpdate()
    }
}
