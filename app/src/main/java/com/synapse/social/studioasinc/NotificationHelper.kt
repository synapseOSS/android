package com.synapse.social.studioasinc

import android.util.Log
import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import com.synapse.social.studioasinc.core.config.NotificationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Enhanced notification system supporting both server-side and client-side OneSignal notifications.
 * 
 * Features:
 * - Toggle between server-side (Cloudflare Workers) and client-side (OneSignal REST API) notification sending
 * - Smart notification suppression when both users are actively chatting
 * - Fallback mechanisms for reliability
 * - Configurable notification settings
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private const val ONESIGNAL_API_URL = "https://api.onesignal.com/notifications"

    private val dbService = SupabaseDatabaseService()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Sends a notification to a user.
     *
     * @param recipientUid The UID of the user to send the notification to.
     * @param senderUid The UID of the user sending the notification.
     * @param message The message to send in the notification.
     * @param notificationType The type of notification to send.
     * @param data A map of additional data to send with the notification.
     */
    @JvmStatic
    fun sendNotification(
        recipientUid: String,
        senderUid: String,
        message: String,
        notificationType: String,
        data: Map<String, String>? = null
    ) {
        if (recipientUid.isNullOrEmpty() || senderUid.isNullOrEmpty()) {
            Log.w(TAG, "Recipient or sender UID is null or empty.")
            return
        }
        if (recipientUid == senderUid) {
            // Don't send notification to self
            return
        }

        scope.launch {
            // Persist notification to Supabase DB for Activity Feed
            // Note: We skip persisting chat messages as they are part of the chat history
            if (notificationType != "chat_message") {
                persistNotification(recipientUid, senderUid, message, notificationType, data)
            }

            try {
                // Fetch recipient data from Supabase
                val userResult = dbService.getSingle("users", "uid", recipientUid)
                val userData = userResult.getOrNull()

                if (userData == null) {
                    Log.e(TAG, "User not found for notification: $recipientUid")
                    return@launch
                }

                val playerId = userData["one_signal_player_id"] as? String
                val status = userData["status"] as? String
                val lastSeenStr = userData["last_seen"] as? String

                // Check presence to suppress notification
                if (shouldSuppressNotification(status, lastSeenStr, senderUid)) {
                    Log.i(TAG, "Notification suppressed: User $recipientUid is active/chatting.")
                    return@launch
                }

                Log.i(TAG, "Sending notification to user $recipientUid via ${NotificationConfig.getNotificationSystemDescription()}")

                if (NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
                    if (!playerId.isNullOrEmpty()) {
                        sendClientSideNotification(
                            playerId,
                            message,
                            senderUid,
                            notificationType,
                            data,
                            recipientUid
                        )
                    } else {
                        Log.w(TAG, "Cannot send client-side notification: No Player ID for user $recipientUid")
                        // Try fallback if enabled, but passing null playerId might limit server fallback
                        if (NotificationConfig.ENABLE_FALLBACK_MECHANISMS) {
                             sendServerSideNotification(recipientUid, message, notificationType, data, playerId)
                        }
                    }
                } else {
                    // Send via server-side worker
                    sendServerSideNotification(recipientUid, message, notificationType, data, playerId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification for $recipientUid", e)
            }
        }
    }

    /**
     * Determines if a notification should be suppressed based on user status and last seen time.
     */
    private fun shouldSuppressNotification(status: String?, lastSeenStr: String?, senderUid: String): Boolean {
        if (status == null) return false

        // 1. Hard suppression: User is currently chatting with the sender
        if (status == "chatting_with_$senderUid") {
            return true
        }

        // 2. Smart suppression: User is online/active recently
        if (NotificationConfig.ENABLE_SMART_SUPPRESSION) {
            val isActive = status == "online" || status.startsWith("chatting_with_")

            if (isActive && lastSeenStr != null) {
                try {
                    // Parse ISO 8601 timestamp (assuming standard format from Supabase)
                    val lastSeenTime = try {
                         java.time.Instant.parse(lastSeenStr).toEpochMilli()
                    } catch (e: Exception) {
                         // Fallback for potential legacy millis string
                         lastSeenStr.toLongOrNull() ?: 0L
                    }

                    val currentTime = System.currentTimeMillis()
                    // If active within threshold, suppress
                    if (currentTime - lastSeenTime < NotificationConfig.RECENT_ACTIVITY_THRESHOLD) {
                        return true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse last_seen timestamp: $lastSeenStr", e)
                }
            }
        }

        return false
    }

    /**
     * Enhanced notification sending with smart presence checking and dual system support.
     *
     * @param senderUid The UID of the message sender
     * @param recipientUid The UID of the message recipient
     * @param recipientOneSignalPlayerId The OneSignal Player ID of the recipient
     * @param message The message content to send in the notification
     * @param chatId Optional chat ID for deep linking (can be null)
     * @deprecated Use sendNotification instead.
     */
    @JvmStatic
    @Deprecated("Use sendNotification instead")
    fun sendMessageAndNotifyIfNeeded(
        senderUid: String,
        recipientUid: String,
        recipientOneSignalPlayerId: String,
        message: String,
        chatId: String? = null
    ) {
        sendNotification(
            recipientUid,
            senderUid,
            message,
            "chat_message",
            if (chatId != null) mapOf("chat_id" to chatId) else null
        )
    }

    /**
     * Sends notification via the existing Cloudflare Worker (server-side).
     * @param recipientUid The UID of the recipient (expected by server as recipientUserId).
     * @param recipientPlayerId Optional Player ID for fallback purposes.
     */
    @JvmStatic
    fun sendServerSideNotification(
        recipientUid: String,
        message: String,
        notificationType: String,
        data: Map<String, String>? = null,
        recipientPlayerId: String? = null
    ) {
        if (notificationType != "chat_message") {
            Log.w(TAG, "Server-side notification for type $notificationType is not yet implemented. Sending a generic message.")
        }

        val client = OkHttpClient()
        val jsonBody = JSONObject()
        try {
            // Server expects 'recipientUserId'
            jsonBody.put("recipientUserId", recipientUid)
            jsonBody.put("notificationMessage", message)
            // Add other fields if server supports them, e.g. type, data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create JSON for server-side notification", e)
            return
        }

        val body = jsonBody.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url(NotificationConfig.WORKER_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to send server-side notification", e)
                if (NotificationConfig.ENABLE_FALLBACK_MECHANISMS && !NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
                    if (!recipientPlayerId.isNullOrEmpty()) {
                        Log.i(TAG, "Falling back to client-side notification due to server failure")
                        sendClientSideNotification(recipientPlayerId, message, null, notificationType, data, recipientUid)
                    } else {
                        Log.w(TAG, "Cannot fallback to client-side: Missing Player ID")
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        Log.i(TAG, "Server-side notification sent successfully.")
                    } else {
                        Log.e(TAG, "Failed to send server-side notification: ${it.code}")
                        if (NotificationConfig.ENABLE_FALLBACK_MECHANISMS && !NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
                            if (!recipientPlayerId.isNullOrEmpty()) {
                                Log.i(TAG, "Falling back to client-side notification due to server error")
                                sendClientSideNotification(recipientPlayerId, message, null, notificationType, data, recipientUid)
                            } else {
                                Log.w(TAG, "Cannot fallback to client-side: Missing Player ID")
                            }
                        }
                    }
                }
            }
        })
    }

    /**
     * Sends notification directly via OneSignal REST API (client-side).
     * @param recipientPlayerId The OneSignal Player ID of the recipient.
     * @param recipientUid Optional Recipient UID for fallback purposes.
     */
    @JvmStatic
    fun sendClientSideNotification(
        recipientPlayerId: String,
        message: String,
        senderUid: String? = null,
        notificationType: String,
        data: Map<String, String>? = null,
        recipientUid: String? = null
    ) {
        val client = OkHttpClient()
        val jsonBody = JSONObject()
        
        try {
            jsonBody.put("app_id", NotificationConfig.ONESIGNAL_APP_ID)
            jsonBody.put("include_subscription_ids", arrayOf(recipientPlayerId))
            jsonBody.put("contents", JSONObject().put("en", message))
            jsonBody.put("headings", JSONObject().put("en", NotificationConfig.getTitleForNotificationType(notificationType)))
            jsonBody.put("subtitle", JSONObject().put("en", NotificationConfig.NOTIFICATION_SUBTITLE))
            
            if (NotificationConfig.ENABLE_DEEP_LINKING) {
                val dataJson = JSONObject()
                if (senderUid != null) {
                    dataJson.put("sender_uid", senderUid)
                }
                dataJson.put("type", notificationType)
                data?.forEach { (key, value) ->
                    dataJson.put(key, value)
                }
                
                // Add deep link URL based on notification type
                val deepLinkUrl = generateDeepLinkUrl(notificationType, senderUid, data)
                if (deepLinkUrl.isNotEmpty()) {
                    jsonBody.put("url", deepLinkUrl)
                    dataJson.put("deep_link", deepLinkUrl)
                }
                
                jsonBody.put("data", dataJson)
            }
            
            jsonBody.put("priority", NotificationConfig.NOTIFICATION_PRIORITY)
            jsonBody.put("android_channel_id", NotificationConfig.NOTIFICATION_CHANNEL_ID)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create JSON for client-side notification", e)
            return
        }

        val body = jsonBody.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url(ONESIGNAL_API_URL)
            .addHeader("Authorization", "Key ${NotificationConfig.ONESIGNAL_REST_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to send client-side notification", e)
                if (NotificationConfig.ENABLE_FALLBACK_MECHANISMS && NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
                    if (!recipientUid.isNullOrEmpty()) {
                        Log.i(TAG, "Falling back to server-side notification due to client-side failure")
                        sendServerSideNotification(recipientUid, message, notificationType, data, recipientPlayerId)
                    } else {
                        Log.w(TAG, "Cannot fallback to server-side: Missing Recipient UID")
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        Log.i(TAG, "Client-side notification sent successfully.")
                    } else {
                        Log.e(TAG, "Failed to send client-side notification: ${it.code} - ${it.body?.string()}")
                        if (NotificationConfig.ENABLE_FALLBACK_MECHANISMS && NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
                            if (!recipientUid.isNullOrEmpty()) {
                                Log.i(TAG, "Falling back to server-side notification due to client-side error")
                                sendServerSideNotification(recipientUid, message, notificationType, data, recipientPlayerId)
                            } else {
                                Log.w(TAG, "Cannot fallback to server-side: Missing Recipient UID")
                            }
                        }
                    }
                }
            }
        })
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use sendMessageAndNotifyIfNeeded with chatId parameter instead
     */
    @JvmStatic
    @Deprecated("Use sendMessageAndNotifyIfNeeded with chatId parameter for better deep linking")
    fun triggerPushNotification(recipientId: String, message: String) {
        sendMessageAndNotifyIfNeeded("", "", recipientId, message)
    }

    /**
     * Gets the current notification system being used.
     * @return true if using client-side notifications, false if using server-side
     */
    @JvmStatic
    fun isUsingClientSideNotifications(): Boolean {
        return NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS
    }

    /**
     * Checks if the notification system is properly configured.
     * @return true if configuration is valid, false otherwise
     */
    @JvmStatic
    fun isNotificationSystemConfigured(): Boolean {
        return NotificationConfig.isConfigurationValid()
    }

    /**
     * Persists the notification to the Supabase "notifications" table.
     * This ensures the notification appears in the user's Activity Feed/Notification Screen.
     */
    private suspend fun persistNotification(
        recipientUid: String,
        senderUid: String,
        message: String,
        notificationType: String,
        data: Map<String, String>?
    ) {
        try {
            // Extract target ID based on notification type
            val targetId = data?.get("postId")
                ?: data?.get("commentId")
                ?: data?.get("followerId")
                ?: data?.get("chat_id")

            // Map to database schema
            // Note: We use 'sender_id' assuming the column exists. If it fails, check schema.
            val notificationData = mutableMapOf<String, Any?>(
                "receiver_id" to recipientUid,
                "sender_id" to senderUid,
                "type" to notificationType,
                "content" to message,
                "is_read" to false,
                "created_at" to java.time.Instant.now().toString()
            )

            if (targetId != null) {
                notificationData["target_id"] = targetId
            }

            val result = dbService.insert("notifications", notificationData)

            if (result.isFailure) {
                 Log.w(TAG, "Failed to persist notification: ${result.exceptionOrNull()?.message}")
            } else {
                 Log.d(TAG, "Notification persisted to Supabase DB")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting notification", e)
        }
    }

    /**
     * Generates a deep link URL based on notification type and data.
     * @param notificationType The type of notification
     * @param senderUid The sender's UID (optional)
     * @param data Additional notification data (optional)
     * @return Deep link URL string
     */
    @JvmStatic
    private fun generateDeepLinkUrl(
        notificationType: String, 
        senderUid: String?, 
        data: Map<String, String>?
    ): String {
        return when (notificationType) {
            "chat_message" -> {
                if (!senderUid.isNullOrBlank()) {
                    val chatId = data?.get("chat_id")
                    if (!chatId.isNullOrBlank()) {
                        "synapse://chat?uid=$senderUid&chatId=$chatId"
                    } else {
                        "synapse://chat?uid=$senderUid"
                    }
                } else ""
            }
            NotificationConfig.NOTIFICATION_TYPE_NEW_POST,
            NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_POST -> {
                if (!senderUid.isNullOrBlank()) {
                    val postId = data?.get("postId")
                    if (!postId.isNullOrBlank()) {
                        "synapse://profile?uid=$senderUid&postId=$postId"
                    } else {
                        "synapse://profile?uid=$senderUid"
                    }
                } else ""
            }
            NotificationConfig.NOTIFICATION_TYPE_NEW_COMMENT,
            NotificationConfig.NOTIFICATION_TYPE_NEW_REPLY,
            NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_COMMENT -> {
                val postId = data?.get("postId")
                val commentId = data?.get("commentId")
                if (!postId.isNullOrBlank()) {
                    if (!commentId.isNullOrBlank()) {
                        "synapse://home?postId=$postId&commentId=$commentId"
                    } else {
                        "synapse://home?postId=$postId"
                    }
                } else ""
            }
            else -> "synapse://home"
        }
    }
}
