package com.etraksolutions.speedsign.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for CameraX camera operations.
 *
 * Handles camera initialization, preview binding, and frame analysis
 * for real-time sign detection.
 */
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: androidx.camera.core.Camera? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    /**
     * Current zoom ratio
     */
    var currentZoom: Float = 1.0f
        private set

    /**
     * Minimum zoom ratio supported by the camera
     */
    val minZoom: Float
        get() = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1.0f

    /**
     * Maximum zoom ratio supported by the camera
     */
    val maxZoom: Float
        get() = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 10.0f

    /**
     * Sets the camera zoom ratio.
     * @param zoomRatio The desired zoom ratio (between minZoom and maxZoom)
     */
    fun setZoom(zoomRatio: Float) {
        camera?.let { cam ->
            val clampedZoom = zoomRatio.coerceIn(minZoom, maxZoom)
            cam.cameraControl.setZoomRatio(clampedZoom)
            currentZoom = clampedZoom
        }
    }

    /**
     * Sets zoom using linear zoom (0.0 to 1.0)
     * @param linearZoom The linear zoom value between 0.0 and 1.0
     */
    fun setLinearZoom(linearZoom: Float) {
        camera?.let { cam ->
            val clampedZoom = linearZoom.coerceIn(0f, 1f)
            cam.cameraControl.setLinearZoom(clampedZoom)
            currentZoom = minZoom + (maxZoom - minZoom) * clampedZoom
        }
    }

    /**
     * Binds the camera preview to a PreviewView and returns a flow of frames for analysis.
     *
     * @param lifecycleOwner The lifecycle owner for camera binding
     * @param previewView The view to display the camera preview
     * @param targetResolution Desired resolution for analysis (default 640x480)
     * @return Flow of Bitmap frames for processing
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        targetResolution: Size = Size(640, 480)
    ): Flow<Bitmap> = callbackFlow {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Preview use case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Image analysis use case
                imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(targetResolution)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            val bitmap = imageProxyToBitmap(imageProxy)
                            bitmap?.let {
                                trySend(it)
                            }
                            imageProxy.close()
                        }
                    }

                // Select back camera
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                // Unbind all use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                close(e)
            }
        }, ContextCompat.getMainExecutor(context))

        awaitClose {
            stopCamera()
        }
    }

    /**
     * Converts an ImageProxy to a Bitmap for processing.
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            if (imageProxy.planes.isEmpty()) {
                return null
            }

            val buffer = imageProxy.planes[0].buffer

            val bitmap = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )

            // Copy pixel data
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)

            // Rotate bitmap if necessary based on rotation degrees
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees != 0) {
                rotateBitmap(bitmap, rotationDegrees)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraManager", "Error converting ImageProxy to Bitmap", e)
            null
        }
    }

    /**
     * Rotates a bitmap by the specified degrees.
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }
        return Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        ).also {
            if (it != bitmap) {
                bitmap.recycle()
            }
        }
    }

    /**
     * Stops the camera and releases resources.
     */
    fun stopCamera() {
        cameraProvider?.unbindAll()
        imageAnalysis?.clearAnalyzer()
    }

    /**
     * Releases all camera resources.
     */
    fun release() {
        stopCamera()
        analysisExecutor.shutdown()
    }
}
