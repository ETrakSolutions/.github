package ca.etrak.wifiautoconnect.ui

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import ca.etrak.wifiautoconnect.WiFiAutoConnectApp
import ca.etrak.wifiautoconnect.databinding.ActivityExportBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExportBinding
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Exporter"

        loadDataSummary()
        setupButtons()
    }

    private fun loadDataSummary() {
        val app = application as WiFiAutoConnectApp
        val repository = app.repository

        lifecycleScope.launch {
            val networkCount = withContext(Dispatchers.IO) { repository.getNetworkCount() }
            val networksWithGps = withContext(Dispatchers.IO) {
                app.database.wifiDao().getNetworksWithLocationSync().size
            }
            val logCount = withContext(Dispatchers.IO) { repository.getLogCount() }

            binding.textDataSummary.text = "$networkCount réseaux ($networksWithGps avec GPS)\n$logCount entrées journal"
        }
    }

    private fun setupButtons() {
        binding.buttonExportCsv.setOnClickListener {
            exportData("csv", save = true)
        }

        binding.buttonExportJson.setOnClickListener {
            exportData("json", save = true)
        }

        binding.buttonShareCsv.setOnClickListener {
            exportData("csv", save = false)
        }

        binding.buttonShareJson.setOnClickListener {
            exportData("json", save = false)
        }
    }

    private fun exportData(format: String, save: Boolean) {
        val app = application as WiFiAutoConnectApp
        val repository = app.repository

        binding.textStatus.text = "Export en cours..."

        lifecycleScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    if (format == "csv") repository.exportToCsv()
                    else repository.exportToJson()
                }

                val filename = "etrak_wifi_${dateFormat.format(Date())}.$format"

                if (save) {
                    // Save to Downloads folder
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, filename)
                    file.writeText(data)

                    binding.textStatus.text = "Fichier sauvegardé: $filename"
                    Toast.makeText(this@ExportActivity, "Export réussi: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                } else {
                    // Share via intent
                    shareData(data, filename, format)
                }

            } catch (e: Exception) {
                binding.textStatus.text = "Erreur: ${e.message}"
                Toast.makeText(this@ExportActivity, "Échec de l'export: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun shareData(data: String, filename: String, format: String) {
        try {
            // Create temp file in cache
            val cacheDir = File(cacheDir, "exports")
            cacheDir.mkdirs()
            val file = File(cacheDir, filename)
            file.writeText(data)

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val mimeType = if (format == "csv") "text/csv" else "application/json"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "E-Trak WiFi Scanner Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Partager via"))
            binding.textStatus.text = "Partage en cours..."

        } catch (e: Exception) {
            binding.textStatus.text = "Erreur partage: ${e.message}"
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
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
