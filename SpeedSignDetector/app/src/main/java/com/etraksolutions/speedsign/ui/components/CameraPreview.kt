package com.etraksolutions.speedsign.ui.components

import android.graphics.RectF
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.etraksolutions.speedsign.ui.theme.DetectionBox

/**
 * Camera preview composable using CameraX.
 *
 * Displays the live camera feed with an optional detection overlay
 * showing the bounding box of detected signs.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onPreviewViewCreated: (PreviewView) -> Unit,
    detectionBox: RectF? = null,
    showOverlay: Boolean = true
) {
    val context = LocalContext.current

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = {
                previewView.also { onPreviewViewCreated(it) }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Detection overlay
        if (showOverlay && detectionBox != null) {
            DetectionOverlay(
                boundingBox = detectionBox,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Scanning frame guide
        if (showOverlay) {
            ScanningFrameGuide(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Draws a bounding box around the detected sign area.
 */
@Composable
fun DetectionOverlay(
    boundingBox: RectF,
    modifier: Modifier = Modifier
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(200),
        label = "box_alpha"
    )

    Canvas(modifier = modifier) {
        val scaleX = size.width / boundingBox.width()
        val scaleY = size.height / boundingBox.height()

        // Scale bounding box to canvas size
        val left = boundingBox.left * scaleX
        val top = boundingBox.top * scaleY
        val width = boundingBox.width() * scaleX
        val height = boundingBox.height() * scaleY

        // Draw detection box
        drawRect(
            color = DetectionBox.copy(alpha = animatedAlpha),
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(width = 4f)
        )

        // Draw corner accents
        val cornerLength = 40f
        val cornerColor = Color.White.copy(alpha = animatedAlpha)

        // Top-left corner
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), 4f)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), 4f)

        // Top-right corner
        drawLine(cornerColor, Offset(left + width, top), Offset(left + width - cornerLength, top), 4f)
        drawLine(cornerColor, Offset(left + width, top), Offset(left + width, top + cornerLength), 4f)

        // Bottom-left corner
        drawLine(cornerColor, Offset(left, top + height), Offset(left + cornerLength, top + height), 4f)
        drawLine(cornerColor, Offset(left, top + height), Offset(left, top + height - cornerLength), 4f)

        // Bottom-right corner
        drawLine(cornerColor, Offset(left + width, top + height), Offset(left + width - cornerLength, top + height), 4f)
        drawLine(cornerColor, Offset(left + width, top + height), Offset(left + width, top + height - cornerLength), 4f)
    }
}

/**
 * Draws a guide frame to help users position signs in view.
 */
@Composable
fun ScanningFrameGuide(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val frameWidth = size.width * 0.7f
        val frameHeight = size.height * 0.4f

        val left = centerX - frameWidth / 2
        val top = centerY - frameHeight / 2
        val right = centerX + frameWidth / 2
        val bottom = centerY + frameHeight / 2

        val cornerLength = 50f
        val guideColor = Color.White.copy(alpha = 0.5f)
        val strokeWidth = 3f

        // Top-left corner
        drawLine(guideColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
        drawLine(guideColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)

        // Top-right corner
        drawLine(guideColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
        drawLine(guideColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)

        // Bottom-left corner
        drawLine(guideColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
        drawLine(guideColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)

        // Bottom-right corner
        drawLine(guideColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
        drawLine(guideColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)
    }
}
