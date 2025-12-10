package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for monitoring network connection status
 * Provides real-time updates on connectivity changes
 */
class ConnectionStatusManager(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectionType = MutableStateFlow(ConnectionType.UNKNOWN)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Connection states
     */
    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING
    }

    /**
     * Connection types
     */
    enum class ConnectionType {
        WIFI,
        CELLULAR,
        ETHERNET,
        UNKNOWN
    }

    /**
     * Start monitoring connection status
     */
    fun startMonitoring() {
        // Initial check
        updateConnectionState()

        // Setup network callback for real-time updates
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _connectionState.value = ConnectionState.CONNECTED
                updateConnectionType(network)
            }

            override fun onLost(network: Network) {
                _connectionState.value = ConnectionState.DISCONNECTED
                _connectionType.value = ConnectionType.UNKNOWN
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                updateConnectionType(network)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback?.let {
            connectivityManager.registerNetworkCallback(networkRequest, it)
        }
    }

    /**
     * Stop monitoring connection status
     */
    fun stopMonitoring() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                // Callback may already be unregistered
            }
        }
        networkCallback = null
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }

    /**
     * Get current connection type
     */
    fun getCurrentConnectionType(): ConnectionType {
        return _connectionType.value
    }

    /**
     * Check if connection is metered (cellular data)
     */
    fun isConnectionMetered(): Boolean {
        return connectivityManager.isActiveNetworkMetered
    }

    /**
     * Update connection state
     */
    private fun updateConnectionState() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        _connectionState.value = if (capabilities != null && 
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            ConnectionState.CONNECTED
        } else {
            ConnectionState.DISCONNECTED
        }

        if (network != null) {
            updateConnectionType(network)
        }
    }

    /**
     * Update connection type
     */
    private fun updateConnectionType(network: Network) {
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        _connectionType.value = when {
            capabilities == null -> ConnectionType.UNKNOWN
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }
    }

    companion object {
        @Volatile
        private var instance: ConnectionStatusManager? = null

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): ConnectionStatusManager {
            return instance ?: synchronized(this) {
                instance ?: ConnectionStatusManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
