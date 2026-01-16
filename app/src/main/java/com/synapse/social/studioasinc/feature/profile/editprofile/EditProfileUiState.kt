package com.synapse.social.studioasinc.presentation.editprofile

import android.net.Uri
import com.synapse.social.studioasinc.domain.model.UserProfile
import kotlinx.serialization.Serializable

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val avatarUploadState: UploadState = UploadState.Idle,
    val coverUploadState: UploadState = UploadState.Idle,
    val username: String = "",
    val usernameValidation: UsernameValidation = UsernameValidation.Valid,
    val nickname: String = "",
    val nicknameError: String? = null,
    val bio: String = "",
    val bioError: String? = null,
    val selectedGender: Gender = Gender.Hidden,
    val selectedRegion: String? = null,
    val hasChanges: Boolean = false,
    val error: String? = null
)

@Serializable
enum class Gender {
    Male, Female, Hidden
}

sealed class UploadState {
    object Idle : UploadState()
    data class Uploading(val progress: Float = 0f) : UploadState()
    object Success : UploadState()
    data class Error(val message: String, val canRetry: Boolean = true) : UploadState()
}

sealed class UsernameValidation {
    object Valid : UsernameValidation()
    object Checking : UsernameValidation()
    data class Error(val message: String) : UsernameValidation()
}

sealed class EditProfileEvent {
    data class UsernameChanged(val username: String) : EditProfileEvent()
    data class NicknameChanged(val nickname: String) : EditProfileEvent()
    data class BiographyChanged(val bio: String) : EditProfileEvent()
    data class GenderSelected(val gender: Gender) : EditProfileEvent()
    data class RegionSelected(val region: String) : EditProfileEvent()
    data class AvatarSelected(val uri: Uri) : EditProfileEvent()
    data class CoverSelected(val uri: Uri) : EditProfileEvent()
    object RetryAvatarUpload : EditProfileEvent()
    object RetryCoverUpload : EditProfileEvent()
    object SaveClicked : EditProfileEvent()
    object BackClicked : EditProfileEvent()
    object ProfileHistoryClicked : EditProfileEvent()
    object CoverHistoryClicked : EditProfileEvent()
}

sealed class EditProfileNavigation {
    object NavigateBack : EditProfileNavigation()
    object NavigateToRegionSelection : EditProfileNavigation()
    object NavigateToProfileHistory : EditProfileNavigation()
    object NavigateToCoverHistory : EditProfileNavigation()
}
