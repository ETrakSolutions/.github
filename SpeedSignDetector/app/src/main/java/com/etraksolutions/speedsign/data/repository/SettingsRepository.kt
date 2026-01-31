package com.etraksolutions.speedsign.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.etraksolutions.speedsign.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DETECT_SPEED_SIGNS = booleanPreferencesKey("detect_speed_signs")
        val DETECT_STOP_SIGNS = booleanPreferencesKey("detect_stop_signs")
        val DETECT_NUMERIC_TEXT = booleanPreferencesKey("detect_numeric_text")
        val DETECT_ALL_SIGNS = booleanPreferencesKey("detect_all_signs")
        val DETECT_TEXT = booleanPreferencesKey("detect_text")
        val DETECT_VEHICLES = booleanPreferencesKey("detect_vehicles")
        val SHOW_DETECTION_BOXES = booleanPreferencesKey("show_detection_boxes")
        val BOX_COLOR_SPEED = longPreferencesKey("box_color_speed")
        val BOX_COLOR_STOP = longPreferencesKey("box_color_stop")
        val BOX_COLOR_TEXT = longPreferencesKey("box_color_text")
        val BOX_COLOR_OTHER = longPreferencesKey("box_color_other")
        val CAMERA_ZOOM = floatPreferencesKey("camera_zoom")
        val PROCESSING_INTERVAL = longPreferencesKey("processing_interval")
        val MIN_CONFIDENCE = floatPreferencesKey("min_confidence")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val SHOW_DEBUG_INFO = booleanPreferencesKey("show_debug_info")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val SOUND_ALERTS = booleanPreferencesKey("sound_alerts")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            detectSpeedSigns = prefs[Keys.DETECT_SPEED_SIGNS] ?: true,
            detectStopSigns = prefs[Keys.DETECT_STOP_SIGNS] ?: true,
            detectNumericText = prefs[Keys.DETECT_NUMERIC_TEXT] ?: true,
            detectAllSigns = prefs[Keys.DETECT_ALL_SIGNS] ?: false,
            detectText = prefs[Keys.DETECT_TEXT] ?: false,
            detectVehicles = prefs[Keys.DETECT_VEHICLES] ?: false,
            showDetectionBoxes = prefs[Keys.SHOW_DETECTION_BOXES] ?: true,
            boxColorSpeed = prefs[Keys.BOX_COLOR_SPEED] ?: 0xFF4CAF50,
            boxColorStop = prefs[Keys.BOX_COLOR_STOP] ?: 0xFFF44336,
            boxColorText = prefs[Keys.BOX_COLOR_TEXT] ?: 0xFF2196F3,
            boxColorOther = prefs[Keys.BOX_COLOR_OTHER] ?: 0xFFFF9800,
            cameraZoom = prefs[Keys.CAMERA_ZOOM] ?: 1f,
            processingIntervalMs = prefs[Keys.PROCESSING_INTERVAL] ?: 100L,
            minConfidence = prefs[Keys.MIN_CONFIDENCE] ?: 0.5f,
            showFps = prefs[Keys.SHOW_FPS] ?: true,
            showDebugInfo = prefs[Keys.SHOW_DEBUG_INFO] ?: true,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            soundAlerts = prefs[Keys.SOUND_ALERTS] ?: false
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DETECT_SPEED_SIGNS] = settings.detectSpeedSigns
            prefs[Keys.DETECT_STOP_SIGNS] = settings.detectStopSigns
            prefs[Keys.DETECT_NUMERIC_TEXT] = settings.detectNumericText
            prefs[Keys.DETECT_ALL_SIGNS] = settings.detectAllSigns
            prefs[Keys.DETECT_TEXT] = settings.detectText
            prefs[Keys.DETECT_VEHICLES] = settings.detectVehicles
            prefs[Keys.SHOW_DETECTION_BOXES] = settings.showDetectionBoxes
            prefs[Keys.BOX_COLOR_SPEED] = settings.boxColorSpeed
            prefs[Keys.BOX_COLOR_STOP] = settings.boxColorStop
            prefs[Keys.BOX_COLOR_TEXT] = settings.boxColorText
            prefs[Keys.BOX_COLOR_OTHER] = settings.boxColorOther
            prefs[Keys.CAMERA_ZOOM] = settings.cameraZoom
            prefs[Keys.PROCESSING_INTERVAL] = settings.processingIntervalMs
            prefs[Keys.MIN_CONFIDENCE] = settings.minConfidence
            prefs[Keys.SHOW_FPS] = settings.showFps
            prefs[Keys.SHOW_DEBUG_INFO] = settings.showDebugInfo
            prefs[Keys.HAPTIC_FEEDBACK] = settings.hapticFeedback
            prefs[Keys.SOUND_ALERTS] = settings.soundAlerts
        }
    }
}
