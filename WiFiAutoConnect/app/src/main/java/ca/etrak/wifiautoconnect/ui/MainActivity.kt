package ca.etrak.wifiautoconnect.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ca.etrak.wifiautoconnect.R
import ca.etrak.wifiautoconnect.WiFiAutoConnectApp
import ca.etrak.wifiautoconnect.data.WiFiNetwork
import ca.etrak.wifiautoconnect.databinding.ActivityMainBinding
import ca.etrak.wifiautoconnect.service.WiFiScanService
import ca.etrak.wifiautoconnect.util.LocationHelper
import ca.etrak.wifiautoconnect.util.PreferencesManager
import ca.etrak.wifiautoconnect.util.WiFiHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var networkAdapter: NetworkAdapter
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var wifiHelper: WiFiHelper
    private lateinit var locationHelper: LocationHelper

    private var currentFilter: NetworkFilter = NetworkFilter.ALL
    private var allNetworks: List<WiFiNetwork> = emptyList()
    private var realtimeNetworkBssids: Set<String> = emptySet()

    enum class NetworkFilter { ALL, OPEN, SECURED }

    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Permissions accordées", Toast.LENGTH_SHORT).show()
            startScanService()
            locationHelper.startLocationUpdates { location ->
                updateGpsDisplay(location.latitude, location.longitude)
            }
        } else {
            showPermissionExplanation()
        }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                updateRealtimeNetworks()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val app = application as WiFiAutoConnectApp
        preferencesManager = PreferencesManager(this)
        wifiHelper = WiFiHelper(this, app.repository)
        locationHelper = LocationHelper(this)

        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(app.repository)
        )[MainViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupButtons()
        setupFilterChips()

        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        binding.switchService.isChecked = preferencesManager.serviceRunning
        binding.switchWardriving.isChecked = preferencesManager.aggressiveScanMode
        binding.switchAutoConnect.isChecked = preferencesManager.autoConnectEnabled
        updateConnectionStatus()
        updateRealtimeNetworks()

        // Register for scan results
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiScanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wifiScanReceiver, filter)
        }

        // Start location updates
        if (hasAllPermissions()) {
            locationHelper.startLocationUpdates { location ->
                updateGpsDisplay(location.latitude, location.longitude)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(wifiScanReceiver)
        } catch (e: Exception) { /* Ignored */ }
        locationHelper.stopLocationUpdates()
    }

    private fun setupRecyclerView() {
        networkAdapter = NetworkAdapter { network ->
            showNetworkDetails(network)
        }

        binding.recyclerNetworks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = networkAdapter
        }
    }

    private fun setupObservers() {
        viewModel.allNetworks.observe(this) { networks ->
            allNetworks = networks
            applyFilterAndDisplay()
            updateStats(networks)
        }

        viewModel.connectionStatus.observe(this) { status ->
            binding.textConnectionStatus.text = status
        }

        // Observe networks with location for stats
        val app = application as WiFiAutoConnectApp
        app.repository.networksWithLocation.observe(this) { networksWithGps ->
            binding.textWithGps.text = "Avec GPS: ${networksWithGps.size}"
        }
    }

    private fun setupButtons() {
        binding.switchService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hasAllPermissions()) {
                    startScanService()
                } else {
                    binding.switchService.isChecked = false
                    checkAndRequestPermissions()
                }
            } else {
                stopScanService()
            }
        }

        binding.switchWardriving.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.aggressiveScanMode = isChecked
            val message = if (isChecked) "Mode Wardriving activé - scan rapide" else "Mode normal"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            // Send toggle command to service if running
            if (preferencesManager.serviceRunning) {
                val intent = Intent(this, WiFiScanService::class.java).apply {
                    action = WiFiScanService.ACTION_TOGGLE_AGGRESSIVE
                }
                startService(intent)
            }
        }

        binding.switchAutoConnect.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.autoConnectEnabled = isChecked
            val message = if (isChecked) "Connexion automatique activée" else "Connexion automatique désactivée"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        binding.buttonScanNow.setOnClickListener {
            if (hasAllPermissions()) {
                performManualScan()
            } else {
                checkAndRequestPermissions()
            }
        }

        binding.buttonViewMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.buttonViewLogs.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = NetworkFilter.ALL
                applyFilterAndDisplay()
            }
        }

        binding.chipOpen.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = NetworkFilter.OPEN
                applyFilterAndDisplay()
            }
        }

        binding.chipSecured.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = NetworkFilter.SECURED
                applyFilterAndDisplay()
            }
        }
    }

    private fun applyFilterAndDisplay() {
        // Filter to show only real-time available networks
        val realtimeNetworks = allNetworks.filter { it.bssid in realtimeNetworkBssids }

        val filteredNetworks = when (currentFilter) {
            NetworkFilter.ALL -> realtimeNetworks
            NetworkFilter.OPEN -> realtimeNetworks.filter { it.isOpen }
            NetworkFilter.SECURED -> realtimeNetworks.filter { !it.isOpen }
        }

        networkAdapter.submitList(filteredNetworks.sortedByDescending { it.signalStrength })

        // Update title
        val filterText = when (currentFilter) {
            NetworkFilter.ALL -> "Tous"
            NetworkFilter.OPEN -> "Ouverts"
            NetworkFilter.SECURED -> "Sécurisés"
        }
        binding.textListTitle.text = "Réseaux $filterText (${filteredNetworks.size} disponibles)"
    }

    private fun updateRealtimeNetworks() {
        if (!hasAllPermissions()) return

        val scanResults = wifiHelper.getScanResults()
        realtimeNetworkBssids = scanResults.map { it.BSSID }.toSet()

        // Update display count
        binding.textNetworkCount.text = "Visibles: ${scanResults.size}"
        binding.textOpenCount.text = "Ouverts: ${scanResults.count { wifiHelper.isOpenNetwork(it) }}"

        applyFilterAndDisplay()
    }

    private fun updateStats(networks: List<WiFiNetwork>) {
        binding.textTotalHistory.text = "Total: ${networks.size}"
    }

    private fun updateGpsDisplay(latitude: Double, longitude: Double) {
        runOnUiThread {
            binding.textGpsLocation.text = String.format("%.4f, %.4f", latitude, longitude)
        }
    }

    private fun showNetworkDetails(network: WiFiNetwork) {
        val message = buildString {
            append("SSID: ${network.ssid}\n")
            append("BSSID: ${network.bssid}\n")
            append("Sécurité: ${if (network.isOpen) "OUVERT" else network.securityType}\n")
            append("Signal: ${network.signalStrength} dBm\n")
            append("Fréquence: ${network.frequencyBand}\n")
            if (network.latitude != null && network.longitude != null) {
                append("GPS: ${String.format("%.6f", network.latitude)}, ${String.format("%.6f", network.longitude)}\n")
            }
            if (network.connectionSuccessful) {
                append("\n✓ Connexion réussie précédemment")
            }
        }

        AlertDialog.Builder(this)
            .setTitle(network.ssid)
            .setMessage(message)
            .setPositiveButton("Fermer", null)
            .apply {
                if (network.isOpen) {
                    setNeutralButton("Connecter") { _, _ ->
                        viewModel.connectToNetwork(network.ssid, network.bssid, wifiHelper)
                    }
                }
            }
            .show()
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun showPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Permissions requises")
            .setMessage("Cette application nécessite les permissions de localisation pour scanner les réseaux Wi-Fi et enregistrer les coordonnées GPS.")
            .setPositiveButton("Paramètres") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun startScanService() {
        WiFiScanService.startService(this)
        binding.switchService.isChecked = true
        Toast.makeText(this, "Service de scan démarré", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanService() {
        WiFiScanService.stopService(this)
        binding.switchService.isChecked = false
        Toast.makeText(this, "Service de scan arrêté", Toast.LENGTH_SHORT).show()
    }

    private fun performManualScan() {
        if (wifiHelper.isWifiEnabled()) {
            wifiHelper.startScan()
            Toast.makeText(this, "Scan en cours...", Toast.LENGTH_SHORT).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Wi-Fi désactivé")
                .setMessage("Voulez-vous activer le Wi-Fi?")
                .setPositiveButton("Oui") { _, _ ->
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                .setNegativeButton("Non", null)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_logs -> {
                startActivity(Intent(this, LogActivity::class.java))
                true
            }
            R.id.action_triangulation -> {
                startActivity(Intent(this, TriangulationActivity::class.java))
                true
            }
            R.id.action_stats -> {
                startActivity(Intent(this, StatsActivity::class.java))
                true
            }
            R.id.action_export -> {
                startActivity(Intent(this, ExportActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateConnectionStatus() {
        val connectionInfo = wifiHelper.getConnectionInfo()
        val ssid = connectionInfo["ssid"] as? String
        val status = if (ssid != null && ssid != "<unknown ssid>" && ssid.isNotBlank()) {
            "Connecté: $ssid"
        } else {
            "Non connecté"
        }
        binding.textConnectionStatus.text = status
    }
}
