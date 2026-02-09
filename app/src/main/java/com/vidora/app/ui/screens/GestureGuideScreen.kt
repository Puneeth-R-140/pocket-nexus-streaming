package com.vidora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureGuideScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gesture Guide") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Player Gestures",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            GestureItem(
                gesture = "Single Tap",
                description = "Show/Hide player controls"
            )
            
            GestureItem(
                gesture = "Double Tap (Left)",
                description = "Seek backward 10 seconds. Consecutive taps add 10s each"
            )
            
            GestureItem(
                gesture = "Double Tap (Right)",
                description = "Seek forward 10 seconds. Consecutive taps add 10s each"
            )
            
            GestureItem(
                gesture = "Long Press",
                description = "Hold to play at 2x speed. Release to return to normal. Shows \"2x\" indicator in top-right"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Other Controls",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            GestureItem(
                gesture = "Quality Button",
                description = "Change video quality (Auto, 1080p, 720p, 480p, 360p)"
            )
            
            GestureItem(
                gesture = "Subtitle Button",
                description = "Toggle subtitles on/off and select language"
            )
            
            GestureItem(
                gesture = "Speed Button",
                description = "Adjust playback speed (0.25x to 2x)"
            )
        }
    }
}

@Composable
private fun GestureItem(
    gesture: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = gesture,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
