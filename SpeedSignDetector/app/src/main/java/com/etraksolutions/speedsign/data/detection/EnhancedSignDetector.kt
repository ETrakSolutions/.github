package com.etraksolutions.speedsign.data.detection

import android.graphics.Bitmap
import android.graphics.RectF
import com.etraksolutions.speedsign.domain.model.AppSettings
import com.etraksolutions.speedsign.domain.model.DetectionConfig
import com.etraksolutions.speedsign.domain.model.DetectionResult
import com.etraksolutions.speedsign.domain.model.SpeedSign
import com.etraksolutions.speedsign.domain.model.TextRegion
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
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
 * Detection types supported by the enhanced detector.
 */
enum class DetectionType {
    SPEED_SIGN,
    STOP_SIGN,
    ARROW_SIGN,
    WARNING_SIGN,
    NUMERIC_TEXT,
    GENERAL_TEXT,
    VEHICLE,
    OBJECT
}

/**
 * Represents any detected item (sign, text, vehicle, etc.)
 */
data class DetectedItem(
    val type: DetectionType,
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    val speedLimit: Int? = null // Only for speed signs
)

/**
 * Enhanced detection result containing all detected items.
 */
data class EnhancedDetectionResult(
    val items: List<DetectedItem>,
    val primarySpeedSign: SpeedSign? = null,
    val processingTimeMs: Long = 0
)

/**
 * Enhanced detector that can detect:
 * - Speed signs (Quebec style)
 * - STOP signs (ARRÊT in Quebec)
 * - Arrow signs
 * - Warning signs
 * - Vehicles (cars, trucks, buses)
 * - General text
 */
@Singleton
class EnhancedSignDetector @Inject constructor() {

    private val textRecognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.Builder().build()
    )

    private val objectDetector: ObjectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    // Quebec speed limit values
    private val speedNumberPattern = Regex("""\b(30|40|50|60|70|80|90|100|110)\b""")

    // Pattern for MAXIMUM/LIMITE text
    private val maximumPattern = Regex("""MAXIMUM|MAX|LIMITE|VITESSE""", RegexOption.IGNORE_CASE)

    // Pattern for STOP/ARRÊT signs
    private val stopPattern = Regex("""STOP|ARR[EÊ]T""", RegexOption.IGNORE_CASE)

    // Pattern for general numeric text (any number)
    private val numericPattern = Regex("""\b\d{1,3}\b""")

    // Arrow indicators
    private val arrowKeywords = listOf("GAUCHE", "DROITE", "LEFT", "RIGHT", "TOUT DROIT", "STRAIGHT")

    // Warning sign keywords
    private val warningKeywords = listOf(
        "ATTENTION", "DANGER", "PRUDENCE", "TRAVAUX",
        "ÉCOLE", "SCHOOL", "ZONE", "CÉDEZ", "YIELD"
    )

    /**
     * Performs comprehensive detection on an image.
     */
    suspend fun detect(
        bitmap: Bitmap,
        settings: AppSettings,
        config: DetectionConfig
    ): EnhancedDetectionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val detectedItems = mutableListOf<DetectedItem>()
        var primarySpeedSign: SpeedSign? = null

        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // Run text recognition if any text-based detection is enabled
            if (settings.detectSpeedSigns || settings.detectStopSigns ||
                settings.detectNumericText || settings.detectText || settings.detectAllSigns) {

                val textResult = recognizeText(inputImage)
                val textRegions = extractTextRegions(textResult, bitmap.width, bitmap.height)

                // Detect speed signs
                if (settings.detectSpeedSigns) {
                    val speedItems = detectSpeedSigns(textRegions, config)
                    detectedItems.addAll(speedItems)

                    // Set primary speed sign
                    primarySpeedSign = speedItems.firstOrNull()?.let { item ->
                        SpeedSign(
                            speedLimit = item.speedLimit ?: 0,
                            confidence = item.confidence,
                            boundingBox = item.boundingBox
                        )
                    }
                }

                // Detect STOP signs
                if (settings.detectStopSigns) {
                    val stopItems = detectStopSigns(textRegions)
                    detectedItems.addAll(stopItems)
                }

                // Detect arrow signs
                if (settings.detectAllSigns) {
                    val arrowItems = detectArrowSigns(textRegions)
                    detectedItems.addAll(arrowItems)

                    val warningItems = detectWarningSigns(textRegions)
                    detectedItems.addAll(warningItems)
                }

                // Detect numeric text
                if (settings.detectNumericText) {
                    val numericItems = detectNumericText(textRegions, config)
                    detectedItems.addAll(numericItems)
                }

                // Detect general text
                if (settings.detectText) {
                    val textItems = detectGeneralText(textRegions)
                    detectedItems.addAll(textItems)
                }
            }

            // Run object detection for vehicles
            if (settings.detectVehicles) {
                val objects = detectObjects(inputImage)
                val vehicleItems = processObjectDetections(objects, bitmap.width, bitmap.height)
                detectedItems.addAll(vehicleItems)
            }

        } catch (e: Exception) {
            // Log error but don't fail completely
            e.printStackTrace()
        }

        val processingTime = System.currentTimeMillis() - startTime

        EnhancedDetectionResult(
            items = detectedItems,
            primarySpeedSign = primarySpeedSign,
            processingTimeMs = processingTime
        )
    }

    /**
     * Performs OCR on the input image.
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
     * Runs ML Kit Object Detection.
     */
    private suspend fun detectObjects(inputImage: InputImage): List<DetectedObject> {
        return suspendCancellableCoroutine { continuation ->
            objectDetector.process(inputImage)
                .addOnSuccessListener { objects ->
                    continuation.resume(objects)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    /**
     * Extracts text regions with normalized coordinates.
     */
    private fun extractTextRegions(
        text: com.google.mlkit.vision.text.Text,
        imageWidth: Int,
        imageHeight: Int
    ): List<TextRegion> {
        val regions = mutableListOf<TextRegion>()

        for (block in text.textBlocks) {
            val blockConfidence = 0.8f

            for (line in block.lines) {
                val boundingBox = line.boundingBox ?: continue

                // Normalize coordinates to 0-1 range for overlay drawing
                val normalizedBox = RectF(
                    boundingBox.left.toFloat() / imageWidth,
                    boundingBox.top.toFloat() / imageHeight,
                    boundingBox.right.toFloat() / imageWidth,
                    boundingBox.bottom.toFloat() / imageHeight
                )

                regions.add(
                    TextRegion(
                        text = line.text,
                        boundingBox = normalizedBox,
                        confidence = blockConfidence
                    )
                )

                for (element in line.elements) {
                    val elementBox = element.boundingBox ?: continue
                    val normalizedElementBox = RectF(
                        elementBox.left.toFloat() / imageWidth,
                        elementBox.top.toFloat() / imageHeight,
                        elementBox.right.toFloat() / imageWidth,
                        elementBox.bottom.toFloat() / imageHeight
                    )

                    regions.add(
                        TextRegion(
                            text = element.text,
                            boundingBox = normalizedElementBox,
                            confidence = blockConfidence
                        )
                    )
                }
            }
        }

        return regions
    }

    /**
     * Detects speed signs from text regions.
     */
    private fun detectSpeedSigns(
        textRegions: List<TextRegion>,
        config: DetectionConfig
    ): List<DetectedItem> {
        val items = mutableListOf<DetectedItem>()
        val maximumRegions = textRegions.filter { maximumPattern.containsMatchIn(it.text) }

        for (region in textRegions) {
            val match = speedNumberPattern.find(region.text)
            if (match != null) {
                val speedValue = match.value.toIntOrNull() ?: continue
                if (speedValue !in config.targetSpeedLimits) continue

                val hasMaximumNearby = maximumRegions.any { isNearby(it.boundingBox, region.boundingBox) }
                val confidence = (region.confidence + if (hasMaximumNearby) 0.2f else 0f).coerceAtMost(1f)

                if (confidence >= config.minConfidence) {
                    items.add(
                        DetectedItem(
                            type = DetectionType.SPEED_SIGN,
                            label = "$speedValue km/h",
                            confidence = confidence,
                            boundingBox = region.boundingBox,
                            speedLimit = speedValue
                        )
                    )
                }
            }
        }

        return items.sortedByDescending { it.confidence }
    }

    /**
     * Detects STOP/ARRÊT signs.
     */
    private fun detectStopSigns(textRegions: List<TextRegion>): List<DetectedItem> {
        return textRegions
            .filter { stopPattern.containsMatchIn(it.text) }
            .map { region ->
                DetectedItem(
                    type = DetectionType.STOP_SIGN,
                    label = "STOP/ARRÊT",
                    confidence = region.confidence + 0.1f,
                    boundingBox = region.boundingBox
                )
            }
    }

    /**
     * Detects arrow/direction signs.
     */
    private fun detectArrowSigns(textRegions: List<TextRegion>): List<DetectedItem> {
        return textRegions
            .filter { region ->
                arrowKeywords.any { keyword ->
                    region.text.contains(keyword, ignoreCase = true)
                }
            }
            .map { region ->
                DetectedItem(
                    type = DetectionType.ARROW_SIGN,
                    label = region.text,
                    confidence = region.confidence,
                    boundingBox = region.boundingBox
                )
            }
    }

    /**
     * Detects warning signs.
     */
    private fun detectWarningSigns(textRegions: List<TextRegion>): List<DetectedItem> {
        return textRegions
            .filter { region ->
                warningKeywords.any { keyword ->
                    region.text.contains(keyword, ignoreCase = true)
                }
            }
            .map { region ->
                DetectedItem(
                    type = DetectionType.WARNING_SIGN,
                    label = region.text,
                    confidence = region.confidence,
                    boundingBox = region.boundingBox
                )
            }
    }

    /**
     * Detects general numeric text.
     */
    private fun detectNumericText(
        textRegions: List<TextRegion>,
        config: DetectionConfig
    ): List<DetectedItem> {
        return textRegions
            .filter { region ->
                numericPattern.containsMatchIn(region.text) &&
                        !speedNumberPattern.containsMatchIn(region.text) // Exclude speed signs
            }
            .filter { it.confidence >= config.minConfidence }
            .map { region ->
                DetectedItem(
                    type = DetectionType.NUMERIC_TEXT,
                    label = region.text,
                    confidence = region.confidence,
                    boundingBox = region.boundingBox
                )
            }
    }

    /**
     * Detects general text.
     */
    private fun detectGeneralText(textRegions: List<TextRegion>): List<DetectedItem> {
        return textRegions
            .filter { it.text.length >= 2 }
            .take(10) // Limit to avoid too many items
            .map { region ->
                DetectedItem(
                    type = DetectionType.GENERAL_TEXT,
                    label = region.text,
                    confidence = region.confidence,
                    boundingBox = region.boundingBox
                )
            }
    }

    /**
     * Processes ML Kit object detections for vehicles.
     */
    private fun processObjectDetections(
        objects: List<DetectedObject>,
        imageWidth: Int,
        imageHeight: Int
    ): List<DetectedItem> {
        val vehicleLabels = setOf(
            "Car", "Automobile", "Vehicle", "Truck", "Bus", "Motorcycle",
            "Bicycle", "Van", "SUV"
        )

        return objects.mapNotNull { obj ->
            val label = obj.labels.firstOrNull()
            val boundingBox = obj.boundingBox

            // Normalize coordinates
            val normalizedBox = RectF(
                boundingBox.left.toFloat() / imageWidth,
                boundingBox.top.toFloat() / imageHeight,
                boundingBox.right.toFloat() / imageWidth,
                boundingBox.bottom.toFloat() / imageHeight
            )

            if (label != null && vehicleLabels.any { it.equals(label.text, ignoreCase = true) }) {
                DetectedItem(
                    type = DetectionType.VEHICLE,
                    label = label.text,
                    confidence = label.confidence,
                    boundingBox = normalizedBox
                )
            } else if (label != null) {
                // Return as general object
                DetectedItem(
                    type = DetectionType.OBJECT,
                    label = label.text,
                    confidence = label.confidence,
                    boundingBox = normalizedBox
                )
            } else {
                null
            }
        }
    }

    /**
     * Checks if two bounding boxes are nearby (for associating text).
     */
    private fun isNearby(box1: RectF, box2: RectF): Boolean {
        val verticalDistance = kotlin.math.abs(box1.bottom - box2.top)
            .coerceAtMost(kotlin.math.abs(box2.bottom - box1.top))
        val maxVerticalDistance = (box1.height() + box2.height())
        val horizontalOverlap = box1.left < box2.right && box2.left < box1.right
        return verticalDistance < maxVerticalDistance && horizontalOverlap
    }

    /**
     * Releases resources.
     */
    fun release() {
        textRecognizer.close()
        objectDetector.close()
    }
}
