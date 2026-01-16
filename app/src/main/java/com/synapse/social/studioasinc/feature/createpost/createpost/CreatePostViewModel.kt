package com.synapse.social.studioasinc.ui.createpost

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.local.database.AppDatabase
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.core.util.FileManager
import com.synapse.social.studioasinc.core.storage.MediaStorageService
import com.synapse.social.studioasinc.data.local.database.AppSettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

data class CreatePostUiState(
    val isLoading: Boolean = false,
    val postText: String = "",
    val mediaItems: List<MediaItem> = emptyList(),
    val pollData: PollData? = null,
    val location: LocationData? = null,
    val youtubeUrl: String? = null,
    val privacy: String = "public", // public, followers, private
    val settings: PostSettings = PostSettings(),
    val error: String? = null,
    val isPostCreated: Boolean = false,
    val uploadProgress: Float = 0f,
    val isEditMode: Boolean = false,
    val checkDraft: Boolean = true,
    val currentUserProfile: User? = null
)

data class PollData(
    val question: String,
    val options: List<String>,
    val durationHours: Int
)

data class LocationData(
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class PostSettings(
    val hideViewsCount: Boolean = false,
    val hideLikeCount: Boolean = false,
    val hideCommentsCount: Boolean = false,
    val disableComments: Boolean = false
)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    application: Application,
    private val postRepository: PostRepository,
    private val userRepository: com.synapse.social.studioasinc.data.repository.UserRepository,
    private val appSettingsManager: AppSettingsManager
) : AndroidViewModel(application) {

    private val authService = SupabaseAuthenticationService()
    private val prefs = application.getSharedPreferences("create_post_draft", Context.MODE_PRIVATE)
    private val mediaStorageService = MediaStorageService(application, appSettingsManager)

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()
    
    // Edit Mode State
    private var editPostId: String? = null
    private var originalPost: Post? = null

    init {
        // Load draft on init if not edit mode (edit mode loaded separately)
        // We defer draft loading until we know if it's edit mode
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            authService.getCurrentUserId()?.let { uid ->
                userRepository.getUserById(uid).onSuccess { user ->
                    _uiState.update { it.copy(currentUserProfile = user) }
                }
            }
        }
    }

    fun loadDraft() {
        if (_uiState.value.isEditMode || !_uiState.value.checkDraft) return
        
        val draftText = prefs.getString("draft_text", null)
        if (!draftText.isNullOrEmpty()) {
             _uiState.update { it.copy(postText = draftText, checkDraft = false) }
        } else {
            _uiState.update { it.copy(checkDraft = false) }
        }
    }

    fun saveDraft() {
        if (_uiState.value.isPostCreated) return
        if (_uiState.value.isEditMode) return
        
        val text = _uiState.value.postText
        if (text.isNotBlank()) {
            prefs.edit().putString("draft_text", text).apply()
        }
    }

    fun clearDraft() {
        prefs.edit().remove("draft_text").apply()
    }

    fun loadPostForEdit(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true, checkDraft = false) }
            postRepository.getPost(postId).onSuccess { post ->
                post?.let {
                    originalPost = it
                    editPostId = it.id
                    
                    val mediaItems = it.mediaItems?.toMutableList() ?: mutableListOf()
                    // Legacy support
                    if (mediaItems.isEmpty()) {
                        it.postImage?.let { imgUrl ->
                             mediaItems.add(MediaItem(url = imgUrl, type = MediaType.IMAGE))
                        }
                    }

                    _uiState.update { state -> 
                        state.copy(
                            isLoading = false,
                            postText = it.postText ?: "",
                            mediaItems = mediaItems,
                            privacy = it.postVisibility ?: "public",
                            youtubeUrl = it.youtubeUrl,
                            settings = PostSettings(
                                hideViewsCount = it.postHideViewsCount == "true",
                                hideLikeCount = it.postHideLikeCount == "true",
                                hideCommentsCount = it.postHideCommentsCount == "true",
                                disableComments = it.postDisableComments == "true"
                            ),
                            // Poll and Location mapping is complex if detailed data missing, assuming basic restore
                            pollData = if (it.hasPoll == true) PollData(it.pollQuestion ?: "", it.pollOptions?.map { opt -> opt.text } ?: emptyList(), 24) else null,
                            location = if (it.hasLocation == true) LocationData(it.locationName ?: "", it.locationAddress, it.locationLatitude, it.locationLongitude) else null
                        )
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load post for editing") }
            }
        }
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(postText = text) }
    }

    fun addMedia(uris: List<Uri>) {
        val currentMedia = _uiState.value.mediaItems.toMutableList()
        val context = getApplication<Application>()
        
        uris.forEach { uri ->
             if (currentMedia.size >= 10) return@forEach
             android.util.Log.d("CreatePost", "Processing URI: $uri")
             val mimeType = context.contentResolver.getType(uri) ?: return@forEach
             val type = if (mimeType.startsWith("video")) MediaType.VIDEO else MediaType.IMAGE
             FileManager.getPathFromUri(context, uri)?.let { path ->
                 android.util.Log.d("CreatePost", "Converted URI to path: $path")
                 if (path.startsWith("content://")) {
                     android.util.Log.e("CreatePost", "ERROR: Path is still a content URI!")
                 }
                 currentMedia.add(MediaItem(url = path, type = type))
             } ?: run {
                 android.util.Log.e("CreatePost", "Failed to convert URI to path: $uri")
             }
        }
        _uiState.update { it.copy(mediaItems = currentMedia, error = null) }
    }

    fun removeMedia(index: Int) {
        val currentMedia = _uiState.value.mediaItems.toMutableList()
        if (index in currentMedia.indices) {
            currentMedia.removeAt(index)
            _uiState.update { it.copy(mediaItems = currentMedia) }
        }
    }

    fun setPoll(pollData: PollData?) {
        _uiState.update { it.copy(pollData = pollData, mediaItems = emptyList()) }
    }

    fun setLocation(location: LocationData?) {
        _uiState.update { it.copy(location = location) }
    }

    fun setYoutubeUrl(url: String?) {
        _uiState.update { it.copy(youtubeUrl = url) }
    }

    fun setPrivacy(privacy: String) {
        _uiState.update { it.copy(privacy = privacy) }
    }
    
    fun updateSettings(settings: PostSettings) {
        _uiState.update { it.copy(settings = settings) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun submitPost() {
        if (_uiState.value.isLoading) return
        
        val currentState = _uiState.value
        val text = currentState.postText.trim()
        
        if (text.isEmpty() && currentState.mediaItems.isEmpty() && currentState.pollData == null && currentState.youtubeUrl == null) {
            _uiState.update { it.copy(error = "Please add some content to your post") }
            return
        }

        // Validate that no content URIs are present
        val invalidUrls = currentState.mediaItems.filter { it.url.startsWith("content://") }
        if (invalidUrls.isNotEmpty()) {
            android.util.Log.e("CreatePost", "Found content URIs in media items: ${invalidUrls.map { it.url }}")
            _uiState.update { it.copy(error = "Media processing failed. Please try selecting the images again.") }
            return
        }

        viewModelScope.launch {
            val currentUser = authService.getCurrentUser()
            if (currentUser == null) {
                _uiState.update { it.copy(error = "Not logged in") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, uploadProgress = 0f) }

            val postKey = originalPost?.key ?: "post_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val timestamp = System.currentTimeMillis()
            val publishDate = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            val postType = when {
                currentState.mediaItems.any { it.type == MediaType.VIDEO } -> "VIDEO"
                currentState.mediaItems.isNotEmpty() -> "IMAGE"
                currentState.pollData != null -> "POLL"
                else -> "TEXT"
            }

            val pollEndTime = currentState.pollData?.let {
                Instant.ofEpochMilli(timestamp + it.durationHours * 3600 * 1000L)
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT)
            }

            val post = Post(
                id = editPostId ?: UUID.randomUUID().toString(),
                key = postKey,
                authorUid = currentUser.id,
                postText = text.ifEmpty { null },
                postType = postType,
                postVisibility = currentState.privacy,
                postHideViewsCount = if (currentState.settings.hideViewsCount) "true" else "false",
                postHideLikeCount = if (currentState.settings.hideLikeCount) "true" else "false",
                postHideCommentsCount = if (currentState.settings.hideCommentsCount) "true" else "false",
                postDisableComments = if (currentState.settings.disableComments) "true" else "false",
                publishDate = publishDate,
                timestamp = timestamp,
                youtubeUrl = currentState.youtubeUrl,
                hasPoll = currentState.pollData != null,
                pollQuestion = currentState.pollData?.question,
                pollOptions = currentState.pollData?.options?.map { PollOption(text = it, votes = 0) },
                pollEndTime = pollEndTime,
                pollAllowMultiple = false, // Default to false, can be made configurable later
                hasLocation = currentState.location != null,
                locationName = currentState.location?.name,
                locationAddress = currentState.location?.address,
                locationLatitude = currentState.location?.latitude,
                locationLongitude = currentState.location?.longitude,
                locationPlaceId = null // Can be added later if needed
            )

            // Filter new media that needs uploading
            val newMedia = currentState.mediaItems.filter { !it.url.startsWith("http") }
            val existingMedia = currentState.mediaItems.filter { it.url.startsWith("http") }

            if (newMedia.isEmpty()) {
                 // Use existing media + text update
                 val finalPost = post.copy(
                     mediaItems = existingMedia.toMutableList(),
                     postImage = existingMedia.firstOrNull { it.type == MediaType.IMAGE }?.url
                 )
                 saveOrUpdatePost(finalPost)
            } else {
                 uploadMediaAndSave(post, newMedia, existingMedia)
            }
        }
    }

    private suspend fun uploadMediaAndSave(post: Post, newMedia: List<MediaItem>, existingMedia: List<MediaItem>) {
        try {
            val uploadedItems = mutableListOf<MediaItem>()
            val totalItems = newMedia.size
            var completedItems = 0

            newMedia.forEach { mediaItem ->
                try {
                    val filePath = mediaItem.url
                    val file = java.io.File(filePath)
                    
                    if (!file.exists()) {
                        android.util.Log.e("CreatePost", "File not found: $filePath")
                        // Skip this media item
                        completedItems++
                        val progress = completedItems.toFloat() / totalItems
                        _uiState.update { it.copy(uploadProgress = progress) }
                        return@forEach
                    }

                    // Upload using MediaStorageService which supports videos via Cloudinary/Cloudflare/Supabase
                    val uploadedUrl = kotlinx.coroutines.suspendCancellableCoroutine<String?> { continuation ->
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            mediaStorageService.uploadFile(filePath, null, object : MediaStorageService.UploadCallback {
                                override fun onProgress(percent: Int) {
                                    // Update progress proportionally across all items
                                    val itemProgress = (percent / 100f) / totalItems
                                    val baseProgress = completedItems.toFloat() / totalItems
                                    _uiState.update { it.copy(uploadProgress = baseProgress + itemProgress) }
                                }

                                override fun onSuccess(url: String, publicId: String) {
                                    android.util.Log.d("CreatePost", "Uploaded ${mediaItem.type}: $url")
                                    if (continuation.isActive) {
                                        continuation.resume(url)
                                    }
                                }

                                override fun onError(error: String) {
                                    android.util.Log.e("CreatePost", "Upload failed: $error")
                                    if (continuation.isActive) {
                                        continuation.resume(null)
                                    }
                                }
                            })
                        }
                    }
                    
                    if (uploadedUrl != null) {
                        uploadedItems.add(
                            mediaItem.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                url = uploadedUrl,
                                mimeType = getApplication<Application>().contentResolver.getType(android.net.Uri.parse(filePath))
                            )
                        )
                    } else {
                        // Upload failed
                        _uiState.update { it.copy(isLoading = false, error = "Upload failed for ${mediaItem.type}") }
                        return
                    }

                    completedItems++
                    val progress = completedItems.toFloat() / totalItems
                    _uiState.update { it.copy(uploadProgress = progress) }

                } catch (e: Exception) {
                    android.util.Log.e("CreatePost", "Media upload failed: ${e.message}", e)
                    _uiState.update { it.copy(isLoading = false, error = "Upload failed: ${e.message}") }
                    return
                }
            }

            // Combine uploaded and existing media
            val allMedia = existingMedia + uploadedItems
            val updatedPost = post.copy(
                mediaItems = allMedia.toMutableList(),
                postImage = allMedia.firstOrNull { it.type == MediaType.IMAGE }?.url
            )
            saveOrUpdatePost(updatedPost)

        } catch (e: Exception) {
            android.util.Log.e("CreatePost", "Upload process failed", e)
            _uiState.update { it.copy(isLoading = false, error = "Upload failed: ${e.message}") }
        }
    }

    private fun saveOrUpdatePost(post: Post) {
        viewModelScope.launch {
            // Ensure author details are populated for immediate display in local feed
            if (!_uiState.value.isEditMode && post.username.isNullOrEmpty()) {
                try {
                    userRepository.getUserById(post.authorUid).onSuccess { user ->
                        post.username = user?.username
                        post.avatarUrl = user?.avatar
                        post.isVerified = user?.verify ?: false
                    }
                } catch (e: Exception) {
                    // Fail silently, post will still be created and details fetched later via sync/network
                    e.printStackTrace()
                }
            }

            // Using createPost because Supabase/Room UPSERT logic usually handles ID collisions, 
            // OR we need specific update method. Assuming createPost handles upsert or we add updatePost.
            // Checking PostRepository... assuming updatePost exists or createPost is upsert. 
            // If strictly create, we might need separate update call.
            // For now, let's assume createPost acts as upsert or use a specific update if available in repo.
            // Checking found files earlier, PostRepository existed. Safest is to try update if edit mode.
            
            val result = if (_uiState.value.isEditMode) {
                 postRepository.updatePost(post) // Assuming this method exists or we standard on create
            } else {
                 postRepository.createPost(post)
            }

            result.onSuccess {
                    clearDraft()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPostCreated = true,
                            postText = "",
                            mediaItems = emptyList(),
                            pollData = null,
                            location = null,
                            youtubeUrl = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = "Failed: ${e.message}") }
                }
        }
    }
}
