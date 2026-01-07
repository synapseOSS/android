package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.repository.ProfileRepository
import javax.inject.Inject

class GetProfileContentUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend fun getPosts(userId: String, limit: Int = 10, offset: Int = 0): Result<List<Any>> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(limit > 0) { "Limit must be positive" }
        require(offset >= 0) { "Offset cannot be negative" }
        return repository.getProfilePosts(userId, limit, offset)
    }

    suspend fun getPhotos(userId: String, limit: Int = 20, offset: Int = 0): Result<List<Any>> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(limit > 0) { "Limit must be positive" }
        require(offset >= 0) { "Offset cannot be negative" }
        return repository.getProfilePhotos(userId, limit, offset)
    }

    suspend fun getReels(userId: String, limit: Int = 20, offset: Int = 0): Result<List<Any>> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(limit > 0) { "Limit must be positive" }
        require(offset >= 0) { "Offset cannot be negative" }
        return repository.getProfileReels(userId, limit, offset)
    }
}
