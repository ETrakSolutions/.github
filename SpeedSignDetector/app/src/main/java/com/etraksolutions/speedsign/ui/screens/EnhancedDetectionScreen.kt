package com.etraksolutions.speedsign.ui.screens

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.etraksolutions.speedsign.data.camera.CameraManager
import com.etraksolutions.speedsign.domain.model.AppSettings
import com.etraksolutions.speedsign.domain.model.DetectionState
import com.etraksolutions.speedsign.ui.components.CameraPreview
import com.etraksolutions.speedsign.ui.components.PermissionRequestScreen
import com.etraksolutions.speedsign.ui.components.SpeedDisplay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

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

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Track FPS
    var frameCount by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(0) }
    var lastFpsUpdate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Update FPS counter
    LaunchedEffect(uiState.detectionState) {
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsUpdate >= 1000) {
            fps = frameCount
            frameCount = 0
            lastFpsUpdate = now
        }
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
                    if (cameraPermissionState.status.isGranted && previewView != null) {
                        // Camera will restart automatically
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cameraManager.release()
        }
    }

    // Start camera when permission granted
    LaunchedEffect(cameraPermissionState.status.isGranted, previewView) {
        if (cameraPermissionState.status.isGranted && previewView != null) {
            val frameFlow = cameraManager.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView!!
            )
            viewModel.startDetection(frameFlow)
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
                showOverlay = uiState.showOverlay
            )

            // Detection overlay boxes
            if (settings.showDetectionBoxes && uiState.boundingBox != null) {
                DetectionOverlay(
                    detectionState = uiState.detectionState,
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
                detectionState = uiState.detectionState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Bottom speed display
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
                SpeedDisplay(
                    detectionState = uiState.detectionState,
                    modifier = Modifier.fillMaxWidth()
                )

                // Detection status chips
                DetectionStatusChips(
                    settings = settings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                // Recent detections
                val successfulDetections = uiState.detectionHistory
                    .filterIsInstance<com.etraksolutions.speedsign.domain.model.DetectionResult.Success>()
                    .take(5)
                if (successfulDetections.isNotEmpty()) {
                    RecentDetections(
                        history = successfulDetections,
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
                    "Panneaux de vitesse du Quebec",
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
                        text = "${settings.processingIntervalMs}ms",
                        color = Color(0xFF2196F3)
                    )
                }
            }
        }

        // Processing status
        if (settings.showDebugInfo) {
            Spacer(modifier = Modifier.height(8.dp))
            ProcessingIndicator(
                isProcessing = detectionState is DetectionState.Scanning
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
fun ProcessingIndicator(isProcessing: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "processing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                Color.Black.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isProcessing) Color(0xFF4CAF50).copy(alpha = alpha)
                    else Color(0xFF9E9E9E)
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (isProcessing) "Analyse en cours..." else "En attente",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
fun DetectionStatusChips(
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (settings.detectSpeedSigns) {
            DetectionChip("Vitesse", Color(0xFF4CAF50), true)
        }
        if (settings.detectStopSigns) {
            DetectionChip("STOP", Color(0xFFF44336), true)
        }
        if (settings.detectNumericText) {
            DetectionChip("Nombres", Color(0xFF2196F3), true)
        }
        if (settings.detectAllSigns) {
            DetectionChip("Panneaux", Color(0xFFFF9800), true)
        }
    }
}

@Composable
fun DetectionChip(
    label: String,
    color: Color,
    enabled: Boolean
) {
    Surface(
        color = if (enabled) color.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
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
                    .background(if (enabled) color else Color.Gray)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = if (enabled) 1f else 0.5f)
            )
        }
    }
}

@Composable
fun DetectionOverlay(
    detectionState: DetectionState,
    settings: AppSettings
) {
    val detected = detectionState as? DetectionState.Detected ?: return

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw detection box
        val boxColor = when {
            detected.speedSign.speedLimit >= 100 -> Color(0xFFF44336)
            detected.speedSign.speedLimit >= 70 -> Color(0xFFFF9800)
            else -> Color(0xFF4CAF50)
        }

        // Animated corner effect
        val cornerSize = 40f
        val strokeWidth = 4f

        detected.speedSign.boundingBox?.let { rect ->
            // Scale to canvas size (assuming normalized coordinates)
            val left = rect.left * size.width
            val top = rect.top * size.height
            val right = rect.right * size.width
            val bottom = rect.bottom * size.height

            // Draw corner brackets
            // Top-left
            drawLine(boxColor, Offset(left, top + cornerSize), Offset(left, top), strokeWidth)
            drawLine(boxColor, Offset(left, top), Offset(left + cornerSize, top), strokeWidth)

            // Top-right
            drawLine(boxColor, Offset(right - cornerSize, top), Offset(right, top), strokeWidth)
            drawLine(boxColor, Offset(right, top), Offset(right, top + cornerSize), strokeWidth)

            // Bottom-left
            drawLine(boxColor, Offset(left, bottom - cornerSize), Offset(left, bottom), strokeWidth)
            drawLine(boxColor, Offset(left, bottom), Offset(left + cornerSize, bottom), strokeWidth)

            // Bottom-right
            drawLine(boxColor, Offset(right - cornerSize, bottom), Offset(right, bottom), strokeWidth)
            drawLine(boxColor, Offset(right, bottom), Offset(right, bottom - cornerSize), strokeWidth)

            // Semi-transparent fill
            drawRoundRect(
                color = boxColor.copy(alpha = 0.1f),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}

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
