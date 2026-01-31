package com.etraksolutions.speedsign.ui.screens

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.etraksolutions.speedsign.data.camera.CameraManager
import com.etraksolutions.speedsign.data.detection.DetectedItem
import com.etraksolutions.speedsign.data.detection.DetectionType
import com.etraksolutions.speedsign.data.detection.EnhancedSignDetector
import com.etraksolutions.speedsign.domain.model.AppSettings
import com.etraksolutions.speedsign.domain.model.DetectionConfig
import com.etraksolutions.speedsign.domain.model.DetectionState
import com.etraksolutions.speedsign.domain.model.SpeedSign
import com.etraksolutions.speedsign.ui.components.CameraPreview
import com.etraksolutions.speedsign.ui.components.PermissionRequestScreen
import com.etraksolutions.speedsign.ui.components.SpeedDisplay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced detection screen with all professional features.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnhancedDetectionScreen(
    viewModel: DetectionViewModel = hiltViewModel(),
    cameraManager: CameraManager,
    settings: AppSettings
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Enhanced detector
    val enhancedDetector = remember { EnhancedSignDetector() }

    // Track detected items for overlay
    var detectedItems by remember { mutableStateOf<List<DetectedItem>>(emptyList()) }
    var currentSpeedSign by remember { mutableStateOf<SpeedSign?>(null) }

    // Track FPS and processing
    var frameCount by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(0) }
    var lastFpsUpdate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var processingTimeMs by remember { mutableLongStateOf(0L) }
    var isProcessing by remember { mutableStateOf(false) }

    // Apply zoom when settings change
    LaunchedEffect(settings.cameraZoom) {
        cameraManager.setZoom(settings.cameraZoom)
    }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopDetection()
                    cameraManager.stopCamera()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Camera will restart via LaunchedEffect
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            enhancedDetector.release()
            cameraManager.release()
        }
    }

    // Start camera and detection when permission granted
    LaunchedEffect(cameraPermissionState.status.isGranted, previewView) {
        if (cameraPermissionState.status.isGranted && previewView != null) {
            val frameFlow = cameraManager.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView!!
            )

            // Apply initial zoom
            delay(500) // Wait for camera to initialize
            cameraManager.setZoom(settings.cameraZoom)

            // Process frames with enhanced detector
            val config = DetectionConfig(
                minConfidence = settings.minConfidence,
                processingInterval = settings.processingIntervalMs
            )

            var lastProcessTime = 0L

            frameFlow.collect { bitmap ->
                val currentTime = System.currentTimeMillis()

                // Throttle processing based on settings
                if (currentTime - lastProcessTime >= settings.processingIntervalMs && !isProcessing) {
                    lastProcessTime = currentTime
                    isProcessing = true

                    scope.launch {
                        try {
                            val result = enhancedDetector.detect(bitmap, settings, config)

                            // Update detected items for overlay
                            detectedItems = result.items
                            processingTimeMs = result.processingTimeMs

                            // Update speed sign if detected
                            result.primarySpeedSign?.let { sign ->
                                currentSpeedSign = sign
                                viewModel.processFrame(bitmap)
                            }

                            // Update FPS
                            frameCount++
                            val now = System.currentTimeMillis()
                            if (now - lastFpsUpdate >= 1000) {
                                fps = frameCount
                                frameCount = 0
                                lastFpsUpdate = now
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            }
        }
    }

    // Error handling
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            // Camera preview
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onPreviewViewCreated = { preview ->
                    previewView = preview
                },
                detectionBox = uiState.boundingBox,
                showOverlay = false // We'll draw our own overlay
            )

            // Detection overlay - draw rectangles for all detected items
            if (settings.showDetectionBoxes && detectedItems.isNotEmpty()) {
                MultiDetectionOverlay(
                    items = detectedItems,
                    settings = settings
                )
            }

            // Top gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Top info bar
            TopInfoBar(
                settings = settings,
                fps = fps,
                processingTimeMs = processingTimeMs,
                detectedCount = detectedItems.size,
                detectionState = uiState.detectionState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Bottom display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(bottom = 16.dp)
            ) {
                // Show detected speed
                SpeedDisplay(
                    detectionState = if (currentSpeedSign != null) {
                        DetectionState.Detected(currentSpeedSign!!)
                    } else {
                        DetectionState.Scanning
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Detection status chips
                DetectionStatusChips(
                    settings = settings,
                    detectedItems = detectedItems,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                // Detected items summary
                if (detectedItems.isNotEmpty()) {
                    DetectedItemsSummary(
                        items = detectedItems,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        } else {
            // Permission request
            PermissionRequestScreen(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                isPermanentlyDenied = !cameraPermissionState.status.shouldShowRationale &&
                        !cameraPermissionState.status.isGranted
            )
        }
    }
}

@Composable
fun TopInfoBar(
    settings: AppSettings,
    fps: Int,
    processingTimeMs: Long,
    detectedCount: Int,
    detectionState: DetectionState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
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
                    "Panneaux du Québec",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Status indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (settings.showFps) {
                    StatusChip(
                        text = "$fps FPS",
                        color = when {
                            fps >= 15 -> Color(0xFF4CAF50)
                            fps >= 10 -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                    )
                }

                if (settings.showDebugInfo) {
                    StatusChip(
                        text = "${processingTimeMs}ms",
                        color = Color(0xFF2196F3)
                    )
                }

                if (detectedCount > 0) {
                    StatusChip(
                        text = "$detectedCount",
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }

        // Zoom indicator
        if (settings.cameraZoom > 1.1f) {
            Spacer(modifier = Modifier.height(8.dp))
            StatusChip(
                text = "Zoom: ${String.format("%.1f", settings.cameraZoom)}x",
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun DetectionStatusChips(
    settings: AppSettings,
    detectedItems: List<DetectedItem>,
    modifier: Modifier = Modifier
) {
    val speedCount = detectedItems.count { it.type == DetectionType.SPEED_SIGN }
    val stopCount = detectedItems.count { it.type == DetectionType.STOP_SIGN }
    val numericCount = detectedItems.count { it.type == DetectionType.NUMERIC_TEXT }
    val vehicleCount = detectedItems.count { it.type == DetectionType.VEHICLE }
    val otherCount = detectedItems.count {
        it.type == DetectionType.ARROW_SIGN ||
        it.type == DetectionType.WARNING_SIGN ||
        it.type == DetectionType.OBJECT
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (settings.detectSpeedSigns) {
            DetectionChip("Vitesse", Color(0xFF4CAF50), speedCount > 0, speedCount)
        }
        if (settings.detectStopSigns) {
            DetectionChip("STOP", Color(0xFFF44336), stopCount > 0, stopCount)
        }
        if (settings.detectNumericText) {
            DetectionChip("Nombres", Color(0xFF2196F3), numericCount > 0, numericCount)
        }
        if (settings.detectVehicles) {
            DetectionChip("Véhicules", Color(0xFF9C27B0), vehicleCount > 0, vehicleCount)
        }
        if (settings.detectAllSigns) {
            DetectionChip("Panneaux", Color(0xFFFF9800), otherCount > 0, otherCount)
        }
    }
}

@Composable
fun DetectionChip(
    label: String,
    color: Color,
    detected: Boolean,
    count: Int = 0
) {
    Surface(
        color = if (detected) color.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (detected) color else Color.Gray)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (count > 0) "$label ($count)" else label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = if (detected) 1f else 0.5f)
            )
        }
    }
}

/**
 * Draws detection rectangles for all detected items.
 */
@Composable
fun MultiDetectionOverlay(
    items: List<DetectedItem>,
    settings: AppSettings
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        items.forEach { item ->
            val boxColor = when (item.type) {
                DetectionType.SPEED_SIGN -> Color(settings.boxColorSpeed)
                DetectionType.STOP_SIGN -> Color(settings.boxColorStop)
                DetectionType.NUMERIC_TEXT -> Color(settings.boxColorText)
                DetectionType.VEHICLE -> Color(0xFF9C27B0) // Purple for vehicles
                DetectionType.ARROW_SIGN -> Color(0xFFFFEB3B) // Yellow for arrows
                DetectionType.WARNING_SIGN -> Color(0xFFFF9800) // Orange for warnings
                else -> Color(settings.boxColorOther)
            }

            val box = item.boundingBox
            val left = box.left * size.width
            val top = box.top * size.height
            val right = box.right * size.width
            val bottom = box.bottom * size.height
            val width = right - left
            val height = bottom - top

            // Draw corner brackets
            val cornerSize = minOf(40f, width * 0.3f, height * 0.3f)
            val strokeWidth = 4f

            // Top-left corner
            drawLine(boxColor, Offset(left, top + cornerSize), Offset(left, top), strokeWidth)
            drawLine(boxColor, Offset(left, top), Offset(left + cornerSize, top), strokeWidth)

            // Top-right corner
            drawLine(boxColor, Offset(right - cornerSize, top), Offset(right, top), strokeWidth)
            drawLine(boxColor, Offset(right, top), Offset(right, top + cornerSize), strokeWidth)

            // Bottom-left corner
            drawLine(boxColor, Offset(left, bottom - cornerSize), Offset(left, bottom), strokeWidth)
            drawLine(boxColor, Offset(left, bottom), Offset(left + cornerSize, bottom), strokeWidth)

            // Bottom-right corner
            drawLine(boxColor, Offset(right - cornerSize, bottom), Offset(right, bottom), strokeWidth)
            drawLine(boxColor, Offset(right, bottom), Offset(right, bottom - cornerSize), strokeWidth)

            // Semi-transparent fill
            drawRoundRect(
                color = boxColor.copy(alpha = 0.15f),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}

@Composable
fun DetectedItemsSummary(
    items: List<DetectedItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Visibility,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            "Détecté:",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )

        // Show unique labels
        items
            .distinctBy { it.label }
            .take(5)
            .forEach { item ->
                ItemBubble(item = item)
            }
    }
}

@Composable
fun ItemBubble(item: DetectedItem) {
    val color = when (item.type) {
        DetectionType.SPEED_SIGN -> Color(0xFF4CAF50)
        DetectionType.STOP_SIGN -> Color(0xFFF44336)
        DetectionType.VEHICLE -> Color(0xFF9C27B0)
        DetectionType.NUMERIC_TEXT -> Color(0xFF2196F3)
        else -> Color(0xFFFF9800)
    }

    Surface(
        color = color.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            item.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}

// Keep old functions for compatibility
@Composable
fun RecentDetections(
    history: List<com.etraksolutions.speedsign.domain.model.DetectionResult.Success>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            "Recent:",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        history.forEach { result ->
            SpeedBubble(speed = result.speedSign.speedLimit)
        }
    }
}

@Composable
fun SpeedBubble(speed: Int) {
    val color = when {
        speed >= 100 -> Color(0xFFF44336)
        speed >= 70 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Surface(
        color = color.copy(alpha = 0.3f),
        shape = CircleShape
    ) {
        Text(
            "$speed",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
