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
    val longitude: Double? = null,
    // New fields for scan tracking
    val scanCount: Int = 1,
    val bestSignalStrength: Int = signalStrength,
    // For triangulation - store multiple readings as JSON string
    val signalReadings: String? = null // Format: "lat,lon,signal;lat,lon,signal;..."
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

    // Parse signal readings for triangulation
    fun getSignalReadingsList(): List<SignalReading> {
        return signalReadings?.split(";")?.mapNotNull { reading ->
            val parts = reading.split(",")
            if (parts.size >= 3) {
                try {
                    SignalReading(
                        latitude = parts[0].toDouble(),
                        longitude = parts[1].toDouble(),
                        signalStrength = parts[2].toInt()
                    )
                } catch (e: NumberFormatException) {
                    null
                }
            } else null
        } ?: emptyList()
    }

    // Add a new signal reading
    fun addSignalReading(lat: Double, lon: Double, signal: Int): String {
        val newReading = "$lat,$lon,$signal"
        return if (signalReadings.isNullOrEmpty()) {
            newReading
        } else {
            // Keep last 20 readings for triangulation
            val readings = signalReadings.split(";").takeLast(19)
            (readings + newReading).joinToString(";")
        }
    }
}

data class SignalReading(
    val latitude: Double,
    val longitude: Double,
    val signalStrength: Int
)
