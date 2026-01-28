package ca.etrak.wifiautoconnect.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import ca.etrak.wifiautoconnect.data.ConnectionLog
import ca.etrak.wifiautoconnect.data.WiFiNetwork
import ca.etrak.wifiautoconnect.data.WiFiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WiFiHelper(private val context: Context, private val repository: WiFiRepository) {

    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    companion object {
        private const val TAG = "WiFiHelper"
    }

    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    fun enableWifi(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            wifiManager.setWifiEnabled(true)
        } else {
            false
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun startScan(): Boolean {
        return if (hasLocationPermission()) {
            @Suppress("DEPRECATION")
            wifiManager.startScan()
        } else {
            Log.w(TAG, "Location permission not granted")
            false
        }
    }

    fun getScanResults(): List<ScanResult> {
        return if (hasLocationPermission()) {
            wifiManager.scanResults ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun isOpenNetwork(scanResult: ScanResult): Boolean {
        val capabilities = scanResult.capabilities
        return !capabilities.contains("WPA") &&
                !capabilities.contains("WEP") &&
                !capabilities.contains("PSK") &&
                !capabilities.contains("EAP")
    }

    suspend fun processScanResults(scanResults: List<ScanResult>, latitude: Double? = null, longitude: Double? = null) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()

            for (result in scanResults) {
                if (result.SSID.isNullOrBlank()) continue

                val isOpen = isOpenNetwork(result)

                val network = WiFiNetwork(
                    ssid = result.SSID,
                    bssid = result.BSSID,
                    signalStrength = result.level,
                    frequency = result.frequency,
                    capabilities = result.capabilities,
                    isOpen = isOpen,
                    firstSeenTimestamp = timestamp,
                    lastSeenTimestamp = timestamp,
                    latitude = latitude,
                    longitude = longitude
                )

                repository.insertOrUpdateNetwork(network)

                // Log the scan
                repository.insertLog(
                    ConnectionLog(
                        ssid = result.SSID,
                        bssid = result.BSSID,
                        timestamp = timestamp,
                        eventType = ConnectionLog.EVENT_SCAN,
                        signalStrength = result.level,
                        details = "Security: ${network.securityType}, Band: ${network.frequencyBand}",
                        latitude = latitude,
                        longitude = longitude
                    )
                )
            }
        }
    }

    suspend fun connectToOpenNetwork(ssid: String, bssid: String): Boolean {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Attempting to connect to open network: $ssid")

            // Log connection attempt
            repository.insertLog(
                ConnectionLog(
                    ssid = ssid,
                    bssid = bssid,
                    timestamp = System.currentTimeMillis(),
                    eventType = ConnectionLog.EVENT_CONNECT_ATTEMPT,
                    signalStrength = 0,
                    details = "Attempting automatic connection"
                )
            )

            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectWithNetworkSpecifier(ssid)
            } else {
                connectLegacy(ssid)
            }

            // Update connection status
            repository.updateConnectionStatus(bssid, attempted = true, successful = success)

            // Log result
            repository.insertLog(
                ConnectionLog(
                    ssid = ssid,
                    bssid = bssid,
                    timestamp = System.currentTimeMillis(),
                    eventType = if (success) ConnectionLog.EVENT_CONNECT_SUCCESS else ConnectionLog.EVENT_CONNECT_FAILED,
                    signalStrength = 0,
                    details = if (success) "Connection successful" else "Connection failed"
                )
            )

            success
        }
    }

    @Suppress("DEPRECATION")
    private fun connectLegacy(ssid: String): Boolean {
        try {
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }

            val existingConfig = wifiManager.configuredNetworks?.find {
                it.SSID == "\"$ssid\""
            }

            val netId = if (existingConfig != null) {
                existingConfig.networkId
            } else {
                wifiManager.addNetwork(wifiConfig)
            }

            if (netId == -1) {
                Log.e(TAG, "Failed to add network configuration")
                return false
            }

            wifiManager.disconnect()
            val enabled = wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()

            return enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to network", e)
            return false
        }
    }

    private fun connectWithNetworkSpecifier(ssid: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false

        try {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()

            var connected = false

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available: $ssid")
                    connected = true
                    connectivityManager.bindProcessToNetwork(network)
                }

                override fun onUnavailable() {
                    Log.d(TAG, "Network unavailable: $ssid")
                    connected = false
                }
            }

            connectivityManager.requestNetwork(request, callback)

            // Wait briefly for connection
            Thread.sleep(3000)

            return connected
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting with network specifier", e)
            return false
        }
    }

    fun suggestOpenNetworks(ssids: List<String>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false

        val suggestions = ssids.map { ssid ->
            WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setIsAppInteractionRequired(false)
                .build()
        }

        val status = wifiManager.addNetworkSuggestions(suggestions)
        return status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
    }

    fun getCurrentConnection(): String? {
        @Suppress("DEPRECATION")
        val connectionInfo = wifiManager.connectionInfo
        return connectionInfo?.ssid?.replace("\"", "")
    }

    fun getConnectionInfo(): Map<String, Any?> {
        @Suppress("DEPRECATION")
        val info = wifiManager.connectionInfo
        return mapOf(
            "ssid" to info?.ssid?.replace("\"", ""),
            "bssid" to info?.bssid,
            "rssi" to info?.rssi,
            "linkSpeed" to info?.linkSpeed,
            "frequency" to info?.frequency,
            "networkId" to info?.networkId
        )
    }
}
