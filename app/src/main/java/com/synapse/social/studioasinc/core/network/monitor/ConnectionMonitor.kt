package com.synapse.social.studioasinc.core.network.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network connection monitor with real-time status updates
 * Provides connection state and quality information
 */
@Singleton
class ConnectionMonitor @Inject constructor(
    @ApplicationContext context: Context
) {

    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        POOR_CONNECTION
    }

    data class ConnectionInfo(
        val state: ConnectionState,
        val type: ConnectionType,
        val quality: ConnectionQuality,
        val isMetered: Boolean
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

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    
    private val _connectionState = MutableStateFlow(getInitialConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _connectionInfo = MutableStateFlow(
        ConnectionInfo(
            getInitialConnectionState(),
            ConnectionType.NONE,
            ConnectionQuality.NONE,
            false
        )
    )
    val connectionInfo: StateFlow<ConnectionInfo> = _connectionInfo.asStateFlow()
    
    /**
     * Get the initial connection state by checking the active network
     */
    private fun getInitialConnectionState(): ConnectionState {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            ConnectionState.CONNECTED
        } else {
            ConnectionState.DISCONNECTED
        }
    }

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
                ConnectionQuality.NONE,
                false
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

        val isMetered = connectivityManager.isActiveNetworkMetered

        _connectionInfo.value = ConnectionInfo(state, type, quality, isMetered)
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
     * Check if currently connected (includes poor connection since network is still available)
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED || 
               _connectionState.value == ConnectionState.POOR_CONNECTION
    }

    /**
     * Check if connection is metered (cellular data)
     */
    fun isConnectionMetered(): Boolean {
        return connectivityManager.isActiveNetworkMetered
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
