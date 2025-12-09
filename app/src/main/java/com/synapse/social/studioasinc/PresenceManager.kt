package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages user online presence in Supabase, writing to the users table.
 * Handles online, offline (timestamp), and chat statuses.
 */
object PresenceManager {

    private val dbService = SupabaseDatabaseService()

    /**
     * Sets user status to "online".
     * @param uid The Supabase user UID.
     */
    @JvmStatic
    fun goOnline(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updateData = mapOf(
                    "status" to "online",
                    "last_seen" to System.currentTimeMillis().toString()
                )
                dbService.update("users", updateData, "uid", uid)
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    /**
     * Explicitly sets the user's status to offline with timestamp.
     * @param uid The Supabase user UID.
     */
    @JvmStatic
    fun goOffline(uid: String) {
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
     * Sets status to "chatting_with_<otherUserUid>".
     * @param currentUserUid The UID of the current user.
     * @param otherUserUid The UID of the user they are chatting with.
     */
    @JvmStatic
    fun setChattingWith(currentUserUid: String, otherUserUid: String) {
        UserActivity.setActivity(currentUserUid, "chatting_with_$otherUserUid")
    }

    /**
     * Reverts the user's status back to "online".
     * @param currentUserUid The UID of the current user.
     */
    @JvmStatic
    fun stopChatting(currentUserUid: String) {
        UserActivity.clearActivity(currentUserUid)
    }

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        UserActivity.setActivity(uid, activity)
    }
}
