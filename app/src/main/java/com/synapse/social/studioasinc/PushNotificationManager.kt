package com.synapse.social.studioasinc

/**
 * Supabase-based PushNotificationManager that replaces Firebase implementation.
 */
object PushNotificationManager {

    /**
     * Saves or updates the user's OneSignal Player ID in the Supabase database.
     *
     * @param userUid The Supabase user UID.
     * @param playerId The OneSignal Player ID to save.
     */
    @JvmStatic
    fun savePlayerIdToRealtimeDatabase(userUid: String, playerId: String) {
        SupabaseOneSignalManager.updatePlayerIdForUser(userUid)
    }

    @JvmStatic
    fun savePlayerIdToSupabase(userUid: String, playerId: String) {
        SupabaseOneSignalManager.updatePlayerIdForUser(userUid)
    }
}
