package ca.etrak.wifiautoconnect.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import ca.etrak.wifiautoconnect.R
import ca.etrak.wifiautoconnect.WiFiAutoConnectApp
import ca.etrak.wifiautoconnect.data.SignalReading
import ca.etrak.wifiautoconnect.data.WiFiNetwork
import ca.etrak.wifiautoconnect.databinding.ActivityTriangulationBinding
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import kotlin.math.pow

class TriangulationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTriangulationBinding
    private var currentGpsLocation: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid configuration
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityTriangulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Triangulation Wi-Fi"

        setupMap()
        getCurrentLocation()
        loadTriangulationData()
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

    private fun loadTriangulationData() {
        val app = application as WiFiAutoConnectApp

        lifecycleScope.launch {
            val networks = withContext(Dispatchers.IO) {
                app.database.wifiDao().getNetworksForTriangulation()
            }

            if (networks.isEmpty()) {
                binding.textStats.text = "Pas assez de données pour triangulation. Scannez depuis plusieurs endroits."
                return@launch
            }

            var estimatedCount = 0
            var totalReadings = 0
            var firstPoint: GeoPoint? = null

            networks.forEach { network ->
                val readings = network.getSignalReadingsList()
                if (readings.size >= 2) {
                    // Estimate position using weighted centroid method
                    val estimatedPosition = calculateEstimatedPosition(readings)
                    estimatedPosition?.let { pos ->
                        if (firstPoint == null) firstPoint = pos

                        estimatedCount++
                        totalReadings += readings.size

                        // Add estimated position marker
                        val estimatedMarker = Marker(binding.mapView).apply {
                            position = pos
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "${network.ssid} (estimé)"
                            snippet = buildString {
                                append(if (network.isOpen) "OUVERT" else network.securityType)
                                append(" | ${readings.size} lectures")
                                append(" | Meilleur: ${network.bestSignalStrength} dBm")
                            }
                            icon = ContextCompat.getDrawable(this@TriangulationActivity, R.drawable.ic_marker_estimated)
                        }
                        binding.mapView.overlays.add(estimatedMarker)

                        // Draw uncertainty circle (larger for fewer readings or weaker signals)
                        val uncertainty = calculateUncertainty(readings)
                        val circle = createCircleOverlay(pos, uncertainty)
                        binding.mapView.overlays.add(circle)

                        // Add small markers for each reading point
                        readings.forEach { reading ->
                            val readingMarker = Marker(binding.mapView).apply {
                                position = GeoPoint(reading.latitude, reading.longitude)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = "Lecture: ${network.ssid}"
                                snippet = "${reading.signalStrength} dBm"
                                icon = ContextCompat.getDrawable(this@TriangulationActivity, R.drawable.ic_reading_point)
                            }
                            binding.mapView.overlays.add(readingMarker)
                        }
                    }
                }
            }

            // Center on GPS or first estimated point
            if (currentGpsLocation != null) {
                binding.mapView.controller.setCenter(currentGpsLocation)
            } else {
                firstPoint?.let {
                    binding.mapView.controller.setCenter(it)
                }
            }

            binding.mapView.invalidate()

            // Update stats
            binding.textStats.text = "Réseaux triangulés: $estimatedCount | Total lectures: $totalReadings"
        }
    }

    /**
     * Calculates estimated position using signal-strength weighted centroid method.
     * Stronger signals are given more weight as they indicate closer proximity.
     */
    private fun calculateEstimatedPosition(readings: List<SignalReading>): GeoPoint? {
        if (readings.isEmpty()) return null

        var weightedLatSum = 0.0
        var weightedLonSum = 0.0
        var totalWeight = 0.0

        readings.forEach { reading ->
            // Convert signal strength to weight (stronger signal = higher weight)
            // Signal ranges from -30 (excellent) to -90 (poor)
            // Convert to positive weight: -30 dBm -> weight ~1000, -90 dBm -> weight ~1
            val weight = 10.0.pow((reading.signalStrength + 100) / 20.0)

            weightedLatSum += reading.latitude * weight
            weightedLonSum += reading.longitude * weight
            totalWeight += weight
        }

        return if (totalWeight > 0) {
            GeoPoint(weightedLatSum / totalWeight, weightedLonSum / totalWeight)
        } else null
    }

    /**
     * Calculates uncertainty radius based on number of readings and signal quality.
     * Returns radius in degrees (approximately meters / 111000)
     */
    private fun calculateUncertainty(readings: List<SignalReading>): Double {
        val avgSignal = readings.map { it.signalStrength }.average()

        // Base uncertainty: 50m for excellent signal, up to 200m for poor
        val signalFactor = when {
            avgSignal >= -50 -> 0.00045  // ~50m
            avgSignal >= -60 -> 0.0007   // ~80m
            avgSignal >= -70 -> 0.001    // ~110m
            avgSignal >= -80 -> 0.0015   // ~170m
            else -> 0.002                // ~220m
        }

        // Reduce uncertainty with more readings
        val readingsFactor = when {
            readings.size >= 10 -> 0.5
            readings.size >= 5 -> 0.7
            readings.size >= 3 -> 0.85
            else -> 1.0
        }

        return signalFactor * readingsFactor
    }

    private fun createCircleOverlay(center: GeoPoint, radius: Double): Polygon {
        val points = ArrayList<GeoPoint>()
        val segments = 36

        for (i in 0 until segments) {
            val angle = i * (360.0 / segments) * (Math.PI / 180.0)
            val lat = center.latitude + radius * kotlin.math.cos(angle)
            val lon = center.longitude + radius * kotlin.math.sin(angle)
            points.add(GeoPoint(lat, lon))
        }
        points.add(points[0]) // Close the circle

        return Polygon().apply {
            this.points = points
            fillPaint.color = Color.argb(50, 21, 101, 192) // Semi-transparent blue
            outlinePaint.color = Color.argb(150, 21, 101, 192)
            outlinePaint.strokeWidth = 2f
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
