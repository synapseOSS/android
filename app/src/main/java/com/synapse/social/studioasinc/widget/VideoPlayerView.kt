package com.synapse.social.studioasinc.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.ViewVideoPlayerBinding

/**
 * Custom video player view that wraps ExoPlayer for inline video playback.
 * Provides controls for play, pause, seek, and full-screen viewing.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
@UnstableApi
class VideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewVideoPlayerBinding
    private var player: ExoPlayer? = null
    private var currentVideoUrl: String? = null
    private var lastPosition: Long = 0L
    private var isFullScreen: Boolean = false
    
    // Listener for full-screen toggle
    var onFullScreenToggle: ((isFullScreen: Boolean) -> Unit)? = null
    
    // Listener for playback state changes
    var onPlaybackStateChanged: ((isPlaying: Boolean) -> Unit)? = null
    
    // Listener for when this player starts playing (to pause other media)
    var onPlaybackStarted: (() -> Unit)? = null

    companion object {
        // Keep track of currently playing instance to pause others
        private var currentlyPlayingView: VideoPlayerView? = null
    }

    init {
        binding = ViewVideoPlayerBinding.inflate(LayoutInflater.from(context), this)
        initializePlayer()
        setupFullScreenButton()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(context)
            .build()
            .apply {
                binding.playerView.player = this
                
                // Enable adaptive streaming for better performance
                // ExoPlayer handles this automatically with default settings
                
                // Add listener for playback state changes
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            // Pause other media when this video starts playing
                            pauseOtherMedia()
                            onPlaybackStarted?.invoke()
                        }
                        onPlaybackStateChanged?.invoke(isPlaying)
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                // Buffering indicator is handled by PlayerView
                                binding.playerView.keepScreenOn = true
                            }
                            Player.STATE_READY -> {
                                // Video is ready to play
                                binding.playerView.keepScreenOn = true
                            }
                            Player.STATE_ENDED -> {
                                // Video playback ended - save position
                                lastPosition = 0L
                                binding.playerView.keepScreenOn = false
                            }
                            Player.STATE_IDLE -> {
                                // Player is idle
                                binding.playerView.keepScreenOn = false
                            }
                        }
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        // Handle playback errors with retry capability
                        handlePlaybackError(error)
                    }
                })
            }
    }
    
    private var retryCount = 0
    private val maxRetries = 3
    
    private fun handlePlaybackError(error: androidx.media3.common.PlaybackException) {
        if (retryCount < maxRetries && currentVideoUrl != null) {
            // Retry loading the video
            retryCount++
            player?.apply {
                stop()
                clearMediaItems()
                setMediaItem(MediaItem.fromUri(currentVideoUrl!!))
                prepare()
                if (lastPosition > 0) {
                    seekTo(lastPosition)
                }
            }
        } else {
            // Max retries reached, show error to user
            retryCount = 0
            // Error is displayed by PlayerView
        }
    }
    
    private fun setupFullScreenButton() {
        // Find the fullscreen button in the custom controls
        val fullScreenButton = binding.playerView.findViewById<ImageButton>(R.id.exo_fullscreen)
        fullScreenButton?.setOnClickListener {
            isFullScreen = !isFullScreen
            updateFullScreenIcon(fullScreenButton)
            onFullScreenToggle?.invoke(isFullScreen)
        }
    }
    
    private fun updateFullScreenIcon(button: ImageButton) {
        // Toggle between fullscreen and exit fullscreen icons
        button.setImageResource(
            if (isFullScreen) R.drawable.ic_fullscreen_exit
            else R.drawable.ic_fullscreen
        )
    }
    
    private fun pauseOtherMedia() {
        // Pause any other currently playing video
        if (currentlyPlayingView != null && currentlyPlayingView != this) {
            currentlyPlayingView?.pause()
        }
        currentlyPlayingView = this
    }

    /**
     * Sets the video URL and prepares the player.
     * Supports adaptive streaming for large videos.
     * 
     * @param url The video URL to load
     */
    fun setVideoUrl(url: String) {
        currentVideoUrl = url
        retryCount = 0 // Reset retry count for new video
        
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
    
    /**
     * Retries loading the current video.
     * Useful when playback fails due to network issues.
     */
    fun retry() {
        currentVideoUrl?.let { url ->
            retryCount = 0
            setVideoUrl(url)
        }
    }

    /**
     * Starts video playback.
     */
    fun play() {
        player?.play()
    }

    /**
     * Pauses video playback.
     */
    fun pause() {
        player?.pause()
    }

    /**
     * Seeks to a specific position in the video.
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
     * Gets the total duration of the video.
     * 
     * @return Duration in milliseconds
     */
    fun getDuration(): Long {
        return player?.duration ?: 0L
    }

    /**
     * Checks if the video is currently playing.
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
     * Sets whether the player is in full-screen mode.
     * 
     * @param fullScreen True for full-screen, false for normal
     */
    fun setFullScreen(fullScreen: Boolean) {
        isFullScreen = fullScreen
        val fullScreenButton = binding.playerView.findViewById<ImageButton>(R.id.exo_fullscreen)
        fullScreenButton?.let { updateFullScreenIcon(it) }
    }
    
    /**
     * Checks if the player is in full-screen mode.
     * 
     * @return True if in full-screen, false otherwise
     */
    fun isFullScreen(): Boolean {
        return isFullScreen
    }

    /**
     * Releases the player and frees resources.
     * Should be called when the view is no longer needed.
     */
    fun release() {
        savePosition()
        if (currentlyPlayingView == this) {
            currentlyPlayingView = null
        }
        player?.release()
        player = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Save position but don't release - let the parent manage lifecycle
        savePosition()
    }
}
