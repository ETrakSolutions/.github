package ca.etrak.wifiautoconnect.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ca.etrak.wifiautoconnect.databinding.ActivitySettingsBinding
import ca.etrak.wifiautoconnect.util.PreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Paramètres"

        preferencesManager = PreferencesManager(this)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        binding.switchAutoConnect.isChecked = preferencesManager.autoConnectEnabled
        binding.switchAutoStart.isChecked = preferencesManager.autoStartOnBoot
        binding.switchNotifications.isChecked = preferencesManager.notifyOnConnection
        binding.switchConnectWhenDisconnected.isChecked = preferencesManager.connectOnlyWhenDisconnected

        binding.sliderMinSignal.value = preferencesManager.minSignalStrength.toFloat()
        binding.textMinSignalValue.text = "${preferencesManager.minSignalStrength} dBm"

        val intervalIndex = when (preferencesManager.scanInterval) {
            15000L -> 0
            30000L -> 1
            60000L -> 2
            120000L -> 3
            300000L -> 4
            else -> 1
        }
        binding.spinnerScanInterval.setSelection(intervalIndex)
    }

    private fun setupListeners() {
        binding.switchAutoConnect.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.autoConnectEnabled = isChecked
        }

        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.autoStartOnBoot = isChecked
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.notifyOnConnection = isChecked
        }

        binding.switchConnectWhenDisconnected.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.connectOnlyWhenDisconnected = isChecked
        }

        binding.sliderMinSignal.addOnChangeListener { _, value, _ ->
            val intValue = value.toInt()
            preferencesManager.minSignalStrength = intValue
            binding.textMinSignalValue.text = "$intValue dBm"
        }

        binding.buttonSave.setOnClickListener {
            saveIntervalSetting()
            Toast.makeText(this, "Paramètres enregistrés", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveIntervalSetting() {
        val interval = when (binding.spinnerScanInterval.selectedItemPosition) {
            0 -> 15000L
            1 -> 30000L
            2 -> 60000L
            3 -> 120000L
            4 -> 300000L
            else -> 30000L
        }
        preferencesManager.scanInterval = interval
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
