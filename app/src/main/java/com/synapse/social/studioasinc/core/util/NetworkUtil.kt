package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

/**
 * Utility class for network connectivity detection and monitoring
 */
object NetworkUtil {

    private const val TAG = "NetworkUtil"

    /**
     * Check if network is currently available
     * @param context Application context
     * @return true if network is available, false otherwise
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            ?: return false

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Register a network callback to monitor connectivity changes
     * @param context Application context
     * @param onNetworkAvailable Callback when network becomes available
     * @param onNetworkLost Callback when network is lost
     * @return NetworkCallback that can be unregistered later
     */
    fun registerNetworkCallback(
        context: Context,
        onNetworkAvailable: () -> Unit,
        onNetworkLost: () -> Unit
    ): ConnectivityManager.NetworkCallback {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                onNetworkAvailable()
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                onNetworkLost()
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        return networkCallback
    }

    /**
     * Unregister a network callback
     * @param context Application context
     * @param callback The NetworkCallback to unregister
     */
    fun unregisterNetworkCallback(context: Context, callback: ConnectivityManager.NetworkCallback) {
        try {
            val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            connectivityManager.unregisterNetworkCallback(callback)
            Log.d(TAG, "Network callback unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network callback", e)
        }
    }
}
