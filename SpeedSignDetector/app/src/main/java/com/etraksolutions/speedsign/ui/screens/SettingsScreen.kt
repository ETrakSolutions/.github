package com.etraksolutions.speedsign.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.etraksolutions.speedsign.domain.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onNavigateBack: () -> Unit,
    onUpdateDetectSpeedSigns: (Boolean) -> Unit,
    onUpdateDetectStopSigns: (Boolean) -> Unit,
    onUpdateDetectNumericText: (Boolean) -> Unit,
    onUpdateDetectAllSigns: (Boolean) -> Unit,
    onUpdateDetectText: (Boolean) -> Unit,
    onUpdateDetectVehicles: (Boolean) -> Unit = {},
    onUpdateShowDetectionBoxes: (Boolean) -> Unit,
    onUpdateCameraZoom: (Float) -> Unit,
    onUpdateProcessingInterval: (Long) -> Unit,
    onUpdateMinConfidence: (Float) -> Unit,
    onUpdateShowFps: (Boolean) -> Unit,
    onUpdateShowDebugInfo: (Boolean) -> Unit,
    onUpdateHapticFeedback: (Boolean) -> Unit,
    onUpdateSoundAlerts: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parametres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Detection Section
            SettingsSection(
                title = "Detection",
                icon = Icons.Default.CameraAlt
            ) {
                SettingsSwitch(
                    title = "Panneaux de vitesse",
                    subtitle = "30, 40, 50... 110 km/h",
                    checked = settings.detectSpeedSigns,
                    onCheckedChange = onUpdateDetectSpeedSigns,
                    iconColor = Color(0xFF4CAF50)
                )
                SettingsSwitch(
                    title = "Panneaux STOP",
                    subtitle = "Detection des arrets",
                    checked = settings.detectStopSigns,
                    onCheckedChange = onUpdateDetectStopSigns,
                    iconColor = Color(0xFFF44336)
                )
                SettingsSwitch(
                    title = "Texte numerique",
                    subtitle = "Tous les nombres visibles",
                    checked = settings.detectNumericText,
                    onCheckedChange = onUpdateDetectNumericText,
                    iconColor = Color(0xFF2196F3)
                )
                SettingsSwitch(
                    title = "Tous les panneaux",
                    subtitle = "Detection generale",
                    checked = settings.detectAllSigns,
                    onCheckedChange = onUpdateDetectAllSigns,
                    iconColor = Color(0xFFFF9800)
                )
                SettingsSwitch(
                    title = "Vehicules",
                    subtitle = "Voitures, camions, bus",
                    checked = settings.detectVehicles,
                    onCheckedChange = onUpdateDetectVehicles,
                    iconColor = Color(0xFF9C27B0)
                )
                SettingsSwitch(
                    title = "Tout le texte",
                    subtitle = "OCR complet (plus lent)",
                    checked = settings.detectText,
                    onCheckedChange = onUpdateDetectText,
                    iconColor = Color(0xFF9C27B0)
                )
            }

            // Visual Section
            SettingsSection(
                title = "Affichage",
                icon = Icons.Default.Visibility
            ) {
                SettingsSwitch(
                    title = "Rectangles de detection",
                    subtitle = "Afficher les zones detectees",
                    checked = settings.showDetectionBoxes,
                    onCheckedChange = onUpdateShowDetectionBoxes
                )
                SettingsSwitch(
                    title = "Afficher FPS",
                    subtitle = "Images par seconde",
                    checked = settings.showFps,
                    onCheckedChange = onUpdateShowFps
                )
                SettingsSwitch(
                    title = "Mode debug",
                    subtitle = "Informations techniques",
                    checked = settings.showDebugInfo,
                    onCheckedChange = onUpdateShowDebugInfo
                )
            }

            // Camera Section
            SettingsSection(
                title = "Camera",
                icon = Icons.Default.PhotoCamera
            ) {
                SettingsSlider(
                    title = "Zoom",
                    value = settings.cameraZoom,
                    valueRange = settings.cameraZoomMin..settings.cameraZoomMax,
                    onValueChange = onUpdateCameraZoom,
                    valueLabel = { "%.1fx".format(it) }
                )
            }

            // Performance Section
            SettingsSection(
                title = "Performance",
                icon = Icons.Default.Speed
            ) {
                SettingsSlider(
                    title = "Intervalle de traitement",
                    value = settings.processingIntervalMs.toFloat(),
                    valueRange = settings.minProcessingInterval.toFloat()..settings.maxProcessingInterval.toFloat(),
                    onValueChange = { onUpdateProcessingInterval(it.toLong()) },
                    valueLabel = { "${it.toLong()} ms" },
                    subtitle = "Plus bas = plus rapide mais plus de batterie"
                )
                SettingsSlider(
                    title = "Confiance minimum",
                    value = settings.minConfidence,
                    valueRange = 0.3f..0.95f,
                    onValueChange = onUpdateMinConfidence,
                    valueLabel = { "${(it * 100).toInt()}%" },
                    subtitle = "Seuil de detection"
                )
            }

            // Feedback Section
            SettingsSection(
                title = "Retour",
                icon = Icons.Default.Notifications
            ) {
                SettingsSwitch(
                    title = "Vibration",
                    subtitle = "Retour haptique",
                    checked = settings.hapticFeedback,
                    onCheckedChange = onUpdateHapticFeedback
                )
                SettingsSwitch(
                    title = "Sons",
                    subtitle = "Alertes sonores",
                    checked = settings.soundAlerts,
                    onCheckedChange = onUpdateSoundAlerts
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconColor != null) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(iconColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueLabel: (Float) -> String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                valueLabel(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
