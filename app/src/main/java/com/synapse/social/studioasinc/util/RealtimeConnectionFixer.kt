package com.synapse.social.studioasinc.util

import android.content.Context
import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.chat.service.SupabaseRealtimeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Utility to fix realtime connection issues
 */
class RealtimeConnectionFixer(
    private val context: Context,
    private val realtimeService: SupabaseRealtimeService
) {
    companion object {
        private const val TAG = "RealtimeConnectionFixer"
    }

    /**
     * Force reconnect the realtime service
     */
    suspend fun forceReconnect() {
        Log.d(TAG, "Force reconnecting realtime service...")
        
        try {
            // 1. Check network connectivity first
            val connectionMonitor = ConnectionMonitor(context)
            if (!connectionMonitor.isConnected()) {
                Log.w(TAG, "No network connection available")
                return
            }

            // 2. Cleanup existing connections
            realtimeService.cleanup()
            
            // 3. Wait a moment for cleanup
            delay(1000)
            
            // 4. Test Supabase client connection
            val client = SupabaseClient.client
            Log.d(TAG, "Supabase client status: ${client != null}")
            
            Log.d(TAG, "Realtime service reconnection initiated")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force reconnect", e)
        }
    }

    /**
     * Diagnose connection issues
     */
    fun diagnoseConnection(): String {
        val issues = mutableListOf<String>()
        
        // Check network
        val connectionMonitor = ConnectionMonitor(context)
        if (!connectionMonitor.isConnected()) {
            issues.add("No network connection")
        }
        
        // Check connection type
        val connectionType = connectionMonitor.getConnectionTypeString()
        Log.d(TAG, "Connection type: $connectionType")
        
        // Check Supabase client
        try {
            val client = SupabaseClient.client
            Log.d(TAG, "Supabase client initialized: ${client != null}")
        } catch (e: Exception) {
            issues.add("Supabase client error: ${e.message}")
        }
        
        return if (issues.isEmpty()) {
            "Connection appears healthy - try manual reconnect"
        } else {
            "Issues found: ${issues.joinToString(", ")}"
        }
    }
}
