package com.synapse.social.studioasinc.domain.usecase.story

import com.synapse.social.studioasinc.data.repository.StoryRepository

class HasActiveStoryUseCase(
    private val storyRepository: StoryRepository
) {
    suspend operator fun invoke(userId: String): Result<Boolean> {
        return storyRepository.hasActiveStory(userId)
    }
}
