package com.etraksolutions.speedsign.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.etraksolutions.speedsign.data.camera.CameraManager
import com.etraksolutions.speedsign.ui.navigation.AppNavigation
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
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsState()

            SpeedSignDetectorTheme(
                // Use dark theme for better contrast with camera
                darkTheme = true,
                dynamicColor = false
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        cameraManager = cameraManager,
                        settings = settings,
                        onUpdateSettings = { viewModel.updateSettings(it) }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
    }
}
