package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.local.database.UserDao
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.UserProfile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class UserRepository(private val userDao: UserDao) {
    
    private val client = SupabaseClient.client
    
    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            var user = userDao.getUserById(userId)?.let { UserMapper.toModel(it) }
            if (user == null) {
                val userProfile = client.from("users")
                    .select() {
                        filter {
                            eq("uid", userId)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()

                userProfile?.let {
                    user = User(
                        uid = it.uid,
                        username = it.username,
                        email = it.email,
                        avatar = it.avatar,
                        verify = it.verify
                    )
                    userDao.insertAll(listOf(UserMapper.toEntity(user!!)))
                }
            }
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to fetch user by ID: $userId", e)
            val errorMessage = when {
                e.message?.contains("relation \"users\" does not exist", ignoreCase = true) == true ->
                    "Database table 'users' does not exist. Please create the users table in your Supabase database."
                e.message?.contains("connection", ignoreCase = true) == true ->
                    "Cannot connect to Supabase. Check your internet connection and Supabase configuration."
                e.message?.contains("unauthorized", ignoreCase = true) == true ->
                    "Unauthorized access to Supabase. Check your API key and RLS policies."
                else -> "Database error: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getUserByUsername(username: String): Result<UserProfile?> {
        return try {
            if (username.isBlank()) {
                return Result.failure(Exception("Username cannot be empty"))
            }
            
            val user = client.from("users")
                .select() {
                    filter {
                        eq("username", username)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
            
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to fetch user by username: $username", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(user: UserProfile): Result<UserProfile> {
        return try {
            if (user.uid.isBlank()) {
                return Result.failure(Exception("User ID cannot be empty"))
            }
            
            val updateData = mapOf(
                "username" to user.username,
                "display_name" to user.displayName,
                "email" to user.email,
                "bio" to user.bio,
                "avatar" to user.avatar,
                "followers_count" to user.followersCount,
                "following_count" to user.followingCount,
                "posts_count" to user.postsCount,
                "status" to user.status,
                "account_type" to user.account_type,
                "verify" to user.verify,
                "banned" to user.banned
            )
            
            client.from("users")
                .update(updateData) {
                    filter {
                        eq("uid", user.uid)
                    }
                }
            
            android.util.Log.d("UserRepository", "User updated successfully: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to update user: ${user.uid}", e)
            Result.failure(Exception("Database update failed for table 'users': ${e.message}", e))
        }
    }
    
    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<UserProfile>> {
        return try {
            if (query.isBlank()) {
                return Result.success(emptyList())
            }
            
            val users = client.from("users")
                .select() {
                    filter {
                        or {
                            ilike("username", "%$query%")
                            ilike("display_name", "%$query%")
                        }
                    }
                    limit(limit.toLong())
                }
                .decodeList<UserProfile>()
            
            android.util.Log.d("UserRepository", "Search found ${users.size} users for query: $query")
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to search users with query: $query", e)
            Result.failure(e)
        }
    }
    
    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val existingUser = client.from("users")
                .select() {
                    filter {
                        eq("username", username)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
            
            Result.success(existingUser == null)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to check username availability: $username", e)
            Result.failure(e)
        }
    }
}
