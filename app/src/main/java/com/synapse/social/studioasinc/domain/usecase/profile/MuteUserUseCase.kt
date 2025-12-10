package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.ProfileActionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MuteUserUseCase(
    private val repository: ProfileActionRepository = ProfileActionRepository()
) {
    operator fun invoke(userId: String, mutedUserId: String): Flow<Result<Unit>> = flow {
        emit(repository.muteUser(userId, mutedUserId))
    }
}
