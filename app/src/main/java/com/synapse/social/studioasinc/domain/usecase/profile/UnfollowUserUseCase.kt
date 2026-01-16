package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.ProfileRepository
import javax.inject.Inject

class UnfollowUserUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(userId: String, targetUserId: String): Result<Unit> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(targetUserId.isNotBlank()) { "Target user ID cannot be blank" }
        return repository.unfollowUser(userId, targetUserId)
    }
}
