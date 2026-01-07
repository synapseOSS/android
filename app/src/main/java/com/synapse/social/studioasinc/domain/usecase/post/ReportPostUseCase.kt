package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.data.repository.PostInteractionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ReportPostUseCase @Inject constructor(private val repository: PostInteractionRepository) {
    operator fun invoke(postId: String, userId: String, reason: String): Flow<Result<Unit>> = flow {
        emit(repository.reportPost(postId, userId, reason))
    }
}
