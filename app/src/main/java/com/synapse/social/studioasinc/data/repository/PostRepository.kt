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

import io.github.jan.supabase.SupabaseClient as JanSupabaseClient

class PostRepository(
    private val postDao: PostDao,
    private val client: JanSupabaseClient = SupabaseClient.client
) {

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
        return SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_POST_MEDIA, storagePath)
    }

    private fun constructAvatarUrl(storagePath: String): String {
        return SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_USER_AVATARS, storagePath)
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

            // Ensure author details are populated (optimistic update for local DB)
            if (post.username == null) {
                val profile = fetchUserProfile(post.authorUid)
                if (profile != null) {
                    post.username = profile.username
                    post.avatarUrl = profile.avatarUrl
                    post.isVerified = profile.isVerified
                }
            }

            val postDto = post.toInsertDto()

            android.util.Log.d(TAG, "Creating post with DTO")
            client.from("posts").insert(postDto)
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
            val post = postDao.getPostById(postId)?.let { entity ->
                val model = PostMapper.toModel(entity)
                // If username is missing, try to fetch it
                if (model.username == null) {
                    fetchUserProfile(model.authorUid)?.let { profile ->
                        model.username = profile.username
                        model.avatarUrl = profile.avatarUrl
                        model.isVerified = profile.isVerified
                    }
                }
                model
            }
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(Exception("Error getting post from database: ${e.message}"))
        }
    }

    fun getPosts(): Flow<Result<List<Post>>> {
        return postDao.getAllPosts().map { entities ->
            val posts = entities.map { PostMapper.toModel(it) }

            // Identify posts with missing usernames
            val missingUserIds = posts.filter { it.username == null }
                .map { it.authorUid }
                .distinct()
                .filter { userId ->
                    // Check if already in cache
                    profileCache[userId]?.let { !it.isExpired() } != true
                }

            // Batch fetch missing profiles
            if (missingUserIds.isNotEmpty()) {
                fetchUserProfilesBatch(missingUserIds)
            }

            // Enrich posts from cache
            posts.forEach { post ->
                if (post.username == null) {
                    profileCache[post.authorUid]?.data?.let { profile ->
                        post.username = profile.username
                        post.avatarUrl = profile.avatarUrl
                        post.isVerified = profile.isVerified
                    }
                }
            }

            Result.success(posts)
        }.catch { e ->
            emit(Result.failure(Exception("Error getting posts from database: ${e.message}")))
        }
    }

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
                .decodeList<PostSelectDto>()

            val posts = response.map { postDto ->
                postDto.toDomain(::constructMediaUrl, ::constructAvatarUrl).also { post ->
                    // Update profile cache if we have user data
                    postDto.user?.let { user ->
                        if (user.uid.isNotEmpty()) {
                            profileCache[user.uid] = CacheEntry(
                                ProfileData(
                                    user.username,
                                    user.avatarUrl?.let { constructAvatarUrl(it) },
                                    user.isVerified ?: false
                                )
                            )
                        }
                    }
                }
            }.sortedByDescending { it.timestamp }

            val postsWithReactions = populatePostReactions(posts)
            postDao.insertAll(postsWithReactions.map { PostMapper.toEntity(it) })

            // Perform a sync of deleted posts only when refreshing the first page (Pull-to-Refresh)
            if (page == 0) {
                syncDeletedPosts()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch posts page: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun syncDeletedPosts() {
        try {
            // Fetch all IDs from server to identify deleted posts using pagination to avoid limits
            val serverIds = mutableSetOf<String>()
            val pageSize = 1000
            var offset = 0

            while (true) {
                val response = client.from("posts")
                    .select(columns = Columns.raw("id")) {
                        range(offset.toLong(), (offset + pageSize - 1).toLong())
                    }
                    .decodeList<JsonObject>()

                val pageIds = response.mapNotNull { it["id"]?.jsonPrimitive?.contentOrNull }
                serverIds.addAll(pageIds)

                if (pageIds.size < pageSize) break
                offset += pageSize
            }

            val localIds = postDao.getAllPostIds()
            val idsToDelete = localIds.filter { !serverIds.contains(it) }

            if (idsToDelete.isNotEmpty()) {
                android.util.Log.d(TAG, "Syncing deletions: removing ${idsToDelete.size} posts")
                // Batch delete to avoid SQLite limits on bind variables (max 999 usually)
                idsToDelete.chunked(500).forEach { batch ->
                    postDao.deletePosts(batch)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to sync deleted posts", e)
            // We do not fail the whole refresh if sync fails, just log it
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
                .decodeList<PostSelectDto>()
            
            val posts = response.map { postDto ->
                postDto.toDomain(::constructMediaUrl, ::constructAvatarUrl).also { post ->
                     postDto.user?.let { user ->
                        if (user.uid.isNotEmpty()) {
                            profileCache[user.uid] = CacheEntry(
                                ProfileData(
                                    user.username,
                                    user.avatarUrl?.let { constructAvatarUrl(it) },
                                    user.isVerified ?: false
                                )
                            )
                        }
                    }
                }
            }
            Result.success(posts)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch user posts: ${e.message}", e)
            Result.failure(e)
        }
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
            val updateDto = post.toUpdateDto()

            client.from("posts").update(updateDto) {
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

    // Delegate to ReactionRepository for Single Source of Truth
    private val reactionRepository = ReactionRepository()

    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: ReactionType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // We ignore userId param as ReactionRepository uses the authenticated user securely
        reactionRepository.toggleReaction(postId, "post", reactionType)
            .map { Unit } // Convert ReactionToggleResult to Unit for backward compatibility
    }

    suspend fun getReactionSummary(postId: String): Result<Map<ReactionType, Int>> = 
        reactionRepository.getReactionSummary(postId, "post")

    suspend fun getUserReaction(postId: String, userId: String): Result<ReactionType?> = 
        // ReactionRepository uses current auth user, so we ignore userId param if it matches current user.
        // If userId != current user, we fall back to manual query or return null as ReactionRepository focuses on 'my' reaction.
        // However, the original method was fetching *specific* user reaction. 
        // ReactionRepository.getUserReaction gets *current* user reaction.
        // If we need another user's reaction, we might need a specific method.
        // But typically we only check OUR reaction for UI highligts.
        if (userId == client.auth.currentUserOrNull()?.id) {
             reactionRepository.getUserReaction(postId, "post")
        } else {
             // Fallback for other users (rarely used for "My Reaction" state)
             // We can keep the manual query here if needed, or assume it's mostly for "me".
             // Let's implement the manual query using the unified table structure just in case.
             withContext(Dispatchers.IO) {
                 try {
                     val reaction = client.from("reactions")
                         .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                         .decodeSingleOrNull<JsonObject>()
                     val typeStr = reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull
                     Result.success(typeStr?.let { ReactionType.fromString(it) })
                 } catch (e: Exception) {
                     Result.failure(Exception("Error fetching user reaction"))
                 }
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

    // Use unified batch fetch logic
    private suspend fun populatePostReactions(posts: List<Post>): List<Post> {
        return reactionRepository.populatePostReactions(posts)
    }

    private suspend fun fetchUserProfilesBatch(userIds: List<String>) {
        try {
            val users = client.from("users").select {
                filter { isIn("uid", userIds) }
            }.decodeList<JsonObject>()

            users.forEach { user ->
                val uid = user["uid"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val profile = ProfileData(
                    username = user["username"]?.jsonPrimitive?.contentOrNull,
                    avatarUrl = user["avatar"]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) },
                    isVerified = user["verify"]?.jsonPrimitive?.booleanOrNull ?: false
                )
                profileCache[uid] = CacheEntry(profile)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to batch fetch user profiles", e)
        }
    }

    private suspend fun fetchUserProfile(userId: String): ProfileData? {
        // Check cache first
        profileCache[userId]?.let { entry ->
            if (!entry.isExpired()) return entry.data
        }

        return try {
            val user = client.from("users").select {
                filter { eq("uid", userId) }
            }.decodeSingleOrNull<JsonObject>()

            if (user != null) {
                val profile = ProfileData(
                    username = user["username"]?.jsonPrimitive?.contentOrNull,
                    avatarUrl = user["avatar"]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) },
                    isVerified = user["verify"]?.jsonPrimitive?.booleanOrNull ?: false
                )
                profileCache[userId] = CacheEntry(profile)
                profile
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch user profile for $userId", e)
            null
        }
    }
}
