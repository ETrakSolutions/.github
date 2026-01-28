package ca.etrak.wifiautoconnect

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import ca.etrak.wifiautoconnect.data.WiFiDatabase
import ca.etrak.wifiautoconnect.data.WiFiRepository

class WiFiAutoConnectApp : Application() {

    val database by lazy { WiFiDatabase.getDatabase(this) }
    val repository by lazy { WiFiRepository(database.wifiDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Service de scan Wi-Fi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications du service de scan Wi-Fi en arrière-plan"
            }

            val connectionChannel = NotificationChannel(
                CHANNEL_CONNECTION,
                "Connexions Wi-Fi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications de connexion aux réseaux Wi-Fi"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(connectionChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE = "wifi_scan_service"
        const val CHANNEL_CONNECTION = "wifi_connection"
    }
}
