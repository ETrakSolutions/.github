package com.etraksolutions.speedsign.domain.repository

import android.graphics.Bitmap
import com.etraksolutions.speedsign.domain.model.DetectionConfig
import com.etraksolutions.speedsign.domain.model.DetectionResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for sign detection operations.
 *
 * This interface abstracts the detection implementation, allowing for
 * easy testing and potential swapping of detection backends (ML Kit, TFLite, etc.).
 */
interface DetectionRepository {

    /**
     * Processes a single frame and attempts to detect speed signs.
     *
     * @param bitmap The image frame to process
     * @param config Detection configuration parameters
     * @return DetectionResult indicating whether a sign was found
     */
    suspend fun detectSpeedSign(
        bitmap: Bitmap,
        config: DetectionConfig = DetectionConfig()
    ): DetectionResult

    /**
     * Starts continuous detection on a stream of frames.
     *
     * @param frameFlow Flow of bitmap frames from the camera
     * @param config Detection configuration parameters
     * @return Flow of detection results
     */
    fun startContinuousDetection(
        frameFlow: Flow<Bitmap>,
        config: DetectionConfig = DetectionConfig()
    ): Flow<DetectionResult>

    /**
     * Releases any resources held by the detection system.
     */
    fun release()
}
