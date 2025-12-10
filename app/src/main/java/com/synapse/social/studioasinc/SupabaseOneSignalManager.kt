package com.synapse.social.studioasinc

import android.content.Context
import com.onesignal.OneSignal
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages OneSignal integration with Supabase.
 * Handles player ID updates and notification management.
 */
object SupabaseOneSignalManager {

    private val dbService = SupabaseDatabaseService()

    /**
     * Initializes OneSignal with the app ID.
     * @param context Application context
     * @param appId OneSignal app ID
     */
    @JvmStatic
    fun initialize(context: Context, appId: String) {
        OneSignal.initWithContext(context, appId)
    }

    /**
     * Updates the OneSignal player ID for a user in Supabase.
     * @param uid User's UID in Supabase
     */
    @JvmStatic
    fun updatePlayerIdForUser(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get current OneSignal Player ID if available
                val playerId = OneSignal.User.pushSubscription.id
                if (!playerId.isNullOrEmpty()) {
                    val updateData = mapOf("one_signal_player_id" to playerId)
                    dbService.update("users", updateData, "uid", uid)
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    /**
     * Sets external user ID for OneSignal.
     * @param uid User's UID
     */
    @JvmStatic
    fun setExternalUserId(uid: String) {
        OneSignal.login(uid)
    }

    /**
     * Removes external user ID from OneSignal.
     */
    @JvmStatic
    fun removeExternalUserId() {
        OneSignal.logout()
    }

    /**
     * Gets the current OneSignal player ID.
     * @return Player ID string or null if not available
     */
    @JvmStatic
    fun getPlayerId(): String? {
        return try {
            OneSignal.User.pushSubscription.id
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sets notification tags for the user.
     * @param tags Map of tag keys and values
     */
    @JvmStatic
    fun setTags(tags: Map<String, String>) {
        try {
            tags.forEach { (key, value) ->
                OneSignal.User.addTag(key, value)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    /**
     * Removes notification tags.
     * @param tagKeys List of tag keys to remove
     */
    @JvmStatic
    fun removeTags(tagKeys: List<String>) {
        try {
            tagKeys.forEach { key ->
                OneSignal.User.removeTag(key)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}
