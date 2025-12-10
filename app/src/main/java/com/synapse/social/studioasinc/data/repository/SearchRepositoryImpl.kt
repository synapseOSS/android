package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.model.SearchResult
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
                    val authorIds = posts.mapNotNull { it["uid"]?.toString() }.distinct()
                    if (authorIds.isEmpty()) return@fold Result.success(emptyList())

                    val authorsResult = databaseService.select("users", "*")
                    val authorsMap = authorsResult.getOrNull()
                        ?.filter { authorIds.contains(it["uid"]?.toString()) }
                        ?.associateBy { it["uid"]?.toString() ?: "" } ?: emptyMap()

                    val mappedPosts = posts.mapNotNull { post ->
                        val authorId = post["uid"]?.toString() ?: return@mapNotNull null
                        val author = authorsMap[authorId]
                        SearchResult.Post(
                            postId = post["post_id"]?.toString() ?: "",
                            authorId = authorId,
                            authorName = author?.get("nickname")?.toString()
                                ?: "@${author?.get("username")?.toString() ?: ""}",
                            authorAvatar = author?.get("avatar")?.toString(),
                            content = post["content"]?.toString() ?: "",
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
            val result = databaseService.select("posts", "*")

            result.fold(
                onSuccess = { posts ->
                    val filteredPosts = posts.filter { post ->
                        when (mediaType) {
                            SearchResult.MediaType.PHOTO -> {
                                val image = post["image"]?.toString()
                                !image.isNullOrEmpty() && image != "null"
                            }
                            SearchResult.MediaType.VIDEO -> {
                                val video = post["video"]?.toString()
                                !video.isNullOrEmpty() && video != "null"
                            }
                            null -> {
                                val image = post["image"]?.toString()
                                val video = post["video"]?.toString()
                                (!image.isNullOrEmpty() && image != "null") ||
                                (!video.isNullOrEmpty() && video != "null")
                            }
                        }
                    }.take(limit)

                    val authorIds = filteredPosts.mapNotNull { it["uid"]?.toString() }.distinct()
                    if (authorIds.isEmpty()) return@fold Result.success(emptyList())

                    val authorsResult = databaseService.select("users", "*")
                    val authorsMap = authorsResult.getOrNull()
                        ?.filter { authorIds.contains(it["uid"]?.toString()) }
                        ?.associateBy { it["uid"]?.toString() ?: "" } ?: emptyMap()

                    val filteredMedia = filteredPosts.mapNotNull { post ->
                        val authorId = post["uid"]?.toString() ?: return@mapNotNull null
                        val video = post["video"]?.toString()
                        val image = post["image"]?.toString()

                        val actualMediaType = when {
                            !video.isNullOrEmpty() && video != "null" -> SearchResult.MediaType.VIDEO
                            !image.isNullOrEmpty() && image != "null" -> SearchResult.MediaType.PHOTO
                            else -> return@mapNotNull null
                        }

                        val mediaUrl = if (actualMediaType == SearchResult.MediaType.VIDEO) video else image
                        val author = authorsMap[authorId]

                        SearchResult.Media(
                            postId = post["post_id"]?.toString() ?: "",
                            authorId = authorId,
                            authorName = author?.get("nickname")?.toString()
                                ?: "@${author?.get("username")?.toString() ?: ""}",
                            authorAvatar = author?.get("avatar")?.toString(),
                            mediaUrl = mediaUrl ?: "",
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
