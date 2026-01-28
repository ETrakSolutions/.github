package ca.etrak.wifiautoconnect.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ca.etrak.wifiautoconnect.util.PreferencesManager

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed received")

            val preferencesManager = PreferencesManager(context)

            if (preferencesManager.autoStartOnBoot) {
                Log.d(TAG, "Starting WiFi scan service on boot")
                WiFiScanService.startService(context)
            }
        }
    }
}
