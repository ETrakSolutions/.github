package com.etraksolutions.speedsign.data.detection

import android.graphics.Bitmap
import android.graphics.RectF
import com.etraksolutions.speedsign.domain.model.DetectionConfig
import com.etraksolutions.speedsign.domain.model.DetectionResult
import com.etraksolutions.speedsign.domain.model.SpeedSign
import com.etraksolutions.speedsign.domain.model.TextRegion
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Speed sign detector using ML Kit Text Recognition.
 *
 * This class processes camera frames to detect Quebec speed limit signs.
 * It uses OCR to read numbers and validates them against known speed limits.
 *
 * Quebec speed signs typically:
 * - Have white background with black border
 * - Display "MAXIMUM" text above the number
 * - Show speed in km/h (30, 40, 50, 60, 70, 80, 90, 100, 110)
 */
@Singleton
class SpeedSignDetector @Inject constructor() {

    private val textRecognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.Builder().build()
    )

    // Regex pattern to match speed limit numbers
    private val speedNumberPattern = Regex("""\b(30|40|50|60|70|80|90|100|110)\b""")

    // Pattern to identify "MAXIMUM" text which often appears on Quebec signs
    private val maximumPattern = Regex("""MAXIMUM|MAX|LIMITE""", RegexOption.IGNORE_CASE)

    /**
     * Processes a bitmap image and attempts to detect speed signs.
     *
     * @param bitmap The image to process
     * @param config Detection configuration
     * @return DetectionResult with the detected sign or no detection
     */
    suspend fun detectSpeedSign(
        bitmap: Bitmap,
        config: DetectionConfig
    ): DetectionResult = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val textResult = recognizeText(inputImage)

            // Extract all text regions from the result
            val textRegions = extractTextRegions(textResult)

            // Find speed numbers in the detected text
            val speedDetection = findSpeedSign(textRegions, config)

            speedDetection ?: DetectionResult.NoDetection
        } catch (e: Exception) {
            DetectionResult.Failure(e)
        }
    }

    /**
     * Performs OCR on the input image using ML Kit.
     */
    private suspend fun recognizeText(inputImage: InputImage): com.google.mlkit.vision.text.Text {
        return suspendCancellableCoroutine { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    continuation.resume(text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    /**
     * Extracts text regions from ML Kit result.
     */
    private fun extractTextRegions(text: com.google.mlkit.vision.text.Text): List<TextRegion> {
        val regions = mutableListOf<TextRegion>()

        for (block in text.textBlocks) {
            for (line in block.lines) {
                val boundingBox = line.boundingBox ?: continue
                regions.add(
                    TextRegion(
                        text = line.text,
                        boundingBox = RectF(boundingBox),
                        confidence = line.confidence ?: 0.5f
                    )
                )

                // Also check individual elements for numbers
                for (element in line.elements) {
                    val elementBox = element.boundingBox ?: continue
                    regions.add(
                        TextRegion(
                            text = element.text,
                            boundingBox = RectF(elementBox),
                            confidence = element.confidence ?: 0.5f
                        )
                    )
                }
            }
        }

        return regions
    }

    /**
     * Finds a valid speed sign from the detected text regions.
     *
     * The algorithm:
     * 1. Look for "MAXIMUM" or similar keywords
     * 2. Find numbers that match valid Quebec speed limits
     * 3. Prefer numbers near "MAXIMUM" text
     * 4. Calculate confidence based on context
     */
    private fun findSpeedSign(
        textRegions: List<TextRegion>,
        config: DetectionConfig
    ): DetectionResult.Success? {
        // Find regions containing "MAXIMUM" or similar
        val maximumRegions = textRegions.filter { maximumPattern.containsMatchIn(it.text) }

        // Find all speed number candidates
        val speedCandidates = mutableListOf<SpeedCandidate>()

        for (region in textRegions) {
            val match = speedNumberPattern.find(region.text)
            if (match != null) {
                val speedValue = match.value.toIntOrNull() ?: continue

                // Check if this speed is in our valid set
                if (speedValue !in config.targetSpeedLimits) continue

                // Calculate confidence boost if near a "MAXIMUM" label
                val hasMaximumNearby = maximumRegions.any { maxRegion ->
                    isNearby(maxRegion.boundingBox, region.boundingBox)
                }

                val confidenceBoost = if (hasMaximumNearby) 0.2f else 0f
                val totalConfidence = (region.confidence + confidenceBoost).coerceAtMost(1f)

                speedCandidates.add(
                    SpeedCandidate(
                        speed = speedValue,
                        confidence = totalConfidence,
                        boundingBox = region.boundingBox,
                        hasMaximumLabel = hasMaximumNearby
                    )
                )
            }
        }

        // Select the best candidate based on confidence and context
        val bestCandidate = speedCandidates
            .filter { it.confidence >= config.minConfidence }
            .maxByOrNull { candidate ->
                // Prioritize: high confidence + has MAXIMUM label
                candidate.confidence + (if (candidate.hasMaximumLabel) 0.3f else 0f)
            }

        return bestCandidate?.let { candidate ->
            DetectionResult.Success(
                SpeedSign(
                    speedLimit = candidate.speed,
                    confidence = candidate.confidence,
                    boundingBox = candidate.boundingBox
                )
            )
        }
    }

    /**
     * Checks if two bounding boxes are near each other (vertically aligned).
     * Used to associate "MAXIMUM" text with the speed number.
     */
    private fun isNearby(box1: RectF, box2: RectF): Boolean {
        // Check if boxes are vertically close (within 2x height distance)
        val verticalDistance = kotlin.math.abs(box1.bottom - box2.top)
            .coerceAtMost(kotlin.math.abs(box2.bottom - box1.top))

        val maxVerticalDistance = (box1.height() + box2.height())

        // Check horizontal overlap
        val horizontalOverlap = box1.left < box2.right && box2.left < box1.right

        return verticalDistance < maxVerticalDistance && horizontalOverlap
    }

    /**
     * Releases resources held by the detector.
     */
    fun release() {
        textRecognizer.close()
    }

    /**
     * Internal class to hold speed detection candidates during processing.
     */
    private data class SpeedCandidate(
        val speed: Int,
        val confidence: Float,
        val boundingBox: RectF,
        val hasMaximumLabel: Boolean
    )
}
