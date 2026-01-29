package ca.etrak.wifiautoconnect.data

import androidx.lifecycle.LiveData

class WiFiRepository(private val wifiDao: WiFiDao) {

    val allNetworks: LiveData<List<WiFiNetwork>> = wifiDao.getAllNetworks()
    val openNetworks: LiveData<List<WiFiNetwork>> = wifiDao.getOpenNetworks()
    val securedNetworks: LiveData<List<WiFiNetwork>> = wifiDao.getSecuredNetworks()
    val networksWithLocation: LiveData<List<WiFiNetwork>> = wifiDao.getNetworksWithLocation()
    val allLogs: LiveData<List<ConnectionLog>> = wifiDao.getAllLogs()

    fun getRecentLogs(limit: Int): LiveData<List<ConnectionLog>> = wifiDao.getRecentLogs(limit)

    fun getLogsForNetwork(ssid: String): LiveData<List<ConnectionLog>> = wifiDao.getLogsForNetwork(ssid)

    suspend fun getNetworkByBssid(bssid: String): WiFiNetwork? = wifiDao.getNetworkByBssid(bssid)

    suspend fun getNetworkBySsid(ssid: String): WiFiNetwork? = wifiDao.getNetworkBySsid(ssid)

    suspend fun insertOrUpdateNetwork(network: WiFiNetwork) {
        val existing = wifiDao.getNetworkByBssid(network.bssid)
        if (existing != null) {
            // Update existing network - increment scan count and update signal readings
            val newReadings = if (network.latitude != null && network.longitude != null) {
                existing.addSignalReading(network.latitude, network.longitude, network.signalStrength)
            } else {
                existing.signalReadings
            }

            wifiDao.updateNetworkScan(
                bssid = network.bssid,
                timestamp = network.lastSeenTimestamp,
                signal = network.signalStrength,
                lat = network.latitude,
                lon = network.longitude,
                readings = newReadings
            )
        } else {
            // Insert new network with initial signal reading
            val initialReadings = if (network.latitude != null && network.longitude != null) {
                "${network.latitude},${network.longitude},${network.signalStrength}"
            } else null

            wifiDao.insertNetwork(network.copy(signalReadings = initialReadings))
        }
    }

    suspend fun updateConnectionStatus(bssid: String, attempted: Boolean, successful: Boolean) {
        wifiDao.updateConnectionStatus(bssid, attempted, successful, System.currentTimeMillis())
    }

    suspend fun insertLog(log: ConnectionLog) {
        wifiDao.insertLog(log)
    }

    suspend fun getNetworkCount(): Int = wifiDao.getNetworkCount()

    suspend fun getOpenNetworkCount(): Int = wifiDao.getOpenNetworkCount()

    suspend fun getSuccessfulConnectionCount(): Int = wifiDao.getSuccessfulConnectionCount()

    suspend fun getLogCount(): Int = wifiDao.getLogCount()

    suspend fun getTotalScans(): Int = wifiDao.getTotalScans()

    suspend fun getNetworksWithLocationSync(): List<WiFiNetwork> = wifiDao.getNetworksWithLocationSync()

    suspend fun getOpenNetworksWithLocationSync(): List<WiFiNetwork> = wifiDao.getOpenNetworksWithLocationSync()

    suspend fun getNetworksForTriangulation(): List<WiFiNetwork> = wifiDao.getNetworksForTriangulation()

    suspend fun deleteAllData() {
        wifiDao.deleteAllNetworks()
        wifiDao.deleteAllLogs()
    }

    suspend fun deleteOldLogs(olderThanDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        wifiDao.deleteOldLogs(cutoffTime)
    }

    // Export data as CSV
    suspend fun exportToCsv(): String {
        val networks = wifiDao.getNetworksWithLocationSync()
        val sb = StringBuilder()
        sb.appendLine("SSID,BSSID,Security,Signal,Frequency,Latitude,Longitude,ScanCount,FirstSeen,LastSeen,Connected")

        networks.forEach { network ->
            sb.appendLine("\"${network.ssid}\",${network.bssid},${network.securityType},${network.signalStrength},${network.frequency},${network.latitude ?: ""},${network.longitude ?: ""},${network.scanCount},${network.firstSeenTimestamp},${network.lastSeenTimestamp},${network.connectionSuccessful}")
        }

        return sb.toString()
    }

    // Export data as JSON
    suspend fun exportToJson(): String {
        val networks = wifiDao.getNetworksWithLocationSync()
        val sb = StringBuilder()
        sb.appendLine("[")

        networks.forEachIndexed { index, network ->
            sb.append("""  {
    "ssid": "${network.ssid}",
    "bssid": "${network.bssid}",
    "security": "${network.securityType}",
    "signal": ${network.signalStrength},
    "frequency": ${network.frequency},
    "latitude": ${network.latitude ?: "null"},
    "longitude": ${network.longitude ?: "null"},
    "scanCount": ${network.scanCount},
    "firstSeen": ${network.firstSeenTimestamp},
    "lastSeen": ${network.lastSeenTimestamp},
    "connected": ${network.connectionSuccessful}
  }""")
            if (index < networks.size - 1) sb.append(",")
            sb.appendLine()
        }

        sb.appendLine("]")
        return sb.toString()
    }
}
