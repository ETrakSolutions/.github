package com.etraksolutions.speedsign.domain.model

/**
 * Application settings model.
 */
data class AppSettings(
    // Detection toggles
    val detectSpeedSigns: Boolean = true,
    val detectStopSigns: Boolean = true,
    val detectNumericText: Boolean = true,
    val detectAllSigns: Boolean = false,
    val detectText: Boolean = false,

    // Visual settings
    val showDetectionBoxes: Boolean = true,
    val boxColorSpeed: Long = 0xFF4CAF50,      // Green
    val boxColorStop: Long = 0xFFF44336,       // Red
    val boxColorText: Long = 0xFF2196F3,       // Blue
    val boxColorOther: Long = 0xFFFF9800,      // Orange

    // Camera settings
    val cameraZoom: Float = 1f,
    val cameraZoomMin: Float = 1f,
    val cameraZoomMax: Float = 10f,

    // Processing settings
    val processingIntervalMs: Long = 100L,
    val minProcessingInterval: Long = 50L,
    val maxProcessingInterval: Long = 500L,
    val minConfidence: Float = 0.6f,

    // UI settings
    val showFps: Boolean = false,
    val showDebugInfo: Boolean = false,
    val hapticFeedback: Boolean = true,
    val soundAlerts: Boolean = false
)

/**
 * Types of detectable objects.
 */
enum class DetectionType {
    SPEED_SIGN,
    STOP_SIGN,
    YIELD_SIGN,
    NUMERIC_TEXT,
    GENERAL_TEXT,
    UNKNOWN_SIGN
}

/**
 * Detected object with visual info.
 */
data class DetectedObject(
    val type: DetectionType,
    val text: String,
    val confidence: Float,
    val boundingBox: android.graphics.RectF,
    val timestamp: Long = System.currentTimeMillis(),
    val color: Long = 0xFFFFFFFF
)

/**
 * Detection statistics.
 */
data class DetectionStats(
    val fps: Float = 0f,
    val processingTimeMs: Long = 0L,
    val totalDetections: Int = 0,
    val speedSignsDetected: Int = 0,
    val stopSignsDetected: Int = 0,
    val textDetected: Int = 0
)
