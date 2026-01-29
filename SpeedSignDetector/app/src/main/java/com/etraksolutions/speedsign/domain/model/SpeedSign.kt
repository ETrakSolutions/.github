package com.etraksolutions.speedsign.domain.model

import android.graphics.RectF

/**
 * Represents a detected speed limit sign.
 *
 * @property speedLimit The detected speed limit value in km/h
 * @property confidence Detection confidence score (0.0 to 1.0)
 * @property boundingBox The bounding box of the detected sign in the image
 * @property timestamp Time when the sign was detected (Unix timestamp in milliseconds)
 */
data class SpeedSign(
    val speedLimit: Int,
    val confidence: Float,
    val boundingBox: RectF,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Returns true if this is a valid Quebec speed limit value.
     * Common speed limits in Quebec: 30, 40, 50, 60, 70, 80, 90, 100, 110 km/h
     */
    val isValidQuebecSpeedLimit: Boolean
        get() = speedLimit in VALID_QUEBEC_SPEED_LIMITS

    /**
     * Returns true if the detection confidence is high enough to be reliable.
     */
    val isHighConfidence: Boolean
        get() = confidence >= HIGH_CONFIDENCE_THRESHOLD

    companion object {
        val VALID_QUEBEC_SPEED_LIMITS = setOf(30, 40, 50, 60, 70, 80, 90, 100, 110)
        const val HIGH_CONFIDENCE_THRESHOLD = 0.75f
    }
}

/**
 * Represents different types of road signs for future expansion.
 */
sealed class RoadSign {
    /**
     * Speed limit sign with detected value.
     */
    data class SpeedLimit(val sign: SpeedSign) : RoadSign()

    /**
     * Stop sign (future implementation).
     */
    data object Stop : RoadSign()

    /**
     * Yield sign (future implementation).
     */
    data object Yield : RoadSign()

    /**
     * Custom sign with text (for future logo + text detection).
     */
    data class CustomWithText(
        val logoType: String,
        val text: String,
        val boundingBox: RectF,
        val confidence: Float
    ) : RoadSign()
}

/**
 * Represents the current detection state.
 */
sealed class DetectionState {
    /**
     * Initial state, detection not yet started.
     */
    data object Idle : DetectionState()

    /**
     * Detection is active but no sign currently detected.
     */
    data object Scanning : DetectionState()

    /**
     * A speed sign has been detected.
     */
    data class Detected(val speedSign: SpeedSign) : DetectionState()

    /**
     * An error occurred during detection.
     */
    data class Error(val message: String) : DetectionState()
}
