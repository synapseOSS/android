package com.synapse.social.studioasinc.presentation.editprofile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.util.FileManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val repository = EditProfileRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<EditProfileNavigation>()
    val navigationEvents: SharedFlow<EditProfileNavigation> = _navigationEvents.asSharedFlow()

    private var usernameValidationJob: Job? = null

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
                return@launch
            }

            repository.getUserProfile(userId).collect { result ->
                result.fold(
                    onSuccess = { profile ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                profile = profile,
                                username = profile.username,
                                nickname = profile.displayName ?: "",
                                bio = profile.bio ?: "",
                                avatarUrl = profile.avatar,
                                coverUrl = profile.profileCoverImage,
                                selectedGender = parseGender(profile.gender),
                                selectedRegion = profile.region.takeIf { it != "null" } // Handle "null" string from DB sometimes
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    // Helper to parse gender. The UserProfile model doesn't have 'gender' field.
    // ProfileEditActivity used `user["gender"]`.
    // I should probably add gender to UserProfile or handle it separately.
    // Given I cannot easily change UserProfile, I'll assume it's part of the profile map in repository or add it to my local state logic.
    // But repository returned UserProfile.
    // I'll stick to what I have, but realize 'gender' might be missing in UserProfile.
    // The activity used: val gender = user["gender"]?.toString() ?: "hidden"
    // I will assume for now 'status' is not gender.
    // I might need to update UserProfile or fetch raw JSON to get gender.
    // The repository method getUserProfile manually maps JSON to UserProfile. I can add gender there if I modify UserProfile, or just fetch it.
    // For now I'll default to Hidden if not found.
    private fun parseGender(genderStr: String?): Gender {
        return when (genderStr?.lowercase()) {
            "male" -> Gender.Male
            "female" -> Gender.Female
            else -> Gender.Hidden
        }
    }

    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.UsernameChanged -> {
                _uiState.update { it.copy(username = event.username, hasChanges = true) }
                validateUsername(event.username)
            }
            is EditProfileEvent.NicknameChanged -> {
                _uiState.update { it.copy(nickname = event.nickname, hasChanges = true) }
                validateNickname(event.nickname)
            }
            is EditProfileEvent.BiographyChanged -> {
                _uiState.update { it.copy(bio = event.bio, hasChanges = true) }
                validateBiography(event.bio)
            }
            is EditProfileEvent.GenderSelected -> {
                _uiState.update { it.copy(selectedGender = event.gender, hasChanges = true) }
            }
            is EditProfileEvent.RegionSelected -> {
                _uiState.update { it.copy(selectedRegion = event.region, hasChanges = true) }
            }
            is EditProfileEvent.AvatarSelected -> {
                handleAvatarSelection(event.uri)
            }
            is EditProfileEvent.CoverSelected -> {
                handleCoverSelection(event.uri)
            }
            EditProfileEvent.RetryAvatarUpload -> {
                retryAvatarUpload()
            }
            EditProfileEvent.RetryCoverUpload -> {
                retryCoverUpload()
            }
            EditProfileEvent.SaveClicked -> {
                saveProfile()
            }
            EditProfileEvent.BackClicked -> {
                viewModelScope.launch { _navigationEvents.emit(EditProfileNavigation.NavigateBack) }
            }
            EditProfileEvent.ProfileHistoryClicked -> {
                viewModelScope.launch { _navigationEvents.emit(EditProfileNavigation.NavigateToProfileHistory) }
            }
            EditProfileEvent.CoverHistoryClicked -> {
                viewModelScope.launch { _navigationEvents.emit(EditProfileNavigation.NavigateToCoverHistory) }
            }
        }
    }

    private fun validateUsername(username: String) {
        usernameValidationJob?.cancel()

        if (username.isEmpty()) {
            _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Username is required")) }
            return
        }

        // Basic Regex Validation
        if (!username.matches(Regex("[a-z0-9_.]+"))) {
            _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Only lowercase letters, numbers, _ and . allowed")) }
            return
        }
        if (!username.first().isLetter()) {
            _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Username must start with a letter")) }
            return
        }
        if (username.length < 3) {
            _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Username must be at least 3 characters")) }
            return
        }
        if (username.length > 25) {
             _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Username max 25 characters")) }
             return
        }

        _uiState.update { it.copy(usernameValidation = UsernameValidation.Checking) }

        usernameValidationJob = viewModelScope.launch {
            delay(500) // Debounce
            val userId = repository.getCurrentUserId() ?: return@launch

            // Check if username is same as current (valid)
            if (username == _uiState.value.profile?.username) {
                 _uiState.update { it.copy(usernameValidation = UsernameValidation.Valid) }
                 return@launch
            }

            val result = repository.checkUsernameAvailability(username, userId)
            result.fold(
                onSuccess = { isAvailable ->
                    if (isAvailable) {
                        _uiState.update { it.copy(usernameValidation = UsernameValidation.Valid) }
                    } else {
                        _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Username is already taken")) }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Failed to check availability")) }
                }
            )
        }
    }

    private fun validateNickname(nickname: String) {
        if (nickname.length > 30) {
            _uiState.update { it.copy(nicknameError = "Nickname must be 30 characters or less") }
        } else {
            _uiState.update { it.copy(nicknameError = null) }
        }
    }

    private fun validateBiography(bio: String) {
        if (bio.length > 250) {
            _uiState.update { it.copy(bioError = "Bio must be 250 characters or less") }
        } else {
            _uiState.update { it.copy(bioError = null) }
        }
    }

    private var lastAvatarUri: Uri? = null
    private var lastCoverUri: Uri? = null

    private fun handleAvatarSelection(uri: Uri) {
        lastAvatarUri = uri
        _uiState.update { it.copy(avatarUploadState = UploadState.Uploading()) }
        
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                android.util.Log.d("EditProfile", "Processing avatar URI: $uri")
                
                // Try to convert URI to file path
                var realFilePath = FileManager.getPathFromUri(context, uri)
                android.util.Log.d("EditProfile", "Converted file path: $realFilePath")
                
                // If conversion failed, try to copy content to temp file
                if (realFilePath == null) {
                    android.util.Log.d("EditProfile", "URI conversion failed, copying content to temp file")
                    val tempInputFile = File(context.cacheDir, "temp_input_avatar_${System.currentTimeMillis()}.jpg")
                    
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempInputFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    if (tempInputFile.exists() && tempInputFile.length() > 0) {
                        realFilePath = tempInputFile.absolutePath
                        android.util.Log.d("EditProfile", "Successfully copied to temp file: $realFilePath")
                    } else {
                        throw Exception("Failed to copy image content from URI")
                    }
                }

                // Validate file exists and is not empty
                val sourceFile = File(realFilePath)
                if (!sourceFile.exists()) {
                    throw Exception("Source file does not exist: $realFilePath")
                }
                if (sourceFile.length() == 0L) {
                    throw Exception("Source file is empty: $realFilePath")
                }

                // Create compressed version
                val tempFile = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
                android.util.Log.d("EditProfile", "Compressing image to: ${tempFile.absolutePath}")
                
                FileManager.resizeBitmapFileRetainRatio(realFilePath, tempFile.absolutePath, 1024)
                
                // Validate compressed file
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    throw Exception("Image compression failed")
                }
                
                android.util.Log.d("EditProfile", "Image compressed successfully, size: ${tempFile.length()} bytes")

                // Upload
                uploadAvatar(tempFile.absolutePath)
                
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Avatar processing failed", e)
                _uiState.update { 
                    it.copy(avatarUploadState = UploadState.Error("Failed to process image: ${e.message}")) 
                }
            }
        }
    }

    private fun uploadAvatar(filePath: String) {
        viewModelScope.launch {
            try {
                val userId = repository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update { 
                        it.copy(avatarUploadState = UploadState.Error("User not logged in")) 
                    }
                    return@launch
                }

                android.util.Log.d("EditProfile", "Starting avatar upload for user: $userId, file: $filePath")
                _uiState.update { it.copy(avatarUploadState = UploadState.Uploading()) }

                val result = repository.uploadAvatar(userId, filePath)
                result.fold(
                    onSuccess = { url ->
                        android.util.Log.d("EditProfile", "Avatar upload successful: $url")
                        _uiState.update { 
                            it.copy(
                                avatarUrl = url,
                                avatarUploadState = UploadState.Success,
                                hasChanges = true
                            ) 
                        }
                        // Add to history in background
                        viewModelScope.launch {
                            try {
                                repository.addToProfileHistory(userId, url)
                            } catch (e: Exception) {
                                android.util.Log.w("EditProfile", "Failed to add to profile history", e)
                            }
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("EditProfile", "Avatar upload failed", error)
                        _uiState.update { 
                            it.copy(avatarUploadState = UploadState.Error("Avatar upload failed: ${error.message}")) 
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Unexpected error during avatar upload", e)
                _uiState.update { 
                    it.copy(avatarUploadState = UploadState.Error("Unexpected error: ${e.message}")) 
                }
            }
        }
    }

    private fun handleCoverSelection(uri: Uri) {
        lastCoverUri = uri
        _uiState.update { it.copy(coverUploadState = UploadState.Uploading()) }
        
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                android.util.Log.d("EditProfile", "Processing cover URI: $uri")
                
                // Try to convert URI to file path
                var realFilePath = FileManager.getPathFromUri(context, uri)
                android.util.Log.d("EditProfile", "Converted file path: $realFilePath")
                
                // If conversion failed, try to copy content to temp file
                if (realFilePath == null) {
                    android.util.Log.d("EditProfile", "URI conversion failed, copying content to temp file")
                    val tempInputFile = File(context.cacheDir, "temp_input_cover_${System.currentTimeMillis()}.jpg")
                    
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempInputFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    if (tempInputFile.exists() && tempInputFile.length() > 0) {
                        realFilePath = tempInputFile.absolutePath
                        android.util.Log.d("EditProfile", "Successfully copied to temp file: $realFilePath")
                    } else {
                        throw Exception("Failed to copy image content from URI")
                    }
                }

                // Validate file exists and is not empty
                val sourceFile = File(realFilePath)
                if (!sourceFile.exists()) {
                    throw Exception("Source file does not exist: $realFilePath")
                }
                if (sourceFile.length() == 0L) {
                    throw Exception("Source file is empty: $realFilePath")
                }

                // Create compressed version
                val tempFile = File(context.cacheDir, "temp_cover_${System.currentTimeMillis()}.jpg")
                android.util.Log.d("EditProfile", "Compressing image to: ${tempFile.absolutePath}")
                
                FileManager.resizeBitmapFileRetainRatio(realFilePath, tempFile.absolutePath, 1024)
                
                // Validate compressed file
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    throw Exception("Image compression failed")
                }
                
                android.util.Log.d("EditProfile", "Image compressed successfully, size: ${tempFile.length()} bytes")

                // Upload
                uploadCover(tempFile.absolutePath)
                
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Cover processing failed", e)
                _uiState.update { 
                    it.copy(coverUploadState = UploadState.Error("Failed to process image: ${e.message}")) 
                }
            }
        }
    }

    private fun uploadCover(filePath: String) {
        viewModelScope.launch {
            try {
                val userId = repository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update { 
                        it.copy(coverUploadState = UploadState.Error("User not logged in")) 
                    }
                    return@launch
                }

                android.util.Log.d("EditProfile", "Starting cover upload for user: $userId, file: $filePath")
                _uiState.update { it.copy(coverUploadState = UploadState.Uploading()) }

                val result = repository.uploadCover(userId, filePath)
                result.fold(
                    onSuccess = { url ->
                        android.util.Log.d("EditProfile", "Cover upload successful: $url")
                        _uiState.update { 
                            it.copy(
                                coverUrl = url,
                                coverUploadState = UploadState.Success,
                                hasChanges = true
                            ) 
                        }
                        // Add to history in background
                        viewModelScope.launch {
                            try {
                                repository.addToCoverHistory(userId, url)
                            } catch (e: Exception) {
                                android.util.Log.w("EditProfile", "Failed to add to cover history", e)
                            }
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("EditProfile", "Cover upload failed", error)
                        _uiState.update { 
                            it.copy(coverUploadState = UploadState.Error("Cover upload failed: ${error.message}")) 
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Unexpected error during cover upload", e)
                _uiState.update { 
                    it.copy(coverUploadState = UploadState.Error("Unexpected error: ${e.message}")) 
                }
            }
        }
    }

    private fun retryAvatarUpload() {
        lastAvatarUri?.let { uri ->
            handleAvatarSelection(uri)
        }
    }

    private fun retryCoverUpload() {
        lastCoverUri?.let { uri ->
            handleCoverSelection(uri)
        }
    }

    private fun saveProfile() {
        val state = _uiState.value

        if (state.usernameValidation is UsernameValidation.Error || state.nicknameError != null || state.bioError != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val userId = repository.getCurrentUserId() 
            
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "User not logged in") }
                return@launch
            }

            try {
                val updateData = mutableMapOf<String, String>()
                
                updateData["username"] = state.username
                updateData["display_name"] = state.nickname
                updateData["bio"] = state.bio
                updateData["gender"] = state.selectedGender.name.lowercase()
                
                state.selectedRegion?.let { updateData["region"] = it }
                state.avatarUrl?.let { updateData["avatar"] = it }
                state.coverUrl?.let { updateData["profile_cover_image"] = it }

                val result = repository.updateProfile(userId, updateData)

                result.fold(
                    onSuccess = {
                         val originalUsername = state.profile?.username
                         if (originalUsername != null && originalUsername != state.username) {
                             val syncResult = repository.syncUsernameChange(originalUsername, state.username, userId)
                             syncResult.fold(
                                 onSuccess = {
                                     _uiState.update { it.copy(isSaving = false, hasChanges = false) }
                                     _navigationEvents.emit(EditProfileNavigation.NavigateBack)
                                 },
                                 onFailure = { error ->
                                     _uiState.update { it.copy(isSaving = false, error = "Profile saved but username sync failed: ${error.message}. Please try again.") }
                                 }
                             )
                         } else {
                             _uiState.update { it.copy(isSaving = false, hasChanges = false) }
                             _navigationEvents.emit(EditProfileNavigation.NavigateBack)
                         }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isSaving = false, error = "Failed to save: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Unexpected error: ${e.message}") }
            }
        }
    }
}
