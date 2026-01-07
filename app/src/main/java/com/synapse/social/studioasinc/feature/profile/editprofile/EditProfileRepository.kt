package com.synapse.social.studioasinc.presentation.editprofile

import android.content.Context
import com.synapse.social.studioasinc.core.storage.MediaStorageService
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.local.database.AppSettingsManager
import com.synapse.social.studioasinc.domain.model.UserProfile
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.HistoryItem
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.UUID
import kotlin.coroutines.resume

class EditProfileRepository(private val context: Context) {

    private val client = SupabaseClient.client
    private val appSettingsManager = AppSettingsManager.getInstance(context)
    private val mediaStorageService = MediaStorageService(context, appSettingsManager)

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

    suspend fun updateProfile(userId: String, updateData: Map<String, String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("users").update(updateData) {
                    filter { eq("uid", userId) }
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
        return uploadFile(imagePath, SupabaseClient.BUCKET_USER_AVATARS)
    }

    suspend fun uploadCover(userId: String, imagePath: String): Result<String> {
        return uploadFile(imagePath, SupabaseClient.BUCKET_USER_COVERS)
    }

    private suspend fun uploadFile(filePath: String, bucketName: String): Result<String> = suspendCancellableCoroutine { continuation ->
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            mediaStorageService.uploadFile(filePath, bucketName, object : MediaStorageService.UploadCallback {
                override fun onProgress(percent: Int) {
                    // Progress callback
                }

                override fun onSuccess(url: String, publicId: String) {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(url))
                    }
                }

                override fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception(error)))
                    }
                }
            })
        }
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
                // Silent fail
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
        val uploadDateStr = json["upload_date"]?.jsonPrimitive?.contentOrNull
        val uploadDate = uploadDateStr?.toLongOrNull() ?: 0L
        val type = json["type"]?.jsonPrimitive?.contentOrNull ?: "url"

        return HistoryItem(key, userId, imageUrl, uploadDate, type)
    }
}
