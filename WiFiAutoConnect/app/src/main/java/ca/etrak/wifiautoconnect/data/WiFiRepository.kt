package ca.etrak.wifiautoconnect.data

import androidx.lifecycle.LiveData

class WiFiRepository(private val wifiDao: WiFiDao) {

    val allNetworks: LiveData<List<WiFiNetwork>> = wifiDao.getAllNetworks()
    val openNetworks: LiveData<List<WiFiNetwork>> = wifiDao.getOpenNetworks()
    val allLogs: LiveData<List<ConnectionLog>> = wifiDao.getAllLogs()

    fun getRecentLogs(limit: Int): LiveData<List<ConnectionLog>> = wifiDao.getRecentLogs(limit)

    fun getLogsForNetwork(ssid: String): LiveData<List<ConnectionLog>> = wifiDao.getLogsForNetwork(ssid)

    suspend fun getNetworkByBssid(bssid: String): WiFiNetwork? = wifiDao.getNetworkByBssid(bssid)

    suspend fun getNetworkBySsid(ssid: String): WiFiNetwork? = wifiDao.getNetworkBySsid(ssid)

    suspend fun insertOrUpdateNetwork(network: WiFiNetwork) {
        val existing = wifiDao.getNetworkByBssid(network.bssid)
        if (existing != null) {
            wifiDao.updateNetwork(
                network.copy(
                    id = existing.id,
                    firstSeenTimestamp = existing.firstSeenTimestamp,
                    connectionAttempted = existing.connectionAttempted,
                    connectionSuccessful = existing.connectionSuccessful,
                    lastConnectionTimestamp = existing.lastConnectionTimestamp,
                    connectionCount = existing.connectionCount
                )
            )
        } else {
            wifiDao.insertNetwork(network)
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

    suspend fun deleteAllData() {
        wifiDao.deleteAllNetworks()
        wifiDao.deleteAllLogs()
    }

    suspend fun deleteOldLogs(olderThanDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        wifiDao.deleteOldLogs(cutoffTime)
    }
}
