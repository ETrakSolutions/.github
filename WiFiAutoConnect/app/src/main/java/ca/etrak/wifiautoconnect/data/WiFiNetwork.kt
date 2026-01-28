package ca.etrak.wifiautoconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wifi_networks")
data class WiFiNetwork(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,
    val frequency: Int,
    val capabilities: String,
    val isOpen: Boolean,
    val firstSeenTimestamp: Long,
    val lastSeenTimestamp: Long,
    val connectionAttempted: Boolean = false,
    val connectionSuccessful: Boolean = false,
    val lastConnectionTimestamp: Long? = null,
    val connectionCount: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val securityType: String
        get() = when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            else -> "OPEN"
        }

    val frequencyBand: String
        get() = if (frequency >= 5000) "5 GHz" else "2.4 GHz"
}
