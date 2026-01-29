package com.etraksolutions.speedsign.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.etraksolutions.speedsign.R
import com.etraksolutions.speedsign.ui.theme.SpeedSignDetectorTheme

/**
 * Screen shown when camera permission is required but not granted.
 *
 * Provides a clear explanation of why the permission is needed
 * and buttons to either grant or open settings.
 */
@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit,
    isPermanentlyDenied: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isPermanentlyDenied) {
                stringResource(R.string.camera_permission_denied)
            } else {
                stringResource(R.string.camera_permission_message)
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isPermanentlyDenied) {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            ) {
                Text(stringResource(R.string.open_settings))
            }
        } else {
            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.grant_permission))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionRequestScreenPreview() {
    SpeedSignDetectorTheme {
        PermissionRequestScreen(
            onRequestPermission = {},
            isPermanentlyDenied = false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionRequestScreenDeniedPreview() {
    SpeedSignDetectorTheme {
        PermissionRequestScreen(
            onRequestPermission = {},
            isPermanentlyDenied = true
        )
    }
}
