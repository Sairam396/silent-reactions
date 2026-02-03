package com.example.silentreactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Silent Reactions", style = MaterialTheme.typography.headlineSmall)

            Text(
                "Hands-free reactions for office meetings.",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider()

            Text("Camera & Privacy", style = MaterialTheme.typography.titleMedium)
            Text(
                "The camera is used to recognize hand gestures.\n\n" +
                        "No images or videos are stored or uploaded. All processing happens on your device",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider()

            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            Text(
                "Camera: required to detect hand gestures in real time.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}