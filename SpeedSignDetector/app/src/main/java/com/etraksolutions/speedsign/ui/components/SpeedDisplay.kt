package com.etraksolutions.speedsign.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etraksolutions.speedsign.domain.model.DetectionState
import com.etraksolutions.speedsign.ui.theme.SpeedDisplayBackground
import com.etraksolutions.speedsign.ui.theme.SpeedSignDetectorTheme
import com.etraksolutions.speedsign.ui.theme.SpeedSignRed
import com.etraksolutions.speedsign.ui.theme.SpeedSignWhite
import com.etraksolutions.speedsign.ui.theme.Success

/**
 * Displays the detected speed limit in a sign-like format.
 *
 * This component mimics a Quebec speed limit sign appearance,
 * showing the detected speed prominently.
 */
@Composable
fun SpeedDisplay(
    detectionState: DetectionState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = detectionState is DetectionState.Detected,
            enter = scaleIn(tween(300)) + fadeIn(tween(300)),
            exit = scaleOut(tween(200)) + fadeOut(tween(200))
        ) {
            val state = detectionState as? DetectionState.Detected
            state?.let {
                SpeedSignCard(
                    speed = it.speedSign.speedLimit,
                    confidence = it.speedSign.confidence
                )
            }
        }

        AnimatedVisibility(
            visible = detectionState is DetectionState.Scanning,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            ScanningIndicator()
        }
    }
}

/**
 * Visual representation of a Quebec speed limit sign.
 */
@Composable
fun SpeedSignCard(
    speed: Int,
    confidence: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Speed sign appearance
        Box(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(SpeedSignWhite)
                .border(4.dp, SpeedSignRed, RoundedCornerShape(8.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MAXIMUM",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedContent(
                    targetState = speed,
                    transitionSpec = {
                        fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                    },
                    label = "speed_animation"
                ) { targetSpeed ->
                    Text(
                        text = targetSpeed.toString(),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = "km/h",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Confidence indicator
        ConfidenceIndicator(confidence = confidence)
    }
}

/**
 * Shows the confidence level of the detection.
 */
@Composable
fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val animatedConfidence by animateFloatAsState(
        targetValue = confidence,
        animationSpec = tween(300),
        label = "confidence_animation"
    )

    Row(
        modifier = modifier
            .background(
                SpeedDisplayBackground,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Confidence dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when {
                        confidence >= 0.8f -> Success
                        confidence >= 0.6f -> Color(0xFFFFC107)
                        else -> Color(0xFFFF5722)
                    }
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Confiance: ${(animatedConfidence * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

/**
 * Indicator shown when scanning for signs.
 */
@Composable
fun ScanningIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                SpeedDisplayBackground,
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulsingDot()
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Recherche de panneaux...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

/**
 * Animated pulsing dot for scanning indicator.
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "pulsing_dot"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(800),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha_animation"
    )

    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Success.copy(alpha = alpha))
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SpeedSignCardPreview() {
    SpeedSignDetectorTheme {
        SpeedSignCard(speed = 50, confidence = 0.85f)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ScanningIndicatorPreview() {
    SpeedSignDetectorTheme {
        ScanningIndicator()
    }
}
