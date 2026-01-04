package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages user activity status in Supabase.
 * Handles setting and clearing user activities like "chatting_with_<uid>", "online", etc.
 */
object UserActivity {

    private val dbService = SupabaseDatabaseService()

    /**
     * Sets a user's activity status.
     * @param uid The user's UID
     * @param activity The activity string (e.g., "chatting_with_123", "online", "typing")
     */
    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updateData = mapOf(
                    "status" to activity,
                    "last_seen" to System.currentTimeMillis().toString()
                )
                dbService.update("users", updateData, "uid", uid)
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    /**
     * Clears a user's activity status (sets to "online").
     * @param uid The user's UID
     */
    @JvmStatic
    fun clearActivity(uid: String) {
        setActivity(uid, "online")
    }

    /**
     * Sets a user's status to offline with timestamp.
     * @param uid The user's UID
     */
    @JvmStatic
    fun setOffline(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updateData = mapOf(
                    "status" to "offline",
                    "last_seen" to System.currentTimeMillis().toString()
                )
                dbService.update("users", updateData, "uid", uid)
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    /**
     * Gets a user's current activity status.
     * @param uid The user's UID
     * @return The user's current status or null if not found
     */
    suspend fun getActivity(uid: String): String? {
        return try {
            val result = dbService.getSingle("users", "uid", uid).getOrNull()
            result?.get("status") as? String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a user is currently online.
     * @param uid The user's UID
     * @return true if user is online, false otherwise
     */
    suspend fun isOnline(uid: String): Boolean {
        val status = getActivity(uid)
        return status == "online" || status?.startsWith("chatting_with_") == true
    }

    /**
     * Checks if a user is currently chatting with another specific user.
     * @param uid The user's UID
     * @param otherUid The other user's UID
     * @return true if user is chatting with the other user, false otherwise
     */
    suspend fun isChattingWith(uid: String, otherUid: String): Boolean {
        val status = getActivity(uid)
        return status == "chatting_with_$otherUid"
    }
}
