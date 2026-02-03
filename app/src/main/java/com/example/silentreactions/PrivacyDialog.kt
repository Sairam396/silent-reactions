package com.example.silentreactions

import android.app.AlertDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PrivacyDialog(
    onAccept: () -> Unit,
    onLearnMore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Camera & Privacy", fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                "Silent Reactions uses the camera only to recognize hand gestures.\n\n" +
                        "No images or videos are stored or uploaded. Processing happens on your device."
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onLearnMore) { Text("About") }
        }
    )
}