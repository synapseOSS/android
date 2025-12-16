package com.synapse.social.studioasinc.presentation.editprofile

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.SupabaseStorageService
import com.synapse.social.studioasinc.model.UserProfile
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.HistoryItem
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull
import java.util.UUID

class EditProfileRepository {

    private val client = SupabaseClient.client
    private val storageService = SupabaseStorageService()

    suspend fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    fun getUserProfile(userId: String): Flow<Result<UserProfile>> = flow {
        try {
            val result = client.from("users")
                .select(columns = Columns.raw("*")) {
                    filter { eq("uid", userId) }
                }
                .decodeSingleOrNull<JsonObject>()

            if (result != null) {
                // Manually mapping JsonObject to UserProfile to ensure all fields are handled correctly
                // similar to ProfileViewModel
                val user = UserProfile(
                    uid = result["uid"]?.toString()?.removeSurrounding("\"") ?: userId,
                    username = result["username"]?.toString()?.removeSurrounding("\"") ?: "",
                    displayName = result["display_name"]?.toString()?.removeSurrounding("\"")
                        ?: result["nickname"]?.toString()?.removeSurrounding("\""),
                    email = result["email"]?.toString()?.removeSurrounding("\""),
                    bio = result["bio"]?.toString()?.removeSurrounding("\"")
                        ?: result["bio"]?.toString()?.removeSurrounding("\""),
                    avatar = result["avatar"]?.toString()?.removeSurrounding("\"")
                        ?: result["avatar"]?.toString()?.removeSurrounding("\""),
                    profileCoverImage = result["profile_cover_image"]?.toString()?.removeSurrounding("\""),
                    gender = result["gender"]?.toString()?.removeSurrounding("\""),
                    region = result["region"]?.toString()?.removeSurrounding("\""),
                    status = result["status"]?.toString()?.removeSurrounding("\"") ?: "offline",
                    // Other fields as needed, using UserProfile defaults
                    followersCount = result["followers_count"]?.toString()?.toIntOrNull() ?: 0,
                    followingCount = result["following_count"]?.toString()?.toIntOrNull() ?: 0,
                    postsCount = result["posts_count"]?.toString()?.toIntOrNull() ?: 0
                )
                emit(Result.success(user))
            } else {
                emit(Result.failure(Exception("User not found")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateProfile(userId: String, updateData: Map<String, Any?>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Convert Any? values to serializable types and filter out nulls
                val serializedData = updateData.mapNotNull { (key, value) ->
                    when (value) {
                        is String -> key to value
                        is Int -> key to value
                        is Boolean -> key to value
                        is Long -> key to value
                        is Double -> key to value
                        is Float -> key to value
                        null -> null // Skip null values
                        else -> key to value.toString()
                    }
                }.toMap()
                
                if (serializedData.isNotEmpty()) {
                    client.from("users").update(serializedData) {
                        filter { eq("uid", userId) }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun syncUsernameChange(oldUsername: String, newUsername: String, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("usernames").delete {
                    filter { eq("username", oldUsername) }
                }

                val email = client.auth.currentUserOrNull()?.email

                val usernameData = mapOf(
                    "uid" to userId,
                    "email" to email,
                    "username" to newUsername
                )
                client.from("usernames").upsert(usernameData)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun checkUsernameAvailability(username: String, currentUserId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from("users")
                    .select(columns = Columns.raw("uid")) {
                        filter { eq("username", username) }
                    }
                    .decodeList<JsonObject>()

                // If list is empty, username is available.
                // If list has items, check if it belongs to current user.
                if (result.isEmpty()) {
                    Result.success(true)
                } else {
                    val existingUserId = result.first()["uid"]?.toString()?.removeSurrounding("\"")
                    Result.success(existingUserId == currentUserId)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun uploadAvatar(userId: String, imagePath: String): Result<String> {
        return storageService.uploadAvatar(userId, imagePath)
    }

    suspend fun uploadCover(userId: String, imagePath: String): Result<String> {
        return storageService.uploadCover(userId, imagePath)
    }

    suspend fun addToProfileHistory(userId: String, imageUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                val historyKey = UUID.randomUUID().toString()
                val historyData = mapOf(
                    "key" to historyKey,
                    "user_id" to userId,
                    "image_url" to imageUrl.trim(),
                    "upload_date" to System.currentTimeMillis().toString(),
                    "type" to "url"
                )
                client.from("profile_history").insert(historyData)
            } catch (e: Exception) {
                // Silent fail as per original
            }
        }
    }

    suspend fun addToCoverHistory(userId: String, imageUrl: String) {
         withContext(Dispatchers.IO) {
            try {
                val historyKey = UUID.randomUUID().toString()
                val historyData = mapOf(
                    "key" to historyKey,
                    "user_id" to userId,
                    "image_url" to imageUrl.trim(),
                    "upload_date" to System.currentTimeMillis().toString(),
                    "type" to "url"
                )
                client.from("cover_image_history").insert(historyData)
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    fun getProfileHistory(userId: String): Flow<Result<List<HistoryItem>>> = flow {
        try {
            val result = client.from("profile_history")
                .select(columns = Columns.raw("*")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JsonObject>()

            val items = result.mapNotNull {
                parseHistoryItem(it)
            }.sortedByDescending { it.uploadDate }

            emit(Result.success(items))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getCoverHistory(userId: String): Flow<Result<List<HistoryItem>>> = flow {
        try {
            val result = client.from("cover_image_history")
                .select(columns = Columns.raw("*")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JsonObject>()

            val items = result.mapNotNull {
                parseHistoryItem(it)
            }.sortedByDescending { it.uploadDate }

            emit(Result.success(items))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteProfileHistoryItem(key: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("profile_history").delete {
                    filter { eq("key", key) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteCoverHistoryItem(key: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("cover_image_history").delete {
                    filter { eq("key", key) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseHistoryItem(json: JsonObject): HistoryItem? {
        val key = json["key"]?.jsonPrimitive?.contentOrNull ?: return null
        val userId = json["user_id"]?.jsonPrimitive?.contentOrNull ?: return null
        val imageUrl = json["image_url"]?.jsonPrimitive?.contentOrNull ?: return null
        // Handle upload_date which might be stored as string or number
        val uploadDateStr = json["upload_date"]?.jsonPrimitive?.contentOrNull
        val uploadDate = uploadDateStr?.toLongOrNull() ?: 0L
        val type = json["type"]?.jsonPrimitive?.contentOrNull ?: "url"

        return HistoryItem(key, userId, imageUrl, uploadDate, type)
    }
}
