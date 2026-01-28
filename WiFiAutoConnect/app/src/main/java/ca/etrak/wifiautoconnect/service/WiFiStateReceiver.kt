package ca.etrak.wifiautoconnect.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log

class WiFiStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WiFiStateReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                Log.d(TAG, "Scan results available")
            }
            WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                Log.d(TAG, "Network state changed")
            }
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                Log.d(TAG, "Connectivity changed")
            }
        }
    }
}
