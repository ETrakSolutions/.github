package ca.etrak.wifiautoconnect.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import ca.etrak.wifiautoconnect.databinding.ActivityMainBinding
import ca.etrak.wifiautoconnect.service.WiFiScanService
import ca.etrak.wifiautoconnect.util.PreferencesManager
import ca.etrak.wifiautoconnect.util.WiFiHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var networkAdapter: NetworkAdapter
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var wifiHelper: WiFiHelper

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
        } else {
            showPermissionExplanation()
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

        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(app.repository)
        )[MainViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupButtons()

        checkAndRequestPermissions()
    }

    private fun setupRecyclerView() {
        networkAdapter = NetworkAdapter { network ->
            // On click, try to connect
            if (network.isOpen) {
                viewModel.connectToNetwork(network.ssid, network.bssid, wifiHelper)
            } else {
                Toast.makeText(this, "Ce réseau n'est pas ouvert", Toast.LENGTH_SHORT).show()
            }
        }

        binding.recyclerNetworks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = networkAdapter
        }
    }

    private fun setupObservers() {
        viewModel.allNetworks.observe(this) { networks ->
            networkAdapter.submitList(networks)
            binding.textNetworkCount.text = "Réseaux détectés: ${networks.size}"

            val openCount = networks.count { it.isOpen }
            binding.textOpenCount.text = "Réseaux ouverts: $openCount"
        }

        viewModel.connectionStatus.observe(this) { status ->
            binding.textConnectionStatus.text = status
        }
    }

    private fun setupButtons() {
        binding.switchService.isChecked = preferencesManager.serviceRunning

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

        binding.switchAutoConnect.isChecked = preferencesManager.autoConnectEnabled

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

        binding.buttonViewLogs.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
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
            .setMessage("Cette application nécessite les permissions de localisation pour scanner les réseaux Wi-Fi. Veuillez accorder les permissions dans les paramètres.")
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.switchService.isChecked = preferencesManager.serviceRunning
        updateConnectionStatus()
    }

    private fun updateConnectionStatus() {
        val connectionInfo = wifiHelper.getConnectionInfo()
        val ssid = connectionInfo["ssid"] as? String
        val status = if (ssid != null && ssid != "<unknown ssid>") {
            "Connecté à: $ssid"
        } else {
            "Non connecté"
        }
        binding.textConnectionStatus.text = status
    }
}
