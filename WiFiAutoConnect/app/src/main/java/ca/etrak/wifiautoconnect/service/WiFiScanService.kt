package ca.etrak.wifiautoconnect.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import ca.etrak.wifiautoconnect.R
import ca.etrak.wifiautoconnect.WiFiAutoConnectApp
import ca.etrak.wifiautoconnect.ui.MainActivity
import ca.etrak.wifiautoconnect.util.PreferencesManager
import ca.etrak.wifiautoconnect.util.WiFiHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WiFiScanService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var wifiHelper: WiFiHelper
    private lateinit var preferencesManager: PreferencesManager
    private val handler = Handler(Looper.getMainLooper())
    private var scanRunnable: Runnable? = null

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    processScanResults()
                }
            }
        }
    }

    companion object {
        private const val TAG = "WiFiScanService"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ca.etrak.wifiautoconnect.START_SERVICE"
        const val ACTION_STOP = "ca.etrak.wifiautoconnect.STOP_SERVICE"
        const val ACTION_SCAN_NOW = "ca.etrak.wifiautoconnect.SCAN_NOW"

        fun startService(context: Context) {
            val intent = Intent(context, WiFiScanService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WiFiScanService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as WiFiAutoConnectApp
        wifiHelper = WiFiHelper(this, app.repository)
        preferencesManager = PreferencesManager(this)

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, filter)

        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startScanning()
                preferencesManager.serviceRunning = true
                Log.d(TAG, "Service started")
            }
            ACTION_STOP -> {
                stopScanning()
                preferencesManager.serviceRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Log.d(TAG, "Service stopped")
            }
            ACTION_SCAN_NOW -> {
                performScan()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
        unregisterReceiver(wifiScanReceiver)
        serviceScope.cancel()
        preferencesManager.serviceRunning = false
        Log.d(TAG, "Service destroyed")
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WiFiScanService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, WiFiAutoConnectApp.CHANNEL_SERVICE)
            .setContentTitle("WiFi Auto Connect")
            .setContentText("Scan en cours...")
            .setSmallIcon(R.drawable.ic_wifi)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Arrêter", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startScanning() {
        scanRunnable = object : Runnable {
            override fun run() {
                performScan()
                handler.postDelayed(this, preferencesManager.scanInterval)
            }
        }
        handler.post(scanRunnable!!)
    }

    private fun stopScanning() {
        scanRunnable?.let { handler.removeCallbacks(it) }
        scanRunnable = null
    }

    private fun performScan() {
        Log.d(TAG, "Performing Wi-Fi scan")
        if (wifiHelper.isWifiEnabled() && wifiHelper.hasLocationPermission()) {
            wifiHelper.startScan()
        } else {
            Log.w(TAG, "Cannot scan: WiFi enabled=${wifiHelper.isWifiEnabled()}, Location permission=${wifiHelper.hasLocationPermission()}")
        }
    }

    private fun processScanResults() {
        serviceScope.launch {
            try {
                val results = wifiHelper.getScanResults()
                Log.d(TAG, "Found ${results.size} networks")

                wifiHelper.processScanResults(results)

                // Auto-connect to open networks if enabled
                if (preferencesManager.autoConnectEnabled) {
                    val currentConnection = wifiHelper.getCurrentConnection()
                    val isConnected = currentConnection != null && currentConnection != "<unknown ssid>"

                    if (!isConnected || !preferencesManager.connectOnlyWhenDisconnected) {
                        val openNetworks = results.filter { result ->
                            wifiHelper.isOpenNetwork(result) &&
                                    result.level >= preferencesManager.minSignalStrength &&
                                    !result.SSID.isNullOrBlank()
                        }.sortedByDescending { it.level }

                        if (openNetworks.isNotEmpty()) {
                            val bestNetwork = openNetworks.first()
                            Log.d(TAG, "Attempting to connect to: ${bestNetwork.SSID} (${bestNetwork.level} dBm)")

                            val success = wifiHelper.connectToOpenNetwork(bestNetwork.SSID, bestNetwork.BSSID)

                            if (success && preferencesManager.notifyOnConnection) {
                                showConnectionNotification(bestNetwork.SSID)
                            }
                        }
                    }
                }

                updateNotification(results.size)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing scan results", e)
            }
        }
    }

    private fun updateNotification(networkCount: Int) {
        val notification = NotificationCompat.Builder(this, WiFiAutoConnectApp.CHANNEL_SERVICE)
            .setContentTitle("WiFi Auto Connect")
            .setContentText("$networkCount réseaux détectés")
            .setSmallIcon(R.drawable.ic_wifi)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showConnectionNotification(ssid: String) {
        val notification = NotificationCompat.Builder(this, WiFiAutoConnectApp.CHANNEL_CONNECTION)
            .setContentTitle("Connecté au Wi-Fi")
            .setContentText("Connexion automatique à: $ssid")
            .setSmallIcon(R.drawable.ic_wifi_connected)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(2001, notification)
    }
}
