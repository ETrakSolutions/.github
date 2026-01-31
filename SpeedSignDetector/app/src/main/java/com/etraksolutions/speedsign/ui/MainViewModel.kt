package com.etraksolutions.speedsign.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etraksolutions.speedsign.data.repository.SettingsRepository
import com.etraksolutions.speedsign.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel that manages app-wide settings.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(newSettings)
        }
    }

    fun updateDetectSpeedSigns(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDetectSpeedSigns(enabled)
        }
    }

    fun updateDetectStopSigns(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDetectStopSigns(enabled)
        }
    }

    fun updateDetectNumericText(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDetectNumericText(enabled)
        }
    }

    fun updateDetectAllSigns(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDetectAllSigns(enabled)
        }
    }

    fun updateDetectText(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDetectText(enabled)
        }
    }

    fun updateShowDetectionBoxes(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowDetectionBoxes(enabled)
        }
    }

    fun updateCameraZoom(zoom: Float) {
        viewModelScope.launch {
            settingsRepository.updateCameraZoom(zoom)
        }
    }

    fun updateProcessingInterval(intervalMs: Long) {
        viewModelScope.launch {
            settingsRepository.updateProcessingInterval(intervalMs)
        }
    }

    fun updateMinConfidence(confidence: Float) {
        viewModelScope.launch {
            settingsRepository.updateMinConfidence(confidence)
        }
    }

    fun updateShowFps(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowFps(enabled)
        }
    }

    fun updateShowDebugInfo(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowDebugInfo(enabled)
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHapticFeedback(enabled)
        }
    }

    fun updateSoundAlerts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSoundAlerts(enabled)
        }
    }
}
