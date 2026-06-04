package com.spoofer.ui.component

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MockLocationSetupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                "Setup Required",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                Text(
                    "To use location spoofing, you need to:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "1. Enable Developer Options",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "2. Go to Select mock location app",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "3. Choose this app",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                context.startActivity(intent)
            }) {
                Text("Open Developer Options")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        },
    )
}
