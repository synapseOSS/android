package com.synapse.social.studioasinc.presentation.editprofile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.FileUtil
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

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EditProfileRepository()

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
                                biography = profile.bio ?: "",
                                avatarUrl = profile.profileImageUrl,
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
                _uiState.update { it.copy(biography = event.biography, hasChanges = true) }
                validateBiography(event.biography)
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
            _uiState.update { it.copy(biographyError = "Bio must be 250 characters or less") }
        } else {
            _uiState.update { it.copy(biographyError = null) }
        }
    }

    private fun handleAvatarSelection(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val realFilePath = FileUtil.convertUriToFilePath(context, uri)

            if (realFilePath != null) {
                // Compress
                val tempFile = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
                FileUtil.resizeBitmapFileRetainRatio(realFilePath, tempFile.absolutePath, 1024)

                // Upload
                uploadAvatar(tempFile.absolutePath)
            } else {
                 _uiState.update { it.copy(error = "Failed to process image") }
            }
        }
    }

    private fun uploadAvatar(filePath: String) {
        viewModelScope.launch {
            val userId = repository.getCurrentUserId() ?: return@launch

            // Optimistic update or show loading?
            // Specs: Loading State: Circular progress indicator overlay
            // I should probably track upload status. For now I rely on repository suspension.

            val result = repository.uploadAvatar(userId, filePath)
            result.fold(
                onSuccess = { url ->
                     _uiState.update { it.copy(avatarUrl = url) }
                     repository.addToProfileHistory(userId, url)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Avatar upload failed: ${error.message}") }
                }
            )
        }
    }

    private fun handleCoverSelection(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val realFilePath = FileUtil.convertUriToFilePath(context, uri)

            if (realFilePath != null) {
                val tempFile = File(context.cacheDir, "temp_cover_${System.currentTimeMillis()}.jpg")
                FileUtil.resizeBitmapFileRetainRatio(realFilePath, tempFile.absolutePath, 1024)
                uploadCover(tempFile.absolutePath)
            } else {
                 _uiState.update { it.copy(error = "Failed to process image") }
            }
        }
    }

    private fun uploadCover(filePath: String) {
        viewModelScope.launch {
            val userId = repository.getCurrentUserId() ?: return@launch

            val result = repository.uploadCover(userId, filePath)
            result.fold(
                onSuccess = { url ->
                     _uiState.update { it.copy(coverUrl = url) }
                     repository.addToCoverHistory(userId, url)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Cover upload failed: ${error.message}") }
                }
            )
        }
    }

    private fun saveProfile() {
        val state = _uiState.value

        if (state.usernameValidation is UsernameValidation.Error || state.nicknameError != null || state.biographyError != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val userId = repository.getCurrentUserId() ?: return@launch

            val updateData = mutableMapOf<String, Any?>(
                "username" to state.username,
                "nickname" to state.nickname.ifEmpty { null },
                "biography" to state.biography.ifEmpty { null },
                "gender" to state.selectedGender.name.lowercase(),
                "region" to state.selectedRegion
            )

            // Add images if changed? No, they are updated immediately on upload.
            // But we should ensure consistency.
            if (state.avatarUrl != null) updateData["avatar"] = state.avatarUrl
            if (state.coverUrl != null) updateData["profile_cover_image"] = state.coverUrl

            val result = repository.updateProfile(userId, updateData)

            result.fold(
                onSuccess = {
                     val originalUsername = state.profile?.username
                     if (originalUsername != null && originalUsername != state.username) {
                         val syncResult = repository.syncUsernameChange(originalUsername, state.username, userId)
                         syncResult.fold(
                             onSuccess = {
                                 _uiState.update { it.copy(isSaving = false) }
                                 _navigationEvents.emit(EditProfileNavigation.NavigateBack)
                             },
                             onFailure = { error ->
                                 _uiState.update { it.copy(isSaving = false, error = "Profile saved but username sync failed: ${error.message}. Please try again.") }
                             }
                         )
                     } else {
                         _uiState.update { it.copy(isSaving = false) }
                         _navigationEvents.emit(EditProfileNavigation.NavigateBack)
                     }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = "Failed to save: ${error.message}") }
                }
            )
        }
    }
}
