package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles pushing user data to Supabase database.
 * Manages user profile updates, status changes, and other user-related data operations.
 */
object SupabaseUserDataPusher {

    private val dbService = SupabaseDatabaseService()

    /**
     * Updates user profile data in Supabase.
     * @param uid User's UID
     * @param userData Map of user data to update
     */
    @JvmStatic
    fun updateUserProfile(uid: String, userData: Map<String, Any?>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dbService.update("users", userData, "uid", uid)
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    /**
     * Creates a new user profile in Supabase.
     * @param uid User's UID
     * @param userData Map of user data to insert
     */
    @JvmStatic
    fun createUserProfile(uid: String, userData: Map<String, Any?>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataWithUid = userData.toMutableMap()
                dataWithUid["id"] = uid
                dataWithUid["uid"] = uid
                dbService.insert("users", dataWithUid)
                
                // Create related records if they don't exist
                try {
                    dbService.insert("user_settings", mapOf("user_id" to uid))
                } catch (e: Exception) {
                    // Ignore if already exists
                }
                
                try {
                    dbService.insert("user_presence", mapOf("user_id" to uid))
                } catch (e: Exception) {
                    // Ignore if already exists
                }
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    /**
     * Updates user's online status.
     * @param uid User's UID
     * @param isOnline Whether user is online
     */
    @JvmStatic
    fun updateOnlineStatus(uid: String, isOnline: Boolean) {
        val status = if (isOnline) "online" else "offline"
        val userData = mapOf(
            "status" to status,
            "last_seen" to System.currentTimeMillis().toString()
        )
        updateUserProfile(uid, userData)
    }

    /**
     * Updates user's OneSignal player ID.
     * @param uid User's UID
     * @param playerId OneSignal player ID
     */
    @JvmStatic
    fun updateOneSignalPlayerId(uid: String, playerId: String) {
        val userData = mapOf("one_signal_player_id" to playerId)
        updateUserProfile(uid, userData)
    }

    /**
     * Updates user's device token for push notifications.
     * @param uid User's UID
     * @param deviceToken Device token
     */
    @JvmStatic
    fun updateDeviceToken(uid: String, deviceToken: String) {
        val userData = mapOf("device_token" to deviceToken)
        updateUserProfile(uid, userData)
    }

    /**
     * Updates user's last seen timestamp.
     * @param uid User's UID
     */
    @JvmStatic
    fun updateLastSeen(uid: String) {
        val userData = mapOf("last_seen" to System.currentTimeMillis().toString())
        updateUserProfile(uid, userData)
    }

    /**
     * Updates user's typing status in a chat.
     * @param uid User's UID
     * @param chatId Chat ID where user is typing
     * @param isTyping Whether user is typing
     */
    @JvmStatic
    fun updateTypingStatus(uid: String, chatId: String, isTyping: Boolean) {
        val status = if (isTyping) "typing_in_$chatId" else "online"
        val userData = mapOf("status" to status)
        updateUserProfile(uid, userData)
    }

    /**
     * Increments user's post count.
     * @param uid User's UID
     */
    @JvmStatic
    fun incrementPostCount(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use direct update to increment post count
                val currentUser = dbService.getSingle("users", "uid", uid).getOrNull()
                val currentCount = (currentUser?.get("posts_count") as? String)?.toIntOrNull() ?: 0
                dbService.update("users", mapOf("posts_count" to (currentCount + 1).toString()), "uid", uid)
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    /**
     * Increments user's follower count.
     * @param uid User's UID
     */
    @JvmStatic
    fun incrementFollowerCount(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use direct update to increment follower count
                val currentUser = dbService.getSingle("users", "uid", uid).getOrNull()
                val currentCount = (currentUser?.get("followers_count") as? String)?.toIntOrNull() ?: 0
                dbService.update("users", mapOf("followers_count" to (currentCount + 1).toString()), "uid", uid)
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    /**
     * Decrements user's follower count.
     * @param uid User's UID
     */
    @JvmStatic
    fun decrementFollowerCount(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use direct update to decrement follower count
                val currentUser = dbService.getSingle("users", "uid", uid).getOrNull()
                val currentCount = (currentUser?.get("followers_count") as? String)?.toIntOrNull() ?: 0
                dbService.update("users", mapOf("followers_count" to maxOf(0, currentCount - 1).toString()), "uid", uid)
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }
}
