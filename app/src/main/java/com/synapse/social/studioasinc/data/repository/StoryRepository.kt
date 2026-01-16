package com.synapse.social.studioasinc.data.repository

import android.net.Uri
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.core.storage.MediaStorageService
import com.synapse.social.studioasinc.data.local.database.AppSettingsManager
import com.synapse.social.studioasinc.data.model.StoryCreateRequest
import com.synapse.social.studioasinc.domain.model.Story
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.StoryPrivacy
import com.synapse.social.studioasinc.domain.model.StoryView
import com.synapse.social.studioasinc.domain.model.StoryViewWithUser
import com.synapse.social.studioasinc.domain.model.StoryWithUser
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.core.util.FileManager
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

interface StoryRepository {
    /**
     * Check if a user has any active (non-expired) stories
     */
    suspend fun hasActiveStory(userId: String): Result<Boolean>
    
    /**
     * Get all active stories from friends and self as a flow
     */
    fun getActiveStories(currentUserId: String): Flow<List<StoryWithUser>>
    
    /**
     * Get stories for a specific user
     */
    suspend fun getUserStories(userId: String): Result<List<Story>>
    
    /**
     * Create a new story with media upload
     */
    suspend fun createStory(
        userId: String,
        mediaUri: Uri,
        mediaType: StoryMediaType,
        privacy: StoryPrivacy,
        duration: Int = 5
    ): Result<Story>
    
    /**
     * Delete a story by ID
     */
    suspend fun deleteStory(storyId: String): Result<Unit>
    
    /**
     * Mark a story as seen by the current user
     */
    suspend fun markAsSeen(storyId: String, viewerId: String): Result<Unit>
    
    /**
     * Get the list of users who viewed a specific story
     */
    suspend fun getStoryViewers(storyId: String): Result<List<StoryViewWithUser>>
    
    /**
     * Check if current user has seen a specific story
     */
    suspend fun hasSeenStory(storyId: String, viewerId: String): Result<Boolean>
}

class StoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val appSettingsManager: AppSettingsManager
) : StoryRepository {
    private val client = SupabaseClient.client
    private val mediaStorageService = MediaStorageService(context, appSettingsManager)
    
    companion object {
        private const val TABLE_STORIES = "stories"
        private const val TABLE_STORY_VIEWS = "story_views"
        private const val TABLE_USERS = "users"
    }

    override suspend fun hasActiveStory(userId: String): Result<Boolean> = try {
        val now = Instant.now().toString()

        val count = client.from(TABLE_STORIES).select {
            filter {
                eq("user_id", userId)
                gt("expires_at", now)
            }
            count(io.github.jan.supabase.postgrest.query.Count.EXACT)
        }.countOrNull() ?: 0

        Result.success(count > 0)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override fun getActiveStories(currentUserId: String): Flow<List<StoryWithUser>> = flow {
        try {
            val now = Instant.now().toString()
            
            // Get all active stories with user data
            val stories = client.from(TABLE_STORIES)
                .select(columns = Columns.raw("*, users!inner(*)")) {
                    filter {
                        gt("expires_at", now)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<JsonObject>()
            
            // Group stories by user
            val storiesByUser = mutableMapOf<String, MutableList<Story>>()
            val usersMap = mutableMapOf<String, User>()
            
            for (storyJson in stories) {
                val userId = storyJson["user_id"]?.jsonPrimitive?.content ?: continue
                
                val story = Story(
                    id = storyJson["id"]?.jsonPrimitive?.content,
                    userId = userId,
                    mediaUrl = storyJson["media_url"]?.jsonPrimitive?.content,
                    mediaType = try {
                        storyJson["media_type"]?.jsonPrimitive?.content?.let {
                            StoryMediaType.valueOf(it.uppercase())
                        }
                    } catch (e: Exception) { null },
                    content = storyJson["content"]?.jsonPrimitive?.content,
                    duration = storyJson["duration"]?.jsonPrimitive?.content?.toIntOrNull(),
                    durationHours = storyJson["duration_hours"]?.jsonPrimitive?.content?.toIntOrNull(),
                    privacy = try {
                        storyJson["privacy_setting"]?.jsonPrimitive?.content?.let {
                            when(it) {
                                "followers" -> StoryPrivacy.ALL_FRIENDS
                                "public" -> StoryPrivacy.PUBLIC
                                else -> null
                            }
                        }
                    } catch (e: Exception) { null },
                    viewCount = storyJson["views_count"]?.jsonPrimitive?.content?.toIntOrNull(),
                    isActive = storyJson["is_active"]?.jsonPrimitive?.content?.toBooleanStrictOrNull(),
                    thumbnailUrl = storyJson["thumbnail_url"]?.jsonPrimitive?.content,
                    mediaWidth = storyJson["media_width"]?.jsonPrimitive?.content?.toIntOrNull(),
                    mediaHeight = storyJson["media_height"]?.jsonPrimitive?.content?.toIntOrNull(),
                    mediaDurationSeconds = storyJson["media_duration_seconds"]?.jsonPrimitive?.content?.toIntOrNull(),
                    fileSizeBytes = storyJson["file_size_bytes"]?.jsonPrimitive?.content?.toLongOrNull(),
                    reactionsCount = storyJson["reactions_count"]?.jsonPrimitive?.content?.toIntOrNull(),
                    repliesCount = storyJson["replies_count"]?.jsonPrimitive?.content?.toIntOrNull(),
                    isReported = storyJson["is_reported"]?.jsonPrimitive?.content?.toBooleanStrictOrNull(),
                    moderationStatus = storyJson["moderation_status"]?.jsonPrimitive?.content,
                    createdAt = storyJson["created_at"]?.jsonPrimitive?.content,
                    expiresAt = storyJson["expires_at"]?.jsonPrimitive?.content
                )
                
                storiesByUser.getOrPut(userId) { mutableListOf() }.add(story)
                
                // Parse user data if not already done
                if (!usersMap.containsKey(userId)) {
                    val userJson = storyJson["users"] as? JsonObject
                    if (userJson != null) {
                        usersMap[userId] = User(
                            id = userJson["id"]?.jsonPrimitive?.content,
                            uid = userJson["uid"]?.jsonPrimitive?.content ?: userId,
                            username = userJson["username"]?.jsonPrimitive?.content,
                            displayName = userJson["display_name"]?.jsonPrimitive?.content,
                            avatar = userJson["avatar"]?.jsonPrimitive?.content,
                            verify = userJson["verify"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                        )
                    }
                }
            }
            
            // Build StoryWithUser list, putting current user first
            val result = mutableListOf<StoryWithUser>()
            
            // Add current user's stories first if they exist
            storiesByUser[currentUserId]?.let { userStories ->
                usersMap[currentUserId]?.let { user ->
                    result.add(
                        StoryWithUser(
                            user = user,
                            stories = userStories.sortedByDescending { it.createdAt },
                            hasUnseenStories = false, // Own stories are always "seen"
                            latestStoryTime = userStories.maxOfOrNull { it.createdAt ?: "" }
                        )
                    )
                }
            }
            
            // Add other users' stories
            for ((userId, userStories) in storiesByUser) {
                if (userId == currentUserId) continue
                usersMap[userId]?.let { user ->
                    result.add(
                        StoryWithUser(
                            user = user,
                            stories = userStories.sortedByDescending { it.createdAt },
                            hasUnseenStories = true, // Will be updated when we check seen status
                            latestStoryTime = userStories.maxOfOrNull { it.createdAt ?: "" }
                        )
                    )
                }
            }
            
            emit(result)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    override suspend fun getUserStories(userId: String): Result<List<Story>> = try {
        val now = Instant.now().toString()
        
        val stories = client.from(TABLE_STORIES)
            .select {
                filter {
                    eq("user_id", userId)
                    gt("expires_at", now)
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Story>()
        
        Result.success(stories)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun createStory(
        userId: String,
        mediaUri: Uri,
        mediaType: StoryMediaType,
        privacy: StoryPrivacy,
        duration: Int
    ): Result<Story> = try {
        // Convert URI to file path
        val filePath = FileManager.getPathFromUri(context, mediaUri)
            ?: throw Exception("Could not convert URI to file path")
        
        // Upload media using MediaStorageService (same as Post Composition)
        val mediaUrl = kotlinx.coroutines.suspendCancellableCoroutine<String?> { continuation ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                mediaStorageService.uploadFile(filePath, null, object : MediaStorageService.UploadCallback {
                    override fun onProgress(percent: Int) {
                        // Progress can be handled by caller if needed
                    }

                    override fun onSuccess(url: String, publicId: String) {
                        android.util.Log.d("StoryRepository", "Uploaded story media: $url")
                        if (continuation.isActive) {
                            continuation.resume(url) {}
                        }
                    }

                    override fun onError(error: String) {
                        android.util.Log.e("StoryRepository", "Upload failed: $error")
                        if (continuation.isActive) {
                            continuation.resume(null) {}
                        }
                    }
                })
            }
        } ?: throw Exception("Media upload failed")
        
        // Calculate expiry (24 hours from now)
        val now = Instant.now()
        val expiresAt = now.plusSeconds(24 * 60 * 60)
        
        // Create story record with explicit fields matching database schema
        val storyData = StoryCreateRequest(
            userId = userId,
            mediaUrl = mediaUrl,
            mediaType = when (mediaType) {
                StoryMediaType.PHOTO -> "photo"
                StoryMediaType.VIDEO -> "video"
            },
            privacySetting = when (privacy) {
                StoryPrivacy.ALL_FRIENDS -> "followers"
                StoryPrivacy.PUBLIC -> "public"
            },
            duration = if (mediaType == StoryMediaType.VIDEO) duration else null,
            durationHours = 24,
            mediaDurationSeconds = if (mediaType == StoryMediaType.VIDEO) duration else null,
            isActive = true,
            reactionsCount = 0,
            repliesCount = 0,
            isReported = false,
            moderationStatus = "pending"
        )
        
        val insertedStory = client.from(TABLE_STORIES)
            .insert(storyData) {
                select()
            }
            .decodeSingle<Story>()
        
        Result.success(insertedStory)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun deleteStory(storyId: String): Result<Unit> = try {
        client.from(TABLE_STORIES)
            .delete {
                filter {
                    eq("id", storyId)
                }
            }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun markAsSeen(storyId: String, viewerId: String): Result<Unit> = try {
        // Check if already viewed
        val existingView = client.from(TABLE_STORY_VIEWS)
            .select {
                filter {
                    eq("story_id", storyId)
                    eq("viewer_id", viewerId)
                }
                count(io.github.jan.supabase.postgrest.query.Count.EXACT)
            }
            .countOrNull() ?: 0
        
        if (existingView == 0L) {
            // Insert new view record
            val view = StoryView(
                storyId = storyId,
                viewerId = viewerId,
                viewedAt = Instant.now().toString()
            )
            
            client.from(TABLE_STORY_VIEWS).insert(view)
            
            // Increment view count on story
            // Note: This could be done with a database trigger instead
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun getStoryViewers(storyId: String): Result<List<StoryViewWithUser>> = try {
        val views = client.from(TABLE_STORY_VIEWS)
            .select(columns = Columns.raw("*, users!viewer_id(*)")) {
                filter {
                    eq("story_id", storyId)
                }
                order("viewed_at", Order.DESCENDING)
            }
            .decodeList<JsonObject>()
        
        val result = views.mapNotNull { viewJson ->
            val storyView = StoryView(
                id = viewJson["id"]?.jsonPrimitive?.content,
                storyId = viewJson["story_id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                viewerId = viewJson["viewer_id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                viewedAt = viewJson["viewed_at"]?.jsonPrimitive?.content
            )
            
            val userJson = viewJson["users"] as? JsonObject
            val viewer = userJson?.let {
                User(
                    id = it["id"]?.jsonPrimitive?.content,
                    uid = it["uid"]?.jsonPrimitive?.content ?: storyView.viewerId,
                    username = it["username"]?.jsonPrimitive?.content,
                    displayName = it["display_name"]?.jsonPrimitive?.content,
                    avatar = it["avatar"]?.jsonPrimitive?.content
                )
            }
            
            StoryViewWithUser(storyView = storyView, viewer = viewer)
        }
        
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun hasSeenStory(storyId: String, viewerId: String): Result<Boolean> = try {
        val count = client.from(TABLE_STORY_VIEWS)
            .select {
                filter {
                    eq("story_id", storyId)
                    eq("viewer_id", viewerId)
                }
                count(io.github.jan.supabase.postgrest.query.Count.EXACT)
            }
            .countOrNull() ?: 0
        
        Result.success(count > 0)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

