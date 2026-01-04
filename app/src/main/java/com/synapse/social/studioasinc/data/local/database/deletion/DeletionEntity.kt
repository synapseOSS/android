package com.synapse.social.studioasinc.data.local.database.deletion

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Entity for tracking deletion operations
 * Requirements: 4.2, 5.4
 */
@Entity(tableName = "deletion_operations")
data class DeletionOperationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val deletionType: String, // COMPLETE_HISTORY or SELECTIVE_CHATS
    val chatIds: String?, // JSON array for selective deletions
    val status: String, // OperationStatus enum value
    val storageType: String, // StorageType enum value
    val messagesAffected: Int = 0,
    val createdAt: Long,
    val completedAt: Long? = null,
    val retryCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * Entity for deletion retry queue
 * Requirements: 4.2, 4.5
 */
@Entity(tableName = "deletion_retry_queue")
data class DeletionRetryQueueEntity(
    @PrimaryKey
    val id: String,
    val operationId: String,
    val scheduledRetryTime: Long,
    val maxRetries: Int = 3,
    val currentRetry: Int = 0
)

/**
 * Type converter for chat IDs list
 */
class ChatIdsConverter {
    @TypeConverter
    fun fromChatIds(chatIds: List<String>?): String? {
        return chatIds?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toChatIds(chatIdsJson: String?): List<String>? {
        return chatIdsJson?.let {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(it, type)
        }
    }
}