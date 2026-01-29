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

    @Query("SELECT * FROM wifi_networks WHERE isOpen = 0 ORDER BY lastSeenTimestamp DESC")
    fun getSecuredNetworks(): LiveData<List<WiFiNetwork>>

    @Query("SELECT * FROM wifi_networks WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY lastSeenTimestamp DESC")
    fun getNetworksWithLocation(): LiveData<List<WiFiNetwork>>

    @Query("SELECT * FROM wifi_networks WHERE latitude IS NOT NULL AND longitude IS NOT NULL")
    suspend fun getNetworksWithLocationSync(): List<WiFiNetwork>

    @Query("SELECT * FROM wifi_networks ORDER BY lastSeenTimestamp DESC")
    suspend fun getAllNetworksSync(): List<WiFiNetwork>

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

    @Query("UPDATE wifi_networks SET scanCount = scanCount + 1, lastSeenTimestamp = :timestamp, signalStrength = :signal, latitude = COALESCE(:lat, latitude), longitude = COALESCE(:lon, longitude), bestSignalStrength = CASE WHEN :signal > bestSignalStrength THEN :signal ELSE bestSignalStrength END, signalReadings = :readings WHERE bssid = :bssid")
    suspend fun updateNetworkScan(bssid: String, timestamp: Long, signal: Int, lat: Double?, lon: Double?, readings: String?)

    @Query("SELECT * FROM wifi_networks WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND isOpen = 1")
    suspend fun getOpenNetworksWithLocationSync(): List<WiFiNetwork>

    @Query("SELECT * FROM wifi_networks WHERE signalReadings IS NOT NULL AND LENGTH(signalReadings) > 10")
    suspend fun getNetworksForTriangulation(): List<WiFiNetwork>

    @Query("SELECT SUM(scanCount) FROM wifi_networks")
    suspend fun getTotalScans(): Int

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
