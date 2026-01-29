package com.etraksolutions.speedsign.data.repository

import android.graphics.Bitmap
import com.etraksolutions.speedsign.data.detection.SpeedSignDetector
import com.etraksolutions.speedsign.domain.model.DetectionConfig
import com.etraksolutions.speedsign.domain.model.DetectionResult
import com.etraksolutions.speedsign.domain.repository.DetectionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DetectionRepository using ML Kit.
 *
 * This repository handles the detection of speed signs using the
 * SpeedSignDetector and provides both single-frame and continuous
 * detection capabilities.
 */
@Singleton
class DetectionRepositoryImpl @Inject constructor(
    private val speedSignDetector: SpeedSignDetector
) : DetectionRepository {

    private var lastProcessingTime = 0L

    override suspend fun detectSpeedSign(
        bitmap: Bitmap,
        config: DetectionConfig
    ): DetectionResult {
        return speedSignDetector.detectSpeedSign(bitmap, config)
    }

    override fun startContinuousDetection(
        frameFlow: Flow<Bitmap>,
        config: DetectionConfig
    ): Flow<DetectionResult> {
        return frameFlow
            .onEach { frame ->
                // Throttle processing to avoid overwhelming the detector
                val currentTime = System.currentTimeMillis()
                val timeSinceLastProcess = currentTime - lastProcessingTime

                if (timeSinceLastProcess < config.processingInterval) {
                    delay(config.processingInterval - timeSinceLastProcess)
                }
                lastProcessingTime = System.currentTimeMillis()
            }
            .map { frame ->
                speedSignDetector.detectSpeedSign(frame, config)
            }
            .catch { e ->
                emit(DetectionResult.Failure(e))
            }
    }

    override fun release() {
        speedSignDetector.release()
    }
}
