package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Network connection monitor with real-time status updates
 * Provides connection state and quality information
 */
class ConnectionMonitor(context: Context) {

    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        POOR_CONNECTION
    }

    data class ConnectionInfo(
        val state: ConnectionState,
        val type: ConnectionType,
        val quality: ConnectionQuality
    )

    enum class ConnectionType {
        WIFI,
        CELLULAR,
        ETHERNET,
        NONE
    }

    enum class ConnectionQuality {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        NONE
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _connectionInfo = MutableStateFlow(
        ConnectionInfo(ConnectionState.DISCONNECTED, ConnectionType.NONE, ConnectionQuality.NONE)
    )
    val connectionInfo: StateFlow<ConnectionInfo> = _connectionInfo.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateConnectionState(ConnectionState.CONNECTED)
            updateConnectionInfo()
        }

        override fun onLost(network: Network) {
            updateConnectionState(ConnectionState.DISCONNECTED)
            updateConnectionInfo()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateConnectionInfo()
        }
    }

    init {
        registerNetworkCallback()
        updateConnectionInfo()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    private fun updateConnectionInfo() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        if (capabilities == null) {
            _connectionInfo.value = ConnectionInfo(
                ConnectionState.DISCONNECTED,
                ConnectionType.NONE,
                ConnectionQuality.NONE
            )
            return
        }

        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.NONE
        }

        val quality = determineConnectionQuality(capabilities)
        val state = if (quality == ConnectionQuality.POOR) {
            ConnectionState.POOR_CONNECTION
        } else {
            ConnectionState.CONNECTED
        }

        _connectionInfo.value = ConnectionInfo(state, type, quality)
        _connectionState.value = state
    }

    private fun determineConnectionQuality(capabilities: NetworkCapabilities): ConnectionQuality {
        val downstreamBandwidth = capabilities.linkDownstreamBandwidthKbps
        val upstreamBandwidth = capabilities.linkUpstreamBandwidthKbps
        
        return when {
            downstreamBandwidth >= 10000 && upstreamBandwidth >= 5000 -> ConnectionQuality.EXCELLENT
            downstreamBandwidth >= 5000 && upstreamBandwidth >= 2000 -> ConnectionQuality.GOOD
            downstreamBandwidth >= 1000 && upstreamBandwidth >= 500 -> ConnectionQuality.FAIR
            downstreamBandwidth > 0 -> ConnectionQuality.POOR
            else -> ConnectionQuality.NONE
        }
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }

    /**
     * Check if connection quality is good enough for media
     */
    fun isGoodForMedia(): Boolean {
        val info = _connectionInfo.value
        return info.quality in listOf(ConnectionQuality.EXCELLENT, ConnectionQuality.GOOD)
    }

    /**
     * Get connection type string for display
     */
    fun getConnectionTypeString(): String {
        return when (_connectionInfo.value.type) {
            ConnectionType.WIFI -> "Wi-Fi"
            ConnectionType.CELLULAR -> "Mobile Data"
            ConnectionType.ETHERNET -> "Ethernet"
            ConnectionType.NONE -> "No Connection"
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Callback might not be registered
        }
    }
}
