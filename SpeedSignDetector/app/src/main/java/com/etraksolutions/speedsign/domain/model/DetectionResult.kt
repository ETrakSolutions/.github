package com.etraksolutions.speedsign.domain.model

import android.graphics.RectF

/**
 * Represents the result of a sign detection operation.
 */
sealed class DetectionResult {
    /**
     * No sign was detected in the current frame.
     */
    data object NoDetection : DetectionResult()

    /**
     * A speed sign was successfully detected.
     */
    data class Success(val speedSign: SpeedSign) : DetectionResult()

    /**
     * Detection failed with an error.
     */
    data class Failure(val error: Throwable) : DetectionResult()
}

/**
 * Represents a text region detected in an image.
 */
data class TextRegion(
    val text: String,
    val boundingBox: RectF,
    val confidence: Float
)

/**
 * Represents analysis results from image processing.
 */
data class ImageAnalysisResult(
    val textRegions: List<TextRegion>,
    val potentialSignRegions: List<RectF>,
    val processingTimeMs: Long
)

/**
 * Configuration for the detection algorithm.
 */
data class DetectionConfig(
    val minConfidence: Float = 0.6f,
    val enableOverlay: Boolean = true,
    val targetSpeedLimits: Set<Int> = SpeedSign.VALID_QUEBEC_SPEED_LIMITS,
    val processingInterval: Long = 100L // milliseconds between frame processing
)
