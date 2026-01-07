package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.ProfileRepository
import javax.inject.Inject

class IsFollowingUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(userId: String, targetUserId: String): Result<Boolean> {
        if (userId.isBlank() || targetUserId.isBlank()) return Result.success(false)
        if (userId == targetUserId) return Result.success(false)
        return repository.isFollowing(userId, targetUserId)
    }
}
