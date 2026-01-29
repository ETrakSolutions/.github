package ca.etrak.wifiautoconnect.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ca.etrak.wifiautoconnect.WiFiAutoConnectApp
import ca.etrak.wifiautoconnect.databinding.ActivityStatsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Statistiques"

        loadStats()
    }

    private fun loadStats() {
        val app = application as WiFiAutoConnectApp
        val repository = app.repository

        lifecycleScope.launch {
            // Load all stats from database
            val totalNetworks = withContext(Dispatchers.IO) { repository.getNetworkCount() }
            val openNetworks = withContext(Dispatchers.IO) { repository.getOpenNetworkCount() }
            val successfulConnections = withContext(Dispatchers.IO) { repository.getSuccessfulConnectionCount() }
            val logCount = withContext(Dispatchers.IO) { repository.getLogCount() }
            val totalScans = withContext(Dispatchers.IO) { repository.getTotalScans() }

            // Get networks with location
            val networksWithGps = withContext(Dispatchers.IO) {
                app.database.wifiDao().getNetworksWithLocationSync()
            }

            // Get all networks for frequency and security analysis
            val allNetworks = withContext(Dispatchers.IO) {
                app.database.wifiDao().getAllNetworksSync()
            }

            // Update UI
            binding.textTotalNetworks.text = totalNetworks.toString()
            binding.textOpenNetworks.text = openNetworks.toString()
            binding.textWithGps.text = networksWithGps.size.toString()
            binding.textTotalScans.text = totalScans.toString()
            binding.textSuccessfulConnections.text = successfulConnections.toString()
            binding.textLogCount.text = logCount.toString()

            // Frequency distribution
            val count24ghz = allNetworks.count { it.frequencyBand == "2.4 GHz" }
            val count5ghz = allNetworks.count { it.frequencyBand == "5 GHz" }
            binding.text24ghz.text = "$count24ghz (${if (totalNetworks > 0) count24ghz * 100 / totalNetworks else 0}%)"
            binding.text5ghz.text = "$count5ghz (${if (totalNetworks > 0) count5ghz * 100 / totalNetworks else 0}%)"

            // Security breakdown
            val securityMap = allNetworks.groupBy { it.securityType }
            val securityBreakdown = StringBuilder()
            securityMap.entries.sortedByDescending { it.value.size }.forEach { (security, networks) ->
                val percentage = if (totalNetworks > 0) networks.size * 100 / totalNetworks else 0
                securityBreakdown.appendLine("$security: ${networks.size} ($percentage%)")
            }
            binding.textSecurityBreakdown.text = if (securityBreakdown.isNotEmpty()) {
                securityBreakdown.toString().trim()
            } else {
                "Aucune donnée"
            }
        }
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
