package ca.etrak.wifiautoconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_logs")
data class ConnectionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val bssid: String,
    val timestamp: Long,
    val eventType: String,  // SCAN, CONNECT_ATTEMPT, CONNECT_SUCCESS, CONNECT_FAILED, DISCONNECT
    val signalStrength: Int,
    val details: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    companion object {
        const val EVENT_SCAN = "SCAN"
        const val EVENT_CONNECT_ATTEMPT = "CONNECT_ATTEMPT"
        const val EVENT_CONNECT_SUCCESS = "CONNECT_SUCCESS"
        const val EVENT_CONNECT_FAILED = "CONNECT_FAILED"
        const val EVENT_DISCONNECT = "DISCONNECT"
    }
}
