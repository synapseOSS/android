package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.SearchResult

interface SearchRepository {
    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<SearchResult.User>>
    suspend fun searchPosts(query: String, limit: Int = 20): Result<List<SearchResult.Post>>
    suspend fun searchMedia(query: String, limit: Int = 20, mediaType: SearchResult.MediaType? = null): Result<List<SearchResult.Media>>
}
