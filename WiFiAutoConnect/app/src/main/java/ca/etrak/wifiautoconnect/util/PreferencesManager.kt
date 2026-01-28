package ca.etrak.wifiautoconnect.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "wifi_auto_connect_prefs"

        private const val KEY_AUTO_CONNECT_ENABLED = "auto_connect_enabled"
        private const val KEY_SCAN_INTERVAL = "scan_interval"
        private const val KEY_MIN_SIGNAL_STRENGTH = "min_signal_strength"
        private const val KEY_AUTO_START_ON_BOOT = "auto_start_on_boot"
        private const val KEY_NOTIFY_ON_CONNECTION = "notify_on_connection"
        private const val KEY_LOG_RETENTION_DAYS = "log_retention_days"
        private const val KEY_SERVICE_RUNNING = "service_running"
        private const val KEY_CONNECT_ONLY_WHEN_DISCONNECTED = "connect_only_when_disconnected"

        const val DEFAULT_SCAN_INTERVAL = 30000L  // 30 seconds
        const val DEFAULT_MIN_SIGNAL_STRENGTH = -75  // dBm
        const val DEFAULT_LOG_RETENTION_DAYS = 30
    }

    var autoConnectEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT_ENABLED, value).apply()

    var scanInterval: Long
        get() = prefs.getLong(KEY_SCAN_INTERVAL, DEFAULT_SCAN_INTERVAL)
        set(value) = prefs.edit().putLong(KEY_SCAN_INTERVAL, value).apply()

    var minSignalStrength: Int
        get() = prefs.getInt(KEY_MIN_SIGNAL_STRENGTH, DEFAULT_MIN_SIGNAL_STRENGTH)
        set(value) = prefs.edit().putInt(KEY_MIN_SIGNAL_STRENGTH, value).apply()

    var autoStartOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_ON_BOOT, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START_ON_BOOT, value).apply()

    var notifyOnConnection: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ON_CONNECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_ON_CONNECTION, value).apply()

    var logRetentionDays: Int
        get() = prefs.getInt(KEY_LOG_RETENTION_DAYS, DEFAULT_LOG_RETENTION_DAYS)
        set(value) = prefs.edit().putInt(KEY_LOG_RETENTION_DAYS, value).apply()

    var serviceRunning: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_RUNNING, value).apply()

    var connectOnlyWhenDisconnected: Boolean
        get() = prefs.getBoolean(KEY_CONNECT_ONLY_WHEN_DISCONNECTED, true)
        set(value) = prefs.edit().putBoolean(KEY_CONNECT_ONLY_WHEN_DISCONNECTED, value).apply()
}
