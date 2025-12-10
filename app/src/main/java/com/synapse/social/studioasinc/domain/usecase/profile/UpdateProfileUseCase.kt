package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.data.repository.ProfileRepository

class UpdateProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(userId: String, profile: UserProfile): Result<UserProfile> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(profile.username.isNotBlank()) { "Username cannot be blank" }
        return repository.updateProfile(userId, profile)
    }
}
