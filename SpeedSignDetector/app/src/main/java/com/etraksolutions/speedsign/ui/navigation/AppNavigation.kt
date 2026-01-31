package com.etraksolutions.speedsign.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.etraksolutions.speedsign.data.camera.CameraManager
import com.etraksolutions.speedsign.domain.model.AppSettings
import com.etraksolutions.speedsign.ui.screens.*

/**
 * Navigation destinations for the app.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Detection : Screen("detection", "Détection", Icons.Default.CameraAlt)
    object Settings : Screen("settings", "Paramètres", Icons.Default.Settings)
    object Info : Screen("info", "Infos", Icons.Default.Info)
}

/**
 * Main navigation host with bottom navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    cameraManager: CameraManager,
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit
) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Detection, Screen.Settings, Screen.Info)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = androidx.compose.ui.unit.dp.times(8)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Detection.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { 100 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { -100 },
                    animationSpec = tween(300)
                )
            }
        ) {
            composable(Screen.Detection.route) {
                EnhancedDetectionScreen(
                    cameraManager = cameraManager,
                    settings = settings
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onNavigateBack = { navController.navigateUp() },
                    onUpdateDetectSpeedSigns = {
                        onUpdateSettings(settings.copy(detectSpeedSigns = it))
                    },
                    onUpdateDetectStopSigns = {
                        onUpdateSettings(settings.copy(detectStopSigns = it))
                    },
                    onUpdateDetectNumericText = {
                        onUpdateSettings(settings.copy(detectNumericText = it))
                    },
                    onUpdateDetectAllSigns = {
                        onUpdateSettings(settings.copy(detectAllSigns = it))
                    },
                    onUpdateDetectText = {
                        onUpdateSettings(settings.copy(detectText = it))
                    },
                    onUpdateShowDetectionBoxes = {
                        onUpdateSettings(settings.copy(showDetectionBoxes = it))
                    },
                    onUpdateCameraZoom = {
                        onUpdateSettings(settings.copy(cameraZoom = it))
                    },
                    onUpdateProcessingInterval = {
                        onUpdateSettings(settings.copy(processingIntervalMs = it))
                    },
                    onUpdateMinConfidence = {
                        onUpdateSettings(settings.copy(minConfidence = it))
                    },
                    onUpdateShowFps = {
                        onUpdateSettings(settings.copy(showFps = it))
                    },
                    onUpdateShowDebugInfo = {
                        onUpdateSettings(settings.copy(showDebugInfo = it))
                    },
                    onUpdateHapticFeedback = {
                        onUpdateSettings(settings.copy(hapticFeedback = it))
                    },
                    onUpdateSoundAlerts = {
                        onUpdateSettings(settings.copy(soundAlerts = it))
                    }
                )
            }

            composable(Screen.Info.route) {
                InfoScreen()
            }
        }
    }
}
