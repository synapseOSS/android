package com.synapse.social.studioasinc.data.repository.deletion

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors network connectivity changes and provides connectivity state
 * Used to trigger automatic retry of failed deletion operations
 * Requirements: 4.1, 4.5
 */
class ConnectivityMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "ConnectivityMonitor"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // Network connectivity state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Network type information
    private val _networkType = MutableStateFlow(NetworkType.NONE)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()
    
    // Connection quality estimation
    private val _connectionQuality = MutableStateFlow(ConnectionQuality.UNKNOWN)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isMonitoring = false
    
    /**
     * Start monitoring network connectivity
     * Requirements: 4.1, 4.5
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Already monitoring network connectivity")
            return
        }
        
        try {
            // Initial connectivity check
            updateConnectivityState()
            
            // Create network callback
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available: $network")
                    updateConnectivityState()
                }
                
                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost: $network")
                    updateConnectivityState()
                }
                
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    Log.d(TAG, "Network capabilities changed: $network")
                    updateConnectivityState(networkCapabilities)
                }
                
                override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) {
                    Log.d(TAG, "Network link properties changed: $network")
                    updateConnectivityState()
                }
                
                override fun onUnavailable() {
                    Log.d(TAG, "Network unavailable")
                    _isConnected.value = false
                    _networkType.value = NetworkType.NONE
                    _connectionQuality.value = ConnectionQuality.NONE
                }
            }
            
            // Register network callback
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
            
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
            isMonitoring = true
            
            Log.d(TAG, "Started monitoring network connectivity")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start network monitoring", e)
        }
    }
    
    /**
     * Stop monitoring network connectivity
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            return
        }
        
        try {
            networkCallback?.let { callback ->
                connectivityManager.unregisterNetworkCallback(callback)
            }
            networkCallback = null
            isMonitoring = false
            
            Log.d(TAG, "Stopped monitoring network connectivity")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping network monitoring", e)
        }
    }
    
    /**
     * Update connectivity state based on current network
     */
    private fun updateConnectivityState(capabilities: NetworkCapabilities? = null) {
        try {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = capabilities ?: connectivityManager.getNetworkCapabilities(network)
            
            if (network == null || networkCapabilities == null) {
                _isConnected.value = false
                _networkType.value = NetworkType.NONE
                _connectionQuality.value = ConnectionQuality.NONE
                return
            }
            
            // Check if network has internet capability and is validated
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            _isConnected.value = hasInternet && isValidated
            
            // Determine network type
            _networkType.value = when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
            
            // Estimate connection quality
            _connectionQuality.value = estimateConnectionQuality(networkCapabilities)
            
            Log.d(TAG, "Connectivity state updated - Connected: ${_isConnected.value}, " +
                      "Type: ${_networkType.value}, Quality: ${_connectionQuality.value}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating connectivity state", e)
            _isConnected.value = false
            _networkType.value = NetworkType.NONE
            _connectionQuality.value = ConnectionQuality.UNKNOWN
        }
    }
    
    /**
     * Estimate connection quality based on network capabilities
     */
    private fun estimateConnectionQuality(capabilities: NetworkCapabilities): ConnectionQuality {
        return try {
            when {
                // WiFi is generally considered good quality
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                        ConnectionQuality.EXCELLENT
                    } else {
                        ConnectionQuality.GOOD
                    }
                }
                
                // Ethernet is excellent
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionQuality.EXCELLENT
                
                // Cellular quality depends on capabilities
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    when {
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> ConnectionQuality.GOOD
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED) -> ConnectionQuality.FAIR
                        else -> ConnectionQuality.POOR
                    }
                }
                
                else -> ConnectionQuality.UNKNOWN
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error estimating connection quality", e)
            ConnectionQuality.UNKNOWN
        }
    }
    
    /**
     * Check if network is suitable for retry operations
     * Requirements: 4.1, 4.5
     */
    fun isSuitableForRetry(): Boolean {
        return _isConnected.value && _connectionQuality.value != ConnectionQuality.NONE
    }
    
    /**
     * Check if network is metered (has data usage limits)
     */
    fun isMeteredConnection(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            capabilities?.let {
                !it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            } ?: true // Assume metered if unknown
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if connection is metered", e)
            true // Assume metered on error
        }
    }
    
    /**
     * Get current connectivity information
     */
    fun getConnectivityInfo(): ConnectivityInfo {
        return ConnectivityInfo(
            isConnected = _isConnected.value,
            networkType = _networkType.value,
            connectionQuality = _connectionQuality.value,
            isMetered = isMeteredConnection(),
            isSuitableForRetry = isSuitableForRetry()
        )
    }
}

/**
 * Network type enumeration
 */
enum class NetworkType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER
}

/**
 * Connection quality estimation
 */
enum class ConnectionQuality {
    NONE,
    POOR,
    FAIR,
    GOOD,
    EXCELLENT,
    UNKNOWN
}

/**
 * Comprehensive connectivity information
 */
data class ConnectivityInfo(
    val isConnected: Boolean,
    val networkType: NetworkType,
    val connectionQuality: ConnectionQuality,
    val isMetered: Boolean,
    val isSuitableForRetry: Boolean
)