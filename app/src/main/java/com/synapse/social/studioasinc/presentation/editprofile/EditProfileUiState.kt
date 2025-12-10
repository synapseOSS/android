package com.synapse.social.studioasinc.presentation.editprofile

import android.net.Uri
import com.synapse.social.studioasinc.model.UserProfile

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val username: String = "",
    val usernameValidation: UsernameValidation = UsernameValidation.Valid,
    val nickname: String = "",
    val nicknameError: String? = null,
    val biography: String = "",
    val biographyError: String? = null,
    val selectedGender: Gender = Gender.Hidden,
    val selectedRegion: String? = null,
    val hasChanges: Boolean = false,
    val error: String? = null
)

enum class Gender {
    Male, Female, Hidden
}

sealed class UsernameValidation {
    object Valid : UsernameValidation()
    object Checking : UsernameValidation()
    data class Error(val message: String) : UsernameValidation()
}

sealed class EditProfileEvent {
    data class UsernameChanged(val username: String) : EditProfileEvent()
    data class NicknameChanged(val nickname: String) : EditProfileEvent()
    data class BiographyChanged(val biography: String) : EditProfileEvent()
    data class GenderSelected(val gender: Gender) : EditProfileEvent()
    data class RegionSelected(val region: String) : EditProfileEvent()
    data class AvatarSelected(val uri: Uri) : EditProfileEvent()
    data class CoverSelected(val uri: Uri) : EditProfileEvent()
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
