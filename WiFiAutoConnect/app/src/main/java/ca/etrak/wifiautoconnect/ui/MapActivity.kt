package ca.etrak.wifiautoconnect.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import ca.etrak.wifiautoconnect.R
import ca.etrak.wifiautoconnect.WiFiAutoConnectApp
import ca.etrak.wifiautoconnect.data.WiFiNetwork
import ca.etrak.wifiautoconnect.databinding.ActivityMapBinding
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import kotlin.math.cos
import kotlin.math.sin

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private var allNetworks: List<WiFiNetwork> = emptyList()
    private var currentFilter: NetworkFilter = NetworkFilter.ALL
    private var currentGpsLocation: GeoPoint? = null

    enum class NetworkFilter { ALL, OPEN, SECURED }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid configuration
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Carte Wi-Fi"

        setupMap()
        setupFilterChips()
        getCurrentLocation()
        loadNetworkMarkers()
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)

            // Default to Montreal, will be updated with GPS
            controller.setCenter(GeoPoint(45.5017, -73.5673))
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = NetworkFilter.ALL
                updateMarkers()
            }
        }

        binding.chipOpen.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = NetworkFilter.OPEN
                updateMarkers()
            }
        }

        binding.chipSecured.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = NetworkFilter.SECURED
                updateMarkers()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentGpsLocation = GeoPoint(it.latitude, it.longitude)
                binding.mapView.controller.setCenter(currentGpsLocation)
                binding.mapView.controller.setZoom(16.0)
            }
        }
    }

    private fun loadNetworkMarkers() {
        val app = application as WiFiAutoConnectApp

        lifecycleScope.launch {
            allNetworks = withContext(Dispatchers.IO) {
                app.database.wifiDao().getNetworksWithLocationSync()
            }
            updateMarkers()
        }
    }

    private fun updateMarkers() {
        binding.mapView.overlays.clear()

        if (allNetworks.isEmpty()) {
            binding.textStats.text = "Aucun réseau avec coordonnées GPS"
            return
        }

        // Filter networks based on current selection
        val filteredNetworks = when (currentFilter) {
            NetworkFilter.ALL -> allNetworks
            NetworkFilter.OPEN -> allNetworks.filter { it.isOpen }
            NetworkFilter.SECURED -> allNetworks.filter { !it.isOpen }
        }

        var openCount = 0
        var securedCount = 0

        // Group networks by approximate location to handle overlapping markers
        val locationGroups = mutableMapOf<String, MutableList<WiFiNetwork>>()

        filteredNetworks.forEach { network ->
            val lat = network.latitude ?: return@forEach
            val lon = network.longitude ?: return@forEach

            // Round to 5 decimal places (~1 meter precision) to group nearby markers
            val key = "${String.format("%.5f", lat)}_${String.format("%.5f", lon)}"
            locationGroups.getOrPut(key) { mutableListOf() }.add(network)
        }

        // Add markers with offset for overlapping
        locationGroups.forEach { (_, networks) ->
            networks.forEachIndexed { index, network ->
                val lat = network.latitude ?: return@forEachIndexed
                val lon = network.longitude ?: return@forEachIndexed

                // Calculate offset for overlapping markers (spiral pattern)
                val offsetLat: Double
                val offsetLon: Double

                if (networks.size > 1 && index > 0) {
                    val angle = (index * 45.0) * (Math.PI / 180.0) // 45 degree increments
                    val distance = 0.00005 * ((index / 8) + 1) // Increase radius every 8 markers
                    offsetLat = lat + (distance * cos(angle))
                    offsetLon = lon + (distance * sin(angle))
                } else {
                    offsetLat = lat
                    offsetLon = lon
                }

                val marker = Marker(binding.mapView).apply {
                    position = GeoPoint(offsetLat, offsetLon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = network.ssid
                    snippet = buildString {
                        append(if (network.isOpen) "OUVERT" else network.securityType)
                        append(" | ${network.signalStrength} dBm")
                        append(" | ${network.frequencyBand}")
                        if (network.scanCount > 1) append(" | x${network.scanCount} scans")
                        if (network.connectionSuccessful) append(" | ✓ Connecté")
                    }

                    // Color based on open/secured
                    if (network.isOpen) {
                        openCount++
                        icon = ContextCompat.getDrawable(this@MapActivity, R.drawable.ic_marker_open)
                    } else {
                        securedCount++
                        icon = ContextCompat.getDrawable(this@MapActivity, R.drawable.ic_marker_secured)
                    }
                }

                binding.mapView.overlays.add(marker)
            }
        }

        // Center on GPS if available, otherwise on first network
        if (currentGpsLocation != null) {
            binding.mapView.controller.setCenter(currentGpsLocation)
        } else {
            filteredNetworks.firstOrNull()?.let { network ->
                if (network.latitude != null && network.longitude != null) {
                    binding.mapView.controller.setCenter(GeoPoint(network.latitude, network.longitude))
                }
            }
        }

        binding.mapView.invalidate()

        // Update stats
        val totalShown = openCount + securedCount
        binding.textStats.text = "Affichés: $totalShown | Ouverts: $openCount | Sécurisés: $securedCount"
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
