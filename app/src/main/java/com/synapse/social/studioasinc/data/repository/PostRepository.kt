package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.data.local.PostDao
import com.synapse.social.studioasinc.data.local.PostEntity
import com.synapse.social.studioasinc.data.repository.PostMapper
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.PollOption
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.model.UserReaction
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import com.synapse.social.studioasinc.util.ImageLoader
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.synapse.social.studioasinc.data.paging.PostPagingSource
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class PostRepository(private val postDao: PostDao) {

    private val client = SupabaseClient.client

    fun getPostsPaged(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { PostPagingSource(client.from("posts")) }
        ).flow
    }
    
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(expirationMs: Long = CACHE_EXPIRATION_MS): Boolean =
            System.currentTimeMillis() - timestamp > expirationMs
    }

    // FIXME: Bug (Concurrency) - These MutableMaps are accessed via Coroutines and are not thread-safe. Suggest ConcurrentHashMap or Mutex.
    private val postsCache = mutableMapOf<String, CacheEntry<List<Post>>>()
    private val profileCache = mutableMapOf<String, CacheEntry<ProfileData>>()

    companion object {
        private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L
        private const val TAG = "PostRepository"
    }

    private data class ProfileData(
        val username: String?,
        val avatarUrl: String?,
        val isVerified: Boolean
    )

    fun invalidateCache() {
        postsCache.clear()
        profileCache.clear()
        android.util.Log.d(TAG, "Cache invalidated")
    }

    // TODO: Refactor (Hardcoding) - URL construction should be centralized in SupabaseClient or a config file, not hardcoded strings.
    fun constructMediaUrl(storagePath: String): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath
        }
        return ImageLoader.constructStorageUrl(storagePath, "post-media")
    }

    private fun constructAvatarUrl(storagePath: String): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath
        }
        return ImageLoader.constructStorageUrl(storagePath, "user-avatars")
    }

    private fun mapSupabaseError(exception: Exception): String {
        val message = exception.message ?: "Unknown error"
        val pgrstMatch = Regex("PGRST\\d+").find(message)
        if (pgrstMatch != null) {
            android.util.Log.e(TAG, "Supabase PostgREST error code: ${pgrstMatch.value}")
        }
        android.util.Log.e(TAG, "Supabase error: $message", exception)
        return when {
            message.contains("PGRST200") -> "Relation/table not found in schema"
            message.contains("PGRST100") -> "Column does not exist"
            message.contains("PGRST116") -> "No rows returned (expected single)"
            message.contains("relation", ignoreCase = true) -> "Database table does not exist"
            message.contains("column", ignoreCase = true) -> "Database column mismatch"
            message.contains("policy", ignoreCase = true) || message.contains("rls", ignoreCase = true) ->
                "Permission denied. Row-level security policy blocked this operation."
            message.contains("connection", ignoreCase = true) || message.contains("network", ignoreCase = true) ->
                "Connection failed. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            message.contains("unauthorized", ignoreCase = true) -> "Permission denied."
            message.contains("serialization", ignoreCase = true) -> "Data format error."
            else -> "Database error: $message"
        }
    }

    // TODO: Refactor (Serialization) - Manual JSON construction is fragile. Suggest using kotlinx.serialization and @Serializable DTOs to map Supabase responses directly to objects.
    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured."))
            }
            val insertData = buildJsonObject {
                put("id", post.id)
                post.key?.let { put("key", it) }
                put("author_uid", post.authorUid)
                post.postText?.let { put("post_text", it) }
                post.postImage?.let { put("post_image", it) }
                post.postType?.let { put("post_type", it) }
                post.postVisibility?.let { put("post_visibility", it) }
                post.postHideViewsCount?.let { put("post_hide_views_count", it) }
                post.postHideLikeCount?.let { put("post_hide_like_count", it) }
                post.postHideCommentsCount?.let { put("post_hide_comments_count", it) }
                post.postDisableComments?.let { put("post_disable_comments", it) }
                post.publishDate?.let { put("publish_date", it) }
                put("timestamp", post.timestamp)
                put("likes_count", 0)
                put("comments_count", 0)
                put("views_count", 0)
                post.mediaItems?.let { items ->
                    put("media_items", buildJsonArray {
                        items.forEach { media ->
                            add(buildJsonObject {
                                put("id", media.id)
                                put("url", media.url)
                                put("type", media.type.name)
                                media.thumbnailUrl?.let { put("thumbnailUrl", it) }
                                media.duration?.let { put("duration", it) }
                                media.size?.let { put("size", it) }
                                media.mimeType?.let { put("mimeType", it) }
                            })
                        }
                    })
                }
                post.hasPoll?.let { put("has_poll", it) }
                post.pollQuestion?.let { put("poll_question", it) }
                post.pollOptions?.let { options ->
                    put("poll_options", buildJsonArray {
                        options.forEach { opt ->
                            add(buildJsonObject {
                                put("text", opt.text)
                                put("votes", opt.votes)
                            })
                        }
                    })
                }
                post.pollEndTime?.let { put("poll_end_time", it) }
                post.hasLocation?.let { put("has_location", it) }
                post.locationName?.let { put("location_name", it) }
                post.locationAddress?.let { put("location_address", it) }
                post.locationLatitude?.let { put("location_latitude", it) }
                post.locationLongitude?.let { put("location_longitude", it) }
                post.youtubeUrl?.let { put("youtube_url", it) }
            }

            android.util.Log.d(TAG, "Creating post with data: $insertData")
            client.from("posts").insert(insertData)
            postDao.insertAll(listOf(PostMapper.toEntity(post)))
            invalidateCache()
            Result.success(post)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to create post", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun getPost(postId: String): Result<Post?> = withContext(Dispatchers.IO) {
        try {
            val post = postDao.getPostById(postId)?.let { PostMapper.toModel(it) }
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(Exception("Error getting post from database: ${e.message}"))
        }
    }

    fun getPosts(): Flow<Result<List<Post>>> {
        return postDao.getAllPosts().map<List<PostEntity>, Result<List<Post>>> { entities ->
            Result.success(entities.map { PostMapper.toModel(it) })
        }.catch { e ->
            emit(Result.failure(Exception("Error getting posts from database: ${e.message}")))
        }
    }

    // TODO: Logic (Sync Strategy) - Currently uses insertAll (upsert) but does not handle local deletions if a post was removed from the server. Suggest implementing a sync strategy (e.g., fetch IDs -> delete missing -> upsert new).
    suspend fun refreshPosts(page: Int, pageSize: Int): Result<Unit> {
        return try {
            val offset = page * pageSize
            val response = client.from("posts")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!posts_author_uid_fkey(uid, username, avatar, verify)
                    """.trimIndent())
                ) {
                    range(offset.toLong(), (offset + pageSize - 1).toLong())
                }
                .decodeList<JsonObject>()

            val posts = response.mapNotNull { postData ->
                try {
                    parsePostWithUserData(postData)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to parse post: ${e.message}", e)
                    null
                }
            }.sortedByDescending { it.timestamp }

            val postsWithReactions = populatePostReactions(posts)
            postDao.insertAll(postsWithReactions.map { PostMapper.toEntity(it) })
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch posts page: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserPosts(userId: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val response = client.from("posts")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!posts_author_uid_fkey(uid, username, avatar, verify)
                    """.trimIndent())
                ) {
                    filter { eq("author_uid", userId) }
                }
                .decodeList<JsonObject>()
            
            val posts = response.mapNotNull { postData ->
                try {
                    parsePostWithUserData(postData)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to parse post: ${e.message}", e)
                    null
                }
            }
            Result.success(posts)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch user posts: ${e.message}", e)
            Result.failure(e)
        }
    }

    // TODO: Refactor (Serialization) - Manual JSON parsing is fragile. Suggest using kotlinx.serialization and @Serializable DTOs.
    private fun parsePostWithUserData(data: JsonObject): Post {
        val post = Post(
            id = data["id"]?.jsonPrimitive?.contentOrNull ?: "",
            key = data["key"]?.jsonPrimitive?.contentOrNull,
            authorUid = data["author_uid"]?.jsonPrimitive?.contentOrNull ?: "",
            postText = data["post_text"]?.jsonPrimitive?.contentOrNull,
            postImage = data["post_image"]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) },
            postType = data["post_type"]?.jsonPrimitive?.contentOrNull,
            postHideViewsCount = data["post_hide_views_count"]?.jsonPrimitive?.contentOrNull,
            postHideLikeCount = data["post_hide_like_count"]?.jsonPrimitive?.contentOrNull,
            postHideCommentsCount = data["post_hide_comments_count"]?.jsonPrimitive?.contentOrNull,
            postDisableComments = data["post_disable_comments"]?.jsonPrimitive?.contentOrNull,
            postVisibility = data["post_visibility"]?.jsonPrimitive?.contentOrNull,
            publishDate = data["publish_date"]?.jsonPrimitive?.contentOrNull,
            timestamp = data["timestamp"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
            likesCount = data["likes_count"]?.jsonPrimitive?.intOrNull ?: 0,
            commentsCount = data["comments_count"]?.jsonPrimitive?.intOrNull ?: 0,
            viewsCount = data["views_count"]?.jsonPrimitive?.intOrNull ?: 0,
            resharesCount = data["reshares_count"]?.jsonPrimitive?.intOrNull ?: 0,
            hasPoll = data["has_poll"]?.jsonPrimitive?.booleanOrNull,
            pollQuestion = data["poll_question"]?.jsonPrimitive?.contentOrNull,
            pollOptions = data["poll_options"]?.jsonArray?.mapNotNull {
                val obj = it.jsonObject
                val text = obj["text"]?.jsonPrimitive?.contentOrNull
                val votes = obj["votes"]?.jsonPrimitive?.intOrNull ?: 0
                if (text != null) PollOption(text, votes) else null
            },
            pollEndTime = data["poll_end_time"]?.jsonPrimitive?.contentOrNull,
            hasLocation = data["has_location"]?.jsonPrimitive?.booleanOrNull,
            locationName = data["location_name"]?.jsonPrimitive?.contentOrNull,
            locationAddress = data["location_address"]?.jsonPrimitive?.contentOrNull,
            locationLatitude = data["location_latitude"]?.jsonPrimitive?.doubleOrNull,
            locationLongitude = data["location_longitude"]?.jsonPrimitive?.doubleOrNull,
            youtubeUrl = data["youtube_url"]?.jsonPrimitive?.contentOrNull
        )
        val userData = data["users"]?.jsonObject
        if (userData != null) {
            post.username = userData["username"]?.jsonPrimitive?.contentOrNull
            post.avatarUrl = userData["avatar"]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) }
            post.isVerified = userData["verify"]?.jsonPrimitive?.booleanOrNull ?: false
            val authorUid = post.authorUid
            if (authorUid.isNotEmpty()) {
                profileCache[authorUid] = CacheEntry(
                    ProfileData(post.username, post.avatarUrl, post.isVerified)
                )
            }
        }
        val mediaData = data["media_items"]?.takeIf { it !is JsonNull }?.jsonArray
        if (mediaData != null && mediaData.isNotEmpty()) {
            post.mediaItems = mediaData.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap["type"]?.jsonPrimitive?.contentOrNull ?: "IMAGE"
                MediaItem(
                    id = mediaMap["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = constructMediaUrl(url),
                    type = if (typeStr.equals("VIDEO", ignoreCase = true)) MediaType.VIDEO else MediaType.IMAGE,
                    thumbnailUrl = mediaMap["thumbnailUrl"]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) },
                    duration = mediaMap["duration"]?.jsonPrimitive?.longOrNull,
                    size = mediaMap["size"]?.jsonPrimitive?.longOrNull,
                    mimeType = mediaMap["mimeType"]?.jsonPrimitive?.contentOrNull
                )
            }.toMutableList()
        }
        return post
    }

    suspend fun updatePost(postId: String, updates: Map<String, Any?>): Result<Post> = withContext(Dispatchers.IO) {
        try {
            client.from("posts").update(updates) {
                filter { eq("id", postId) }
            }
            invalidateCache()
            Result.success(Post(id = postId, authorUid = ""))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to update post", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun updatePost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val updateData = buildJsonObject {
                post.postText?.let { put("post_text", it) }
                // Handle image update logic carefully - if null it might mean no change or deleted. 
                // For simplicity assuming we send what we have.
                if (post.postImage != null) put("post_image", post.postImage)
                
                post.postType?.let { put("post_type", it) }
                post.postVisibility?.let { put("post_visibility", it) }
                post.postHideViewsCount?.let { put("post_hide_views_count", it) }
                post.postHideLikeCount?.let { put("post_hide_like_count", it) }
                post.postHideCommentsCount?.let { put("post_hide_comments_count", it) }
                post.postDisableComments?.let { put("post_disable_comments", it) }
                // Don't update publish_date usually
                put("updated_at", System.currentTimeMillis()) 
                
                post.mediaItems?.let { items ->
                    put("media_items", buildJsonArray {
                        items.forEach { media ->
                            add(buildJsonObject {
                                put("id", media.id)
                                put("url", media.url)
                                put("type", media.type.name)
                                media.thumbnailUrl?.let { put("thumbnailUrl", it) }
                                media.duration?.let { put("duration", it) }
                                media.size?.let { put("size", it) }
                                media.mimeType?.let { put("mimeType", it) }
                            })
                        }
                    })
                }
                
                // Poll updates (if allowed)
                post.pollQuestion?.let { put("poll_question", it) }
                post.pollOptions?.let { options ->
                    put("poll_options", buildJsonArray {
                        options.forEach { opt ->
                            add(buildJsonObject {
                                put("text", opt.text)
                                put("votes", opt.votes)
                            })
                        }
                    })
                }
                
                post.youtubeUrl?.let { put("youtube_url", it) }
                post.locationName?.let { put("location_name", it) }
                post.locationAddress?.let { put("location_address", it) }
                post.locationLatitude?.let { put("location_latitude", it) }
                post.locationLongitude?.let { put("location_longitude", it) }
            }

            client.from("posts").update(updateData) {
                filter { eq("id", post.id) }
            }
            
            // Also update local DB
            postDao.insertAll(listOf(PostMapper.toEntity(post)))
            invalidateCache()
            
            Result.success(post)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to update full post", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("posts").delete {
                filter { eq("id", postId) }
            }
            postDao.deletePost(postId)
            invalidateCache()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to delete post", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun searchPosts(query: String): Result<List<Post>> = Result.success(emptyList())

    fun observePosts(): Flow<List<Post>> = flow { emit(emptyList()) }

    // TODO: Optimization (Network/Atomicity) - This function lacks atomicity and makes multiple network calls. Suggest replacing this logic with a Supabase Edge Function or Postgres RPC.
    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: ReactionType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "Toggling reaction: ${reactionType.name} for post $postId")
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null || userId.isEmpty()) {
                return@withContext Result.failure(Exception("User must be authenticated to react"))
            }
            var lastException: Exception? = null
            repeat(3) { attempt ->
                try {
                    val existingReaction = client.from("reactions")
                        .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                        .decodeSingleOrNull<JsonObject>()

                    if (existingReaction != null) {
                        val existingType = existingReaction["reaction_type"]?.jsonPrimitive?.contentOrNull
                        if (existingType == reactionType.name.lowercase()) {
                            client.from("reactions")
                                .delete { filter { eq("post_id", postId); eq("user_id", userId) } }
                            android.util.Log.d(TAG, "Reaction removed")
                        } else {
                            client.from("reactions")
                                .update({
                                    set("reaction_type", reactionType.name.lowercase())
                                    set("updated_at", java.time.Instant.now().toString())
                                }) { filter { eq("post_id", postId); eq("user_id", userId) } }
                            android.util.Log.d(TAG, "Reaction updated to ${reactionType.name}")
                        }
                    } else {
                        client.from("reactions").insert(buildJsonObject {
                            put("user_id", userId)
                            put("post_id", postId)
                            put("reaction_type", reactionType.name.lowercase())
                        })
                        android.util.Log.d(TAG, "New reaction created")
                    }
                    return@withContext Result.success(Unit)
                } catch (e: Exception) {
                    lastException = e
                    val isRLSError = e.message?.contains("policy", true) == true
                    if (isRLSError || attempt == 2) throw e
                    kotlinx.coroutines.delay(100L * (attempt + 1))
                }
            }
            Result.failure(Exception(mapSupabaseError(lastException ?: Exception("Unknown"))))
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun getReactionSummary(postId: String): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        try {
            val reactions = client.from("reactions")
                .select { filter { eq("post_id", postId) } }
                .decodeList<JsonObject>()

            val summary = reactions
                .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                .mapValues { it.value.size }
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    // TODO: Optimization (N+1 Query) - Fetches reactions first, then users. Suggest refactoring to a single query using Supabase resource embedding (joins).
    suspend fun getUsersWhoReacted(
        postId: String,
        reactionType: ReactionType? = null
    ): Result<List<UserReaction>> = withContext(Dispatchers.IO) {
        try {
            val reactions = client.from("reactions")
                .select {
                    filter {
                        eq("post_id", postId)
                        if (reactionType != null) eq("reaction_type", reactionType.name)
                    }
                }
                .decodeList<JsonObject>()

            if (reactions.isEmpty()) return@withContext Result.success(emptyList())

            val userIds = reactions.mapNotNull { it["user_id"]?.jsonPrimitive?.contentOrNull }
            if (userIds.isEmpty()) return@withContext Result.success(emptyList())

            val users = client.from("users")
                .select { filter { isIn("uid", userIds) } }
                .decodeList<JsonObject>()
                .associateBy { it["uid"]?.jsonPrimitive?.contentOrNull }

            val userReactions = reactions.mapNotNull { reaction ->
                val userId = reaction["user_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val user = users[userId]
                UserReaction(
                    userId = userId,
                    username = user?.get("username")?.jsonPrimitive?.contentOrNull ?: "Unknown",
                    profileImage = user?.get("avatar")?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) },
                    isVerified = user?.get("verify")?.jsonPrimitive?.booleanOrNull ?: false,
                    reactionType = reaction["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE",
                    reactedAt = reaction["created_at"]?.jsonPrimitive?.contentOrNull
                )
            }
            Result.success(userReactions)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun getUserReaction(postId: String, userId: String): Result<ReactionType?> = withContext(Dispatchers.IO) {
        try {
            val reaction = client.from("reactions")
                .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()

            val typeStr = reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull
            Result.success(typeStr?.let { ReactionType.fromString(it) })
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    // TODO: Scalability - It uses isIn("post_id", postIds). For large pages, this list of IDs can exceed URL length limits. Suggest chunking or alternative query strategies.
    private suspend fun populatePostReactions(posts: List<Post>): List<Post> {
        if (posts.isEmpty()) return posts

        return try {
            val postIds = posts.map { it.id }
            val currentUserId = client.auth.currentUserOrNull()?.id

            val allReactions = client.from("reactions")
                .select { filter { isIn("post_id", postIds) } }
                .decodeList<JsonObject>()

            val reactionsByPost = allReactions.groupBy { it["post_id"]?.jsonPrimitive?.contentOrNull }

            posts.map { post ->
                val postReactions = reactionsByPost[post.id] ?: emptyList()
                val summary = postReactions
                    .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                    .mapValues { it.value.size }

                val userReactionType = if (currentUserId != null) {
                    postReactions.find { it["user_id"]?.jsonPrimitive?.contentOrNull == currentUserId }
                        ?.let { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                } else null

                post.copy(reactions = summary, userReaction = userReactionType)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to populate reactions", e)
            posts
        }
    }
}
