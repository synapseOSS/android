package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.util.Log
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationClickEvent

/**
 * Handles notification clicks and routes users to the appropriate screens within the app.
 * This implements deep linking functionality for different types of notifications with Supabase integration.
 */
class NotificationClickHandler : INotificationClickListener {
    
    companion object {
        private const val TAG = "NotificationClickHandler"
    }
    
    override fun onClick(event: INotificationClickEvent) {
        try {
            val context = SynapseApp.getContext()
            val notification = event.notification
            val additionalData = notification.additionalData
            
            Log.d(TAG, "Notification clicked with data: $additionalData")
            
            // Parse notification data
            val notificationType = additionalData?.optString("type") ?: ""
            val senderUid = additionalData?.optString("sender_uid")
            val chatId = additionalData?.optString("chat_id")
            val postId = additionalData?.optString("postId")
            val commentId = additionalData?.optString("commentId")
            
            // Handle different notification types
            when (notificationType) {
                "chat_message" -> handleChatNotification(context, senderUid, chatId)
                NotificationConfig.NOTIFICATION_TYPE_NEW_POST -> handlePostNotification(context, senderUid, postId)
                NotificationConfig.NOTIFICATION_TYPE_NEW_COMMENT -> handleCommentNotification(context, postId, commentId)
                NotificationConfig.NOTIFICATION_TYPE_NEW_REPLY -> handleReplyNotification(context, postId, commentId)
                NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_POST -> handleLikePostNotification(context, postId)
                NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_COMMENT -> handleLikeCommentNotification(context, postId, commentId)
                NotificationConfig.NOTIFICATION_TYPE_MENTION -> handleMentionNotification(context, postId, commentId)
                NotificationConfig.NOTIFICATION_TYPE_NEW_FOLLOWER -> handleFollowNotification(context, senderUid)
                else -> handleDefaultNotification(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification click: ${e.message}")
            handleDefaultNotification(SynapseApp.getContext())
        }
    }
    
    /**
     * Handle chat message notifications - open the specific chat
     */
    private fun handleChatNotification(context: Context, senderUid: String?, chatId: String?) {
        if (senderUid.isNullOrBlank()) {
            Log.w(TAG, "Chat notification missing sender UID")
            handleDefaultNotification(context)
            return
        }
        
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("uid", senderUid) // Use "uid" for consistency with existing code
            putExtra("recipientUid", senderUid)
            if (!chatId.isNullOrBlank()) {
                putExtra("chatId", chatId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Log.d(TAG, "Opening chat with user: $senderUid")
        context.startActivity(intent)
    }
    
    /**
     * Handle new post notifications - open the user's profile or the specific post
     */
    private fun handlePostNotification(context: Context, senderUid: String?, postId: String?) {
        if (senderUid.isNullOrBlank()) {
            Log.w(TAG, "Post notification missing sender UID")
            handleDefaultNotification(context)
            return
        }
        
        val intent = Intent(context, ProfileActivity::class.java).apply {
            putExtra("uid", senderUid)
            if (!postId.isNullOrBlank()) {
                putExtra("postId", postId)
                putExtra("scrollToPost", true)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Log.d(TAG, "Opening profile for user: $senderUid with post: $postId")
        context.startActivity(intent)
    }
    
    /**
     * Handle comment notifications - open the post with comments
     */
    private fun handleCommentNotification(context: Context, postId: String?, commentId: String?) {
        if (postId.isNullOrBlank()) {
            Log.w(TAG, "Comment notification missing post ID")
            handleDefaultNotification(context)
            return
        }
        
        // Open home activity and navigate to the specific post
        val intent = Intent(context, HomeActivity::class.java).apply {
            putExtra("openPost", postId)
            putExtra("showComments", true)
            if (!commentId.isNullOrBlank()) {
                putExtra("highlightComment", commentId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Log.d(TAG, "Opening post: $postId with comment: $commentId")
        context.startActivity(intent)
    }
    
    /**
     * Handle reply notifications - open the post with the specific comment thread
     */
    private fun handleReplyNotification(context: Context, postId: String?, commentId: String?) {
        // Similar to comment notification but with reply context
        if (postId.isNullOrBlank()) {
            Log.w(TAG, "Reply notification missing post ID")
            handleDefaultNotification(context)
            return
        }
        
        val intent = Intent(context, HomeActivity::class.java).apply {
            putExtra("openPost", postId)
            putExtra("showComments", true)
            putExtra("expandReplies", true)
            if (!commentId.isNullOrBlank()) {
                putExtra("highlightComment", commentId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Log.d(TAG, "Opening post: $postId with reply to comment: $commentId")
        context.startActivity(intent)
    }
    
    /**
     * Handle post like notifications - open the specific post
     */
    private fun handleLikePostNotification(context: Context, postId: String?) {
        handleCommentNotification(context, postId, null)
    }
    
    /**
     * Handle comment like notifications - open the post with the specific comment
     */
    private fun handleLikeCommentNotification(context: Context, postId: String?, commentId: String?) {
        handleCommentNotification(context, postId, commentId)
    }
    
    /**
     * Handle mention notifications - open the post where user was mentioned
     */
    private fun handleMentionNotification(context: Context, postId: String?, commentId: String?) {
        handleCommentNotification(context, postId, commentId)
    }
    
    /**
     * Handle follow notifications - open the follower's profile
     */
    private fun handleFollowNotification(context: Context, followerUid: String?) {
        if (followerUid.isNullOrBlank()) {
            Log.w(TAG, "Follow notification missing follower UID")
            handleDefaultNotification(context)
            return
        }
        
        val intent = Intent(context, ProfileActivity::class.java).apply {
            putExtra("uid", followerUid)
            putExtra("showFollowButton", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Log.d(TAG, "Opening follower profile: $followerUid")
        context.startActivity(intent)
    }
    
    /**
     * Default handler - open the home activity
     */
    private fun handleDefaultNotification(context: Context) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Log.d(TAG, "Opening default home activity")
        context.startActivity(intent)
    }
}
