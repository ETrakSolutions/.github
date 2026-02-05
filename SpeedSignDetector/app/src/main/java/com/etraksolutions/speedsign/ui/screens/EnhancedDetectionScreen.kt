package com.etraksolutions.speedsign.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.etraksolutions.speedsign.data.camera.CameraManager
import com.etraksolutions.speedsign.data.detection.DetectedItem
import com.etraksolutions.speedsign.data.detection.DetectionType
import com.etraksolutions.speedsign.data.detection.EnhancedSignDetector
import com.etraksolutions.speedsign.domain.model.AppSettings
import com.etraksolutions.speedsign.domain.model.DetectionConfig
import com.etraksolutions.speedsign.ui.components.PermissionRequestScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "EnhancedDetection"

data class DetectionHistoryItem(
    val item: DetectedItem,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnhancedDetectionScreen(
    cameraManager: CameraManager,
    settings: AppSettings
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var isCameraReady by remember { mutableStateOf(false) }

    // Use remember with keys to recreate detector when needed
    // Lazy creation to avoid crash on init
    val detector = remember {
        try {
            EnhancedSignDetector()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create detector", e)
            EnhancedSignDetector() // Retry
        }
    }

    // Detection state
    var detectedItems by remember { mutableStateOf<List<DetectedItem>>(emptyList()) }
    var detectionHistory by remember { mutableStateOf<List<DetectionHistoryItem>>(emptyList()) }

    // Stats - use atomic for thread safety
    var fps by remember { mutableIntStateOf(0) }
    var processingTimeMs by remember { mutableLongStateOf(0L) }
    var totalFrames by remember { mutableIntStateOf(0) }
    var processedFrames by remember { mutableIntStateOf(0) }

    // Remember current settings for use in coroutine
    val currentSettings by rememberUpdatedState(settings)

    // Apply zoom
    LaunchedEffect(settings.cameraZoom, isCameraReady) {
        if (isCameraReady) {
            Log.d(TAG, "Applying zoom: ${settings.cameraZoom}")
            cameraManager.setZoom(settings.cameraZoom)
        }
    }

    // Lifecycle handling
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d(TAG, "ON_PAUSE - stopping camera")
                    cameraManager.stopCamera()
                    isCameraReady = false
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d(TAG, "ON_DESTROY - releasing")
                    detector.release()
                    cameraManager.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Main camera and detection loop
    LaunchedEffect(cameraPermissionState.status.isGranted, previewView) {
        if (cameraPermissionState.status.isGranted && previewView != null) {
            Log.d(TAG, "Starting camera and detection loop...")

            val isProcessing = AtomicBoolean(false)
            val lastProcessTime = AtomicLong(0L)
            var frameCount = 0
            var lastFpsTime = System.currentTimeMillis()

            try {
                val frameFlow = cameraManager.startCamera(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView!!
                )

                // Wait for camera initialization
                delay(500)
                isCameraReady = true
                cameraManager.setZoom(currentSettings.cameraZoom)
                Log.d(TAG, "Camera ready")

                // Collect and process frames
                frameFlow.collect { bitmap ->
                    totalFrames++
                    frameCount++

                    // Update FPS every second
                    val now = System.currentTimeMillis()
                    if (now - lastFpsTime >= 1000) {
                        fps = frameCount
                        frameCount = 0
                        lastFpsTime = now
                    }

                    // Process frame if not already processing and enough time has passed
                    val timeSinceLastProcess = now - lastProcessTime.get()
                    val shouldProcess = timeSinceLastProcess >= currentSettings.processingIntervalMs

                    if (shouldProcess && isProcessing.compareAndSet(false, true)) {
                        lastProcessTime.set(now)

                        try {
                            val startTime = System.currentTimeMillis()

                            val config = DetectionConfig(
                                minConfidence = currentSettings.minConfidence,
                                processingInterval = currentSettings.processingIntervalMs
                            )

                            // Run detection on Default dispatcher
                            val result = withContext(Dispatchers.Default) {
                                detector.detect(bitmap, currentSettings, config)
                            }

                            val elapsed = System.currentTimeMillis() - startTime
                            processingTimeMs = elapsed
                            processedFrames++

                            // Update UI state
                            detectedItems = result.items

                            // Add to history
                            if (result.items.isNotEmpty()) {
                                val newItems = result.items.map { DetectionHistoryItem(it) }
                                detectionHistory = (newItems + detectionHistory).take(100)
                                Log.d(TAG, "Detected ${result.items.size} items: ${result.items.map { it.label }}")
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Detection error: ${e.message}", e)
                        } finally {
                            isProcessing.set(false)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera/Detection loop error: ${e.message}", e)
            }
        }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also {
                        it.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = it
                        Log.d(TAG, "PreviewView created")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Detection overlay
            if (settings.showDetectionBoxes && detectedItems.isNotEmpty()) {
                DetectionOverlayCanvas(items = detectedItems, settings = settings)
            }

            // Top bar with stats
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Speed Sign Detector",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Panneaux du Quebec",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (settings.showFps) {
                                    InfoChip("$fps FPS", when {
                                        fps >= 15 -> Color(0xFF4CAF50)
                                        fps >= 8 -> Color(0xFFFFC107)
                                        else -> Color(0xFFF44336)
                                    })
                                }
                                if (settings.showDebugInfo) {
                                    InfoChip("${processingTimeMs}ms", Color(0xFF2196F3))
                                    InfoChip("#$processedFrames", Color(0xFF9C27B0))
                                }
                            }
                        }

                        if (settings.cameraZoom > 1.1f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoChip("Zoom ${String.format("%.1f", settings.cameraZoom)}x", Color(0xFFFF9800))
                        }

                        // Active detection modes
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (settings.detectSpeedSigns) MiniChip("Vitesse", Color(0xFF4CAF50))
                            if (settings.detectStopSigns) MiniChip("STOP", Color(0xFFF44336))
                            if (settings.detectVehicles) MiniChip("Vehicules", Color(0xFF9C27B0))
                            if (settings.detectNumericText) MiniChip("Nombres", Color(0xFF2196F3))
                            if (settings.detectText) MiniChip("Texte", Color(0xFF607D8B))
                        }
                    }
                }
            }

            // Bottom panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                // Current detections
                val speedCount = detectedItems.count { it.type == DetectionType.SPEED_SIGN }
                val stopCount = detectedItems.count { it.type == DetectionType.STOP_SIGN }
                val vehicleCount = detectedItems.count { it.type == DetectionType.VEHICLE }
                val textCount = detectedItems.count {
                    it.type == DetectionType.NUMERIC_TEXT || it.type == DetectionType.GENERAL_TEXT
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (settings.detectSpeedSigns) DetectionTypeChip("Vitesse", Color(0xFF4CAF50), speedCount)
                    if (settings.detectStopSigns) DetectionTypeChip("STOP", Color(0xFFF44336), stopCount)
                    if (settings.detectVehicles) DetectionTypeChip("Vehicules", Color(0xFF9C27B0), vehicleCount)
                    if (settings.detectNumericText || settings.detectText) DetectionTypeChip("Texte", Color(0xFF2196F3), textCount)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (detectedItems.isNotEmpty()) {
                    Text("Detecte maintenant:", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detectedItems.distinctBy { it.label }.take(10).forEach { DetectedItemChip(it) }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recherche en cours...", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.5f))
                    }
                }

                // History
                if (detectionHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Historique (${detectionHistory.size}):", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        detectionHistory.distinctBy { it.item.label }.take(20).forEach { HistoryChip(it.item) }
                    }
                }
            }
        } else {
            PermissionRequestScreen(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                isPermanentlyDenied = !cameraPermissionState.status.shouldShowRationale && !cameraPermissionState.status.isGranted
            )
        }
    }
}

@Composable
fun MiniChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun InfoChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.25f), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun DetectionTypeChip(label: String, color: Color, count: Int) {
    val isActive = count > 0
    Surface(
        color = if (isActive) color.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isActive) color else Color.Gray))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isActive) "$label ($count)" else label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = Color.White.copy(alpha = if (isActive) 1f else 0.5f)
            )
        }
    }
}

@Composable
fun DetectedItemChip(item: DetectedItem) {
    val color = when (item.type) {
        DetectionType.SPEED_SIGN -> Color(0xFF4CAF50)
        DetectionType.STOP_SIGN -> Color(0xFFF44336)
        DetectionType.VEHICLE -> Color(0xFF9C27B0)
        DetectionType.NUMERIC_TEXT -> Color(0xFF2196F3)
        DetectionType.ARROW_SIGN -> Color(0xFFFFEB3B)
        DetectionType.WARNING_SIGN -> Color(0xFFFF9800)
        else -> Color(0xFF607D8B)
    }
    Surface(color = color.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp)) {
        Text(
            item.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun HistoryChip(item: DetectedItem) {
    val color = when (item.type) {
        DetectionType.SPEED_SIGN -> Color(0xFF4CAF50)
        DetectionType.STOP_SIGN -> Color(0xFFF44336)
        DetectionType.VEHICLE -> Color(0xFF9C27B0)
        DetectionType.NUMERIC_TEXT -> Color(0xFF2196F3)
        else -> Color(0xFF607D8B)
    }
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
        Text(
            item.label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun DetectionOverlayCanvas(items: List<DetectedItem>, settings: AppSettings) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        items.forEach { item ->
            val color = when (item.type) {
                DetectionType.SPEED_SIGN -> Color(settings.boxColorSpeed)
                DetectionType.STOP_SIGN -> Color(settings.boxColorStop)
                DetectionType.NUMERIC_TEXT -> Color(settings.boxColorText)
                DetectionType.VEHICLE -> Color(0xFF9C27B0)
                DetectionType.ARROW_SIGN -> Color(0xFFFFEB3B)
                DetectionType.WARNING_SIGN -> Color(0xFFFF9800)
                else -> Color(settings.boxColorOther)
            }

            val box = item.boundingBox
            val left = box.left * size.width
            val top = box.top * size.height
            val right = box.right * size.width
            val bottom = box.bottom * size.height
            val width = right - left
            val height = bottom - top

            if (width > 5 && height > 5) {
                val cs = minOf(25f, width * 0.2f, height * 0.2f)
                val sw = 4f

                // Draw corners
                drawLine(color, Offset(left, top + cs), Offset(left, top), sw)
                drawLine(color, Offset(left, top), Offset(left + cs, top), sw)
                drawLine(color, Offset(right - cs, top), Offset(right, top), sw)
                drawLine(color, Offset(right, top), Offset(right, top + cs), sw)
                drawLine(color, Offset(left, bottom - cs), Offset(left, bottom), sw)
                drawLine(color, Offset(left, bottom), Offset(left + cs, bottom), sw)
                drawLine(color, Offset(right - cs, bottom), Offset(right, bottom), sw)
                drawLine(color, Offset(right, bottom), Offset(right, bottom - cs), sw)

                // Fill
                drawRoundRect(
                    color = color.copy(alpha = 0.15f),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(8f)
                )
            }
        }
    }
}

@Composable
fun RecentDetections(
    history: List<com.etraksolutions.speedsign.domain.model.DetectionResult.Success>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        Text("Recent:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        history.forEach { SpeedBubble(it.speedSign.speedLimit) }
    }
}

@Composable
fun SpeedBubble(speed: Int) {
    val color = when { speed >= 100 -> Color(0xFFF44336); speed >= 70 -> Color(0xFFFF9800); else -> Color(0xFF4CAF50) }
    Surface(color = color.copy(alpha = 0.3f), shape = CircleShape) {
        Text("$speed", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
