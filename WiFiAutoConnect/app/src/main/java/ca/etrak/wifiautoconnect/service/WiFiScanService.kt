package ca.etrak.wifiautoconnect.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import ca.etrak.wifiautoconnect.R
import ca.etrak.wifiautoconnect.WiFiAutoConnectApp
import ca.etrak.wifiautoconnect.ui.MainActivity
import ca.etrak.wifiautoconnect.util.LocationHelper
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
    private lateinit var locationHelper: LocationHelper
    private lateinit var preferencesManager: PreferencesManager
    private val handler = Handler(Looper.getMainLooper())
    private var scanRunnable: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var totalNetworksFound = 0
    private var openNetworksFound = 0
    private var successfulConnections = 0

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                processScanResults(success)

                // For aggressive wardriving mode, start next scan immediately after processing
                if (preferencesManager.aggressiveScanMode) {
                    handler.postDelayed({ performScan() }, 500) // 500ms delay between scans
                }
            }
        }
    }

    companion object {
        private const val TAG = "WiFiScanService"
        private const val NOTIFICATION_ID = 1001
        private const val WAKELOCK_TAG = "WiFiAutoConnect:ScanWakeLock"

        const val ACTION_START = "ca.etrak.wifiautoconnect.START_SERVICE"
        const val ACTION_STOP = "ca.etrak.wifiautoconnect.STOP_SERVICE"
        const val ACTION_SCAN_NOW = "ca.etrak.wifiautoconnect.SCAN_NOW"
        const val ACTION_TOGGLE_AGGRESSIVE = "ca.etrak.wifiautoconnect.TOGGLE_AGGRESSIVE"

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
        locationHelper = LocationHelper(this)
        preferencesManager = PreferencesManager(this)

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiScanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wifiScanReceiver, filter)
        }

        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                acquireWakeLock()
                locationHelper.startLocationUpdates()
                startScanning()
                preferencesManager.serviceRunning = true
                Log.d(TAG, "Service started with GPS tracking")
            }
            ACTION_STOP -> {
                stopScanning()
                locationHelper.stopLocationUpdates()
                releaseWakeLock()
                preferencesManager.serviceRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Log.d(TAG, "Service stopped")
            }
            ACTION_SCAN_NOW -> {
                performScan()
            }
            ACTION_TOGGLE_AGGRESSIVE -> {
                preferencesManager.aggressiveScanMode = !preferencesManager.aggressiveScanMode
                if (preferencesManager.aggressiveScanMode) {
                    performScan() // Start aggressive scanning immediately
                }
                updateNotification()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
        locationHelper.stopLocationUpdates()
        releaseWakeLock()
        try {
            unregisterReceiver(wifiScanReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver not registered")
        }
        serviceScope.cancel()
        preferencesManager.serviceRunning = false
        Log.d(TAG, "Service destroyed")
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            acquire(10 * 60 * 60 * 1000L) // 10 hours max
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WiFiScanService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, WiFiScanService::class.java).apply {
            action = ACTION_TOGGLE_AGGRESSIVE
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 2, toggleIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val modeText = if (preferencesManager.aggressiveScanMode) "Mode Wardriving" else "Mode Normal"
        val toggleText = if (preferencesManager.aggressiveScanMode) "Mode Normal" else "Mode Rapide"

        return NotificationCompat.Builder(this, WiFiAutoConnectApp.CHANNEL_SERVICE)
            .setContentTitle("WiFi Scanner - $modeText")
            .setContentText("$totalNetworksFound réseaux | $openNetworksFound ouverts")
            .setSmallIcon(R.drawable.ic_wifi)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_scan, toggleText, togglePendingIntent)
            .addAction(R.drawable.ic_stop, "Arrêter", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startScanning() {
        // Start with immediate scan
        performScan()

        // Schedule periodic scans (fallback if aggressive mode is off)
        scanRunnable = object : Runnable {
            override fun run() {
                if (!preferencesManager.aggressiveScanMode) {
                    performScan()
                }
                handler.postDelayed(this, preferencesManager.scanInterval)
            }
        }
        handler.postDelayed(scanRunnable!!, preferencesManager.scanInterval)
    }

    private fun stopScanning() {
        scanRunnable?.let { handler.removeCallbacks(it) }
        scanRunnable = null
    }

    private fun performScan() {
        if (wifiHelper.isWifiEnabled() && wifiHelper.hasLocationPermission()) {
            val started = wifiHelper.startScan()
            Log.d(TAG, "Scan initiated: $started")
        } else {
            Log.w(TAG, "Cannot scan: WiFi=${wifiHelper.isWifiEnabled()}, Location=${wifiHelper.hasLocationPermission()}")
        }
    }

    private fun processScanResults(scanSuccess: Boolean) {
        serviceScope.launch {
            try {
                val results = wifiHelper.getScanResults()
                if (results.isEmpty() && !scanSuccess) {
                    Log.d(TAG, "No scan results available")
                    return@launch
                }

                Log.d(TAG, "Processing ${results.size} networks")

                // Get current GPS location
                val location = locationHelper.getCurrentLocation()
                val latitude = location?.latitude
                val longitude = location?.longitude

                Log.d(TAG, "GPS: lat=$latitude, lon=$longitude")

                // Process scan results with GPS coordinates
                wifiHelper.processScanResults(results, latitude, longitude)

                // Update counters
                totalNetworksFound = results.size
                openNetworksFound = results.count { wifiHelper.isOpenNetwork(it) }

                // Auto-connect to open networks
                tryAutoConnect(results)

                // Update notification with stats
                updateNotification()

            } catch (e: Exception) {
                Log.e(TAG, "Error processing scan results", e)
            }
        }
    }

    private suspend fun tryAutoConnect(results: List<android.net.wifi.ScanResult>) {
        if (!preferencesManager.autoConnectEnabled) return

        val currentConnection = wifiHelper.getCurrentConnection()
        val isConnected = currentConnection != null &&
                         currentConnection != "<unknown ssid>" &&
                         currentConnection.isNotBlank()

        // Skip if already connected and set to only connect when disconnected
        if (isConnected && preferencesManager.connectOnlyWhenDisconnected) {
            Log.d(TAG, "Already connected to: $currentConnection, skipping auto-connect")
            return
        }

        val openNetworks = results.filter { result ->
            wifiHelper.isOpenNetwork(result) &&
                result.level >= preferencesManager.minSignalStrength &&
                !result.SSID.isNullOrBlank() &&
                result.SSID != currentConnection // Don't reconnect to current network
        }.sortedByDescending { it.level }

        if (openNetworks.isEmpty()) {
            Log.d(TAG, "No suitable open networks found")
            return
        }

        // Try to connect to each open network until success
        for (network in openNetworks.take(3)) { // Try top 3 strongest
            Log.d(TAG, "Attempting connection to: ${network.SSID} (${network.level} dBm)")

            val success = wifiHelper.connectToOpenNetwork(network.SSID, network.BSSID)

            if (success) {
                successfulConnections++
                Log.d(TAG, "Successfully connected to: ${network.SSID}")

                if (preferencesManager.notifyOnConnection) {
                    showConnectionNotification(network.SSID)
                }
                break
            } else {
                Log.d(TAG, "Failed to connect to: ${network.SSID}")
            }
        }
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showConnectionNotification(ssid: String) {
        val location = locationHelper.getCurrentLocation()
        val locationText = if (location != null) {
            " (${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)})"
        } else ""

        val notification = NotificationCompat.Builder(this, WiFiAutoConnectApp.CHANNEL_CONNECTION)
            .setContentTitle("Connecté au Wi-Fi")
            .setContentText("$ssid$locationText")
            .setSmallIcon(R.drawable.ic_wifi_connected)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(2001, notification)
    }
}
