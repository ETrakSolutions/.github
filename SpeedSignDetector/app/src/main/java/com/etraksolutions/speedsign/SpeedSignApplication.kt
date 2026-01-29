package com.etraksolutions.speedsign

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Speed Sign Detector.
 *
 * This class serves as the entry point for the Hilt dependency injection framework.
 * It initializes all application-wide dependencies and configurations.
 */
@HiltAndroidApp
class SpeedSignApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Application-level initialization can be added here
        // For example: Analytics, Crash reporting, etc.
    }
}
