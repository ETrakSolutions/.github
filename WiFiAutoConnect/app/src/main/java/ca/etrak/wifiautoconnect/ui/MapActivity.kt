package ca.etrak.wifiautoconnect.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import ca.etrak.wifiautoconnect.R
import ca.etrak.wifiautoconnect.WiFiAutoConnectApp
import ca.etrak.wifiautoconnect.databinding.ActivityMapBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding

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
        loadNetworkMarkers()
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)

            // Default to a central position (will be updated with actual data)
            controller.setCenter(GeoPoint(45.5017, -73.5673)) // Montreal default
        }
    }

    private fun loadNetworkMarkers() {
        val app = application as WiFiAutoConnectApp

        lifecycleScope.launch {
            val networks = withContext(Dispatchers.IO) {
                app.database.wifiDao().getNetworksWithLocationSync()
            }

            if (networks.isEmpty()) {
                binding.textStats.text = "Aucun réseau avec coordonnées GPS"
                return@launch
            }

            var openCount = 0
            var securedCount = 0
            var firstPoint: GeoPoint? = null

            networks.forEach { network ->
                val lat = network.latitude ?: return@forEach
                val lon = network.longitude ?: return@forEach

                if (firstPoint == null) {
                    firstPoint = GeoPoint(lat, lon)
                }

                val marker = Marker(binding.mapView).apply {
                    position = GeoPoint(lat, lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = network.ssid
                    snippet = buildString {
                        append(if (network.isOpen) "OUVERT" else network.securityType)
                        append(" | ${network.signalStrength} dBm")
                        append(" | ${network.frequencyBand}")
                        if (network.connectionSuccessful) append(" | Connecté")
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

            // Center map on first network found
            firstPoint?.let {
                binding.mapView.controller.setCenter(it)
            }

            binding.mapView.invalidate()

            // Update stats
            binding.textStats.text = "Total: ${networks.size} | Ouverts: $openCount | Sécurisés: $securedCount"
        }
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
