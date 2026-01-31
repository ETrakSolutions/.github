package com.etraksolutions.speedsign.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Information screen explaining the technology used in the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen() {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Technologies",
                        fontWeight = FontWeight.Bold
                    )
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header with animation
            AnimatedAppHeader()

            // Technology Stack
            TechCard(
                icon = Icons.Default.Code,
                title = "Jetpack Compose",
                subtitle = "UI Moderne",
                description = "Framework declaratif de Google pour construire des interfaces utilisateur natives Android. " +
                        "Simplifie le developpement UI avec un code Kotlin reactif et moderne.",
                color = Color(0xFF4285F4),
                features = listOf(
                    "Composition declarative",
                    "State management reactif",
                    "Animations fluides",
                    "Material Design 3"
                )
            )

            TechCard(
                icon = Icons.Default.CameraAlt,
                title = "CameraX",
                subtitle = "Capture Video",
                description = "Bibliotheque Jetpack pour l'integration camera. Gere automatiquement le cycle de vie, " +
                        "la rotation, et fournit une API simplifiee pour l'analyse d'images en temps reel.",
                color = Color(0xFF34A853),
                features = listOf(
                    "Lifecycle-aware",
                    "Analyse frame par frame",
                    "Gestion automatique rotation",
                    "Support zoom optique"
                )
            )

            TechCard(
                icon = Icons.Default.Psychology,
                title = "ML Kit",
                subtitle = "Intelligence Artificielle",
                description = "SDK de Google pour le machine learning on-device. Utilise des modeles optimises " +
                        "pour detecter et reconnaitre le texte dans les images en temps reel.",
                color = Color(0xFFEA4335),
                features = listOf(
                    "OCR (reconnaissance texte)",
                    "Detection temps reel",
                    "Traitement on-device",
                    "Bounding boxes precis"
                )
            )

            TechCard(
                icon = Icons.Default.Architecture,
                title = "Clean Architecture",
                subtitle = "Structure du Code",
                description = "Architecture en couches separant UI, logique metier et donnees. " +
                        "Facilite les tests, la maintenance et l'evolution de l'application.",
                color = Color(0xFF9C27B0),
                features = listOf(
                    "Separation des concerns",
                    "Inversion des dependances",
                    "Testabilite maximale",
                    "MVVM pattern"
                )
            )

            TechCard(
                icon = Icons.Default.Hub,
                title = "Hilt",
                subtitle = "Injection de Dependances",
                description = "Framework d'injection de dependances base sur Dagger. " +
                        "Simplifie la gestion des dependances et ameliore la testabilite.",
                color = Color(0xFFFF9800),
                features = listOf(
                    "Injection automatique",
                    "Scopes Android",
                    "ViewModel integration",
                    "Compile-time safety"
                )
            )

            TechCard(
                icon = Icons.Default.Stream,
                title = "Kotlin Coroutines & Flow",
                subtitle = "Programmation Asynchrone",
                description = "Gestion elegante des operations asynchrones avec les coroutines. " +
                        "Flow permet le streaming reactif des donnees camera et detections.",
                color = Color(0xFF00BCD4),
                features = listOf(
                    "Non-blocking I/O",
                    "Structured concurrency",
                    "StateFlow pour l'UI",
                    "Cold & Hot streams"
                )
            )

            // Architecture Diagram
            ArchitectureDiagram()

            // Detection Pipeline
            DetectionPipelineCard()

            // Footer
            AppFooter()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AnimatedAppHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "header")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Speed Sign Detector",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Detection de panneaux de vitesse du Quebec",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TechBadge("Kotlin")
                TechBadge("Android")
                TechBadge("ML Kit")
            }
        }
    }
}

@Composable
fun TechBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
    color: Color,
    features: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    features.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                feature,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchitectureDiagram() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Architecture de l'Application",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Architecture layers
            ArchitectureLayer(
                title = "UI Layer",
                items = listOf("Compose Screens", "ViewModels", "UI State"),
                color = Color(0xFF4285F4)
            )

            ArchitectureArrow()

            ArchitectureLayer(
                title = "Domain Layer",
                items = listOf("Use Cases", "Models", "Repository Interfaces"),
                color = Color(0xFF34A853)
            )

            ArchitectureArrow()

            ArchitectureLayer(
                title = "Data Layer",
                items = listOf("Repositories", "Camera Manager", "ML Kit Detector"),
                color = Color(0xFFEA4335)
            )
        }
    }
}

@Composable
fun ArchitectureLayer(
    title: String,
    items: List<String>,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                items.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ArchitectureArrow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DetectionPipelineCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Pipeline de Detection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            PipelineStep(1, "Capture", "CameraX capture les frames video", Icons.Default.CameraAlt)
            PipelineStep(2, "Pretraitement", "Conversion et rotation de l'image", Icons.Default.Transform)
            PipelineStep(3, "OCR", "ML Kit detecte le texte dans l'image", Icons.Default.TextFields)
            PipelineStep(4, "Filtrage", "Extraction des vitesses valides (30-110)", Icons.Default.FilterAlt)
            PipelineStep(5, "Affichage", "Mise a jour de l'UI avec le resultat", Icons.Default.Visibility)
        }
    }
}

@Composable
fun PipelineStep(
    number: Int,
    title: String,
    description: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun AppFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "ETrak Solutions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Version 1.0.0-beta",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Concu pour Android 9+ (API 28+)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
