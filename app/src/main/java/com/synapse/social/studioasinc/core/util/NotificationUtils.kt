package com.synapse.social.studioasinc.core.util

import android.content.Context
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.core.config.NotificationConfig
import com.synapse.social.studioasinc.NotificationHelper
import com.synapse.social.studioasinc.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility class for sending various types of notifications using Supabase backend.
 * Handles post likes, comments, mentions, and other social interactions.
 */
object NotificationUtils {

    /**
     * Send notification when someone likes a post
     */
    fun sendPostLikeNotification(context: Context, postKey: String, postAuthorUid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(AppDatabase.getDatabase(context.applicationContext).userDao())
                
                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == postAuthorUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: "Someone"
                val message = "$senderName liked your post"

                val data = hashMapOf<String, String>().apply {
                    put("postId", postKey)
                }

                NotificationHelper.sendNotification(
                    postAuthorUid,
                    currentUser.id,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_POST,
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send post like notification: ${e.message}")
            }
        }
    }

    /**
     * Send notification when someone comments on a post
     */
    fun sendPostCommentNotification(context: Context, postKey: String, postAuthorUid: String, commentText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(AppDatabase.getDatabase(context.applicationContext).userDao())
                
                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == postAuthorUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: "Someone"
                val message = "$senderName commented on your post"

                val data = hashMapOf<String, String>().apply {
                    put("postId", postKey)
                    put("commentText", commentText.take(100)) // Limit comment text
                }

                NotificationHelper.sendNotification(
                    postAuthorUid,
                    currentUser.id,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_NEW_COMMENT,
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send comment notification: ${e.message}")
            }
        }
    }

    /**
     * Send notification when someone likes a comment
     */
    fun sendCommentLikeNotification(context: Context, postKey: String, commentKey: String, commentAuthorUid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(AppDatabase.getDatabase(context.applicationContext).userDao())
                
                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == commentAuthorUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: "Someone"
                val message = "$senderName liked your comment"

                val data = hashMapOf<String, String>().apply {
                    put("postId", postKey)
                    put("commentId", commentKey)
                }

                NotificationHelper.sendNotification(
                    commentAuthorUid,
                    currentUser.id,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_COMMENT,
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send comment like notification: ${e.message}")
            }
        }
    }

    /**
     * Send notification when someone follows a user
     */
    fun sendFollowNotification(context: Context, followedUid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(AppDatabase.getDatabase(context.applicationContext).userDao())
                
                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == followedUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: "Someone"
                val message = "$senderName started following you"

                val data = hashMapOf<String, String>().apply {
                    put("followerId", currentUser.id)
                }

                NotificationHelper.sendNotification(
                    followedUid,
                    currentUser.id,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_NEW_FOLLOWER,
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send follow notification: ${e.message}")
            }
        }
    }

    /**
     * Send notification for chat messages
     */
    fun sendChatMessageNotification(context: Context, recipientUid: String, messageText: String, chatId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(AppDatabase.getDatabase(context.applicationContext).userDao())
                
                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == recipientUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: "Someone"
                
                // Truncate message for notification
                val truncatedMessage = if (messageText.length > 50) {
                    messageText.take(47) + "..."
                } else {
                    messageText
                }

                val data = hashMapOf<String, String>().apply {
                    put("type", "chat_message")
                    put("sender_uid", currentUser.id)
                    put("chat_id", chatId)
                    put("message_preview", truncatedMessage)
                }

                NotificationHelper.sendNotification(
                    recipientUid,
                    currentUser.id,
                    "$senderName: $truncatedMessage",
                    "chat_message",
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send chat message notification: ${e.message}")
            }
        }
    }

    /**
     * Send mention notification (used by MentionUtils)
     */
    fun sendMentionNotification(context: Context, mentionedUid: String, postKey: String, commentKey: String?, contentType: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(AppDatabase.getDatabase(context.applicationContext).userDao())
                
                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == mentionedUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: "Someone"
                val message = "$senderName mentioned you in a $contentType"

                val data = hashMapOf<String, String>().apply {
                    put("postId", postKey)
                    commentKey?.let { put("commentId", it) }
                }

                NotificationHelper.sendNotification(
                    mentionedUid,
                    currentUser.id,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_MENTION,
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send mention notification: ${e.message}")
            }
        }
    }
}
