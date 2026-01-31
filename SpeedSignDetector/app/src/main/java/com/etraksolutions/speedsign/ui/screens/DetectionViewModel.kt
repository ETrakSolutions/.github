package com.etraksolutions.speedsign.ui.screens

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etraksolutions.speedsign.domain.model.DetectionConfig
import com.etraksolutions.speedsign.domain.model.DetectionResult
import com.etraksolutions.speedsign.domain.model.DetectionState
import com.etraksolutions.speedsign.domain.model.SpeedSign
import com.etraksolutions.speedsign.domain.usecase.DetectSpeedSignUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the detection screen.
 */
data class DetectionUiState(
    val detectionState: DetectionState = DetectionState.Idle,
    val currentSpeed: Int? = null,
    val boundingBox: RectF? = null,
    val showOverlay: Boolean = true,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val detectionHistory: List<DetectionResult> = emptyList()
)

/**
 * ViewModel for the detection screen.
 *
 * Manages the detection state, camera frame processing, and UI state updates.
 */
@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val detectSpeedSignUseCase: DetectSpeedSignUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectionUiState())
    val uiState: StateFlow<DetectionUiState> = _uiState.asStateFlow()

    private var detectionJob: Job? = null
    private var lastDetectionTime = 0L
    private var noDetectionCount = 0

    private val config = DetectionConfig(
        minConfidence = 0.6f,
        enableOverlay = true,
        processingInterval = 150L
    )

    /**
     * Starts the detection process with a flow of camera frames.
     */
    fun startDetection(frameFlow: Flow<Bitmap>) {
        stopDetection()

        _uiState.update { it.copy(detectionState = DetectionState.Scanning) }

        detectionJob = viewModelScope.launch {
            detectSpeedSignUseCase.continuous(frameFlow, config).collect { result ->
                handleDetectionResult(result)
            }
        }
    }

    /**
     * Processes a single frame for detection.
     */
    fun processFrame(bitmap: Bitmap) {
        val currentTime = System.currentTimeMillis()

        // Throttle processing
        if (currentTime - lastDetectionTime < config.processingInterval) {
            return
        }

        if (_uiState.value.isProcessing) {
            return
        }

        lastDetectionTime = currentTime

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val result = detectSpeedSignUseCase(bitmap, config)
            handleDetectionResult(result)

            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    /**
     * Handles the result of a detection attempt.
     */
    private fun handleDetectionResult(result: DetectionResult) {
        when (result) {
            is DetectionResult.Success -> {
                noDetectionCount = 0
                val sign = result.speedSign

                _uiState.update { currentState ->
                    val newHistory = (listOf(result) + currentState.detectionHistory).take(10)

                    currentState.copy(
                        detectionState = DetectionState.Detected(sign),
                        currentSpeed = sign.speedLimit,
                        boundingBox = sign.boundingBox,
                        errorMessage = null,
                        detectionHistory = newHistory
                    )
                }

                // Schedule return to scanning state after a delay if no new detection
                viewModelScope.launch {
                    delay(2000)
                    if (_uiState.value.currentSpeed == sign.speedLimit) {
                        // Only clear if we're still showing the same detection
                        noDetectionCount++
                        if (noDetectionCount > 5) {
                            _uiState.update {
                                it.copy(
                                    detectionState = DetectionState.Scanning,
                                    currentSpeed = null,
                                    boundingBox = null
                                )
                            }
                        }
                    }
                }
            }

            is DetectionResult.NoDetection -> {
                noDetectionCount++

                // Only transition to scanning after several frames with no detection
                if (noDetectionCount > 10) {
                    _uiState.update {
                        if (it.detectionState is DetectionState.Detected) {
                            it.copy(
                                detectionState = DetectionState.Scanning,
                                boundingBox = null
                            )
                        } else {
                            it
                        }
                    }
                }
            }

            is DetectionResult.Failure -> {
                _uiState.update {
                    it.copy(
                        detectionState = DetectionState.Error(result.error.message ?: "Unknown error"),
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    /**
     * Stops the detection process.
     */
    fun stopDetection() {
        detectionJob?.cancel()
        detectionJob = null
        _uiState.update { it.copy(detectionState = DetectionState.Idle) }
    }

    /**
     * Toggles the detection overlay visibility.
     */
    fun toggleOverlay() {
        _uiState.update { it.copy(showOverlay = !it.showOverlay) }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Gets the most frequently detected speed in recent history.
     */
    fun getMostFrequentSpeed(): Int? {
        return _uiState.value.detectionHistory
            .filterIsInstance<DetectionResult.Success>()
            .groupingBy { it.speedSign.speedLimit }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    override fun onCleared() {
        super.onCleared()
        stopDetection()
    }
}
