package com.synapse.social.studioasinc.data.remote.services

import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase Follow Service
 * Handles follow/unfollow operations and follower management
 */
@Singleton
class SupabaseFollowService @Inject constructor() {
    
    private val client = SupabaseClient.client
    private val databaseService = SupabaseDatabaseService()
    
    /**
     * Follow a user
     */
    suspend fun followUser(followerId: String, followingId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured"))
                }
                
                // Check if already following
                val existingFollow = client.from("follows")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("follower_id", followerId)
                            eq("following_id", followingId)
                        }
                    }
                    .decodeList<JsonObject>()
                
                if (existingFollow.isNotEmpty()) {
                    // Already following - return success instead of failure
                    return@withContext Result.success(Unit)
                }
                
                // Create follow relationship
                val followData = mapOf(
                    "follower_id" to followerId,
                    "following_id" to followingId
                )
                
                val insertResult = databaseService.insert("follows", followData)
                
                insertResult.fold(
                    onSuccess = {
                        // Update follower counts
                        updateFollowerCounts(followerId, followingId, true)
                        return@withContext Result.success(Unit)
                    },
                    onFailure = { error -> 
                        return@withContext Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to follow user", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Unfollow a user
     */
    suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured"))
                }
                
                // Delete follow relationship
                client.from("follows").delete {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }
                
                // Update follower counts
                updateFollowerCounts(followerId, followingId, false)
                
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to unfollow user", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if user is following another user
     */
    suspend fun isFollowing(followerId: String, followingId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(false)
                }
                
                val result = client.from("follows")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("follower_id", followerId)
                            eq("following_id", followingId)
                        }
                    }
                    .decodeList<JsonObject>()
                
                Result.success(result.isNotEmpty())
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to check follow status", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get user's followers
     */
    suspend fun getFollowers(userId: String, limit: Int = 50): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(emptyList())
                }
                
                // Get follower IDs
                val followsResult = client.from("follows")
                    .select(columns = Columns.raw("follower_id")) {
                        filter { eq("following_id", userId) }
                        limit(limit.toLong())
                    }
                    .decodeList<JsonObject>()
                
                val followerIds = followsResult.map { 
                    it["follower_id"].toString().removeSurrounding("\"") 
                }
                
                if (followerIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }
                
                // Get user details for followers
                val usersResult = client.from("users")
                    .select(columns = Columns.raw("uid, username, display_name, avatar, verify")) {
                        filter { isIn("uid", followerIds) }
                    }
                    .decodeList<JsonObject>()
                
                val followers = usersResult.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                Result.success(followers)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to get followers", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get users that the user is following
     */
    suspend fun getFollowing(userId: String, limit: Int = 50): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(emptyList())
                }
                
                // Get following IDs
                val followsResult = client.from("follows")
                    .select(columns = Columns.raw("following_id")) {
                        filter { eq("follower_id", userId) }
                        limit(limit.toLong())
                    }
                    .decodeList<JsonObject>()
                
                val followingIds = followsResult.map { 
                    it["following_id"].toString().removeSurrounding("\"") 
                }
                
                if (followingIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }
                
                // Get user details for following
                val usersResult = client.from("users")
                    .select(columns = Columns.raw("uid, username, display_name, avatar, verify")) {
                        filter { isIn("uid", followingIds) }
                    }
                    .decodeList<JsonObject>()
                
                val following = usersResult.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                Result.success(following)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to get following", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get follow statistics for a user
     */
    suspend fun getFollowStats(userId: String): Result<Map<String, Int>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(mapOf(
                        "followers_count" to 0,
                        "following_count" to 0
                    ))
                }
                
                // Get user's current counts from users table
                val userResult = databaseService.selectWhere("users", "followers_count, following_count", "uid", userId)
                
                userResult.fold(
                    onSuccess = { users ->
                        val user = users.firstOrNull()
                        if (user != null) {
                            val stats = mapOf(
                                "followers_count" to (user["followers_count"]?.toString()?.toIntOrNull() ?: 0),
                                "following_count" to (user["following_count"]?.toString()?.toIntOrNull() ?: 0)
                            )
                            Result.success(stats)
                        } else {
                            Result.success(mapOf("followers_count" to 0, "following_count" to 0))
                        }
                    },
                    onFailure = { error -> Result.failure(error) }
                )
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to get follow stats", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update follower counts for both users
     */
    private suspend fun updateFollowerCounts(followerId: String, followingId: String, isFollow: Boolean) {
        try {
            // Update follower's following count
            val followerStats = getFollowStats(followerId).getOrNull()
            if (followerStats != null) {
                val newFollowingCount = if (isFollow) {
                    followerStats["following_count"]!! + 1
                } else {
                    maxOf(0, followerStats["following_count"]!! - 1)
                }
                
                databaseService.update(
                    "users",
                    mapOf("following_count" to newFollowingCount),
                    "uid",
                    followerId
                )
            }
            
            // Update following user's followers count
            val followingStats = getFollowStats(followingId).getOrNull()
            if (followingStats != null) {
                val newFollowersCount = if (isFollow) {
                    followingStats["followers_count"]!! + 1
                } else {
                    maxOf(0, followingStats["followers_count"]!! - 1)
                }
                
                databaseService.update(
                    "users",
                    mapOf("followers_count" to newFollowersCount),
                    "uid",
                    followingId
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseFollowService", "Failed to update follower counts", e)
        }
    }
    
    /**
     * Get suggested users to follow (users not currently followed)
     */
    suspend fun getSuggestedUsers(userId: String, limit: Int = 20): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(emptyList())
                }
                
                // Get users that the current user is already following
                val followingResult = client.from("follows")
                    .select(columns = Columns.raw("following_id")) {
                        filter { eq("follower_id", userId) }
                    }
                    .decodeList<JsonObject>()
                
                val followingIds = followingResult.map { 
                    it["following_id"].toString().removeSurrounding("\"") 
                }.toMutableList()
                
                // Add current user to exclude list
                followingIds.add(userId)
                
                // Get suggested users (excluding already followed and self)
                // Note: We'll get all users and filter client-side since Supabase doesn't support NOT IN easily
                val allUsersResult = client.from("users")
                    .select(columns = Columns.raw("uid, username, display_name, avatar, verify, followers_count")) {
                        limit(100) // Get more users to filter from
                    }
                    .decodeList<JsonObject>()
                
                // Filter out already followed users and self
                val suggestedResult = allUsersResult.filter { jsonObject ->
                    val uid = jsonObject["uid"].toString().removeSurrounding("\"")
                    !followingIds.contains(uid)
                }.take(limit)
                
                val suggested = suggestedResult.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                Result.success(suggested)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to get suggested users", e)
                Result.failure(e)
            }
        }
    }
}
