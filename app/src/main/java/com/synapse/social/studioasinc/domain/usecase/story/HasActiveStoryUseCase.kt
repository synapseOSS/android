package com.synapse.social.studioasinc.domain.usecase.story

import com.synapse.social.studioasinc.data.repository.StoryRepository
import javax.inject.Inject

class HasActiveStoryUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    suspend operator fun invoke(userId: String): Result<Boolean> {
        return storyRepository.hasActiveStory(userId)
    }
}
