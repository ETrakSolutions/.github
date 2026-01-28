package ca.etrak.wifiautoconnect.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WiFiDao {

    // WiFi Networks
    @Query("SELECT * FROM wifi_networks ORDER BY lastSeenTimestamp DESC")
    fun getAllNetworks(): LiveData<List<WiFiNetwork>>

    @Query("SELECT * FROM wifi_networks WHERE isOpen = 1 ORDER BY lastSeenTimestamp DESC")
    fun getOpenNetworks(): LiveData<List<WiFiNetwork>>

    @Query("SELECT * FROM wifi_networks WHERE bssid = :bssid LIMIT 1")
    suspend fun getNetworkByBssid(bssid: String): WiFiNetwork?

    @Query("SELECT * FROM wifi_networks WHERE ssid = :ssid LIMIT 1")
    suspend fun getNetworkBySsid(ssid: String): WiFiNetwork?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetwork(network: WiFiNetwork): Long

    @Update
    suspend fun updateNetwork(network: WiFiNetwork)

    @Query("UPDATE wifi_networks SET connectionAttempted = :attempted, connectionSuccessful = :successful, lastConnectionTimestamp = :timestamp, connectionCount = connectionCount + 1 WHERE bssid = :bssid")
    suspend fun updateConnectionStatus(bssid: String, attempted: Boolean, successful: Boolean, timestamp: Long)

    @Query("DELETE FROM wifi_networks")
    suspend fun deleteAllNetworks()

    @Query("SELECT COUNT(*) FROM wifi_networks")
    suspend fun getNetworkCount(): Int

    @Query("SELECT COUNT(*) FROM wifi_networks WHERE isOpen = 1")
    suspend fun getOpenNetworkCount(): Int

    @Query("SELECT COUNT(*) FROM wifi_networks WHERE connectionSuccessful = 1")
    suspend fun getSuccessfulConnectionCount(): Int

    // Connection Logs
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC")
    fun getAllLogs(): LiveData<List<ConnectionLog>>

    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): LiveData<List<ConnectionLog>>

    @Query("SELECT * FROM connection_logs WHERE ssid = :ssid ORDER BY timestamp DESC")
    fun getLogsForNetwork(ssid: String): LiveData<List<ConnectionLog>>

    @Query("SELECT * FROM connection_logs WHERE eventType = :eventType ORDER BY timestamp DESC")
    fun getLogsByEventType(eventType: String): LiveData<List<ConnectionLog>>

    @Insert
    suspend fun insertLog(log: ConnectionLog): Long

    @Query("DELETE FROM connection_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM connection_logs WHERE timestamp < :timestamp")
    suspend fun deleteOldLogs(timestamp: Long)

    @Query("SELECT COUNT(*) FROM connection_logs")
    suspend fun getLogCount(): Int

    @Query("SELECT COUNT(*) FROM connection_logs WHERE eventType = :eventType")
    suspend fun getLogCountByType(eventType: String): Int
}
