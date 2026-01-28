package ca.etrak.wifiautoconnect.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ca.etrak.wifiautoconnect.WiFiAutoConnectApp
import ca.etrak.wifiautoconnect.databinding.ActivityLogBinding

class LogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding
    private lateinit var viewModel: LogViewModel
    private lateinit var logAdapter: LogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Historique des connexions"

        val app = application as WiFiAutoConnectApp
        viewModel = ViewModelProvider(
            this,
            LogViewModelFactory(app.repository)
        )[LogViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupButtons()
    }

    private fun setupRecyclerView() {
        logAdapter = LogAdapter()
        binding.recyclerLogs.apply {
            layoutManager = LinearLayoutManager(this@LogActivity)
            adapter = logAdapter
        }
    }

    private fun setupObservers() {
        viewModel.allLogs.observe(this) { logs ->
            logAdapter.submitList(logs)
            binding.textLogCount.text = "Total: ${logs.size} entrées"
        }
    }

    private fun setupButtons() {
        binding.buttonClearLogs.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Effacer les logs")
                .setMessage("Voulez-vous vraiment effacer tous les logs?")
                .setPositiveButton("Oui") { _, _ ->
                    viewModel.clearAllLogs()
                }
                .setNegativeButton("Non", null)
                .show()
        }

        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter(null)
        }

        binding.chipConnections.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter("CONNECTION")
        }

        binding.chipScans.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter("SCAN")
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
