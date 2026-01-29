package com.etraksolutions.speedsign.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.etraksolutions.speedsign.data.camera.CameraManager
import com.etraksolutions.speedsign.ui.screens.DetectionScreen
import com.etraksolutions.speedsign.ui.theme.SpeedSignDetectorTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for the Speed Sign Detector application.
 *
 * This activity hosts the Compose UI and manages the app lifecycle.
 * It uses Hilt for dependency injection and edge-to-edge display
 * for an immersive camera experience.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display for immersive camera experience
        enableEdgeToEdge()

        setContent {
            SpeedSignDetectorTheme(
                // Use dark theme for better contrast with camera
                darkTheme = true,
                dynamicColor = false
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DetectionScreen(cameraManager = cameraManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
    }
}
