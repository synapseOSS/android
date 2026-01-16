package com.synapse.social.studioasinc.feature.stories.creator

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.repository.StoryRepository
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.StoryPrivacy
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryCreatorState(
    val capturedMediaUri: Uri? = null,
    val mediaType: StoryMediaType = StoryMediaType.PHOTO,
    val flashMode: FlashMode = FlashMode.OFF,
    val isFrontCamera: Boolean = false,
    val isRecording: Boolean = false,
    val recordingProgress: Float = 0f,
    val textOverlays: List<TextOverlay> = emptyList(),
    val selectedPrivacy: StoryPrivacy = StoryPrivacy.ALL_FRIENDS,
    val isPosting: Boolean = false,
    val isPosted: Boolean = false,
    val error: String? = null,
    val sharedPost: Post? = null
)

@HiltViewModel
class StoryCreatorViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val postRepository: PostRepository
) : ViewModel() {
    
    companion object {
        private const val MAX_VIDEO_DURATION_MS = 15_000L // 15 seconds
    }
    
    private val _state = MutableStateFlow(StoryCreatorState())
    val state: StateFlow<StoryCreatorState> = _state.asStateFlow()
    
    private var recordingJob: Job? = null
    
    private val currentUserId: String?
        get() = SupabaseClient.client.auth.currentUserOrNull()?.id

    fun loadSharedPost(postId: String) {
        viewModelScope.launch {
            postRepository.getPost(postId).onSuccess { post ->
                _state.update { it.copy(sharedPost = post) }
            }.onFailure { e ->
                _state.update { it.copy(error = "Failed to load post: ${e.message}") }
            }
        }
    }
    
    /**
     * Toggle flash mode: OFF -> ON -> AUTO -> OFF
     */
    fun toggleFlash() {
        _state.update { state ->
            val newMode = when (state.flashMode) {
                FlashMode.OFF -> FlashMode.ON
                FlashMode.ON -> FlashMode.AUTO
                FlashMode.AUTO -> FlashMode.OFF
            }
            state.copy(flashMode = newMode)
        }
    }
    
    /**
     * Flip between front and back camera
     */
    fun flipCamera() {
        _state.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }
    
    /**
     * Capture a photo
     * In production, this would interface with CameraX
     */
    fun capturePhoto() {
        // Placeholder - actual implementation would use CameraX
        // For now, just set a demo state
        _state.update {
            it.copy(
                capturedMediaUri = null, // Would be set by CameraX
                mediaType = StoryMediaType.PHOTO
            )
        }
    }
    
    /**
     * Start video recording (up to 15 seconds)
     */
    fun startVideoRecording() {
        _state.update { it.copy(isRecording = true, recordingProgress = 0f) }
        
        recordingJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            
            while (_state.value.isRecording) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / MAX_VIDEO_DURATION_MS).coerceIn(0f, 1f)
                
                _state.update { it.copy(recordingProgress = progress) }
                
                if (elapsed >= MAX_VIDEO_DURATION_MS) {
                    stopVideoRecording()
                    break
                }
                
                delay(50) // Update every 50ms
            }
        }
    }
    
    /**
     * Stop video recording
     */
    fun stopVideoRecording() {
        recordingJob?.cancel()
        recordingJob = null
        
        val duration = (_state.value.recordingProgress * MAX_VIDEO_DURATION_MS / 1000).toInt()
        
        _state.update {
            it.copy(
                isRecording = false,
                // capturedMediaUri would be set by CameraX
                mediaType = StoryMediaType.VIDEO
            )
        }
    }
    
    /**
     * Set media from gallery selection
     */
    fun setMediaFromGallery(uri: Uri) {
        // Determine media type from URI/MimeType
        // This is simplified - actual implementation would check MIME type
        val isVideo = uri.toString().contains("video")
        
        _state.update {
            it.copy(
                capturedMediaUri = uri,
                mediaType = if (isVideo) StoryMediaType.VIDEO else StoryMediaType.PHOTO
            )
        }
    }

    /**
     * Set captured media from camera
     */
    fun setCapturedMedia(uri: Uri, type: StoryMediaType) {
        _state.update {
            it.copy(
                capturedMediaUri = uri,
                mediaType = type
            )
        }
    }
    
    /**
     * Clear captured media and return to capture mode
     */
    fun clearCapturedMedia() {
        _state.update {
            it.copy(
                capturedMediaUri = null,
                textOverlays = emptyList()
            )
        }
    }
    
    /**
     * Add a new text overlay
     */
    fun addTextOverlay() {
        _state.update { state ->
            state.copy(
                textOverlays = state.textOverlays + TextOverlay(
                    text = "",
                    position = Offset(100f, 100f)
                )
            )
        }
    }
    
    /**
     * Remove a text overlay by index
     */
    fun removeTextOverlay(index: Int) {
        _state.update { state ->
            state.copy(
                textOverlays = state.textOverlays.filterIndexed { i, _ -> i != index }
            )
        }
    }
    
    /**
     * Update text overlay position
     */
    fun updateTextPosition(index: Int, position: Offset) {
        _state.update { state ->
            state.copy(
                textOverlays = state.textOverlays.mapIndexed { i, overlay ->
                    if (i == index) overlay.copy(position = position) else overlay
                }
            )
        }
    }
    
    /**
     * Update text overlay content
     */
    fun updateTextContent(index: Int, content: String) {
        _state.update { state ->
            state.copy(
                textOverlays = state.textOverlays.mapIndexed { i, overlay ->
                    if (i == index) overlay.copy(text = content) else overlay
                }
            )
        }
    }
    
    /**
     * Set privacy setting
     */
    fun setPrivacy(privacy: StoryPrivacy) {
        _state.update { it.copy(selectedPrivacy = privacy) }
    }
    
    /**
     * Post the story
     */
    fun postStory() {
        val userId = currentUserId ?: return
        val mediaUri = _state.value.capturedMediaUri ?: return
        
        viewModelScope.launch {
            _state.update { it.copy(isPosting = true, error = null) }
            
            val duration = if (_state.value.mediaType == StoryMediaType.VIDEO) {
                (_state.value.recordingProgress * 15).toInt().coerceAtLeast(1)
            } else {
                5
            }
            
            storyRepository.createStory(
                userId = userId,
                mediaUri = mediaUri,
                mediaType = _state.value.mediaType,
                privacy = _state.value.selectedPrivacy,
                duration = duration
            ).onSuccess {
                _state.update { it.copy(isPosting = false, isPosted = true) }
            }.onFailure { error ->
                _state.update { 
                    it.copy(isPosting = false, error = error.message ?: "Failed to post story")
                }
            }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
    }
}
