package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.data.repository.PostInteractionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UnsavePostUseCase @Inject constructor(private val repository: PostInteractionRepository) {
    operator fun invoke(postId: String, userId: String): Flow<Result<Unit>> = flow {
        emit(repository.unsavePost(postId, userId))
    }
}
