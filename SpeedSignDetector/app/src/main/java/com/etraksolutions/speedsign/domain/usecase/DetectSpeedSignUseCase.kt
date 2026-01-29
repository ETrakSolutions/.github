package com.etraksolutions.speedsign.domain.usecase

import android.graphics.Bitmap
import com.etraksolutions.speedsign.domain.model.DetectionConfig
import com.etraksolutions.speedsign.domain.model.DetectionResult
import com.etraksolutions.speedsign.domain.repository.DetectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for detecting speed signs in images.
 *
 * This use case encapsulates the business logic for speed sign detection,
 * providing a clean API for the presentation layer.
 */
class DetectSpeedSignUseCase @Inject constructor(
    private val detectionRepository: DetectionRepository
) {
    /**
     * Detects a speed sign in a single image frame.
     *
     * @param bitmap The image to analyze
     * @param config Optional detection configuration
     * @return The detection result
     */
    suspend operator fun invoke(
        bitmap: Bitmap,
        config: DetectionConfig = DetectionConfig()
    ): DetectionResult {
        return detectionRepository.detectSpeedSign(bitmap, config)
    }

    /**
     * Starts continuous detection from a flow of camera frames.
     *
     * @param frameFlow Flow of bitmap frames
     * @param config Detection configuration
     * @return Flow of detection results
     */
    fun continuous(
        frameFlow: Flow<Bitmap>,
        config: DetectionConfig = DetectionConfig()
    ): Flow<DetectionResult> {
        return detectionRepository.startContinuousDetection(frameFlow, config)
    }
}
