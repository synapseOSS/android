package com.synapse.social.studioasinc.util

import android.content.Context
import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Diagnostic utilities for connection issues
 */
object ConnectionDiagnostics {
    private const val TAG = "ConnectionDiagnostics"

    /**
     * Run comprehensive connection diagnostics
     */
    fun runDiagnostics(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "=== CONNECTION DIAGNOSTICS ===")
            
            // 1. Network connectivity
            val connectionMonitor = ConnectionMonitor(context)
            Log.d(TAG, "Network connected: ${connectionMonitor.isConnected()}")
            Log.d(TAG, "Connection type: ${connectionMonitor.getConnectionTypeString()}")
            Log.d(TAG, "Good for media: ${connectionMonitor.isGoodForMedia()}")
            
            // 2. Supabase client status
            try {
                val client = SupabaseClient.client
                Log.d(TAG, "Supabase client initialized: ${client != null}")
                // Note: Realtime module access removed to fix compilation
            } catch (e: Exception) {
                Log.e(TAG, "Supabase client error: ${e.message}")
            }
            
            // 3. Connection status manager
            val statusManager = ConnectionStatusManager.getInstance(context)
            Log.d(TAG, "Connection status: ${statusManager.connectionState.value}")
            Log.d(TAG, "Connection type: ${statusManager.connectionType.value}")
            Log.d(TAG, "Is metered: ${statusManager.isConnectionMetered()}")
            
            Log.d(TAG, "=== END DIAGNOSTICS ===")
        }
    }

    /**
     * Log current connection state for debugging
     */
    fun logConnectionState(context: Context, tag: String = TAG) {
        val connectionMonitor = ConnectionMonitor(context)
        Log.d(tag, "Connection State - Connected: ${connectionMonitor.isConnected()}, Type: ${connectionMonitor.getConnectionTypeString()}")
    }
}
