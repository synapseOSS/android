package com.synapse.social.studioasinc.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val lastMessage: String?,
    val timestamp: Long,
    val isGroup: Boolean,
    val lastMessageSender: String?,
    val createdAt: Long,
    val isActive: Boolean
)
