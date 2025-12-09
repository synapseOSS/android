package com.synapse.social.studioasinc.models

import kotlinx.serialization.Serializable

/**
 * Data models for Supabase database tables
 */

// User model moved to separate file for compatibility

data class Chat(
    val id: String? = null,
    val chat_id: String,
    val participant_1: String,
    val participant_2: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class Message(
    val id: String? = null,
    val message_key: String,
    val chat_id: String,
    val sender_id: String,
    val message_text: String? = null,
    val message_type: String = "text",
    val attachment_url: String? = null,
    val attachment_name: String? = null,
    val voice_duration: Int? = null,
    val reply_to_message_id: String? = null,
    val push_date: String? = null,
    val edited_at: String? = null,
    val deleted_at: String? = null,
    val created_at: String? = null
)

data class Story(
    val id: String? = null,
    val uid: String,
    val story_url: String,
    val story_type: String = "image",
    val created_at: String? = null,
    val expires_at: String? = null
)

// Post model moved to separate file for compatibility
