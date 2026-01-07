package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import com.synapse.social.studioasinc.domain.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepositoryImpl(
    private val databaseService: SupabaseDatabaseService = SupabaseDatabaseService()
) : SearchRepository {

    override suspend fun searchUsers(query: String, limit: Int): Result<List<SearchResult.User>> = withContext(Dispatchers.IO) {
        try {
            val result = databaseService.searchUsers(query, limit)
            result.fold(
                onSuccess = { users ->
                    Result.success(users.map { user ->
                        SearchResult.User(
                            uid = user["uid"]?.toString() ?: "",
                            username = user["username"]?.toString() ?: "",
                            nickname = user["nickname"]?.toString()?.takeIf { it != "null" },
                            avatar = user["avatar"]?.toString()?.takeIf { it != "null" },
                            gender = user["gender"]?.toString()?.takeIf { it != "null" },
                            accountType = user["account_type"]?.toString()?.takeIf { it != "null" },
                            isPremium = user["account_premium"]?.toString() == "true",
                            isVerified = user["verify"]?.toString() == "true",
                            isBanned = user["banned"]?.toString() == "true",
                            status = user["status"]?.toString()?.takeIf { it != "null" }
                        )
                    })
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchPosts(query: String, limit: Int): Result<List<SearchResult.Post>> = withContext(Dispatchers.IO) {
        try {
            val result = databaseService.searchPosts(query, limit)
            result.fold(
                onSuccess = { posts ->
                    val authorIds = posts.mapNotNull { it["author_uid"]?.toString() }.distinct()
                    if (authorIds.isEmpty()) return@fold Result.success(emptyList())

                    val authorsResult = databaseService.selectWhereIn("users", "*", "uid", authorIds)
                    val authorsMap = authorsResult.getOrNull()
                        ?.associateBy { it["uid"]?.toString() ?: "" } ?: emptyMap()

                    val mappedPosts = posts.mapNotNull { post ->
                        val authorId = post["author_uid"]?.toString() ?: return@mapNotNull null
                        val author = authorsMap[authorId]
                        SearchResult.Post(
                            postId = post["id"]?.toString() ?: "",
                            authorId = authorId,
                            authorName = author?.get("nickname")?.toString()
                                ?: "@${author?.get("username")?.toString() ?: ""}",
                            authorAvatar = author?.get("avatar")?.toString(),
                            content = post["post_text"]?.toString() ?: "",
                            timestamp = post["timestamp"]?.toString()?.toLongOrNull() ?: 0L,
                            likesCount = post["likes_count"]?.toString()?.toIntOrNull() ?: 0,
                            commentsCount = post["comments_count"]?.toString()?.toIntOrNull() ?: 0
                        )
                    }
                    Result.success(mappedPosts)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchMedia(query: String, limit: Int, mediaType: SearchResult.MediaType?): Result<List<SearchResult.Media>> = withContext(Dispatchers.IO) {
        try {
            val result = databaseService.searchMedia(query, mediaType, limit)

            result.fold(
                onSuccess = { posts ->
                    val authorIds = posts.mapNotNull { it["author_uid"]?.toString() }.distinct()
                    if (authorIds.isEmpty()) return@fold Result.success(emptyList())

                    val authorsResult = databaseService.selectWhereIn("users", "*", "uid", authorIds)
                    val authorsMap = authorsResult.getOrNull()
                        ?.associateBy { it["uid"]?.toString() ?: "" } ?: emptyMap()

                    val filteredMedia = posts.mapNotNull { post ->
                        val authorId = post["author_uid"]?.toString() ?: return@mapNotNull null
                        val postType = post["post_type"]?.toString()
                        val image = post["post_image"]?.toString()
                        // Note: ideally parse media_items JSON, but sticking to existing pattern for now

                        val actualMediaType = when {
                            postType == "VIDEO" -> SearchResult.MediaType.VIDEO
                            !image.isNullOrEmpty() && image != "null" -> SearchResult.MediaType.PHOTO
                            postType == "IMAGE" -> SearchResult.MediaType.PHOTO
                            else -> return@mapNotNull null
                        }

                        // Use post_image if available, otherwise would need to extract from media_items
                        val mediaUrl = image

                        if (mediaUrl.isNullOrEmpty() || mediaUrl == "null") return@mapNotNull null

                        val author = authorsMap[authorId]

                        SearchResult.Media(
                            postId = post["id"]?.toString() ?: "",
                            authorId = authorId,
                            authorName = author?.get("nickname")?.toString()
                                ?: "@${author?.get("username")?.toString() ?: ""}",
                            authorAvatar = author?.get("avatar")?.toString(),
                            mediaUrl = mediaUrl,
                            mediaType = actualMediaType,
                            timestamp = post["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                        )
                    }
                    Result.success(filteredMedia)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
