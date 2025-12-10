package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.ProfileActionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ReportUserUseCase(
    private val repository: ProfileActionRepository = ProfileActionRepository()
) {
    operator fun invoke(userId: String, reportedUserId: String, reason: String): Flow<Result<Unit>> = flow {
        emit(repository.reportUser(userId, reportedUserId, reason))
    }
}
